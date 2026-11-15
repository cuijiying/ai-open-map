import { Input } from 'antd'
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
      <p className="hint">常用指令会直接执行。其他问题交给已启用的模型；没启用时会提示可以怎么说。</p>
      <div className="quick">
        {QUICK.map((item) => (
          <button key={item} type="button" className="chip" disabled={loading} onClick={() => send(item)}>{item}</button>
        ))}
      </div>
      <div className="messages">
        {messages.length === 0 && (
          <div className="empty-ask">
            <strong>直接说你要看的地方</strong>
            <p>例如“定位到杭州”“只显示高速公路”，或“把道路改成红色”。统计和查找针对当前勾选的省份。</p>
          </div>
        )}
        {messages.map((item, index) => (
          <div key={index} className={`bubble ${item.role}`}>
            {item.role === 'error' ? <div className="bubble-tag">无法完成</div> : null}
            {item.role === 'assistant' && item.source ? <div className="bubble-tag">{item.source === 'model' ? '模型' : '指令'}</div> : null}
            <div className="bubble-text">{item.content}</div>
            {item.tables?.map((table) => (
              <div key={table.title} className="bubble-table">
                <div className="bubble-table-title">{table.title}</div>
                <table>
                  <thead>
                    <tr>{table.columns.map((column) => <th key={column}>{column}</th>)}</tr>
                  </thead>
                  <tbody>
                    {table.rows.map((row, rowIndex) => (
                      <tr key={rowIndex}>{row.map((cell, cellIndex) => <td key={cellIndex}>{cell}</td>)}</tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ))}
          </div>
        ))}
        {loading ? <div className="bubble assistant pending">正在询问模型…</div> : null}
        <div ref={bottom} />
      </div>
      <div className="composer">
        <Input.TextArea
          value={text}
          disabled={loading}
          autoSize={{ minRows: 2, maxRows: 4 }}
          placeholder={loading ? '正在等待回复' : '输入指令，Enter 发送'}
          onChange={(event) => setText(event.target.value)}
          onPressEnter={(event) => {
            if (!event.shiftKey) {
              event.preventDefault()
              send(text)
            }
          }}
        />
        <button type="button" className="send" disabled={loading || text.trim().length === 0} onClick={() => send(text)}>
          发送
        </button>
      </div>
    </section>
  )
}
