import { Button, List, Progress, Select, Steps, Tag, Typography } from 'antd'
import { JobView, LogView, RegionOption } from '../types'

const PHASES = [
  { key: 'DOWNLOAD', title: '下载' },
  { key: 'PARSE', title: '解析' },
  { key: 'IMPORT', title: '入库' },
  { key: 'INDEX', title: '索引' }
]

interface Props {
  regions: RegionOption[]
  regionCode: string
  job: JobView | null
  logs: LogView[]
  loading: boolean
  onRegionChange: (code: string) => void
  onStart: () => void
}

export default function JobPanel({ regions, regionCode, job, logs, loading, onRegionChange, onStart }: Props) {
  const running = job?.status === 'RUNNING'
  const current = Math.max(0, PHASES.findIndex((item) => item.key === job?.phase))
  const selected = regions.find((item) => item.code === regionCode)
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
      {running && job && job.regionCode !== regionCode ? (
        <Typography.Paragraph type="secondary" className="hint">
          正在下载{job.regionName}，完成前不能开始新的省份任务。
        </Typography.Paragraph>
      ) : null}
      <Typography.Paragraph type="secondary" className="hint">
        {selected && selected.featureCount > 0
          ? `${selected.name}已有 ${selected.featureCount.toLocaleString('zh-CN')} 条要素，切换后地图只展示该省。再次下载会替换该省数据，不影响其他省。`
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
