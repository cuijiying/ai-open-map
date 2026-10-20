import { Button, Collapse, Select, Slider, Switch } from 'antd'
import {
  BASEMAP_OPTIONS,
  BasemapId,
  emptyOverrides,
  OpacityKey,
  STYLE_GROUPS,
  StyleOverrides,
  styleIsDirty,
  ThemeId,
  themeById,
  THEMES,
  TokenKey
} from '../mapStyle'

interface Props {
  themeId: ThemeId
  overrides: StyleOverrides
  resolvedTokens: Record<TokenKey, string>
  resolvedOpacity: Record<OpacityKey, number>
  widthScale: number
  labels: boolean
  basemap: BasemapId
  onTheme: (id: ThemeId) => void
  onOverrides: (next: StyleOverrides) => void
}

export default function StylePanel(props: Props) {
  const theme = themeById(props.themeId)
  const dirty = styleIsDirty(props.themeId, props.overrides)

  function patch(next: StyleOverrides) {
    props.onOverrides(next)
  }

  function setColor(key: TokenKey, value: string) {
    patch({ ...props.overrides, colors: { ...props.overrides.colors, [key]: value } })
  }

  function clearColor(key: TokenKey) {
    const colors = { ...props.overrides.colors }
    delete colors[key]
    patch({ ...props.overrides, colors })
  }

  function setOpacity(key: OpacityKey, value: number) {
    const opacity = { ...props.overrides.opacity }
    if (Math.abs(value - theme.numbers[key]) < 0.001) {
      delete opacity[key]
    } else {
      opacity[key] = value
    }
    patch({ ...props.overrides, opacity })
  }

  function setWidth(value: number) {
    patch({ ...props.overrides, widthScale: Math.abs(value - 1) < 0.001 ? undefined : value })
  }

  function setLabels(checked: boolean) {
    patch({ ...props.overrides, labels: checked ? undefined : false })
  }

  function setBasemap(value: BasemapId) {
    patch({ ...props.overrides, basemap: value === theme.basemap ? undefined : value })
  }

  return (
    <section className="panel style-panel">
      <div className="panel-title">
        <span>地图风格</span>
        {dirty && <Button type="link" size="small" onClick={() => props.onOverrides(emptyOverrides())}>恢复</Button>}
      </div>
      <div className="style-grid">
        {THEMES.map((item) => (
          <button
            key={item.id}
            type="button"
            className={item.id === props.themeId ? 'style-card on' : 'style-card'}
            aria-pressed={item.id === props.themeId}
            onClick={() => props.onTheme(item.id)}
          >
            <span className="style-preview" style={{ background: `linear-gradient(90deg, ${item.preview.join(',')})` }} />
            <span className="style-card-name">{item.label}</span>
          </button>
        ))}
      </div>
      <p className="style-note">{theme.description}</p>
      <div className="road-legend" aria-label="道路图例">
        <Legend color={props.resolvedTokens.highwayMotorway} label="高速" />
        <Legend color={props.resolvedTokens.highwayTrunk} label="主干" />
        <Legend color={props.resolvedTokens.highwayPrimary} label="主要" />
        <Legend color={props.resolvedTokens.highwayMinor} label="普通" />
        <Legend color={props.resolvedTokens.waterFill} label="水体" />
      </div>
      <div className="style-row">
        <span>底图</span>
        <Select
          size="small"
          className="style-basemap"
          value={props.basemap}
          options={BASEMAP_OPTIONS.map((item) => ({ value: item.id, label: item.label }))}
          onChange={setBasemap}
        />
      </div>
      <div className="style-row">
        <span>线宽</span>
        <Slider min={0.6} max={2} step={0.1} value={props.widthScale} onChange={setWidth} tooltip={{ formatter: (value) => `${value?.toFixed(1)}×` }} />
      </div>
      <div className="style-row">
        <span>注记</span>
        <Switch size="small" checked={props.labels} onChange={setLabels} />
      </div>
      <Collapse
        className="style-editor"
        bordered={false}
        size="small"
        items={STYLE_GROUPS.map((group) => ({
          key: group.key,
          label: group.title,
          children: (
            <div>
              {group.colors.map((field) => (
                <div className="token-row" key={field.key}>
                  <span>{field.label}</span>
                  <input
                    type="color"
                    aria-label={field.label}
                    value={props.resolvedTokens[field.key]}
                    onChange={(event) => setColor(field.key, event.target.value)}
                  />
                  <button
                    type="button"
                    className="token-clear"
                    aria-label={`还原${field.label}`}
                    disabled={!props.overrides.colors[field.key]}
                    onClick={() => clearColor(field.key)}
                  >
                    ×
                  </button>
                </div>
              ))}
              {group.opacity?.map((item) => (
                <div className="style-row" key={item.key}>
                  <span>{item.label}</span>
                  <Slider min={0.1} max={1} step={0.05} value={props.resolvedOpacity[item.key]} onChange={(value) => setOpacity(item.key, value)} />
                </div>
              ))}
            </div>
          )
        }))}
      />
      <p className="style-note">未改色时按道路等级、用地类型和地名级别分色。调整保存在这台浏览器。</p>
    </section>
  )
}

function Legend({ color, label }: { color: string; label: string }) {
  return <span className="legend-chip"><i style={{ background: color }} />{label}</span>
}
