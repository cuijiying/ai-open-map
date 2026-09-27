import { Button, Input, Table } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { ChatMessage } from '../types'

interface Props {
  messages: ChatMessage[]
  loading: boolean
  onSend: (text: string) => void
}

const QUICK = ['显示道路和水系', '定位到合肥', '只显示高速公路', '统计各类数据数量', '查找大学', '分析当前视野内道路里程', '重置地图']

export default function ChatPanel({ messages, loading, onSend }: Props) {
  const [text, setText] = useState('')
  const bottom = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottom.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  function send(value: string) {
    const content = value.trim()
    if (!content || loading) {
      return
    }
    setText('')
    onSend(content)
  }

  return (
    <section className="chat">
      <div className="panel-title"><span>地图对话</span></div>
      <div className="quick">
        {QUICK.map((item) => (
          <Button key={item} size="small" onClick={() => send(item)}>{item}</Button>
        ))}
      </div>
      <div className="messages">
        {messages.length === 0 && <div className="hint">可以直接说“定位到合肥”或“只显示高速公路”。未配置模型时使用内置指令。</div>}
        {messages.map((message, index) => (
          <div key={index} className={`bubble ${message.role}`}>
            <div className="bubble-text">{message.content}</div>
            {message.tables?.map((table) => (
              <Table
                key={table.title}
                size="small"
                pagination={false}
                title={() => table.title}
                columns={table.columns.map((column, columnIndex) => ({ title: column, dataIndex: String(columnIndex), key: column }))}
                dataSource={table.rows.map((row, rowIndex) => {
                  const record: Record<string, string | number> = { key: rowIndex }
                  row.forEach((cell, cellIndex) => {
                    record[String(cellIndex)] = cell
                  })
                  return record
                })}
              />
            ))}
          </div>
        ))}
        <div ref={bottom} />
      </div>
      <Input.TextArea
        value={text}
        autoSize={{ minRows: 2, maxRows: 4 }}
        placeholder="输入指令，Enter 发送"
        onChange={(event) => setText(event.target.value)}
        onPressEnter={(event) => {
          if (!event.shiftKey) {
            event.preventDefault()
            send(text)
          }
        }}
      />
    </section>
  )
}
