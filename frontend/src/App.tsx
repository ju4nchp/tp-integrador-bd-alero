import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { MotorProvider } from './context/MotorContext'
import { EstadisticasPage } from './pages/EstadisticasPage'
import { ReservaFormPage } from './pages/ReservaFormPage'
import { ReservasPage } from './pages/ReservasPage'
import { ResenasPage } from './pages/ResenasPage'

export function App() {
  return (
    <MotorProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route path="/" element={<Navigate to="/reservas" replace />} />
            <Route path="/reservas" element={<ReservasPage />} />
            <Route path="/reservas/nueva" element={<ReservaFormPage />} />
            <Route path="/reservas/:id" element={<ReservasPage />} />
            <Route path="/reservas/:id/editar" element={<ReservaFormPage />} />
            <Route path="/resenas" element={<ResenasPage />} />
            <Route path="/estadisticas" element={<EstadisticasPage />} />
            <Route path="*" element={<Navigate to="/reservas" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </MotorProvider>
  )
}
