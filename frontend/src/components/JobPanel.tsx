import { Button, List, Progress, Select, Steps, Tag, Typography } from 'antd'
import { JobView, LogView, RegionOption, SourceOption, SourceProbe, sourceLabel } from '../types'

const PHASES = [
  { key: 'DOWNLOAD', title: '下载' },
  { key: 'PARSE', title: '解析' },
  { key: 'IMPORT', title: '入库' },
  { key: 'INDEX', title: '索引' }
]

interface Props {
  regions: RegionOption[]
  regionCode: string
  mapRegions: string[]
  sources: SourceOption[]
  sourceId: string
  probe: SourceProbe | null
  probing: boolean
  job: JobView | null
  logs: LogView[]
  loading: boolean
  onRegionChange: (code: string) => void
  onMapRegionsChange: (codes: string[]) => void
  onFit: () => void
  onLocate: () => void
  onSourceChange: (id: string) => void
  onTest: () => void
  onStart: () => void
}

function elapsedText(ms: number) {
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)} 秒` : `${ms} 毫秒`
}

export default function JobPanel({
  regions, regionCode, mapRegions, sources, sourceId, probe, probing, job, logs, loading,
  onRegionChange, onMapRegionsChange, onFit, onLocate, onSourceChange, onTest, onStart
}: Props) {
  const running = job?.status === 'RUNNING'
  const current = Math.max(0, PHASES.findIndex((item) => item.key === job?.phase))
  const selectedRegion = regions.find((item) => item.code === regionCode)
  const selectedSource = sources.find((item) => item.id === sourceId)
  const jobSource = sourceLabel(job?.sourceUrl)
  const usingName = running && jobSource ? jobSource : selectedSource?.name
  const usingUrl = running && job?.sourceUrl ? job.sourceUrl : selectedSource?.url
  return (
    <section className="panel">
      <div className="panel-title">
        <span>地图省份</span>
        <Button size="small" disabled={mapRegions.length === 0} onClick={onFit}>缩放</Button>
      </div>
      <Select
        className="region-select"
        mode="multiple"
        allowClear
        showSearch
        optionFilterProp="label"
        placeholder="选择要同时显示的省份"
        maxTagCount="responsive"
        value={mapRegions}
        options={regions.map((item) => ({
          value: item.code,
          label: item.featureCount > 0 ? `${item.name} · ${item.featureCount.toLocaleString('zh-CN')}` : `${item.name} · 未入库`
        }))}
        onChange={onMapRegionsChange}
      />
      <Typography.Paragraph type="secondary" className="hint">
        可以同时勾选多个省，矢量切片会把这些省画在同一张地图上。图层数量是所选省份的合计。
      </Typography.Paragraph>
      <div className="panel-title">
        <span>数据入库</span>
        <Button type="primary" size="small" loading={loading || running} disabled={running} onClick={onStart}>
          下载
        </Button>
      </div>
      <div className="source-current">
        <Select
          className="region-select region-select-inline"
          showSearch
          optionFilterProp="label"
          value={regionCode}
          options={regions.map((item) => ({
            value: item.code,
            label: item.featureCount > 0 ? `${item.name} · 已入库` : item.name
          }))}
          onChange={onRegionChange}
        />
        <Button size="small" onClick={onLocate}>定位</Button>
      </div>
      <Select
        className="region-select"
        value={sourceId}
        options={sources.map((item) => ({ value: item.id, label: item.name }))}
        onChange={onSourceChange}
      />
      <div className="source-current">
        <span>当前使用</span>
        <strong>{usingName || '—'}</strong>
        {running && jobSource ? <Tag>本任务</Tag> : null}
        <Button size="small" loading={probing} disabled={running || !sourceId} onClick={onTest}>
          测试连接
        </Button>
      </div>
      {usingUrl ? (
        <Typography.Paragraph className="source-url" ellipsis={{ rows: 2, tooltip: usingUrl }}>
          {usingUrl}
        </Typography.Paragraph>
      ) : null}
      {selectedSource?.note && !(running && jobSource) ? (
        <Typography.Paragraph type="secondary" className="hint">{selectedSource.note}</Typography.Paragraph>
      ) : null}
      {probe && probe.id === sourceId ? (
        <div className="source-probe">
          <Tag color={probe.ok ? 'success' : 'error'}>{probe.ok ? '可连接' : '不可连接'}</Tag>
          <span>{probe.message} · {elapsedText(probe.elapsedMs)}</span>
        </div>
      ) : null}
      {running && job ? (
        <Typography.Paragraph type="secondary" className="hint">
          正在处理{job.regionName}。下载、解析和建索引不改已发布的地图，完成前不能再开始另一个入库任务。
        </Typography.Paragraph>
      ) : null}
      <Typography.Paragraph type="secondary" className="hint">
        {selectedRegion && selectedRegion.featureCount > 0
          ? `${selectedRegion.name}已有 ${selectedRegion.featureCount.toLocaleString('zh-CN')} 条要素。再次下载只替换该省分区，其他省的切片保持可用。`
          : '该省尚未入库。下载完成后会加入地图。每天 03:00 自动更新安徽省。'}
      </Typography.Paragraph>
      {job && (
        <>
          <Steps
            size="small"
            current={job.status === 'SUCCESS' ? 4 : current}
            status={job.status === 'FAILED' ? 'error' : job.status === 'SUCCESS' ? 'finish' : 'process'}
            items={PHASES.map((item) => ({ title: item.title }))}
          />
          <Progress percent={job.progress} size="small" status={job.status === 'FAILED' ? 'exception' : job.status === 'SUCCESS' ? 'success' : 'active'} />
          <div className="job-message">{job.message}</div>
          {job.featureCount ? <Tag color="blue">要素 {job.featureCount.toLocaleString('zh-CN')}</Tag> : null}
        </>
      )}
      <List
        className="log-list"
        size="small"
        dataSource={logs}
        renderItem={(item) => (
          <List.Item className={item.level === 'ERROR' ? 'log-error' : ''}>
            <span>{item.message}</span>
          </List.Item>
        )}
      />
    </section>
  )
}
