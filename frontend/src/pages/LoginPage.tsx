import { FormEvent, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Form, Input, Typography, message } from 'antd'
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
    <div className="login-page">
      <form className="login-card" onSubmit={submit}>
        <Typography.Title level={3}>AI Open Map</Typography.Title>
        <Typography.Paragraph type="secondary">
          {mode === 'login' ? '登录后使用地图、入库和对话。' : '注册后会创建一个个人空间。'}
        </Typography.Paragraph>
        <Form layout="vertical">
          {mode === 'register' ? (
            <Form.Item label="名称">
              <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="你的名字" />
            </Form.Item>
          ) : null}
          <Form.Item label="邮箱">
            <Input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com" />
          </Form.Item>
          <Form.Item label="密码">
            <Input.Password value={password} onChange={(event) => setPassword(event.target.value)} placeholder="至少 8 位" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            {mode === 'login' ? '登录' : '注册'}
          </Button>
          <Button type="link" block onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>
            {mode === 'login' ? '没有账号，去注册' : '已有账号，去登录'}
          </Button>
        </Form>
      </form>
    </div>
  )
}
