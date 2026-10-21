import { useCallback, useEffect, useMemo, useState } from 'react'
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

function regionsQuery(codes: string[]) {
  const regions = codes.filter(Boolean)
  if (regions.length === 0) {
    return 'region=anhui'
  }
  if (regions.length === 1) {
    return `region=${encodeURIComponent(regions[0])}`
  }
  return `regions=${encodeURIComponent(regions.join(','))}`
}

function loadSourceId() {
  const saved = localStorage.getItem(SOURCE_KEY)
  return saved === 'geofabrik' || saved === 'osm-france' ? saved : 'osm-france'
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

  const displayRegions = useMemo(() => {
    const loaded = regions.filter((item) => item.featureCount > 0).map((item) => item.code)
    return loaded.length > 0 ? loaded : [regionCode]
  }, [regions, regionCode])

  const refreshLayers = useCallback(async (codes: string[]) => {
    const query = regionsQuery(codes)
    setCounts(await api<LayerCount[]>(`/api/geo/layers?${query}`))
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
    refreshSources(regionCode).catch((error: Error) => message.error(error.message))
    refreshLayers(displayRegions).catch((error: Error) => message.error(error.message))
    refreshJob(regionCode).catch((error: Error) => message.error(error.message))
  }, [displayRegions, refreshLayers, refreshRegions, refreshSources, refreshJob, regionCode])

  useEffect(() => {
    if (job?.status !== 'RUNNING') {
      return
    }
    const timer = window.setInterval(() => {
      refreshJob(regionCode)
        .then((latest) => {
          if (latest && latest.status !== 'RUNNING') {
            refreshRegions()
              .then((list) => {
                const loaded = list.filter((item) => item.featureCount > 0).map((item) => item.code)
                return refreshLayers(loaded.length > 0 ? loaded : [regionCode])
              })
              .catch(() => undefined)
          }
        })
        .catch(() => undefined)
    }, 2000)
    return () => window.clearInterval(timer)
  }, [displayRegions, job?.status, regionCode, refreshJob, refreshLayers, refreshRegions])

  function showRegion(code: string) {
    const region = regions.find((item) => item.code === code)
    setRegionCode(code)
    setMarkers([])
    if (!region) {
      return
    }
    setCenter([region.lon, region.lat])
    setZoom(region.zoom)
    setFlyToken((value) => value + 1)
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
          map: { center, zoom, bbox, visibleLayers: Object.keys(visible).filter((key) => visible[key]), regionCode }
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

  const regionName = regions.find((item) => item.code === regionCode)?.name ?? '安徽省'
  const mapRegionLabel = useMemo(() => {
    if (displayRegions.length <= 1) {
      return regions.find((item) => item.code === displayRegions[0])?.name ?? regionName
    }
    return `地图展示 ${displayRegions.length} 个省`
  }, [displayRegions, regionName, regions])
  const runningLabel = useMemo(() => {
    if (job?.status === 'RUNNING') {
      return `${job.message}（其他省切片仍可用）`
    }
    return mapRegionLabel
  }, [job, mapRegionLabel])

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
            sources={sources}
            sourceId={sourceId}
            probe={probe}
            probing={probing}
            job={job}
            logs={logs}
            loading={starting}
            onRegionChange={showRegion}
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
            regions={displayRegions}
            flyToken={flyToken}
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
