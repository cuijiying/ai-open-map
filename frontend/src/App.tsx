import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Layout, message } from 'antd'
import { api } from './api'
import ChatPanel from './components/ChatPanel'
import JobPanel from './components/JobPanel'
import LayerPanel from './components/LayerPanel'
import MapView from './components/MapView'
import ModelModal from './components/ModelModal'
import { DEFAULT_VISIBLE, LAYERS } from './layers'
import { ChatMessage, ChatResponse, JobView, LayerCount, LogView, MapAction, MarkerPoint } from './types'

const { Header, Sider, Content } = Layout

function initialVisible() {
  return Object.fromEntries(LAYERS.map((layer) => [layer.id, DEFAULT_VISIBLE.includes(layer.id)]))
}

export default function App() {
  const [visible, setVisible] = useState<Record<string, boolean>>(initialVisible)
  const [filters, setFilters] = useState<Record<string, string[] | undefined>>({})
  const [counts, setCounts] = useState<LayerCount[]>([])
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

  const refreshLayers = useCallback(async () => {
    setCounts(await api<LayerCount[]>('/api/geo/layers'))
  }, [])

  const refreshJob = useCallback(async () => {
    const latest = await api<JobView | null>('/api/ingest/jobs/latest')
    setJob(latest)
    if (latest) {
      setLogs(await api<LogView[]>(`/api/ingest/jobs/${latest.id}/logs?afterId=0`))
    }
    return latest
  }, [])

  useEffect(() => {
    refreshLayers().catch((error: Error) => message.error(error.message))
    refreshJob().catch((error: Error) => message.error(error.message))
  }, [refreshLayers, refreshJob])

  useEffect(() => {
    if (job?.status !== 'RUNNING') {
      return
    }
    const timer = window.setInterval(() => {
      refreshJob()
        .then((latest) => {
          if (latest && latest.status !== 'RUNNING') {
            refreshLayers().catch(() => undefined)
          }
        })
        .catch(() => undefined)
    }, 2000)
    return () => window.clearInterval(timer)
  }, [job?.status, refreshJob, refreshLayers])

  async function startJob() {
    setStarting(true)
    try {
      const created = await api<JobView>('/api/ingest/jobs', {
        method: 'POST',
        body: JSON.stringify({ regionCode: 'anhui' })
      })
      setJob(created)
      message.success('已开始下载安徽省数据')
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
          map: { center, zoom, bbox, visibleLayers: Object.keys(visible).filter((key) => visible[key]) }
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

  const runningLabel = useMemo(() => job?.status === 'RUNNING' ? job.message : '安徽省', [job])

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
          <JobPanel job={job} logs={logs} loading={starting} onStart={startJob} />
        </Sider>
        <Content className="map-content">
          <MapView
            visible={visible}
            filters={filters}
            markers={markers}
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
