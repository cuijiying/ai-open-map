export interface LayerStyle {
  id: string
  label: string
  color: string
  minzoom: number
  kind: 'line' | 'fill' | 'circle'
}

export const LAYERS: LayerStyle[] = [
  { id: 'landuse', label: '土地利用', color: '#b7d7a8', minzoom: 10, kind: 'fill' },
  { id: 'water', label: '水体', color: '#7eb6d6', minzoom: 6, kind: 'fill' },
  { id: 'building', label: '建筑', color: '#d9d3c5', minzoom: 13, kind: 'fill' },
  { id: 'waterway', label: '水系线', color: '#3d8ebf', minzoom: 7, kind: 'line' },
  { id: 'railway', label: '铁路', color: '#6b7280', minzoom: 8, kind: 'line' },
  { id: 'highway', label: '道路', color: '#e67e22', minzoom: 4, kind: 'line' },
  { id: 'boundary', label: '行政区划', color: '#7c3aed', minzoom: 4, kind: 'line' },
  { id: 'poi', label: '兴趣点', color: '#e11d48', minzoom: 13, kind: 'circle' },
  { id: 'place', label: '地名', color: '#111827', minzoom: 4, kind: 'circle' }
]

export const DEFAULT_VISIBLE = ['highway', 'waterway', 'water', 'place', 'boundary']

export function layerOf(id: string): LayerStyle {
  return LAYERS.find((item) => item.id === id) ?? LAYERS[0]
}
