import { useEffect, useRef } from 'react'
import maplibregl, { GeoJSONSource, Map, VectorTileSource } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { LAYERS } from '../layers'
import { applyLayerState, interactiveLayerIds, overlayLayers, rememberBasemap, syncOverlayStyle } from '../mapPaint'
import { attributionOf, featureCaption, maxzoomOf, ResolvedStyle, tilesOf } from '../mapStyle'
import { MarkerPoint } from '../types'

interface Props {
  visible: Record<string, boolean>
  filters: Record<string, string[] | undefined>
  markers: MarkerPoint[]
  regions: string[]
  flyToken: number
  center: [number, number]
  zoom: number
  style: ResolvedStyle
  onBounds: (bbox: [number, number, number, number]) => void
}

export default function MapView({ visible, filters, markers, regions, flyToken, center, zoom, style, onBounds }: Props) {
  const container = useRef<HTMLDivElement>(null)
  const mapRef = useRef<Map | null>(null)
  const ready = useRef(false)
  const visibleRef = useRef(visible)
  const filtersRef = useRef(filters)
  const onBoundsRef = useRef(onBounds)
  const regionsRef = useRef(regions)
  const styleRef = useRef(style)
  visibleRef.current = visible
  filtersRef.current = filters
  onBoundsRef.current = onBounds
  regionsRef.current = regions
  styleRef.current = style

  useEffect(() => {
    if (!container.current || mapRef.current) {
      return
    }
    const initial = styleRef.current
    const basemap = initial.basemap === 'none' ? 'light' : initial.basemap
    const map = new maplibregl.Map({
      container: container.current,
      center: [117.28, 31.86],
      zoom: 6.4,
      attributionControl: false,
      localIdeographFontFamily: "'Microsoft YaHei', 'PingFang SC', sans-serif",
      style: {
        version: 8,
        glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
        sources: {
          basemap: {
            type: 'raster',
            tiles: tilesOf(basemap),
            tileSize: 256,
            maxzoom: maxzoomOf(basemap)
          }
        },
        layers: [
          { id: 'background', type: 'background', paint: { 'background-color': initial.tokens.background } },
          {
            id: 'basemap',
            type: 'raster',
            source: 'basemap',
            layout: { visibility: initial.basemap === 'none' ? 'none' : 'visible' }
          }
        ]
      }
    })
    rememberBasemap(map, initial.basemap)
    map.addControl(new maplibregl.NavigationControl(), 'top-right')
    map.addControl(new maplibregl.ScaleControl({ unit: 'metric' }), 'bottom-left')
    map.on('load', () => {
      for (const layer of LAYERS) {
        map.addSource(layer.id, {
          type: 'vector',
          tiles: [tileUrl(regionsRef.current, layer.id)],
          minzoom: layer.minzoom,
          maxzoom: 16
        })
      }
      for (const spec of overlayLayers(styleRef.current)) {
        map.addLayer(spec)
      }
      map.addSource('markers', { type: 'geojson', data: emptyCollection() })
      map.addLayer({
        id: 'markers',
        type: 'circle',
        source: 'markers',
        paint: {
          'circle-radius': 7,
          'circle-color': styleRef.current.tokens.marker,
          'circle-stroke-width': 2,
          'circle-stroke-color': styleRef.current.tokens.markerStroke
        }
      })
      ready.current = true
      syncOverlayStyle(map, styleRef.current)
      applyLayerState(map, visibleRef.current, filtersRef.current, styleRef.current.labels)
      for (const id of [...interactiveLayerIds(), 'markers']) {
        map.on('mouseenter', id, () => {
          map.getCanvas().style.cursor = 'pointer'
        })
        map.on('mouseleave', id, () => {
          map.getCanvas().style.cursor = ''
        })
      }
    })
    map.on('moveend', () => {
      const box = map.getBounds()
      onBoundsRef.current([box.getWest(), box.getSouth(), box.getEast(), box.getNorth()])
    })
    map.on('click', (event) => {
      const features = map.queryRenderedFeatures(event.point).filter((feature) => feature.source !== 'basemap' && feature.layer?.id !== 'background')
      if (features.length === 0) {
        return
      }
      const feature = features[0]
      const props = feature.properties ?? {}
      const source = String(feature.sourceLayer || feature.source || '')
      const name = String(props.name || '未命名')
      const caption = featureCaption(source, String(props.subtype || ''))
      new maplibregl.Popup({ closeButton: true, maxWidth: '280px' })
        .setLngLat(event.lngLat)
        .setHTML(`<div class="map-popup"><div class="map-popup-name">${escapeHtml(name)}</div><div class="map-popup-meta">${escapeHtml(caption)}</div></div>`)
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
    syncOverlayStyle(map, style)
    applyLayerState(map, visibleRef.current, filtersRef.current, style.labels)
  }, [style])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready.current) {
      return
    }
    applyLayerState(map, visible, filters, styleRef.current.labels)
  }, [visible, filters])

  useEffect(() => {
    const map = mapRef.current
    if (!map || !ready.current) {
      return
    }
    for (const layer of LAYERS) {
      const source = map.getSource(layer.id)
      if (source instanceof VectorTileSource) {
        source.setTiles([tileUrl(regions, layer.id)])
      }
    }
  }, [regions])

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

  return (
    <div className="map-root">
      <div ref={container} className="map-canvas" />
      <div className="map-attrib">{attributionOf(style.basemap)}</div>
    </div>
  )
}

function tileUrl(regions: string[], layer: string) {
  const query = regionsQuery(regions)
  return `${window.location.origin}/api/tiles/${layer}/{z}/{x}/{y}.pbf?${query}`
}

function regionsQuery(regions: string[]) {
  const codes = regions.filter(Boolean)
  if (codes.length === 0) {
    return 'region=anhui'
  }
  if (codes.length === 1) {
    return `region=${encodeURIComponent(codes[0])}`
  }
  return `regions=${encodeURIComponent(codes.join(','))}`
}

function emptyCollection() {
  return { type: 'FeatureCollection' as const, features: [] as GeoJSON.Feature[] }
}

function escapeHtml(value: string) {
  return value.replace(/[&<>"']/g, (char) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[char] ?? char))
}
