import { ESTADOS } from '../domain/textos'
import type { EstadoReserva } from '../domain/tipos'

interface Props {
  estado: EstadoReserva
  activa?: boolean
  grande?: boolean
}

export function Sello({ estado, activa = true, grande = false }: Props) {
  const clase = activa ? estado.toLowerCase() : 'baja'
  const texto = activa ? ESTADOS[estado] : 'Dada de baja'
  return (
    <span key={clase} className={`sello sello--${clase}${grande ? ' sello--grande sello--recien' : ''}`}>
      {texto}
    </span>
  )
}

export function BarraEstadia({ noches }: { noches: number }) {
  return (
    <span className="barra-estadia" title={`${noches} noches`}>
      <span style={{ width: `${Math.min(noches, 10) * 7}px` }} />
    </span>
  )
}
