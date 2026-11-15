import { create } from 'zustand'

export const CHINA_CENTER: [number, number] = [104.195, 35.861]
export const CHINA_ZOOM = 3.8
export const CHINA_BBOX: [number, number, number, number] = [73.5, 18, 135, 53.6]

const MAP_REGIONS_KEY = 'aomap.mapRegions'

function loadMapRegions() {
  try {
    const saved = JSON.parse(localStorage.getItem(MAP_REGIONS_KEY) || '')
    if (Array.isArray(saved) && saved.every((item) => typeof item === 'string' && item)) {
      return Array.from(new Set(saved as string[]))
    }
  } catch {
    // 没有保存过省份时保持未选择。
  }
  return []
}

interface MapSession {
  mapRegions: string[]
  center: [number, number]
  zoom: number
  bbox: [number, number, number, number]
  flyToken: number
  bounds: [[number, number], [number, number]] | null
  boundsToken: number
  setMapRegions: (codes: string[]) => void
  flyTo: (center: [number, number], zoom: number) => void
  setBbox: (bbox: [number, number, number, number]) => void
  fitBounds: (bounds: [[number, number], [number, number]]) => void
}

export const useMapSession = create<MapSession>((set) => ({
  mapRegions: loadMapRegions(),
  center: CHINA_CENTER,
  zoom: CHINA_ZOOM,
  bbox: CHINA_BBOX,
  flyToken: 0,
  bounds: null,
  boundsToken: 0,
  setMapRegions: (codes) => {
    localStorage.setItem(MAP_REGIONS_KEY, JSON.stringify(codes))
    set({ mapRegions: codes })
  },
  flyTo: (center, zoom) => set((state) => ({ center, zoom, flyToken: state.flyToken + 1 })),
  setBbox: (bbox) => set({ bbox }),
  fitBounds: (bounds) => set((state) => ({ bounds, boundsToken: state.boundsToken + 1 }))
}))
