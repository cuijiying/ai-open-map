import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input, message } from 'antd'
import { api, saveSession, Session } from '../api'

interface AuthResponse {
  token: string
  profile: {
    userId: number
    orgId: number
    role: string
    name: string
    email: string
  }
}

export default function LoginPage() {
  const navigate = useNavigate()
  const [mode, setMode] = useState<'login' | 'register'>('login')
  const [email, setEmail] = useState('')
  const [name, setName] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    try {
      const path = mode === 'login' ? '/api/v1/auth/login' : '/api/v1/auth/register'
      const result = await api<AuthResponse>(path, {
        method: 'POST',
        body: JSON.stringify({ email, name, password })
      })
      const session: Session = { token: result.token, ...result.profile }
      saveSession(session)
      navigate('/')
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="gate">
      <section className="gate-hero">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true" />
          <span>GeoMind</span>
        </div>
        <h1>用一句话看懂一片区域</h1>
        <p>开放地图数据、矢量切片和空间查询放在同一张地图上。说出所在省份、要看的图层，或让它统计当前视野。</p>
        <ul>
          <li><strong>数据</strong><span>下载省级开放地图，入库期间已发布的省份仍然可看。</span></li>
          <li><strong>地图</strong><span>多个省叠在同一张图上，按道路、水系、建筑和地名分层。</span></li>
          <li><strong>助手</strong><span>用中文定位、过滤、改色，并给出数量、里程和面积。</span></li>
        </ul>
      </section>
      <section className="gate-form">
        <form onSubmit={submit}>
          <h2>{mode === 'login' ? '进入工作台' : '创建空间'}</h2>
          <p className="hint">{mode === 'login' ? '使用注册时的邮箱。' : '注册后会生成一个个人空间。'}</p>
          {mode === 'register' ? (
            <label>
              名称
              <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="你的名字" />
            </label>
          ) : null}
          <label>
            邮箱
            <Input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com" autoComplete="username" />
          </label>
          <label>
            密码
            <Input.Password value={password} onChange={(event) => setPassword(event.target.value)} placeholder="至少 8 位" autoComplete={mode === 'login' ? 'current-password' : 'new-password'} />
          </label>
          <Button type="primary" htmlType="submit" block loading={loading}>
            {mode === 'login' ? '登录' : '注册并进入'}
          </Button>
          <button type="button" className="text-link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? '没有账号，创建空间' : '已有账号，返回登录'}
          </button>
        </form>
      </section>
    </div>
  )
}
