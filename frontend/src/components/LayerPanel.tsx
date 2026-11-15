import { LAYERS } from '../layers'
import { LayerCount } from '../types'

interface Props {
  counts: LayerCount[]
  visible: Record<string, boolean>
  swatches: Record<string, string>
  onToggle: (layer: string, checked: boolean) => void
}

export default function LayerPanel({ counts, visible, swatches, onToggle }: Props) {
  const countOf = (id: string) => counts.find((item) => item.layer === id)?.count ?? 0
  const shown = LAYERS.filter((layer) => visible[layer.id]).length
  return (
    <section className="panel">
      <div className="panel-title">
        <span>图层</span>
        <em>{shown}/{LAYERS.length}</em>
      </div>
      <p className="hint">点一下开关图层。数字是当前所选省份的要素合计。</p>
      <div className="layer-list">
        {LAYERS.map((layer) => {
          const on = !!visible[layer.id]
          return (
            <button
              key={layer.id}
              type="button"
              className={on ? 'layer-item on' : 'layer-item'}
              aria-pressed={on}
              onClick={() => onToggle(layer.id, !on)}
            >
              <i className="swatch" style={{ background: swatches[layer.id] || layer.color }} />
              <span className="layer-name">{layer.label}</span>
              <span className="count">{countOf(layer.id).toLocaleString('zh-CN')}</span>
            </button>
          )
        })}
      </div>
    </section>
  )
}
