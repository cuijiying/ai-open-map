import { Checkbox, List } from 'antd'
import { LAYERS } from '../layers'
import { LayerCount } from '../types'

interface Props {
  counts: LayerCount[]
  visible: Record<string, boolean>
  onToggle: (layer: string, checked: boolean) => void
}

export default function LayerPanel({ counts, visible, onToggle }: Props) {
  const countOf = (id: string) => counts.find((item) => item.layer === id)?.count ?? 0
  return (
    <section className="panel">
      <div className="panel-title"><span>图层</span></div>
      <List
        size="small"
        dataSource={LAYERS}
        renderItem={(layer) => (
          <List.Item className="layer-row">
            <Checkbox checked={!!visible[layer.id]} onChange={(event) => onToggle(layer.id, event.target.checked)}>
              <i className="swatch" style={{ background: layer.color }} />
              {layer.label}
            </Checkbox>
            <span className="count">{countOf(layer.id).toLocaleString('zh-CN')}</span>
          </List.Item>
        )}
      />
    </section>
  )
}
