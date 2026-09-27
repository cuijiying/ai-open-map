import { Button, List, Progress, Steps, Tag, Typography } from 'antd'
import { JobView, LogView } from '../types'

const PHASES = [
  { key: 'DOWNLOAD', title: '下载' },
  { key: 'PARSE', title: '解析' },
  { key: 'IMPORT', title: '入库' },
  { key: 'INDEX', title: '索引' }
]

interface Props {
  job: JobView | null
  logs: LogView[]
  loading: boolean
  onStart: () => void
}

export default function JobPanel({ job, logs, loading, onStart }: Props) {
  const running = job?.status === 'RUNNING'
  const current = Math.max(0, PHASES.findIndex((item) => item.key === job?.phase))
  return (
    <section className="panel">
      <div className="panel-title">
        <span>数据入库</span>
        <Button type="primary" size="small" loading={loading || running} onClick={onStart}>
          下载安徽省
        </Button>
      </div>
      <Typography.Paragraph type="secondary" className="hint">
        从 Geofabrik 获取安徽省 OpenStreetMap 数据，解析后写入 PostGIS。每天 03:00 也会自动执行一次。
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
