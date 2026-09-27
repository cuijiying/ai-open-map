import { useEffect, useRef } from 'react'
import maplibregl, { FilterSpecification, GeoJSONSource, Map } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { LAYERS, LayerStyle } from '../layers'
import { MarkerPoint } from '../types'

interface Props {
  visible: Record<string, boolean>
  filters: Record<string, string[] | undefined>
  markers: MarkerPoint[]
  flyToken: number
  center: [number, number]
  zoom: number
  onBounds: (bbox: [number, number, number, number]) => void
}

export default function MapView({ visible, filters, markers, flyToken, center, zoom, onBounds }: Props) {
  const container = useRef<HTMLDivElement>(null)
  const mapRef = useRef<Map | null>(null)
  const ready = useRef(false)
  const visibleRef = useRef(visible)
  const filtersRef = useRef(filters)
  const onBoundsRef = useRef(onBounds)
  visibleRef.current = visible
  filtersRef.current = filters
  onBoundsRef.current = onBounds

  useEffect(() => {
    if (!container.current || mapRef.current) {
      return
    }
    const map = new maplibregl.Map({
      container: container.current,
      center: [117.28, 31.86],
      zoom: 6.4,
      localIdeographFontFamily: "'Microsoft YaHei', 'PingFang SC', sans-serif",
      style: {
        version: 8,
        glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
        sources: {
          basemap: {
            type: 'raster',
            tiles: ['https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png'],
            tileSize: 256,
            attribution: '© OpenStreetMap © CARTO'
          }
        },
        layers: [{ id: 'basemap', type: 'raster', source: 'basemap' }]
      }
    })
    map.addControl(new maplibregl.NavigationControl(), 'top-right')
    map.addControl(new maplibregl.ScaleControl({ unit: 'metric' }), 'bottom-left')
    map.on('load', () => {
      for (const layer of LAYERS) {
        map.addSource(layer.id, {
          type: 'vector',
          tiles: [`${window.location.origin}/api/tiles/${layer.id}/{z}/{x}/{y}.pbf`],
          minzoom: layer.minzoom,
          maxzoom: 16
        })
        map.addLayer(paintLayer(layer))
        if (layer.id === 'place') {
          map.addLayer({
            id: 'place-label',
            type: 'symbol',
            source: 'place',
            'source-layer': 'place',
            minzoom: 5,
            layout: {
              'text-field': ['get', 'name'],
              'text-font': ['Open Sans Regular'],
              'text-size': 13,
              'text-offset': [0, 0.8],
              'text-anchor': 'top'
            },
            paint: {
              'text-color': '#1f2937',
              'text-halo-color': '#ffffff',
              'text-halo-width': 1.2
            }
          })
        }
      }
      map.addSource('markers', { type: 'geojson', data: emptyCollection() })
      map.addLayer({
        id: 'markers',
        type: 'circle',
        source: 'markers',
        paint: {
          'circle-radius': 7,
          'circle-color': '#111827',
          'circle-stroke-width': 2,
          'circle-stroke-color': '#ffffff'
        }
      })
      ready.current = true
      applyVisibility(map, visibleRef.current, filtersRef.current)
    })
    map.on('moveend', () => {
      const box = map.getBounds()
      onBoundsRef.current([box.getWest(), box.getSouth(), box.getEast(), box.getNorth()])
    })
    map.on('click', (event) => {
      const features = map.queryRenderedFeatures(event.point).filter((feature) => feature.source !== 'basemap')
      if (features.length === 0) {
        return
      }
      const feature = features[0]
      const props = feature.properties ?? {}
      const name = props.name || '未命名'
      const subtype = props.subtype || ''
      new maplibregl.Popup({ closeButton: true, maxWidth: '280px' })
        .setLngLat(event.lngLat)
        .setHTML(`<strong>${escapeHtml(String(name))}</strong><br/>${escapeHtml(String(feature.sourceLayer || feature.source))} ${escapeHtml(String(subtype))}`)
        .addTo(map)
    })
    mapRef.current = map
    return () => {
      map.remove()
      mapRef.current = null
      ready.current = false
    }
  }, [])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready.current) {
      return
    }
    applyVisibility(map, visible, filters)
    const label = map.getLayer('place-label')
    if (label) {
      map.setLayoutProperty('place-label', 'visibility', visible.place ? 'visible' : 'none')
    }
  }, [visible, filters])

  useEffect(() => {
    const map = mapRef.current
    if (!map) {
      return
    }
    map.flyTo({ center, zoom, essential: true })
  }, [flyToken])

  useEffect(() => {
    const map = mapRef.current
    const source = map?.getSource('markers')
    if (!(source instanceof GeoJSONSource)) {
      return
    }
    source.setData({
      type: 'FeatureCollection',
      features: markers.map((point) => ({
        type: 'Feature',
        properties: { name: point.name ?? '' },
        geometry: { type: 'Point', coordinates: [point.lon, point.lat] }
      }))
    })
  }, [markers])

  return <div ref={container} className="map-root" />
}

function paintLayer(layer: LayerStyle): maplibregl.LayerSpecification {
  const layout = { visibility: 'visible' as const }
  if (layer.kind === 'fill') {
    return {
      id: layer.id,
      type: 'fill',
      source: layer.id,
      'source-layer': layer.id,
      minzoom: layer.minzoom,
      layout,
      paint: {
        'fill-color': layer.color,
        'fill-opacity': layer.id === 'building' ? 0.85 : 0.55,
        'fill-outline-color': '#ffffff'
      }
    }
  }
  if (layer.kind === 'circle') {
    return {
      id: layer.id,
      type: 'circle',
      source: layer.id,
      'source-layer': layer.id,
      minzoom: layer.minzoom,
      layout,
      paint: {
        'circle-radius': layer.id === 'place' ? 4 : 5,
        'circle-color': layer.color,
        'circle-stroke-width': 1,
        'circle-stroke-color': '#ffffff'
      }
    }
  }
  return {
    id: layer.id,
    type: 'line',
    source: layer.id,
    'source-layer': layer.id,
    minzoom: layer.minzoom,
    layout: { ...layout, 'line-cap': 'round', 'line-join': 'round' },
    paint: {
      'line-color': layer.id === 'highway'
        ? ['match', ['get', 'subtype'], 'motorway', '#c0392b', 'motorway_link', '#c0392b', 'trunk', '#e67e22', 'trunk_link', '#e67e22', 'primary', '#d4a017', 'primary_link', '#d4a017', '#f0b429']
        : layer.color,
      'line-width': ['interpolate', ['linear'], ['zoom'], 5, 0.6, 10, 1.6, 14, 3.2],
      ...(layer.id === 'boundary' ? { 'line-dasharray': [2, 1.5] as [number, number] } : {})
    }
  }
}

function applyVisibility(map: Map, visible: Record<string, boolean>, filters: Record<string, string[] | undefined>) {
  for (const layer of LAYERS) {
    if (!map.getLayer(layer.id)) {
      continue
    }
    map.setLayoutProperty(layer.id, 'visibility', visible[layer.id] ? 'visible' : 'none')
    const subtypes = filters[layer.id]
    const filter: FilterSpecification | null = subtypes && subtypes.length > 0
      ? ['in', ['get', 'subtype'], ['literal', subtypes]]
      : null
    map.setFilter(layer.id, filter)
  }
}

function emptyCollection() {
  return { type: 'FeatureCollection' as const, features: [] as GeoJSON.Feature[] }
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char] ?? char))
}
