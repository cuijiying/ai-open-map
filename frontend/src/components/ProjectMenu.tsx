import { useEffect, useState } from 'react'
import { Button, Form, Input, InputNumber, List, Modal, Space, message } from 'antd'
import { api, loadSession } from '../api'
import { StyleOverrides } from '../mapStyle'
import { MarkerPoint } from '../types'

export interface ProjectState {
  regions: string[]
  center: [number, number]
  zoom: number
  visible: Record<string, boolean>
  filters: Record<string, string[] | undefined>
  themeId: string
  styleOverrides?: StyleOverrides
  markers: MarkerPoint[]
}

interface ProjectView {
  id: number
  name: string
  state: string
  updatedAt?: string
}

interface ShareLink {
  token: string
  passwordRequired: boolean
}

export default function ProjectMenu({ snapshot, onLoad }: {
  snapshot: () => ProjectState
  onLoad: (name: string, state: ProjectState) => void
}) {
  const [open, setOpen] = useState(false)
  const [projects, setProjects] = useState<ProjectView[]>([])
  const [currentId, setCurrentId] = useState<number | null>(null)
  const [name, setName] = useState('未命名项目')
  const [shareOpen, setShareOpen] = useState(false)
  const role = loadSession()?.role
  const canWrite = role === 'OWNER' || role === 'EDITOR'

  useEffect(() => {
    if (!open) {
      return
    }
    api<ProjectView[]>('/api/v1/projects').then(setProjects).catch((error: Error) => message.error(error.message))
  }, [open])

  async function save() {
    const state = snapshot()
    try {
      const saved = currentId == null
        ? await api<ProjectView>('/api/v1/projects', { method: 'POST', body: JSON.stringify({ name, state }) })
        : await api<ProjectView>(`/api/v1/projects/${currentId}`, { method: 'PUT', body: JSON.stringify({ name, state }) })
      setCurrentId(saved.id)
      message.success('项目已保存')
      setProjects(await api<ProjectView[]>('/api/v1/projects'))
    } catch (error) {
      message.error(error instanceof Error ? error.message : '保存失败')
    }
  }

  function apply(project: ProjectView) {
    const state = JSON.parse(project.state) as ProjectState
    setCurrentId(project.id)
    setName(project.name)
    onLoad(project.name, state)
    setOpen(false)
  }

  async function remove(project: ProjectView) {
    try {
      await api(`/api/v1/projects/${project.id}`, { method: 'DELETE' })
      if (currentId === project.id) {
        setCurrentId(null)
      }
      setProjects(await api<ProjectView[]>('/api/v1/projects'))
    } catch (error) {
      message.error(error instanceof Error ? error.message : '删除失败')
    }
  }

  async function share(values: { days?: number; password?: string }) {
    if (currentId == null) {
      message.error('请先保存项目')
      return
    }
    try {
      const link = await api<ShareLink>(`/api/v1/projects/${currentId}/shares`, {
        method: 'POST',
        body: JSON.stringify({ days: values.days ?? 7, password: values.password || undefined })
      })
      const url = `${window.location.origin}/share/${link.token}`
      await navigator.clipboard.writeText(url)
      message.success('分享链接已复制')
      setShareOpen(false)
    } catch (error) {
      message.error(error instanceof Error ? error.message : '分享失败')
    }
  }

  return (
    <>
      <Button onClick={() => setOpen(true)}>项目</Button>
      <Modal title="地图项目" open={open} onCancel={() => setOpen(false)} footer={null} destroyOnClose>
        <Space.Compact style={{ width: '100%', marginBottom: 12 }}>
          <Input value={name} maxLength={128} onChange={(event) => setName(event.target.value)} disabled={!canWrite} />
          <Button type="primary" onClick={save} disabled={!canWrite}>保存</Button>
          <Button onClick={() => setShareOpen(true)} disabled={!canWrite || currentId == null}>分享</Button>
        </Space.Compact>
        <List
          dataSource={projects}
          locale={{ emptyText: '还没有保存过项目' }}
          renderItem={(project) => (
            <List.Item
              actions={[
                <Button key="open" type="link" onClick={() => apply(project)}>打开</Button>,
                <Button key="delete" type="link" danger onClick={() => remove(project)}>删除</Button>
              ]}
            >
              <List.Item.Meta title={project.name} description={project.updatedAt ? new Date(project.updatedAt).toLocaleString() : ''} />
            </List.Item>
          )}
        />
      </Modal>
      <Modal title="分享只读链接" open={shareOpen} onCancel={() => setShareOpen(false)} footer={null} destroyOnClose>
        <Form layout="vertical" onFinish={share} initialValues={{ days: 7 }}>
          <Form.Item name="days" label="有效天数">
            <InputNumber min={1} max={30} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="password" label="访问密码（可留空）">
            <Input.Password maxLength={72} />
          </Form.Item>
          <Button type="primary" htmlType="submit">生成并复制</Button>
        </Form>
      </Modal>
    </>
  )
}
