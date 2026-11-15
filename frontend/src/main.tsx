import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider, theme } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import App from './App'
import './styles.css'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ConfigProvider
      locale={zhCN}
      button={{ autoInsertSpace: false }}
      theme={{
        algorithm: theme.darkAlgorithm,
        token: {
          colorPrimary: '#7ee7ff',
          colorInfo: '#7ee7ff',
          colorBgBase: '#081018',
          colorBgContainer: '#101b2a',
          colorBgElevated: '#142436',
          colorBorder: 'rgba(154, 184, 214, 0.22)',
          colorText: '#e8f0f8',
          colorTextSecondary: '#93a4b8',
          colorTextLightSolid: '#062028',
          borderRadius: 12,
          fontFamily: '"Manrope", "Segoe UI", "Microsoft YaHei", sans-serif',
          controlHeight: 36
        },
        components: {
          Button: { primaryShadow: 'none', defaultShadow: 'none' }
        }
      }}
    >
      <App />
    </ConfigProvider>
  </StrictMode>
)
