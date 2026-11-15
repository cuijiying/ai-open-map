import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { BgColorsOutlined, DatabaseOutlined, MessageOutlined, AppstoreOutlined } from '@ant-design/icons'
import { Button, message } from 'antd'
import { api, clearSession, loadSession } from '../api'
import ChatPanel from '../components/ChatPanel'
import JobPanel from '../components/JobPanel'
import LayerPanel from '../components/LayerPanel'
import MapView from '../components/MapView'
import ModelModal from '../components/ModelModal'
import ProjectMenu, { ProjectState } from '../components/ProjectMenu'
import StylePanel from '../components/StylePanel'
import { DEFAULT_VISIBLE, LAYERS } from '../layers'
import { emptyOverrides, isThemeId, loadMapStyle, loadThemeOverrides, paintLayers, resetStoredStyles, resolveStyle, saveMapStyle, swatchOf, ThemeId } from '../mapStyle'
import { useMapSession } from '../store/mapSession'
import { ChatMessage, ChatResponse, JobView, LayerCount, LogView, MapAction, MarkerPoint, PrebuildStatus, RegionOption, SourceOption, SourceProbe } from '../types'

const SOURCE_KEY = 'aomap.downloadSource'

function loadSourceId() {
  const saved = localStorage.getItem(SOURCE_KEY)
  return saved === 'geofabrik' || saved === 'osm-france' ? saved : 'osm-france'
}

function initialVisible() {
  return Object.fromEntries(LAYERS.map((layer) => [layer.id, DEFAULT_VISIBLE.includes(layer.id)]))
}

export default function Workbench() {
  const mapRegions = useMapSession((state) => state.mapRegions)
  const setMapRegions = useMapSession((state) => state.setMapRegions)
  const center = useMapSession((state) => state.center)
  const zoom = useMapSession((state) => state.zoom)
  const flyToken = useMapSession((state) => state.flyToken)
  const bbox = useMapSession((state) => state.bbox)
  const bounds = useMapSession((state) => state.bounds)
  const boundsToken = useMapSession((state) => state.boundsToken)
  const flyTo = useMapSession((state) => state.flyTo)
  const setBbox = useMapSession((state) => state.setBbox)
  const fitBounds = useMapSession((state) => state.fitBounds)
  const [visible, setVisible] = useState<Record<string, boolean>>(initialVisible)
  const [filters, setFilters] = useState<Record<string, string[] | undefined>>({})
  const [counts, setCounts] = useState<LayerCount[]>([])
  const [regions, setRegions] = useState<RegionOption[]>([])
  const [regionCode, setRegionCode] = useState('')
  const mapRegionsRef = useRef(mapRegions)
  mapRegionsRef.current = mapRegions
  const regionsRef = useRef(regions)
  regionsRef.current = regions
  const runningIdsRef = useRef<number[]>([])
  const [tileRevision, setTileRevision] = useState(0)
  const [sources, setSources] = useState<SourceOption[]>([])
  const [sourceId, setSourceId] = useState(loadSourceId)
  const [probe, setProbe] = useState<SourceProbe | null>(null)
  const [probing, setProbing] = useState(false)
  const [job, setJob] = useState<JobView | null>(null)
  const [logs, setLogs] = useState<LogView[]>([])
  const [starting, setStarting] = useState(false)
  const [activeCount, setActiveCount] = useState(0)
  const [prebuild, setPrebuild] = useState<PrebuildStatus | null>(null)
  const [prebuilding, setPrebuilding] = useState(false)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [chatting, setChatting] = useState(false)
  const [modelOpen, setModelOpen] = useState(false)
  const [dock, setDock] = useState<'layers' | 'data' | 'style'>('layers')
  const [dockOpen, setDockOpen] = useState(true)
  const [assistantOpen, setAssistantOpen] = useState(true)
  const [markers, setMarkers] = useState<MarkerPoint[]>([])
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
    setCounts(await api<LayerCount[]>(`/api/v1/geo/layers?regions=${encodeURIComponent(regionsParam)}`))
  }, [])

  const refreshRegions = useCallback(async () => {
    const list = await api<RegionOption[]>('/api/v1/ingest/regions')
    setRegions(list)
    return list
  }, [])

  const refreshSources = useCallback(async (code: string) => {
    setProbe(null)
    const list = await api<SourceOption[]>(`/api/v1/ingest/sources?region=${encodeURIComponent(code)}`)
    setSources(list)
    setSourceId((current) => list.some((item) => item.id === current) ? current : (list[0]?.id ?? current))
    return list
  }, [])

  const refreshJob = useCallback(async (code: string) => {
    const list = await api<JobView[]>('/api/v1/ingest/jobs?limit=40')
    const running = list.filter((item) => item.status === 'QUEUED' || item.status === 'RUNNING')
    const matched = running.find((item) => item.regionCode === code)
      ?? list.find((item) => item.regionCode === code)
      ?? null
    setJob(matched)
    setLogs(matched ? await api<LogView[]>(`/api/v1/ingest/jobs/${matched.id}/logs?afterId=0`) : [])
    const previous = runningIdsRef.current
    const finished = previous
      .map((id) => list.find((item) => item.id === id))
      .filter((item): item is JobView => item != null && item.status !== 'QUEUED' && item.status !== 'RUNNING')
    runningIdsRef.current = running.map((item) => item.id)
    setActiveCount(running.length)
    const succeeded = finished.filter((item) => item.status === 'SUCCESS' && item.regionCode)
    if (succeeded.length > 0) {
      refreshRegions().catch(() => undefined)
      setTileRevision((value) => value + 1)
      const unseen = succeeded.filter((item) => !mapRegionsRef.current.includes(item.regionCode))
      if (unseen.length > 0) {
        const current = useMapSession.getState().mapRegions
        const extra = unseen.map((item) => item.regionCode).filter((item) => !current.includes(item))
        if (extra.length > 0) {
          setMapRegions([...current, ...extra])
        }
        const region = regionsRef.current.find((item) => item.code === unseen[unseen.length - 1].regionCode)
        if (region) {
          flyTo([region.lon, region.lat], region.zoom)
        }
      }
    }
    return running.length
  }, [flyTo, refreshRegions, setMapRegions])

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
    const current = useMapSession.getState().mapRegions
    const known = current.filter((code) => regions.some((item) => item.code === code))
    if (known.join() !== current.join()) {
      setMapRegions(known)
    }
  }, [regions, setMapRegions])

  useEffect(() => {
    if (!regionCode) {
      setSources([])
      setJob(null)
      setLogs([])
      return
    }
    refreshSources(regionCode).catch((error: Error) => message.error(error.message))
    refreshJob(regionCode).catch((error: Error) => message.error(error.message))
  }, [refreshSources, refreshJob, regionCode])

  useEffect(() => {
    refreshLayers(mapRegions).catch((error: Error) => message.error(error.message))
  }, [refreshLayers, mapRegions, tileRevision])

  useEffect(() => {
    if (activeCount === 0 && job?.status !== 'QUEUED' && job?.status !== 'RUNNING') {
      return
    }
    const timer = window.setInterval(() => {
      refreshJob(regionCode).catch(() => undefined)
    }, 2000)
    return () => window.clearInterval(timer)
  }, [activeCount, job?.status, regionCode, refreshJob])

  const refreshPrebuild = useCallback(async () => {
    const status = await api<PrebuildStatus>('/api/v1/geo/tiles/prebuild')
    setPrebuild(status)
    return status
  }, [])

  useEffect(() => {
    refreshPrebuild().catch(() => undefined)
  }, [refreshPrebuild])

  useEffect(() => {
    if (prebuild?.state !== 'RUNNING') {
      return
    }
    const timer = window.setInterval(() => {
      refreshPrebuild().catch(() => undefined)
    }, 2000)
    return () => window.clearInterval(timer)
  }, [prebuild?.state, refreshPrebuild])

  function chooseRegion(code: string) {
    setRegionCode(code)
  }

  function locateRegion(code: string) {
    const region = regions.find((item) => item.code === code)
    if (!region) {
      return
    }
    flyTo([region.lon, region.lat], region.zoom)
  }

  function includeRegion(code: string) {
    const region = regions.find((item) => item.code === code)
    if (!region) {
      return
    }
    const current = useMapSession.getState().mapRegions
    const added = !current.includes(code)
    if (added) {
      setMapRegions([...current, code])
    }
    if (added && region.featureCount <= 0) {
      message.info(`${region.name}尚未入库，地图上还没有该省的矢量数据`)
    }
  }

  function changeMapRegions(next: string[]) {
    const added = next.filter((code) => !mapRegions.includes(code))
    setMapRegions(next)
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
    fitBounds([[minLon, minLat], [maxLon, maxLat]])
  }

  function chooseSource(id: string) {
    setSourceId(id)
    setProbe(null)
    localStorage.setItem(SOURCE_KEY, id)
  }

  async function testSource() {
    setProbing(true)
    try {
      const result = await api<SourceProbe>('/api/v1/ingest/sources/test', {
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

  async function cancelJob(id: number) {
    try {
      const updated = await api<JobView>(`/api/v1/ingest/jobs/${id}/cancel`, { method: 'POST' })
      setJob(updated)
      message.success('已取消')
      await refreshJob(regionCode)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '取消失败')
    }
  }

  async function retryJob(id: number) {
    setStarting(true)
    try {
      const created = await api<JobView>(`/api/v1/ingest/jobs/${id}/retry`, { method: 'POST' })
      setJob(created)
      setActiveCount((count) => Math.max(count, 1))
      if (!runningIdsRef.current.includes(created.id)) {
        runningIdsRef.current = [...runningIdsRef.current, created.id]
      }
      message.success('已重新加入下载队列')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '重试失败')
    } finally {
      setStarting(false)
    }
  }

  async function startJob() {
    const region = regions.find((item) => item.code === regionCode)
    setStarting(true)
    try {
      const created = await api<JobView>('/api/v1/ingest/jobs', {
        method: 'POST',
        body: JSON.stringify({ regionCode, sourceId })
      })
      setJob(created)
      setActiveCount((count) => Math.max(count, 1))
      if (!runningIdsRef.current.includes(created.id)) {
        runningIdsRef.current = [...runningIdsRef.current, created.id]
      }
      const sourceName = sources.find((item) => item.id === sourceId)?.name ?? ''
      message.success(`已把${region?.name ?? ''}加入下载队列${sourceName ? `（${sourceName}）` : ''}`)
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
        if (action.regionCode) {
          includeRegion(action.regionCode)
        }
        flyTo([action.center[0], action.center[1]], action.zoom ?? 11)
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
      const result = await api<ChatResponse>('/api/v1/ai/chat', {
        method: 'POST',
        body: JSON.stringify({
          messages: history
            .filter((item) => item.role !== 'error')
            .map((item) => ({ role: item.role, content: item.content })),
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
      setMessages([...history, { role: 'assistant', content: result.reply, tables: result.tables, source: result.source }])
    } catch (error) {
      setMessages([...history, { role: 'error', content: error instanceof Error ? error.message : '对话失败' }])
    } finally {
      setChatting(false)
    }
  }

  const mapNotice = useMemo(() => {
    if (regions.length === 0 || mapRegions.length === 0) {
      return undefined
    }
    const selected = regions.filter((item) => mapRegions.includes(item.code))
    if (selected.length === 0 || selected.some((item) => item.featureCount > 0)) {
      return undefined
    }
    return '当前勾选的省份都还没有入库，地图上暂时没有矢量数据。下载完成后会出现。'
  }, [mapRegions, regions])

  const mapLabel = mapRegions.length === 0
    ? '未选择省份'
    : mapRegions.map((code) => regions.find((item) => item.code === code)?.name ?? code).join('、')
  async function startPrebuild() {
    setPrebuilding(true)
    try {
      const status = await api<PrebuildStatus>('/api/v1/geo/tiles/prebuild', {
        method: 'POST',
        body: JSON.stringify({ maxZoom: 6 })
      })
      setPrebuild(status)
      message.success('已开始预生成全国低缩放瓦片')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '预生成失败')
    } finally {
      setPrebuilding(false)
    }
  }

  function applyProject(state: ProjectState) {
    setMapRegions(state.regions ?? [])
    if (state.center && state.center.length === 2 && typeof state.zoom === 'number') {
      flyTo(state.center, state.zoom)
    }
    setVisible(state.visible ?? initialVisible())
    setFilters(state.filters ?? {})
    if (isThemeId(state.themeId)) {
      setThemeId(state.themeId)
      setStyleOverrides(state.styleOverrides ?? loadThemeOverrides(state.themeId))
    }
    setMarkers(state.markers ?? [])
    message.success('已打开项目')
  }

  const runningLabel = useMemo(() => {
    if (job?.status === 'QUEUED' || job?.status === 'RUNNING') {
      return `${job.message ?? '正在入库'} · 地图仍显示 ${mapLabel}`
    }
    if (activeCount > 0) {
      return `${activeCount} 个省正在排队或入库 · 地图仍显示 ${mapLabel}`
    }
    return mapLabel
  }, [activeCount, job, mapLabel])

  function chooseDock(next: 'layers' | 'data' | 'style') {
    if (dockOpen && dock === next) {
      setDockOpen(false)
      return
    }
    setDock(next)
    setDockOpen(true)
  }

  const session = loadSession()

  return (
    <div className={`stage${dockOpen ? ' dock-open' : ''}${assistantOpen ? ' assistant-open' : ''}`}>
      <div className="map-content">
        <MapView
          visible={visible}
          filters={filters}
          markers={markers}
          regions={mapRegions}
          regionNames={Object.fromEntries(regions.map((item) => [item.code, item.name]))}
          notice={mapNotice}
          tileRevision={tileRevision}
          flyToken={flyToken}
          bounds={bounds}
          boundsToken={boundsToken}
          center={center}
          zoom={zoom}
          style={mapStyle}
          onBounds={(bbox) => setBbox(bbox)}
        />
      </div>
      <header className="command">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span>GeoMind</span>
        </div>
        <div className="status-pill" title={runningLabel}>{runningLabel}</div>
        <div className="command-actions">
          <ProjectMenu
            snapshot={() => ({
              regions: mapRegions,
              center,
              zoom,
              visible,
              filters,
              themeId,
              styleOverrides,
              markers
            })}
            onLoad={(_name, state) => applyProject(state)}
          />
          <Button className="cmd" onClick={() => setModelOpen(true)}>模型</Button>
          <span className="user-chip">{session?.name}</span>
          <Button className="cmd" onClick={() => { clearSession(); window.location.assign('/login') }}>退出</Button>
        </div>
      </header>
      <nav className="rail" aria-label="工作台">
        <button type="button" className={dockOpen && dock === 'layers' ? 'rail-btn on' : 'rail-btn'} onClick={() => chooseDock('layers')}>
          <AppstoreOutlined />
          <span>图层</span>
        </button>
        <button type="button" className={dockOpen && dock === 'data' ? 'rail-btn on' : 'rail-btn'} onClick={() => chooseDock('data')}>
          <DatabaseOutlined />
          <span>数据</span>
        </button>
        <button type="button" className={dockOpen && dock === 'style' ? 'rail-btn on' : 'rail-btn'} onClick={() => chooseDock('style')}>
          <BgColorsOutlined />
          <span>风格</span>
        </button>
      </nav>
      {dockOpen ? (
        <aside className="sheet">
          {dock === 'layers' ? (
            <LayerPanel counts={counts} visible={visible} swatches={swatches} onToggle={(layer, checked) => {
              setVisible((current) => ({ ...current, [layer]: checked }))
              if (checked) {
                setFilters((current) => ({ ...current, [layer]: undefined }))
              }
            }} />
          ) : null}
          {dock === 'data' ? (
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
              activeCount={activeCount}
              prebuild={prebuild}
              prebuilding={prebuilding}
              onPrebuild={startPrebuild}
              onRegionChange={chooseRegion}
              onMapRegionsChange={changeMapRegions}
              onFit={fitMapRegions}
              onLocate={() => locateRegion(regionCode)}
              onSourceChange={chooseSource}
              onTest={testSource}
              onStart={startJob}
              onCancel={cancelJob}
              onRetry={retryJob}
            />
          ) : null}
          {dock === 'style' ? (
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
          ) : null}
        </aside>
      ) : null}
      {assistantOpen ? (
        <aside className="assistant">
          <ChatPanel messages={messages} loading={chatting} onSend={send} onClose={() => setAssistantOpen(false)} />
        </aside>
      ) : (
        <button type="button" className="assistant-launcher" onClick={() => setAssistantOpen(true)}>
          <MessageOutlined />
          问地图
        </button>
      )}
      <ModelModal open={modelOpen} onClose={() => setModelOpen(false)} />
    </div>
  )
}
