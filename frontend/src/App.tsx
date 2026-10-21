import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { Button, Layout, message } from 'antd'
import { api } from './api'
import ChatPanel from './components/ChatPanel'
import JobPanel from './components/JobPanel'
import LayerPanel from './components/LayerPanel'
import MapView from './components/MapView'
import ModelModal from './components/ModelModal'
import StylePanel from './components/StylePanel'
import { DEFAULT_VISIBLE, LAYERS } from './layers'
import { emptyOverrides, isThemeId, loadMapStyle, loadThemeOverrides, paintLayers, resetStoredStyles, resolveStyle, saveMapStyle, swatchOf, ThemeId } from './mapStyle'
import { ChatMessage, ChatResponse, JobView, LayerCount, LogView, MapAction, MarkerPoint, RegionOption, SourceOption, SourceProbe } from './types'

const SOURCE_KEY = 'aomap.downloadSource'
const MAP_REGIONS_KEY = 'aomap.mapRegions'

function loadSourceId() {
  const saved = localStorage.getItem(SOURCE_KEY)
  return saved === 'geofabrik' || saved === 'osm-france' ? saved : 'osm-france'
}

function loadMapRegions() {
  try {
    const saved = JSON.parse(localStorage.getItem(MAP_REGIONS_KEY) || '')
    if (Array.isArray(saved) && saved.every((item) => typeof item === 'string' && item)) {
      return Array.from(new Set(saved as string[]))
    }
  } catch {
    // 本地没有可用的省份选择时回到安徽省。
  }
  return ['anhui']
}

function rememberMapRegions(codes: string[]) {
  localStorage.setItem(MAP_REGIONS_KEY, JSON.stringify(codes))
}

const { Header, Sider, Content } = Layout

function initialVisible() {
  return Object.fromEntries(LAYERS.map((layer) => [layer.id, DEFAULT_VISIBLE.includes(layer.id)]))
}

export default function App() {
  const [visible, setVisible] = useState<Record<string, boolean>>(initialVisible)
  const [filters, setFilters] = useState<Record<string, string[] | undefined>>({})
  const [counts, setCounts] = useState<LayerCount[]>([])
  const [regions, setRegions] = useState<RegionOption[]>([])
  const [regionCode, setRegionCode] = useState('anhui')
  const [mapRegions, setMapRegions] = useState<string[]>(loadMapRegions)
  const mapRegionsRef = useRef(mapRegions)
  mapRegionsRef.current = mapRegions
  const [tileRevision, setTileRevision] = useState(0)
  const [bounds, setBounds] = useState<[[number, number], [number, number]] | null>(null)
  const [boundsToken, setBoundsToken] = useState(0)
  const [sources, setSources] = useState<SourceOption[]>([])
  const [sourceId, setSourceId] = useState(loadSourceId)
  const [probe, setProbe] = useState<SourceProbe | null>(null)
  const [probing, setProbing] = useState(false)
  const [job, setJob] = useState<JobView | null>(null)
  const [logs, setLogs] = useState<LogView[]>([])
  const [starting, setStarting] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [chatting, setChatting] = useState(false)
  const [modelOpen, setModelOpen] = useState(false)
  const [markers, setMarkers] = useState<MarkerPoint[]>([])
  const [center, setCenter] = useState<[number, number]>([117.28, 31.86])
  const [zoom, setZoom] = useState(6.4)
  const [flyToken, setFlyToken] = useState(0)
  const [bbox, setBbox] = useState<[number, number, number, number]>([114.88, 29.39, 119.65, 34.65])
  const [themeId, setThemeId] = useState<ThemeId>(() => loadMapStyle().themeId)
  const [styleOverrides, setStyleOverrides] = useState(() => loadMapStyle().overrides)
  const mapStyle = useMemo(() => resolveStyle(themeId, styleOverrides), [themeId, styleOverrides])
  const swatches = useMemo(
    () => Object.fromEntries(LAYERS.map((layer) => [layer.id, swatchOf(mapStyle, layer.id)])),
    [mapStyle]
  )

  const refreshLayers = useCallback(async (codes: string[]) => {
    if (codes.length === 0) {
      setCounts([])
      return
    }
    const regionsParam = [...codes].sort().join(',')
    setCounts(await api<LayerCount[]>(`/api/geo/layers?regions=${encodeURIComponent(regionsParam)}`))
  }, [])

  const refreshRegions = useCallback(async () => {
    const list = await api<RegionOption[]>('/api/ingest/regions')
    setRegions(list)
    return list
  }, [])

  const refreshSources = useCallback(async (code: string) => {
    setProbe(null)
    const list = await api<SourceOption[]>(`/api/ingest/sources?region=${encodeURIComponent(code)}`)
    setSources(list)
    setSourceId((current) => list.some((item) => item.id === current) ? current : (list[0]?.id ?? current))
    return list
  }, [])

  const refreshJob = useCallback(async (code: string) => {
    const latest = await api<JobView | null>('/api/ingest/jobs/latest')
    const matched = latest?.status === 'RUNNING'
      ? latest
      : (await api<JobView[]>('/api/ingest/jobs?limit=40')).find((item) => item.regionCode === code) ?? null
    setJob(matched)
    setLogs(matched ? await api<LogView[]>(`/api/ingest/jobs/${matched.id}/logs?afterId=0`) : [])
    return latest
  }, [])

  useEffect(() => {
    saveMapStyle(themeId, styleOverrides)
  }, [themeId, styleOverrides])

  function chooseTheme(id: ThemeId) {
    setThemeId(id)
    setStyleOverrides(loadThemeOverrides(id))
  }

  useEffect(() => {
    refreshRegions().catch((error: Error) => message.error(error.message))
  }, [refreshRegions])

  useEffect(() => {
    if (regions.length === 0) {
      return
    }
    setMapRegions((current) => {
      const known = current.filter((code) => regions.some((item) => item.code === code))
      const next = known.length > 0 ? known : ['anhui']
      if (next.join() === current.join()) {
        return current
      }
      rememberMapRegions(next)
      return next
    })
  }, [regions])

  useEffect(() => {
    refreshSources(regionCode).catch((error: Error) => message.error(error.message))
    refreshJob(regionCode).catch((error: Error) => message.error(error.message))
  }, [refreshSources, refreshJob, regionCode])

  useEffect(() => {
    refreshLayers(mapRegions).catch((error: Error) => message.error(error.message))
  }, [refreshLayers, mapRegions, tileRevision])

  useEffect(() => {
    if (job?.status !== 'RUNNING') {
      return
    }
    const timer = window.setInterval(() => {
      refreshJob(regionCode)
        .then((latest) => {
          if (!latest || latest.status === 'RUNNING') {
            return
          }
          refreshRegions().catch(() => undefined)
          setTileRevision((value) => value + 1)
          if (latest.status === 'SUCCESS' && latest.regionCode) {
            const finished = latest.regionCode
            const alreadyShown = mapRegionsRef.current.includes(finished)
            setMapRegions((current) => {
              if (current.includes(finished)) {
                return current
              }
              const next = [...current, finished]
              rememberMapRegions(next)
              return next
            })
            if (!alreadyShown) {
              locateRegion(finished)
            }
          }
        })
        .catch(() => undefined)
    }, 2000)
    return () => window.clearInterval(timer)
  }, [job?.status, regionCode, refreshJob, refreshRegions])

  function chooseRegion(code: string) {
    setRegionCode(code)
  }

  function locateRegion(code: string) {
    const region = regions.find((item) => item.code === code)
    if (!region) {
      return
    }
    setCenter([region.lon, region.lat])
    setZoom(region.zoom)
    setFlyToken((value) => value + 1)
  }

  function changeMapRegions(next: string[]) {
    const added = next.filter((code) => !mapRegions.includes(code))
    setMapRegions(next)
    rememberMapRegions(next)
    if (added.length === 1) {
      locateRegion(added[0])
    }
  }

  function fitMapRegions() {
    const picked = regions.filter((item) => mapRegions.includes(item.code))
    if (picked.length === 0) {
      return
    }
    let minLon = 180
    let minLat = 90
    let maxLon = -180
    let maxLat = -90
    for (const item of picked) {
      const span = 360 / (2 ** item.zoom) * 0.9
      minLon = Math.min(minLon, item.lon - span / 2)
      maxLon = Math.max(maxLon, item.lon + span / 2)
      minLat = Math.min(minLat, item.lat - span / 2)
      maxLat = Math.max(maxLat, item.lat + span / 2)
    }
    setBounds([[minLon, minLat], [maxLon, maxLat]])
    setBoundsToken((value) => value + 1)
  }

  function chooseSource(id: string) {
    setSourceId(id)
    setProbe(null)
    localStorage.setItem(SOURCE_KEY, id)
  }

  async function testSource() {
    setProbing(true)
    try {
      const result = await api<SourceProbe>('/api/ingest/sources/test', {
        method: 'POST',
        body: JSON.stringify({ regionCode, sourceId })
      })
      setProbe(result)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '连接测试失败')
    } finally {
      setProbing(false)
    }
  }

  async function startJob() {
    const region = regions.find((item) => item.code === regionCode)
    setStarting(true)
    try {
      const created = await api<JobView>('/api/ingest/jobs', {
        method: 'POST',
        body: JSON.stringify({ regionCode, sourceId })
      })
      setJob(created)
      const sourceName = sources.find((item) => item.id === sourceId)?.name ?? ''
      message.success(`已开始从${sourceName}下载${region?.name ?? ''}数据`)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '启动失败')
    } finally {
      setStarting(false)
    }
  }

  function applyActions(actions: MapAction[]) {
    setVisible((current) => {
      const next = { ...current }
      for (const action of actions) {
        if (action.type === 'show_layers') {
          action.layers?.forEach((layer) => { next[layer] = true })
        }
        if (action.type === 'hide_layers') {
          action.layers?.forEach((layer) => { next[layer] = false })
        }
        if (action.type === 'only_layers') {
          LAYERS.forEach((layer) => { next[layer.id] = action.layers?.includes(layer.id) ?? false })
        }
      }
      return next
    })
    setFilters((current) => {
      const next = { ...current }
      for (const action of actions) {
        if (action.type === 'only_layers') {
          LAYERS.forEach((layer) => { next[layer.id] = undefined })
        }
        if ((action.type === 'show_layers' || action.type === 'only_layers') && action.filters) {
          Object.entries(action.filters).forEach(([layer, subtypes]) => { next[layer] = subtypes })
        }
        if (action.type === 'show_layers' && action.layers && !action.filters) {
          action.layers.forEach((layer) => { next[layer] = undefined })
        }
      }
      return next
    })
    for (const action of actions) {
      if (action.type === 'fly_to' && action.center && action.center.length >= 2) {
        setCenter([action.center[0], action.center[1]])
        setZoom(action.zoom ?? 11)
        setFlyToken((value) => value + 1)
      }
      if (action.type === 'markers') {
        setMarkers(action.points ?? [])
      }
      if (action.type === 'clear_markers') {
        setMarkers([])
      }
      if (action.type === 'set_style' && action.label === 'reset') {
        resetStoredStyles()
        setThemeId('light')
        setStyleOverrides(emptyOverrides())
      } else if (action.type === 'set_style' && isThemeId(action.label)) {
        const nextTheme = action.label
        setThemeId(nextTheme)
        setStyleOverrides(loadThemeOverrides(nextTheme))
      }
      if (action.type === 'paint_layer' && action.label && action.layers && action.layers.length > 0) {
        const layers = action.layers
        const color = action.label
        const actionFilters = action.filters
        setStyleOverrides((current) => paintLayers(current, layers, color, actionFilters))
      }
    }
  }

  async function send(text: string) {
    const history = [...messages, { role: 'user' as const, content: text }]
    setMessages(history)
    setChatting(true)
    try {
      const result = await api<ChatResponse>('/api/ai/chat', {
        method: 'POST',
        body: JSON.stringify({
          messages: history.map((item) => ({ role: item.role, content: item.content })),
          map: {
            center,
            zoom,
            bbox,
            visibleLayers: Object.keys(visible).filter((key) => visible[key]),
            regionCode: mapRegions[0] ?? regionCode,
            regionCodes: mapRegions
          }
        })
      })
      applyActions(result.actions ?? [])
      setMessages([...history, { role: 'assistant', content: result.reply, tables: result.tables }])
    } catch (error) {
      setMessages([...history, { role: 'assistant', content: error instanceof Error ? error.message : '对话失败' }])
    } finally {
      setChatting(false)
    }
  }

  const mapLabel = mapRegions.length === 0
    ? '未选择省份'
    : mapRegions.map((code) => regions.find((item) => item.code === code)?.name ?? code).join('、')
  const runningLabel = useMemo(
    () => job?.status === 'RUNNING' ? `${job.message ?? '正在入库'} · 地图仍显示 ${mapLabel}` : mapLabel,
    [job, mapLabel]
  )

  return (
    <Layout className="app">
      <Header className="topbar">
        <div className="brand">AI Open Map</div>
        <div className="top-status">{runningLabel}</div>
        <Button onClick={() => setModelOpen(true)}>模型配置</Button>
      </Header>
      <Layout className="body">
        <Sider width={320} theme="light" className="side">
          <StylePanel
            themeId={themeId}
            overrides={styleOverrides}
            resolvedTokens={mapStyle.tokens}
            resolvedOpacity={mapStyle.numbers}
            widthScale={mapStyle.widthScale}
            labels={mapStyle.labels}
            basemap={mapStyle.basemap}
            onTheme={chooseTheme}
            onOverrides={setStyleOverrides}
          />
          <LayerPanel counts={counts} visible={visible} swatches={swatches} onToggle={(layer, checked) => {
            setVisible((current) => ({ ...current, [layer]: checked }))
            if (checked) {
              setFilters((current) => ({ ...current, [layer]: undefined }))
            }
          }} />
          <JobPanel
            regions={regions}
            regionCode={regionCode}
            mapRegions={mapRegions}
            sources={sources}
            sourceId={sourceId}
            probe={probe}
            probing={probing}
            job={job}
            logs={logs}
            loading={starting}
            onRegionChange={chooseRegion}
            onMapRegionsChange={changeMapRegions}
            onFit={fitMapRegions}
            onLocate={() => locateRegion(regionCode)}
            onSourceChange={chooseSource}
            onTest={testSource}
            onStart={startJob}
          />
        </Sider>
        <Content className="map-content">
          <MapView
            visible={visible}
            filters={filters}
            markers={markers}
            regions={mapRegions}
            regionNames={Object.fromEntries(regions.map((item) => [item.code, item.name]))}
            tileRevision={tileRevision}
            flyToken={flyToken}
            bounds={bounds}
            boundsToken={boundsToken}
            center={center}
            zoom={zoom}
            style={mapStyle}
            onBounds={(bbox) => setBbox(bbox)}
          />
        </Content>
        <Sider width={380} theme="light" className="side">
          <ChatPanel messages={messages} loading={chatting} onSend={send} />
        </Sider>
      </Layout>
      <ModelModal open={modelOpen} onClose={() => setModelOpen(false)} />
    </Layout>
  )
}
