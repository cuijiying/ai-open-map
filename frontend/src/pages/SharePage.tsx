import { useEffect, useMemo, useState } from 'react'
import { Button, Input, message } from 'antd'
import { useParams } from 'react-router-dom'
import { api } from '../api'
import MapView from '../components/MapView'
import { ProjectState } from '../components/ProjectMenu'
import { emptyOverrides, isThemeId, resolveStyle } from '../mapStyle'
import { CHINA_CENTER, CHINA_ZOOM } from '../store/mapSession'

interface OpenedProject {
  name: string
  state: string
}

export default function SharePage() {
  const { token = '' } = useParams()
  const [password, setPassword] = useState('')
  const [asking, setAsking] = useState(false)
  const [project, setProject] = useState<OpenedProject | null>(null)
  const state = useMemo(() => (project ? JSON.parse(project.state) as ProjectState : null), [project])
  const themeId = isThemeId(state?.themeId) ? state.themeId : 'light'
  const style = useMemo(
    () => resolveStyle(themeId, state?.styleOverrides ?? emptyOverrides()),
    [state?.styleOverrides, themeId]
  )

  async function open(nextPassword: string) {
    try {
      const opened = await api<OpenedProject>(`/api/v1/shares/${encodeURIComponent(token)}/open`, {
        method: 'POST',
        body: JSON.stringify({ password: nextPassword || undefined })
      })
      setProject(opened)
      setAsking(false)
    } catch (error) {
      const text = error instanceof Error ? error.message : '无法打开分享'
      if (text.includes('访问密码')) {
        setAsking(true)
        if (nextPassword) {
          message.error(text)
        }
        return
      }
      message.error(text)
    }
  }

  useEffect(() => {
    setProject(null)
    setAsking(false)
    open('').catch(() => undefined)
  }, [token])

  return (
    <div className="stage share-stage">
      <header className="command">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span>GeoMind</span>
        </div>
        <div className="status-pill">{project ? `只读 · ${project.name}` : '只读分享'}</div>
      </header>
      <div className="map-content">
        {asking && !project ? (
          <div className="share-lock">
            <form onSubmit={(event) => { event.preventDefault(); open(password) }}>
              <h2>这个分享需要密码</h2>
              <Input.Password value={password} onChange={(event) => setPassword(event.target.value)} placeholder="访问密码" />
              <Button type="primary" htmlType="submit">打开</Button>
            </form>
          </div>
        ) : null}
        {state ? (
          <MapView
            visible={state.visible ?? {}}
            filters={state.filters ?? {}}
            markers={state.markers ?? []}
            regions={state.regions ?? []}
            regionNames={{}}
            tileRevision={0}
            flyToken={0}
            bounds={null}
            boundsToken={0}
            center={state.center ?? CHINA_CENTER}
            zoom={state.zoom ?? CHINA_ZOOM}
            style={style}
            onBounds={() => undefined}
          />
        ) : null}
      </div>
    </div>
  )
}
