import { BrowserRouter, Route, Routes } from 'react-router-dom'
import Workbench from './pages/Workbench'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="*" element={<Workbench />} />
      </Routes>
    </BrowserRouter>
  )
}
