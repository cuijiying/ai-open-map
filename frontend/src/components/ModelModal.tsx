import { Form, Input, InputNumber, Modal, Switch, Table, message } from 'antd'
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
  temperature: number
}

export default function ModelModal({ open, onClose }: Props) {
  const [models, setModels] = useState<ModelView[]>([])
  const [form] = Form.useForm<FormValue>()

  async function reload() {
    setModels(await api<ModelView[]>('/api/ai/models'))
  }

  useEffect(() => {
    if (open) {
      reload().catch((error: Error) => message.error(error.message))
      form.setFieldsValue({
        name: '本地模型',
        baseUrl: 'http://127.0.0.1:11434/v1',
        model: 'qwen2.5:7b',
        temperature: 0.2
      })
    }
  }, [open, form])

  async function save() {
    const value = await form.validateFields()
    if (value.id) {
      await api(`/api/ai/models/${value.id}`, { method: 'PUT', body: JSON.stringify(value) })
    } else {
      await api('/api/ai/models', { method: 'POST', body: JSON.stringify(value) })
    }
    message.success('已保存')
    form.setFieldValue('id', undefined)
    form.setFieldValue('apiKey', '')
    await reload()
  }

  async function test() {
    const value = await form.validateFields()
    const result = await api<{ reply: string }>('/api/ai/models/test', { method: 'POST', body: JSON.stringify(value) })
    message.success(`连接成功：${result.reply}`)
  }

  return (
    <Modal title="模型配置" open={open} onCancel={onClose} onOk={save} okText="保存" width={720}>
      <Form form={form} layout="vertical">
        <Form.Item name="id" hidden><Input /></Form.Item>
        <Form.Item name="name" label="名称" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="baseUrl" label="接口地址" rules={[{ required: true }]} extra="OpenAI 兼容地址，例如 https://api.deepseek.com/v1">
          <Input />
        </Form.Item>
        <Form.Item name="apiKey" label="API Key" extra="留空表示沿用已保存的密钥；本地无鉴权接口可以不填">
          <Input.Password />
        </Form.Item>
        <Form.Item name="model" label="模型" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="temperature" label="温度"><InputNumber min={0} max={2} step={0.1} /></Form.Item>
      </Form>
      <ButtonRow onTest={test} />
      <Table
        size="small"
        rowKey="id"
        pagination={false}
        dataSource={models}
        columns={[
          { title: '名称', dataIndex: 'name' },
          { title: '模型', dataIndex: 'model' },
          { title: '密钥', dataIndex: 'apiKeyMasked' },
          {
            title: '启用',
            dataIndex: 'enabled',
            render: (enabled: boolean, record: ModelView) => (
              <Switch checked={enabled} onChange={async () => { await api(`/api/ai/models/${record.id}/enable`, { method: 'POST' }); await reload() }} />
            )
          },
          {
            title: '',
            render: (_value, record: ModelView) => (
              <a onClick={() => form.setFieldsValue({
                id: record.id,
                name: record.name,
                baseUrl: record.baseUrl,
                model: record.model,
                temperature: record.temperature,
                apiKey: ''
              })}>编辑</a>
            )
          }
        ]}
      />
    </Modal>
  )
}

function ButtonRow({ onTest }: { onTest: () => Promise<void> }) {
  return <div className="test-row"><a onClick={() => onTest().catch((error: Error) => message.error(error.message))}>测试连接</a></div>
}
