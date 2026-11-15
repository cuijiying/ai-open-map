import { Alert, Form, Input, InputNumber, Modal, Popconfirm, Select, Switch, Table, message } from 'antd'
import { useEffect, useState } from 'react'
import { api } from '../api'
import { ModelView } from '../types'

interface Props {
  open: boolean
  onClose: () => void
}

interface FormValue {
  id?: number
  name: string
  baseUrl: string
  apiKey?: string
  model: string
  protocol: string
  temperature: number
}

export default function ModelModal({ open, onClose }: Props) {
  const [models, setModels] = useState<ModelView[]>([])
  const [notice, setNotice] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null)
  const [testing, setTesting] = useState(false)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm<FormValue>()

  async function reload() {
    setModels(await api<ModelView[]>('/api/v1/ai/models'))
  }

  useEffect(() => {
    if (!open) {
      return
    }
    setNotice({ type: 'info', text: '地址填到 /v1。对话补全会请求 /chat/completions，Responses 会请求 /responses。' })
    form.setFieldsValue({
      id: undefined,
      name: '',
      baseUrl: '',
      model: '',
      protocol: 'chat',
      temperature: 0.2,
      apiKey: ''
    })
    reload().catch((error: Error) => setNotice({ type: 'error', text: error.message }))
  }, [open])

  async function save() {
    const value = await form.validateFields()
    setSaving(true)
    setNotice(null)
    try {
      if (value.id) {
        await api(`/api/v1/ai/models/${value.id}`, { method: 'PUT', body: JSON.stringify(value) })
      } else {
        await api('/api/v1/ai/models', { method: 'POST', body: JSON.stringify(value) })
      }
      form.setFieldValue('id', undefined)
      form.setFieldValue('apiKey', '')
      setNotice({ type: 'success', text: '已保存。启用后，内置指令之外的问题会交给这个模型。' })
      message.success('已保存')
      await reload()
    } catch (error) {
      setNotice({ type: 'error', text: error instanceof Error ? error.message : '保存失败' })
    } finally {
      setSaving(false)
    }
  }

  async function test() {
    const value = await form.validateFields()
    setTesting(true)
    setNotice(null)
    try {
      const result = await api<{ reply: string }>('/api/v1/ai/models/test', { method: 'POST', body: JSON.stringify(value) })
      setNotice({ type: 'success', text: `连接成功：${result.reply}` })
    } catch (error) {
      setNotice({ type: 'error', text: error instanceof Error ? error.message : '连接测试失败' })
    } finally {
      setTesting(false)
    }
  }

  return (
    <Modal
      title="模型配置"
      open={open}
      onCancel={onClose}
      onOk={save}
      okText="保存"
      confirmLoading={saving}
      width={760}
      className="model-modal"
    >
      {notice ? <Alert className="model-alert" type={notice.type} showIcon message={notice.text} /> : null}
      <Form form={form} layout="vertical">
        <Form.Item name="id" hidden><Input /></Form.Item>
        <Form.Item name="name" label="名称" rules={[{ required: true, message: '请填写名称' }]}><Input /></Form.Item>
        <Form.Item name="protocol" label="接口类型" rules={[{ required: true, message: '请选择接口类型' }]}>
          <Select options={[
            { value: 'chat', label: '对话补全' },
            { value: 'responses', label: 'Responses' }
          ]} />
        </Form.Item>
        <Form.Item name="baseUrl" label="接口地址" rules={[{ required: true, message: '请填写接口地址' }]} extra="填到 /v1 即可。已经写到具体路径时会按原样请求。">
          <Input placeholder="https://example.com/v1" />
        </Form.Item>
        <Form.Item name="apiKey" label="API Key" extra="留空表示沿用已保存的密钥。密钥只保存在本地数据库，列表里只显示末四位。">
          <Input.Password />
        </Form.Item>
        <div className="model-grid">
          <Form.Item name="model" label="模型" rules={[{ required: true, message: '请填写模型名称' }]}><Input placeholder="供应商提供的模型名" /></Form.Item>
          <Form.Item name="temperature" label="温度"><InputNumber min={0} max={2} step={0.1} /></Form.Item>
        </div>
      </Form>
      <div className="test-row">
        <button type="button" className="text-link inline" disabled={testing} onClick={() => test()}>
          {testing ? '正在测试…' : '测试连接'}
        </button>
      </div>
      <Table
        size="small"
        rowKey="id"
        pagination={false}
        dataSource={models}
        locale={{ emptyText: '还没有模型。保存后打开启用开关。' }}
        columns={[
          { title: '名称', dataIndex: 'name' },
          { title: '模型', dataIndex: 'model' },
          { title: '接口', dataIndex: 'protocol', render: (value: string) => value === 'responses' ? 'Responses' : '对话补全' },
          { title: '密钥', dataIndex: 'apiKeyMasked', render: (value: string) => value || '未填写' },
          {
            title: '启用',
            dataIndex: 'enabled',
            render: (enabled: boolean, record: ModelView) => (
              <Switch checked={enabled} onChange={async () => {
                try {
                  await api(`/api/v1/ai/models/${record.id}/enable`, { method: 'POST' })
                  await reload()
                } catch (error) {
                  setNotice({ type: 'error', text: error instanceof Error ? error.message : '启用失败' })
                }
              }} />
            )
          },
          {
            title: '',
            render: (_value, record: ModelView) => (
              <span className="row-actions">
                <a onClick={() => {
                  setNotice({ type: 'info', text: '正在编辑已有配置。密钥留空会继续使用已保存的值。' })
                  form.setFieldsValue({
                    id: record.id,
                    name: record.name,
                    baseUrl: record.baseUrl,
                    model: record.model,
                    protocol: record.protocol || 'chat',
                    temperature: record.temperature,
                    apiKey: ''
                  })
                }}>编辑</a>
                <Popconfirm title="删除这个模型配置？" okText="删除" cancelText="取消" onConfirm={async () => {
                  try {
                    await api(`/api/v1/ai/models/${record.id}`, { method: 'DELETE' })
                    await reload()
                  } catch (error) {
                    setNotice({ type: 'error', text: error instanceof Error ? error.message : '删除失败' })
                  }
                }}>
                  <a>删除</a>
                </Popconfirm>
              </span>
            )
          }
        ]}
      />
    </Modal>
  )
}
