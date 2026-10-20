export type ThemeId = 'light' | 'dark' | 'voyager' | 'paper' | 'imagery' | 'plain'
export type BasemapId = 'light' | 'dark' | 'voyager' | 'imagery' | 'none'

export interface StyleTokens {
  background: string
  label: string
  labelHalo: string
  marker: string
  markerStroke: string
  highwayMotorway: string
  highwayTrunk: string
  highwayPrimary: string
  highwaySecondary: string
  highwayTertiary: string
  highwayMinor: string
  highwayService: string
  highwayPath: string
  highwayCasing: string
  waterFill: string
  waterLine: string
  landuseResidential: string
  landuseCommercial: string
  landuseIndustrial: string
  landuseGreen: string
  landuseFarm: string
  landusePark: string
  landuseOther: string
  buildingFill: string
  buildingOutline: string
  railway: string
  railwayCasing: string
  boundary: string
  poiAmenity: string
  poiShop: string
  poiTourism: string
  poiHistoric: string
  poiOther: string
  place: string
}

export interface StyleNumbers {
  landuseOpacity: number
  waterOpacity: number
  buildingOpacity: number
  labelHaloWidth: number
}

export type OpacityKey = 'landuseOpacity' | 'waterOpacity' | 'buildingOpacity'
export type TokenKey = keyof StyleTokens

export interface StyleOverrides {
  basemap?: BasemapId
  widthScale?: number
  labels?: boolean
  colors: Partial<StyleTokens>
  opacity: Partial<Pick<StyleNumbers, OpacityKey>>
}

export interface ThemePreset {
  id: ThemeId
  label: string
  description: string
  basemap: BasemapId
  preview: [string, string, string, string]
  tokens: StyleTokens
  numbers: StyleNumbers
}

export interface ResolvedStyle {
  id: ThemeId
  label: string
  basemap: BasemapId
  tokens: StyleTokens
  numbers: StyleNumbers
  labels: boolean
  widthScale: number
}

export interface ColorField {
  key: TokenKey
  label: string
}

export interface StyleGroup {
  key: string
  title: string
  colors: ColorField[]
  opacity?: { key: OpacityKey; label: string }[]
}

const LIGHT: StyleTokens = {
  background: '#e7eef2',
  label: '#1f2937',
  labelHalo: '#ffffff',
  marker: '#111827',
  markerStroke: '#ffffff',
  highwayMotorway: '#e06a5c',
  highwayTrunk: '#f09a4a',
  highwayPrimary: '#f6d365',
  highwaySecondary: '#f8e7a8',
  highwayTertiary: '#ffffff',
  highwayMinor: '#ffffff',
  highwayService: '#f4f4f5',
  highwayPath: '#c4b5a0',
  highwayCasing: '#d3c8b8',
  waterFill: '#a4d4e6',
  waterLine: '#5aa4cc',
  landuseResidential: '#f1e4d4',
  landuseCommercial: '#f3d2d4',
  landuseIndustrial: '#e3d2e4',
  landuseGreen: '#b5d5a4',
  landuseFarm: '#efe6b4',
  landusePark: '#a9dba0',
  landuseOther: '#e6e0d4',
  buildingFill: '#e6ddd0',
  buildingOutline: '#cbbfae',
  railway: '#f8fafc',
  railwayCasing: '#64748b',
  boundary: '#7c6aad',
  poiAmenity: '#e11d48',
  poiShop: '#7c3aed',
  poiTourism: '#0284c7',
  poiHistoric: '#b45309',
  poiOther: '#475569',
  place: '#1f2937'
}

export const THEMES: ThemePreset[] = [
  {
    id: 'light',
    label: '浅色',
    description: '浅色底图，道路按等级分色，适合日常浏览。',
    basemap: 'light',
    preview: ['#e06a5c', '#f6d365', '#a4d4e6', '#b5d5a4'],
    tokens: LIGHT,
    numbers: { landuseOpacity: 0.72, waterOpacity: 0.86, buildingOpacity: 0.92, labelHaloWidth: 1.3 }
  },
  {
    id: 'dark',
    label: '深色',
    description: '深色底图，夜间阅读，路网用亮色拉开层次。',
    basemap: 'dark',
    preview: ['#0f172a', '#ff6b6b', '#3d8ebf', '#1e3a2f'],
    tokens: {
      background: '#0b1220',
      label: '#e5e7eb',
      labelHalo: '#0b1220',
      marker: '#f8fafc',
      markerStroke: '#0b1220',
      highwayMotorway: '#ff6b6b',
      highwayTrunk: '#ff9f43',
      highwayPrimary: '#f6c945',
      highwaySecondary: '#d6b25e',
      highwayTertiary: '#9aa4b5',
      highwayMinor: '#8b95a7',
      highwayService: '#6b7280',
      highwayPath: '#78716c',
      highwayCasing: '#070b14',
      waterFill: '#163a5f',
      waterLine: '#3d8ebf',
      landuseResidential: '#2a3142',
      landuseCommercial: '#3a2a38',
      landuseIndustrial: '#33283a',
      landuseGreen: '#1e3a2f',
      landuseFarm: '#3a3a28',
      landusePark: '#1f4a38',
      landuseOther: '#262c38',
      buildingFill: '#334155',
      buildingOutline: '#1e293b',
      railway: '#cbd5e1',
      railwayCasing: '#020617',
      boundary: '#a78bfa',
      poiAmenity: '#fb7185',
      poiShop: '#c4b5fd',
      poiTourism: '#38bdf8',
      poiHistoric: '#fdba74',
      poiOther: '#cbd5e1',
      place: '#f8fafc'
    },
    numbers: { landuseOpacity: 0.62, waterOpacity: 0.82, buildingOpacity: 0.78, labelHaloWidth: 1.4 }
  },
  {
    id: 'voyager',
    label: '彩色',
    description: '彩色底图，水系和绿地更醒目。',
    basemap: 'voyager',
    preview: ['#f4a261', '#2a9d8f', '#8ab86a', '#e9c46a'],
    tokens: {
      ...LIGHT,
      background: '#efe6d6',
      highwayMotorway: '#e76f51',
      highwayTrunk: '#f4a261',
      highwayPrimary: '#e9c46a',
      highwaySecondary: '#f3dd9a',
      highwayCasing: '#d9c7a6',
      waterFill: '#8ecae6',
      waterLine: '#219ebc',
      landuseGreen: '#95c17a',
      landusePark: '#7fb069',
      landuseFarm: '#ead9a0',
      boundary: '#9b5de5',
      place: '#3d405b'
    },
    numbers: { landuseOpacity: 0.48, waterOpacity: 0.62, buildingOpacity: 0.82, labelHaloWidth: 1.3 }
  },
  {
    id: 'paper',
    label: '纸图',
    description: '去掉底图，用纸色和墨色画道路、水系。',
    basemap: 'none',
    preview: ['#f3ead7', '#8f3d36', '#8d6a3a', '#b7cfc8'],
    tokens: {
      background: '#f3ead7',
      label: '#3d342b',
      labelHalo: '#f7f1e4',
      marker: '#3d342b',
      markerStroke: '#f7f1e4',
      highwayMotorway: '#8f3d36',
      highwayTrunk: '#a15c32',
      highwayPrimary: '#8d6a3a',
      highwaySecondary: '#74624a',
      highwayTertiary: '#6a5c4c',
      highwayMinor: '#6e6256',
      highwayService: '#8a7d6c',
      highwayPath: '#9a8b74',
      highwayCasing: '#f7f1e4',
      waterFill: '#b7cfc8',
      waterLine: '#6f9790',
      landuseResidential: '#eadcc4',
      landuseCommercial: '#e6d0c4',
      landuseIndustrial: '#ddd0c4',
      landuseGreen: '#c9d2b2',
      landuseFarm: '#e4dcb4',
      landusePark: '#c3d4ae',
      landuseOther: '#e7dcc8',
      buildingFill: '#e7dcc6',
      buildingOutline: '#d3c4a8',
      railway: '#f7f1e4',
      railwayCasing: '#4a4036',
      boundary: '#7a5b4a',
      poiAmenity: '#9a3412',
      poiShop: '#7c4a2d',
      poiTourism: '#3f6f68',
      poiHistoric: '#92400e',
      poiOther: '#57534e',
      place: '#3d342b'
    },
    numbers: { landuseOpacity: 0.88, waterOpacity: 0.9, buildingOpacity: 0.94, labelHaloWidth: 1.5 }
  },
  {
    id: 'imagery',
    label: '影像',
    description: '卫星影像上叠加高对比路网和半透明要素。',
    basemap: 'imagery',
    preview: ['#1f2937', '#ffd166', '#38bdf8', '#ffffff'],
    tokens: {
      background: '#111827',
      label: '#ffffff',
      labelHalo: '#111827',
      marker: '#ffd166',
      markerStroke: '#111827',
      highwayMotorway: '#ffd166',
      highwayTrunk: '#ffb703',
      highwayPrimary: '#ffffff',
      highwaySecondary: '#f1f5f9',
      highwayTertiary: '#e2e8f0',
      highwayMinor: '#e2e8f0',
      highwayService: '#cbd5e1',
      highwayPath: '#fde68a',
      highwayCasing: '#111827',
      waterFill: '#38bdf8',
      waterLine: '#7dd3fc',
      landuseResidential: '#f8fafc',
      landuseCommercial: '#fda4af',
      landuseIndustrial: '#d8b4fe',
      landuseGreen: '#86efac',
      landuseFarm: '#fde68a',
      landusePark: '#4ade80',
      landuseOther: '#e5e7eb',
      buildingFill: '#ffffff',
      buildingOutline: '#ffffff',
      railway: '#ffffff',
      railwayCasing: '#111827',
      boundary: '#f472b6',
      poiAmenity: '#fb7185',
      poiShop: '#e9d5ff',
      poiTourism: '#7dd3fc',
      poiHistoric: '#fdba74',
      poiOther: '#ffffff',
      place: '#ffffff'
    },
    numbers: { landuseOpacity: 0.22, waterOpacity: 0.4, buildingOpacity: 0.3, labelHaloWidth: 1.8 }
  },
  {
    id: 'plain',
    label: '素底',
    description: '不要底图，只画矢量数据，方便对照类型。',
    basemap: 'none',
    preview: ['#f8fafc', '#dc2626', '#0284c7', '#65a30d'],
    tokens: {
      background: '#f8fafc',
      label: '#0f172a',
      labelHalo: '#ffffff',
      marker: '#0f172a',
      markerStroke: '#ffffff',
      highwayMotorway: '#dc2626',
      highwayTrunk: '#ea580c',
      highwayPrimary: '#ca8a04',
      highwaySecondary: '#65a30d',
      highwayTertiary: '#0284c7',
      highwayMinor: '#64748b',
      highwayService: '#94a3b8',
      highwayPath: '#a8a29e',
      highwayCasing: '#ffffff',
      waterFill: '#7dd3fc',
      waterLine: '#0284c7',
      landuseResidential: '#fde68a',
      landuseCommercial: '#fecdd3',
      landuseIndustrial: '#e9d5ff',
      landuseGreen: '#86efac',
      landuseFarm: '#fef08a',
      landusePark: '#4ade80',
      landuseOther: '#e2e8f0',
      buildingFill: '#e2e8f0',
      buildingOutline: '#94a3b8',
      railway: '#ffffff',
      railwayCasing: '#334155',
      boundary: '#7c3aed',
      poiAmenity: '#e11d48',
      poiShop: '#7c3aed',
      poiTourism: '#0284c7',
      poiHistoric: '#b45309',
      poiOther: '#334155',
      place: '#0f172a'
    },
    numbers: { landuseOpacity: 0.78, waterOpacity: 0.85, buildingOpacity: 0.9, labelHaloWidth: 1.3 }
  }
]

export const BASEMAP_OPTIONS: { id: BasemapId; label: string }[] = [
  { id: 'light', label: '浅色底图' },
  { id: 'dark', label: '深色底图' },
  { id: 'voyager', label: '彩色底图' },
  { id: 'imagery', label: '卫星影像' },
  { id: 'none', label: '无底图' }
]

export const STYLE_GROUPS: StyleGroup[] = [
  {
    key: 'road',
    title: '道路',
    colors: [
      { key: 'highwayMotorway', label: '高速' },
      { key: 'highwayTrunk', label: '主干' },
      { key: 'highwayPrimary', label: '主要' },
      { key: 'highwaySecondary', label: '次要' },
      { key: 'highwayTertiary', label: '支路' },
      { key: 'highwayMinor', label: '居住区' },
      { key: 'highwayPath', label: '小路' },
      { key: 'highwayCasing', label: '描边' }
    ]
  },
  {
    key: 'water',
    title: '水系',
    colors: [
      { key: 'waterFill', label: '水面' },
      { key: 'waterLine', label: '河流' }
    ],
    opacity: [{ key: 'waterOpacity', label: '水面透明度' }]
  },
  {
    key: 'land',
    title: '用地与建筑',
    colors: [
      { key: 'landuseResidential', label: '居住' },
      { key: 'landuseCommercial', label: '商业' },
      { key: 'landuseIndustrial', label: '工业' },
      { key: 'landuseGreen', label: '林地' },
      { key: 'landuseFarm', label: '农田' },
      { key: 'landusePark', label: '公园' },
      { key: 'buildingFill', label: '建筑' },
      { key: 'buildingOutline', label: '建筑轮廓' }
    ],
    opacity: [
      { key: 'landuseOpacity', label: '用地透明度' },
      { key: 'buildingOpacity', label: '建筑透明度' }
    ]
  },
  {
    key: 'other',
    title: '注记与其他',
    colors: [
      { key: 'railwayCasing', label: '铁路' },
      { key: 'boundary', label: '区划' },
      { key: 'place', label: '地名' },
      { key: 'label', label: '注记' },
      { key: 'labelHalo', label: '注记描边' },
      { key: 'poiAmenity', label: '设施' },
      { key: 'poiShop', label: '商店' },
      { key: 'poiTourism', label: '旅游' }
    ]
  }
]

const LAYER_TOKENS: Record<string, TokenKey[]> = {
  highway: ['highwayMotorway', 'highwayTrunk', 'highwayPrimary', 'highwaySecondary', 'highwayTertiary', 'highwayMinor', 'highwayService', 'highwayPath'],
  water: ['waterFill', 'waterLine'],
  waterway: ['waterLine'],
  landuse: ['landuseResidential', 'landuseCommercial', 'landuseIndustrial', 'landuseGreen', 'landuseFarm', 'landusePark', 'landuseOther'],
  building: ['buildingFill'],
  railway: ['railway'],
  boundary: ['boundary'],
  poi: ['poiAmenity', 'poiShop', 'poiTourism', 'poiHistoric', 'poiOther'],
  place: ['place']
}

const HIGHWAY_TOKEN: Record<string, TokenKey> = {
  motorway: 'highwayMotorway',
  motorway_link: 'highwayMotorway',
  trunk: 'highwayTrunk',
  trunk_link: 'highwayTrunk',
  primary: 'highwayPrimary',
  primary_link: 'highwayPrimary',
  secondary: 'highwaySecondary',
  secondary_link: 'highwaySecondary',
  tertiary: 'highwayTertiary',
  tertiary_link: 'highwayTertiary',
  residential: 'highwayMinor',
  unclassified: 'highwayMinor',
  living_street: 'highwayMinor',
  road: 'highwayMinor',
  service: 'highwayService',
  footway: 'highwayPath',
  path: 'highwayPath',
  cycleway: 'highwayPath',
  pedestrian: 'highwayPath',
  steps: 'highwayPath',
  bridleway: 'highwayPath',
  track: 'highwayPath'
}

const STORAGE_KEY = 'aomap.map-style.v1'

interface StyleMemory {
  themeId: ThemeId
  themes: Partial<Record<ThemeId, StyleOverrides>>
}
const HEX = /^#[0-9a-fA-F]{6}$/
const THEME_IDS = new Set<string>(THEMES.map((item) => item.id))
const BASEMAP_IDS = new Set<string>(BASEMAP_OPTIONS.map((item) => item.id))
const TOKEN_KEYS = new Set<string>(Object.keys(LIGHT))
const OPACITY_KEYS: OpacityKey[] = ['landuseOpacity', 'waterOpacity', 'buildingOpacity']

export function emptyOverrides(): StyleOverrides {
  return { colors: {}, opacity: {} }
}

export function isThemeId(value: string | undefined): value is ThemeId {
  return !!value && THEME_IDS.has(value)
}

export function isBasemapId(value: string | undefined): value is BasemapId {
  return !!value && BASEMAP_IDS.has(value)
}

export function themeById(id: string | undefined): ThemePreset {
  return THEMES.find((item) => item.id === id) ?? THEMES[0]
}

export function tilesOf(basemap: BasemapId): string[] {
  switch (basemap) {
    case 'dark':
      return ['https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile/{z}/{y}/{x}']
    case 'voyager':
      return ['https://server.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile/{z}/{y}/{x}']
    case 'imagery':
      return ['https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}']
    case 'none':
    case 'light':
      return ['https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}']
  }
}

export function maxzoomOf(basemap: BasemapId) {
  if (basemap === 'imagery' || basemap === 'voyager') {
    return 18
  }
  return 16
}

export function attributionOf(basemap: BasemapId): string {
  if (basemap === 'none') {
    return '© OpenStreetMap'
  }
  return '© OpenStreetMap © Esri'
}

export function resolveStyle(themeId: string, overrides: StyleOverrides): ResolvedStyle {
  const theme = themeById(themeId)
  const colors = sanitizeColors(overrides.colors)
  const tokens = { ...theme.tokens, ...colors }
  const numbers: StyleNumbers = { ...theme.numbers }
  for (const key of OPACITY_KEYS) {
    const value = overrides.opacity?.[key]
    if (typeof value === 'number' && Number.isFinite(value)) {
      numbers[key] = clamp(value, 0.05, 1)
    }
  }
  return {
    id: theme.id,
    label: theme.label,
    basemap: isBasemapId(overrides.basemap) ? overrides.basemap : theme.basemap,
    tokens,
    numbers,
    labels: overrides.labels !== false,
    widthScale: clamp(overrides.widthScale ?? 1, 0.4, 2.4)
  }
}

export function styleIsDirty(themeId: string, overrides: StyleOverrides): boolean {
  const theme = themeById(themeId)
  if (isBasemapId(overrides.basemap) && overrides.basemap !== theme.basemap) {
    return true
  }
  if (typeof overrides.widthScale === 'number' && Math.abs(overrides.widthScale - 1) > 0.001) {
    return true
  }
  if (overrides.labels === false) {
    return true
  }
  if (Object.keys(sanitizeColors(overrides.colors)).length > 0) {
    return true
  }
  return OPACITY_KEYS.some((key) => {
    const value = overrides.opacity?.[key]
    return typeof value === 'number' && Math.abs(value - theme.numbers[key]) > 0.001
  })
}

export function loadMapStyle(): { themeId: ThemeId; overrides: StyleOverrides } {
  const memory = readMemory()
  return { themeId: memory.themeId, overrides: memory.themes[memory.themeId] ?? emptyOverrides() }
}

export function loadThemeOverrides(themeId: ThemeId): StyleOverrides {
  return readMemory().themes[themeId] ?? emptyOverrides()
}

export function saveMapStyle(themeId: ThemeId, overrides: StyleOverrides) {
  const memory = readMemory()
  memory.themeId = themeId
  memory.themes[themeId] = sanitizeOverrides(overrides)
  localStorage.setItem(STORAGE_KEY, JSON.stringify(memory))
}

export function resetStoredStyles() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ themeId: 'light', themes: {} } satisfies StyleMemory))
}

export function paintLayers(overrides: StyleOverrides, layers: string[], color: string, filters?: Record<string, string[] | undefined>): StyleOverrides {
  if (!HEX.test(color)) {
    return overrides
  }
  const hex = color.toLowerCase()
  const colors = { ...overrides.colors }
  for (const layer of layers) {
    for (const token of tokensFor(layer, filters?.[layer])) {
      colors[token] = hex
    }
    if (layer === 'railway') {
      colors.railwayCasing = hex
      colors.railway = contrastHex(hex)
    }
    if (layer === 'building') {
      colors.buildingOutline = contrastHex(hex)
    }
  }
  return { ...overrides, colors }
}

export function swatchOf(style: ResolvedStyle, layerId: string): string {
  const token = style.tokens
  switch (layerId) {
    case 'highway':
      return `linear-gradient(90deg, ${token.highwayMotorway} 0 34%, ${token.highwayPrimary} 34% 67%, ${token.highwayMinor} 67% 100%)`
    case 'water':
      return token.waterFill
    case 'waterway':
      return token.waterLine
    case 'landuse':
      return `linear-gradient(90deg, ${token.landuseGreen} 0 34%, ${token.landuseResidential} 34% 67%, ${token.landuseFarm} 67% 100%)`
    case 'building':
      return token.buildingFill
    case 'railway':
      return token.railwayCasing
    case 'boundary':
      return token.boundary
    case 'poi':
      return token.poiAmenity
    case 'place':
      return token.place
    default:
      return '#94a3b8'
  }
}

export function featureCaption(source: string, subtype: string): string {
  const layer = layerLabel(source)
  const detail = subtypeLabel(source, subtype)
  if (!detail) {
    return layer
  }
  return `${layer} · ${detail}`
}

function tokensFor(layer: string, subtypes?: string[]): TokenKey[] {
  if (layer === 'highway' && subtypes && subtypes.length > 0) {
    const picked = new Set<TokenKey>()
    for (const subtype of subtypes) {
      const token = HIGHWAY_TOKEN[subtype]
      if (token) {
        picked.add(token)
      }
    }
    if (picked.size > 0) {
      return [...picked]
    }
  }
  return LAYER_TOKENS[layer] ?? []
}

function sanitizeOverrides(value: StyleOverrides | undefined): StyleOverrides {
  const opacity: StyleOverrides['opacity'] = {}
  for (const key of OPACITY_KEYS) {
    const raw = value?.opacity?.[key]
    if (typeof raw === 'number' && Number.isFinite(raw)) {
      opacity[key] = clamp(raw, 0.05, 1)
    }
  }
  const width = value?.widthScale
  return {
    basemap: isBasemapId(value?.basemap) ? value.basemap : undefined,
    widthScale: typeof width === 'number' && Number.isFinite(width) ? clamp(width, 0.4, 2.4) : undefined,
    labels: value?.labels === false ? false : undefined,
    colors: sanitizeColors(value?.colors),
    opacity
  }
}

function sanitizeColors(value: Partial<StyleTokens> | undefined): Partial<StyleTokens> {
  const colors: Partial<StyleTokens> = {}
  if (!value) {
    return colors
  }
  for (const [key, color] of Object.entries(value)) {
    if (TOKEN_KEYS.has(key) && typeof color === 'string' && HEX.test(color)) {
      colors[key as TokenKey] = color.toLowerCase()
    }
  }
  return colors
}

function clamp(value: number, min: number, max: number) {
  return Math.min(max, Math.max(min, value))
}

function contrastHex(hex: string) {
  const red = Number.parseInt(hex.slice(1, 3), 16)
  const green = Number.parseInt(hex.slice(3, 5), 16)
  const blue = Number.parseInt(hex.slice(5, 7), 16)
  return red * 0.299 + green * 0.587 + blue * 0.114 > 160 ? '#111827' : '#f8fafc'
}

function readMemory(): StyleMemory {
  const empty: StyleMemory = { themeId: 'light', themes: {} }
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return empty
    }
    const parsed = JSON.parse(raw) as Partial<StyleMemory>
    const themeId = isThemeId(parsed.themeId) ? parsed.themeId : 'light'
    const themes: StyleMemory['themes'] = {}
    for (const id of THEMES.map((item) => item.id)) {
      const stored = parsed.themes?.[id]
      if (stored) {
        themes[id] = sanitizeOverrides(stored)
      }
    }
    return { themeId, themes }
  } catch {
    return empty
  }
}

function layerLabel(source: string) {
  switch (source) {
    case 'highway':
      return '道路'
    case 'railway':
      return '铁路'
    case 'waterway':
      return '水系'
    case 'water':
      return '水体'
    case 'building':
      return '建筑'
    case 'landuse':
      return '用地'
    case 'boundary':
      return '区划'
    case 'poi':
      return '兴趣点'
    case 'place':
      return '地名'
    case 'markers':
      return '标记'
    default:
      return source
  }
}

function subtypeLabel(source: string, subtype: string) {
  if (!subtype) {
    return ''
  }
  if (source === 'highway') {
    return HIGHWAY_NAMES[subtype] ?? subtype
  }
  if (source === 'place') {
    return PLACE_NAMES[subtype] ?? subtype
  }
  if (source === 'boundary') {
    return BOUNDARY_NAMES[subtype] ?? `级别 ${subtype}`
  }
  if (source === 'water' || source === 'waterway') {
    return WATER_NAMES[subtype] ?? subtype
  }
  if (source === 'landuse') {
    return LANDUSE_NAMES[subtype] ?? subtype
  }
  if (source === 'poi') {
    const value = subtype.includes('=') ? subtype.slice(subtype.indexOf('=') + 1) : subtype
    return POI_NAMES[value] ?? value
  }
  if (source === 'railway') {
    return RAIL_NAMES[subtype] ?? subtype
  }
  if (source === 'building' && subtype !== 'yes') {
    return BUILDING_NAMES[subtype] ?? subtype
  }
  return ''
}

const HIGHWAY_NAMES: Record<string, string> = {
  motorway: '高速公路',
  motorway_link: '高速匝道',
  trunk: '快速路',
  trunk_link: '快速路匝道',
  primary: '主干道',
  primary_link: '主干道连接',
  secondary: '次干道',
  secondary_link: '次干道连接',
  tertiary: '支路',
  tertiary_link: '支路连接',
  residential: '居住区道路',
  unclassified: '一般道路',
  living_street: '生活街道',
  service: '服务道路',
  track: '机耕路',
  footway: '人行道',
  path: '小径',
  cycleway: '自行车道',
  pedestrian: '步行街',
  steps: '台阶',
  road: '道路'
}

const PLACE_NAMES: Record<string, string> = {
  city: '城市',
  town: '镇',
  county: '县',
  suburb: '城区',
  village: '村',
  hamlet: '村庄',
  neighbourhood: '街区',
  quarter: '片区',
  borough: '区',
  state: '州',
  province: '省'
}

const BOUNDARY_NAMES: Record<string, string> = {
  '2': '国界',
  '3': '省级界',
  '4': '省级界',
  '5': '地区界',
  '6': '地级界',
  '7': '区县界',
  '8': '区县界'
}

const WATER_NAMES: Record<string, string> = {
  water: '水体',
  riverbank: '河岸',
  wetland: '湿地',
  bay: '海湾',
  dock: '码头',
  river: '河流',
  canal: '运河',
  stream: '溪流',
  ditch: '沟渠',
  drain: '排水渠',
  flowline: '水流'
}

const LANDUSE_NAMES: Record<string, string> = {
  residential: '居住',
  commercial: '商业',
  retail: '零售',
  industrial: '工业',
  quarry: '采石场',
  military: '军事',
  construction: '施工',
  forest: '林地',
  wood: '树林',
  grass: '草地',
  meadow: '草甸',
  farmland: '农田',
  farmyard: '农院',
  orchard: '果园',
  vineyard: '葡萄园',
  park: '公园',
  garden: '花园',
  pitch: '运动场',
  playground: '游乐场'
}

const POI_NAMES: Record<string, string> = {
  school: '学校',
  university: '大学',
  college: '学院',
  hospital: '医院',
  clinic: '诊所',
  pharmacy: '药店',
  restaurant: '餐厅',
  cafe: '咖啡馆',
  fuel: '加油站',
  bank: '银行',
  parking: '停车场',
  bus_station: '汽车站',
  place_of_worship: '宗教场所',
  library: '图书馆',
  police: '警察局',
  fire_station: '消防站',
  post_office: '邮局',
  supermarket: '超市',
  hotel: '酒店',
  attraction: '景点',
  museum: '博物馆',
  viewpoint: '观景点'
}

const RAIL_NAMES: Record<string, string> = {
  rail: '铁路',
  subway: '地铁',
  light_rail: '轻轨',
  tram: '有轨电车',
  narrow_gauge: '窄轨'
}

const BUILDING_NAMES: Record<string, string> = {
  residential: '住宅',
  commercial: '商业',
  industrial: '工业',
  retail: '零售',
  school: '学校',
  hospital: '医院',
  apartments: '公寓',
  house: '房屋'
}
