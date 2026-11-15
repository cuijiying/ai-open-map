import { Input, Table } from 'antd'
import { useEffect, useRef, useState } from 'react'
import { ChatMessage } from '../types'

interface Props {
  messages: ChatMessage[]
  loading: boolean
  onSend: (text: string) => void
  onClose?: () => void
}

const QUICK = ['显示道路和水系', '定位到合肥', '切换深色风格', '切换影像风格', '只显示高速公路', '统计各类数据数量', '查找大学', '重置地图']

export default function ChatPanel({ messages, loading, onSend, onClose }: Props) {
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
      <div className="panel-title">
        <span>地图助手</span>
        {onClose ? <button type="button" className="icon-x" onClick={onClose} aria-label="收起助手">×</button> : null}
      </div>
      <div className="quick">
        {QUICK.map((item) => (
          <button key={item} type="button" className="chip" onClick={() => send(item)}>{item}</button>
        ))}
      </div>
      <div className="messages">
        {messages.length === 0 && (
          <div className="empty-ask">
            <strong>直接说你要看的地方</strong>
            <p>例如“定位到杭州”“只显示高速公路”，或“把道路改成红色”。统计和查找针对当前勾选的省份。</p>
          </div>
        )}
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
