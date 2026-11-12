import { CalendarRange, ChartColumn, Database, MessageSquareText } from 'lucide-react'
import { NavLink, Outlet } from 'react-router-dom'
import { Logo } from './Logo'
import './Layout.css'

const SECCIONES = [
  { ruta: '/reservas', nombre: 'Reservas', icono: CalendarRange },
  { ruta: '/resenas', nombre: 'Reseñas', icono: MessageSquareText },
  { ruta: '/estadisticas', nombre: 'Estadísticas', icono: ChartColumn },
]

// Qué base de datos usa cada sección (lo pide la consigna para el frontend).
const BASES = [
  { seccion: 'Reservas', base: 'MySQL' },
  { seccion: 'Reseñas', base: 'MongoDB' },
  { seccion: 'Estadísticas', base: 'Las dos' },
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
        <div className="menu__bases">
          <div className="menu__bases-titulo">
            <Database size={16} strokeWidth={2.2} />
            Bases de datos
          </div>
          <dl>
            {BASES.map(({ seccion, base }) => (
              <div key={seccion}>
                <dt>{seccion}</dt>
                <dd>{base}</dd>
              </div>
            ))}
          </dl>
        </div>
      </aside>
      <main className="contenido">
        <Outlet />
      </main>
    </div>
  )
}
