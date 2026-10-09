import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Layout, message } from 'antd'
import { api } from './api'
import ChatPanel from './components/ChatPanel'
import JobPanel from './components/JobPanel'
import LayerPanel from './components/LayerPanel'
import MapView from './components/MapView'
import ModelModal from './components/ModelModal'
import { DEFAULT_VISIBLE, LAYERS } from './layers'
import { ChatMessage, ChatResponse, JobView, LayerCount, LogView, MapAction, MarkerPoint, RegionOption } from './types'

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

  const refreshLayers = useCallback(async (code: string) => {
    setCounts(await api<LayerCount[]>(`/api/geo/layers?region=${encodeURIComponent(code)}`))
  }, [])

  const refreshRegions = useCallback(async () => {
    const list = await api<RegionOption[]>('/api/ingest/regions')
    setRegions(list)
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
    refreshRegions().catch((error: Error) => message.error(error.message))
    refreshLayers(regionCode).catch((error: Error) => message.error(error.message))
    refreshJob(regionCode).catch((error: Error) => message.error(error.message))
  }, [refreshLayers, refreshRegions, refreshJob, regionCode])

  useEffect(() => {
    if (job?.status !== 'RUNNING') {
      return
    }
    const timer = window.setInterval(() => {
      refreshJob(regionCode)
        .then((latest) => {
          if (latest && latest.status !== 'RUNNING') {
            refreshRegions().catch(() => undefined)
            refreshLayers(regionCode).catch(() => undefined)
          }
        })
        .catch(() => undefined)
    }, 2000)
    return () => window.clearInterval(timer)
  }, [job?.status, regionCode, refreshJob, refreshLayers, refreshRegions])

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

  async function startJob() {
    const region = regions.find((item) => item.code === regionCode)
    setStarting(true)
    try {
      const created = await api<JobView>('/api/ingest/jobs', {
        method: 'POST',
        body: JSON.stringify({ regionCode })
      })
      setJob(created)
      message.success(`已开始下载${region?.name ?? ''}数据`)
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
  const runningLabel = useMemo(() => job?.status === 'RUNNING' ? job.message : regionName, [job, regionName])

  return (
    <Layout className="app">
      <Header className="topbar">
        <div className="brand">AI Open Map</div>
        <div className="top-status">{runningLabel}</div>
        <Button onClick={() => setModelOpen(true)}>模型配置</Button>
      </Header>
      <Layout className="body">
        <Sider width={320} theme="light" className="side">
          <LayerPanel counts={counts} visible={visible} onToggle={(layer, checked) => {
            setVisible((current) => ({ ...current, [layer]: checked }))
            if (checked) {
              setFilters((current) => ({ ...current, [layer]: undefined }))
            }
          }} />
          <JobPanel
            regions={regions}
            regionCode={regionCode}
            job={job}
            logs={logs}
            loading={starting}
            onRegionChange={showRegion}
            onStart={startJob}
          />
        </Sider>
        <Content className="map-content">
          <MapView
            visible={visible}
            filters={filters}
            markers={markers}
            region={regionCode}
            flyToken={flyToken}
            center={center}
            zoom={zoom}
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
