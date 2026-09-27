export interface LayerCount {
  layer: string
  label: string
  count: number
}

export interface JobView {
  id: number
  regionCode: string
  regionName: string
  status: string
  phase: string
  progress: number
  message?: string
  fileBytes?: number
  featureCount?: number
  layerCounts?: Record<string, number>
  triggerType: string
  startedAt?: string
  finishedAt?: string
  error?: string
}

export interface LogView {
  id: number
  phase: string
  level: string
  message: string
  createdAt: string
}

export interface ModelView {
  id: number
  name: string
  baseUrl: string
  apiKeyMasked: string
  hasKey: boolean
  model: string
  temperature: number
  enabled: boolean
}

export interface MarkerPoint {
  name?: string
  layer?: string
  subtype?: string
  lon: number
  lat: number
}

export interface MapAction {
  type: string
  layers?: string[]
  filters?: Record<string, string[]>
  center?: [number, number]
  zoom?: number
  label?: string
  points?: MarkerPoint[]
}

export interface DataTable {
  title: string
  columns: string[]
  rows: (string | number)[][]
}

export interface ChatResponse {
  reply: string
  source: string
  actions: MapAction[]
  tables: DataTable[]
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  tables?: DataTable[]
}
