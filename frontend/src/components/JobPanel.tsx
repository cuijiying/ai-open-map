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
  sources: SourceOption[]
  sourceId: string
  probe: SourceProbe | null
  probing: boolean
  job: JobView | null
  logs: LogView[]
  loading: boolean
  onRegionChange: (code: string) => void
  onSourceChange: (id: string) => void
  onTest: () => void
  onStart: () => void
}

function elapsedText(ms: number) {
  return ms >= 1000 ? `${(ms / 1000).toFixed(1)} 秒` : `${ms} 毫秒`
}

export default function JobPanel({
  regions, regionCode, sources, sourceId, probe, probing, job, logs, loading,
  onRegionChange, onSourceChange, onTest, onStart
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
        <span>数据入库</span>
        <Button type="primary" size="small" loading={loading || running} disabled={running} onClick={onStart}>
          下载
        </Button>
      </div>
      <Select
        className="region-select"
        showSearch
        optionFilterProp="label"
        value={regionCode}
        options={regions.map((item) => ({
          value: item.code,
          label: item.featureCount > 0 ? `${item.name} · 已入库` : item.name
        }))}
        onChange={onRegionChange}
      />
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
      {running && job && job.regionCode !== regionCode ? (
        <Typography.Paragraph type="secondary" className="hint">
          正在下载{job.regionName}，完成前不能开始新的省份任务。
        </Typography.Paragraph>
      ) : null}
      <Typography.Paragraph type="secondary" className="hint">
        {selectedRegion && selectedRegion.featureCount > 0
          ? `${selectedRegion.name}已有 ${selectedRegion.featureCount.toLocaleString('zh-CN')} 条要素，切换后地图只展示该省。再次下载会替换该省数据，不影响其他省。`
          : '该省尚未入库。下载完成后地图会展示这一省。每天 03:00 自动更新安徽省。'}
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
