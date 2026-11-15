import { Navigate, BrowserRouter, Route, Routes } from 'react-router-dom'
import { loadSession } from './api'
import LoginPage from './pages/LoginPage'
import Workbench from './pages/Workbench'

function RequireAuth() {
  if (!loadSession()) {
    return <Navigate to="/login" replace />
  }
  return <Workbench />
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="*" element={<RequireAuth />} />
      </Routes>
    </BrowserRouter>
  )
}
