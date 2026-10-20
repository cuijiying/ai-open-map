import maplibregl, { ExpressionSpecification, FilterSpecification, LayerSpecification, Map } from 'maplibre-gl'
import { LAYERS } from './layers'
import { maxzoomOf, ResolvedStyle, StyleTokens, tilesOf } from './mapStyle'

type RoadClass = 'motorway' | 'motorwayLink' | 'trunk' | 'trunkLink' | 'primary' | 'primaryLink' | 'secondary' | 'tertiary' | 'minor' | 'service' | 'path' | 'track'

const ROAD_ORDER: RoadClass[] = ['motorway', 'motorwayLink', 'trunk', 'trunkLink', 'primary', 'primaryLink', 'secondary', 'tertiary', 'minor', 'service', 'path', 'track']

const ROAD_SUBTYPES: Record<RoadClass, string[]> = {
  motorway: ['motorway'],
  motorwayLink: ['motorway_link'],
  trunk: ['trunk'],
  trunkLink: ['trunk_link'],
  primary: ['primary'],
  primaryLink: ['primary_link'],
  secondary: ['secondary', 'secondary_link'],
  tertiary: ['tertiary', 'tertiary_link'],
  minor: ['residential', 'unclassified', 'living_street', 'road'],
  service: ['service'],
  path: ['footway', 'path', 'cycleway', 'pedestrian', 'steps', 'bridleway'],
  track: ['track']
}

const WIDTH_STOPS: [number, Record<RoadClass, number>][] = [
  [4, { motorway: 1.15, motorwayLink: 0.8, trunk: 0.95, trunkLink: 0.65, primary: 0.6, primaryLink: 0.45, secondary: 0.28, tertiary: 0.18, minor: 0.12, service: 0.1, path: 0.08, track: 0.1 }],
  [8, { motorway: 2.05, motorwayLink: 1.35, trunk: 1.75, trunkLink: 1.15, primary: 1.35, primaryLink: 0.95, secondary: 0.85, tertiary: 0.48, minor: 0.28, service: 0.14, path: 0.12, track: 0.2 }],
  [11, { motorway: 3.5, motorwayLink: 2.2, trunk: 3.05, trunkLink: 1.9, primary: 2.45, primaryLink: 1.65, secondary: 1.75, tertiary: 1.25, minor: 0.9, service: 0.48, path: 0.36, track: 0.58 }],
  [14, { motorway: 6.4, motorwayLink: 3.9, trunk: 5.5, trunkLink: 3.3, primary: 4.7, primaryLink: 3.1, secondary: 3.5, tertiary: 2.7, minor: 2.15, service: 1.25, path: 1, track: 1.4 }],
  [16, { motorway: 9.2, motorwayLink: 5.5, trunk: 7.8, trunkLink: 4.5, primary: 6.6, primaryLink: 4.1, secondary: 5.1, tertiary: 4.1, minor: 3.4, service: 1.9, path: 1.5, track: 2.2 }]
]

const CASING_PAD = [0.85, 1.15, 1.65, 2.25, 2.9]

const THIN_ROADS = new Set<RoadClass>(['service', 'path', 'track'])

const basemapCache = new WeakMap<Map, string>()

export function rememberBasemap(map: Map, basemap: string) {
  basemapCache.set(map, basemap)
}

export function overlayLayers(style: ResolvedStyle): LayerSpecification[] {
  const token = style.tokens
  const scale = style.widthScale
  return [
    fill('landuse', landuseColor(token), style.numbers.landuseOpacity, 10),
    fill('water', token.waterFill, style.numbers.waterOpacity, 6, token.waterLine),
    fill('building', token.buildingFill, style.numbers.buildingOpacity, 13, token.buildingOutline),
    line('waterway', token.waterLine, waterwayWidth(scale), 7),
    line('railway-casing', token.railwayCasing, zoomWidth(scale, [[8, 2.1], [12, 3.3], [15, 4.6]]), 8, 'railway'),
    line('railway', token.railway, zoomWidth(scale, [[8, 1.15], [12, 1.7], [15, 2.5]]), 8, 'railway', { dash: [2.2, 1.8], cap: 'butt' }),
    line('boundary', token.boundary, boundaryWidth(scale), 4, 'boundary', { dash: [2, 1.3], cap: 'butt', opacity: 0.9 }),
    line('highway-casing', token.highwayCasing, highwayWidth(scale, true), 4, 'highway'),
    line('highway', highwayColor(token), highwayWidth(scale, false), 4, 'highway'),
    lineLabel('waterway-label', 'waterway', style, 11),
    lineLabel('highway-label', 'highway', style, 12),
    poi(style),
    placeCircle(style),
    placeLabel(style)
  ]
}

export function syncOverlayStyle(map: Map, style: ResolvedStyle) {
  if (map.getLayer('background')) {
    map.setPaintProperty('background', 'background-color', style.tokens.background)
  }
  syncBasemap(map, style)
  for (const spec of overlayLayers(style)) {
    if (!map.getLayer(spec.id) || !('paint' in spec) || !spec.paint) {
      continue
    }
    for (const [key, value] of Object.entries(spec.paint)) {
      map.setPaintProperty(spec.id, key as never, value as never)
    }
  }
  if (map.getLayer('markers')) {
    map.setPaintProperty('markers', 'circle-color', style.tokens.marker)
    map.setPaintProperty('markers', 'circle-stroke-color', style.tokens.markerStroke)
  }
}

export function applyLayerState(map: Map, visible: Record<string, boolean>, filters: Record<string, string[] | undefined>, labels: boolean) {
  for (const layer of LAYERS) {
    const show = !!visible[layer.id]
    for (const part of partsOf(layer.id)) {
      if (!map.getLayer(part.id)) {
        continue
      }
      map.setLayoutProperty(part.id, 'visibility', show && (!part.label || labels) ? 'visible' : 'none')
      map.setFilter(part.id, layerFilter(layer.id, filters[layer.id], part.label))
    }
  }
}

export function interactiveLayerIds() {
  return LAYERS.map((layer) => layer.id)
}

function syncBasemap(map: Map, style: ResolvedStyle) {
  if (!map.getLayer('basemap')) {
    return
  }
  const source = map.getSource('basemap')
  if (style.basemap === 'none') {
    map.setLayoutProperty('basemap', 'visibility', 'none')
    basemapCache.set(map, 'none')
    return
  }
  map.setLayoutProperty('basemap', 'visibility', 'visible')
  if (basemapCache.get(map) !== style.basemap && source instanceof maplibregl.RasterTileSource) {
    source.maxzoom = maxzoomOf(style.basemap)
    source.setTiles(tilesOf(style.basemap))
    basemapCache.set(map, style.basemap)
  }
}

function partsOf(layerId: string) {
  switch (layerId) {
    case 'highway':
      return [
        { id: 'highway-casing', label: false },
        { id: 'highway', label: false },
        { id: 'highway-label', label: true }
      ]
    case 'railway':
      return [
        { id: 'railway-casing', label: false },
        { id: 'railway', label: false }
      ]
    case 'waterway':
      return [
        { id: 'waterway', label: false },
        { id: 'waterway-label', label: true }
      ]
    case 'place':
      return [
        { id: 'place', label: false },
        { id: 'place-label', label: true }
      ]
    default:
      return [{ id: layerId, label: false }]
  }
}

function layerFilter(layerId: string, subtypes: string[] | undefined, label: boolean): FilterSpecification | null {
  const parts: FilterSpecification[] = []
  if (label) {
    parts.push(['!=', ['get', 'name'], ''])
    if (layerId === 'waterway') {
      parts.push(['in', ['get', 'subtype'], ['literal', ['river', 'canal']]])
    }
    if (layerId === 'highway') {
      parts.push(['!', ['in', ['get', 'subtype'], ['literal', ['footway', 'path', 'steps', 'cycleway', 'bridleway', 'track']]]])
    }
  }
  if (subtypes && subtypes.length > 0) {
    parts.push(['in', ['get', 'subtype'], ['literal', subtypes]])
  }
  if (parts.length === 0) {
    return null
  }
  if (parts.length === 1) {
    return parts[0]
  }
  return ['all', ...parts] as FilterSpecification
}

function fill(id: string, color: string | ExpressionSpecification, opacity: number, minzoom: number, outline?: string | ExpressionSpecification): LayerSpecification {
  return {
    id,
    type: 'fill',
    source: id,
    'source-layer': id,
    minzoom,
    paint: {
      'fill-color': color,
      'fill-opacity': opacity,
      'fill-outline-color': outline ?? color
    }
  }
}

function line(
  id: string,
  color: string | ExpressionSpecification,
  width: ExpressionSpecification,
  minzoom: number,
  source = id,
  extra?: { dash?: [number, number]; cap?: 'round' | 'butt'; opacity?: number }
): LayerSpecification {
  return {
    id,
    type: 'line',
    source,
    'source-layer': source,
    minzoom,
    layout: {
      'line-cap': extra?.cap ?? 'round',
      'line-join': extra?.cap === 'butt' ? 'miter' : 'round'
    },
    paint: {
      'line-color': color,
      'line-width': width,
      'line-opacity': extra?.opacity ?? 1,
      ...(extra?.dash ? { 'line-dasharray': extra.dash } : {})
    }
  }
}

function lineLabel(id: string, source: string, style: ResolvedStyle, minzoom: number): LayerSpecification {
  return {
    id,
    type: 'symbol',
    source,
    'source-layer': source,
    minzoom,
    layout: {
      'symbol-placement': 'line',
      'text-field': ['get', 'name'],
      'text-font': ['Open Sans Regular'],
      'text-size': ['interpolate', ['linear'], ['zoom'], minzoom, 11, 16, 13],
      'text-max-angle': 28,
      'symbol-spacing': source === 'highway' ? 320 : 260,
      'text-padding': 2,
      'text-rotation-alignment': 'map'
    },
    paint: textPaint(style)
  } as LayerSpecification
}

function poi(style: ResolvedStyle): LayerSpecification {
  const token = style.tokens
  return {
    id: 'poi',
    type: 'circle',
    source: 'poi',
    'source-layer': 'poi',
    minzoom: 13,
    paint: {
      'circle-radius': ['interpolate', ['linear'], ['zoom'], 13, 2.4, 16, 5],
      'circle-color': ['case',
        ['==', ['slice', ['coalesce', ['get', 'subtype'], ''], 0, 8], 'amenity='], token.poiAmenity,
        ['==', ['slice', ['coalesce', ['get', 'subtype'], ''], 0, 11], 'healthcare='], token.poiAmenity,
        ['==', ['slice', ['coalesce', ['get', 'subtype'], ''], 0, 5], 'shop='], token.poiShop,
        ['==', ['slice', ['coalesce', ['get', 'subtype'], ''], 0, 8], 'tourism='], token.poiTourism,
        ['==', ['slice', ['coalesce', ['get', 'subtype'], ''], 0, 9], 'historic='], token.poiHistoric,
        token.poiOther
      ],
      'circle-stroke-width': 1,
      'circle-stroke-color': token.labelHalo,
      'circle-opacity': 0.95
    }
  }
}

function placeCircle(style: ResolvedStyle): LayerSpecification {
  return {
    id: 'place',
    type: 'circle',
    source: 'place',
    'source-layer': 'place',
    minzoom: 4,
    paint: {
      'circle-radius': rankSize([[4, 3.2, 2.4, 1.5], [8, 4.2, 3.2, 2], [12, 4.6, 3.6, 2.4]]),
      'circle-color': style.tokens.place,
      'circle-stroke-width': 1.25,
      'circle-stroke-color': style.tokens.labelHalo,
      'circle-opacity': 0.92
    }
  }
}

function placeLabel(style: ResolvedStyle): LayerSpecification {
  return {
    id: 'place-label',
    type: 'symbol',
    source: 'place',
    'source-layer': 'place',
    minzoom: 4,
    layout: {
      'text-field': ['get', 'name'],
      'text-font': ['Open Sans Regular'],
      'text-size': rankSize([[5, 13, 11, 10], [9, 16, 13, 11], [13, 18, 15, 12]]),
      'text-offset': [0, 0.55],
      'text-anchor': 'top',
      'text-padding': 4,
      'text-optional': true
    },
    paint: textPaint(style)
  } as LayerSpecification
}

function textPaint(style: ResolvedStyle) {
  return {
    'text-color': style.tokens.label,
    'text-halo-color': style.tokens.labelHalo,
    'text-halo-width': style.numbers.labelHaloWidth
  }
}

function highwayColor(token: StyleTokens): ExpressionSpecification {
  return roadMatch({
    motorway: token.highwayMotorway,
    motorwayLink: token.highwayMotorway,
    trunk: token.highwayTrunk,
    trunkLink: token.highwayTrunk,
    primary: token.highwayPrimary,
    primaryLink: token.highwayPrimary,
    secondary: token.highwaySecondary,
    tertiary: token.highwayTertiary,
    minor: token.highwayMinor,
    service: token.highwayService,
    path: token.highwayPath,
    track: token.highwayPath
  }, token.highwayMinor)
}

function highwayWidth(scale: number, casing: boolean): ExpressionSpecification {
  const expr: unknown[] = ['interpolate', ['linear'], ['zoom']]
  WIDTH_STOPS.forEach((stop, index) => {
    const [zoom, values] = stop
    const pad = casing ? CASING_PAD[index] : 0
    const sized = {} as Record<RoadClass, number>
    for (const kind of ROAD_ORDER) {
      const extra = THIN_ROADS.has(kind) ? pad * 0.4 : pad
      sized[kind] = (values[kind] + extra) * scale
    }
    expr.push(zoom, roadMatch(sized, sized.minor))
  })
  return expr as ExpressionSpecification
}

function waterwayWidth(scale: number): ExpressionSpecification {
  return ['interpolate', ['linear'], ['zoom'],
    7, ['*', scale, ['match', ['get', 'subtype'], 'river', 0.85, 'canal', 0.75, 'flowline', 0.5, 0.3]],
    11, ['*', scale, ['match', ['get', 'subtype'], 'river', 1.9, 'canal', 1.55, 'stream', 0.7, 0.85]],
    14, ['*', scale, ['match', ['get', 'subtype'], 'river', 3.5, 'canal', 2.7, 'stream', 1.35, 'ditch', 0.8, 1.25]],
    16, ['*', scale, ['match', ['get', 'subtype'], 'river', 5.4, 'canal', 3.9, 'stream', 2.1, 1.7]]
  ] as ExpressionSpecification
}

function boundaryWidth(scale: number): ExpressionSpecification {
  const rank = (province: number, city: number, county: number) => ['match', ['get', 'subtype'],
    '2', province + 0.3, '3', province, '4', province, '5', city + 0.2, '6', city, '7', county, '8', county, county * 0.8] as ExpressionSpecification
  return ['interpolate', ['linear'], ['zoom'],
    4, ['*', scale, rank(1.5, 0.9, 0.55)],
    8, ['*', scale, rank(2.3, 1.45, 0.9)],
    12, ['*', scale, rank(3.3, 2.1, 1.3)]
  ] as ExpressionSpecification
}

function zoomWidth(scale: number, stops: [number, number][]): ExpressionSpecification {
  const expr: unknown[] = ['interpolate', ['linear'], ['zoom']]
  for (const [zoom, width] of stops) {
    expr.push(zoom, width * scale)
  }
  return expr as ExpressionSpecification
}

function landuseColor(token: StyleTokens): ExpressionSpecification {
  return ['match', ['get', 'subtype'],
    'residential', token.landuseResidential,
    'commercial', token.landuseCommercial,
    'retail', token.landuseCommercial,
    'industrial', token.landuseIndustrial,
    'quarry', token.landuseIndustrial,
    'military', token.landuseIndustrial,
    'construction', token.landuseIndustrial,
    'brownfield', token.landuseIndustrial,
    'forest', token.landuseGreen,
    'wood', token.landuseGreen,
    'grass', token.landuseGreen,
    'meadow', token.landuseGreen,
    'village_green', token.landuseGreen,
    'recreation_ground', token.landuseGreen,
    'scrub', token.landuseGreen,
    'farmland', token.landuseFarm,
    'farmyard', token.landuseFarm,
    'orchard', token.landuseFarm,
    'vineyard', token.landuseFarm,
    'allotments', token.landuseFarm,
    'park', token.landusePark,
    'garden', token.landusePark,
    'pitch', token.landusePark,
    'playground', token.landusePark,
    token.landuseOther
  ]
}

function roadMatch(values: Record<RoadClass, string | number>, fallback: string | number): ExpressionSpecification {
  const expr: unknown[] = ['match', ['get', 'subtype']]
  for (const kind of ROAD_ORDER) {
    for (const subtype of ROAD_SUBTYPES[kind]) {
      expr.push(subtype, values[kind])
    }
  }
  expr.push(fallback)
  return expr as ExpressionSpecification
}

function rankSize(stops: [number, number, number, number][]): ExpressionSpecification {
  const expr: unknown[] = ['interpolate', ['linear'], ['zoom']]
  for (const [zoom, city, town, rest] of stops) {
    expr.push(zoom, ['match', ['get', 'subtype'], 'city', city, 'town', town, 'county', town, 'province', city, 'state', city, rest])
  }
  return expr as ExpressionSpecification
}
