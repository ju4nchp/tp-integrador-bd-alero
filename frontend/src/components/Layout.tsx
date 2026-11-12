import { CalendarRange, ChartColumn, MessageSquareText } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { Logo } from './Logo'
import './Layout.css'

const SECCIONES = [
  { ruta: '/reservas', nombre: 'Reservas', icono: CalendarRange },
  { ruta: '/resenas', nombre: 'Reseñas', icono: MessageSquareText },
  { ruta: '/estadisticas', nombre: 'Estadísticas', icono: ChartColumn },
]

export function Layout() {
  return (
    <div className="app">
      <aside className="menu">
        <Logo />
        <nav className="menu__secciones" aria-label="Secciones">
          {SECCIONES.map(({ ruta, nombre, icono: Icono }) => (
            <NavLink key={ruta} to={ruta} className="menu__item">
              <Icono size={20} strokeWidth={2} />
              <span>{nombre}</span>
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="contenido">
        <Outlet />
      </main>
    </div>
  )
}
