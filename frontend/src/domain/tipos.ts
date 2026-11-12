export type EstadoReserva = 'PENDIENTE' | 'CONFIRMADA' | 'COMPLETADA' | 'CANCELADA'

export type Alcance = 'ACTIVAS' | 'TODAS' | 'BAJAS'

export interface Cliente {
  id: string
  nombre: string
  edad: number
  genero: string
  ciudad: string
  pais: string
}

export interface Hotel {
  id: number
  nombre: string
  ciudad: string
  pais: string
  region: string
}

export interface Ciudad {
  nombre: string
  pais: string
}

export interface Reserva {
  id: number
  cliente: Cliente
  hotel: Hotel
  fechaReserva: string
  checkIn: string
  checkOut: string
  noches: number
  adultos: number
  ninos: number
  viajeros: number
  habitaciones: number
  regimen: string
  medioPago: string
  transporte: string
  descuento: number
  costoTotal: number
  totalFinal: number
  estado: EstadoReserva
  motivoCancelacion: string | null
  activa: boolean
  modificable: boolean
  confirmable: boolean
  completable: boolean
}

export interface Pagina<T> {
  contenido: T[]
  pagina: number
  totalPaginas: number
  totalElementos: number
}

export interface RespuestaAgencia {
  texto: string
  fecha: string
}

export interface Resena {
  reservaId: number
  hotelId: number
  hotel: string
  destino: string
  puntajeViaje: number
  puntajeHotel: number
  comentario: string | null
  etiquetas: string[]
  respuestaAgencia: RespuestaAgencia | null
}

export interface Catalogos {
  hoteles: Hotel[]
  ciudades: Ciudad[]
  regimenes: string[]
  mediosPago: string[]
  transportes: string[]
  motivosCancelacion: string[]
  estados: EstadoReserva[]
}

export interface EstadisticaPais {
  pais: string
  region: string
  reservas: number
  canceladas: number
  facturacion: number
  tasaCancelacion: number
}

export interface EstadisticaHotel {
  hotelId: number
  hotel: string
  ciudad: string
  pais: string
  reservas: number
  canceladas: number
  facturacion: number
  promedioHotel: number | null
  promedioViaje: number | null
  cantidadResenas: number
}

export interface FiltroReservas {
  estado?: EstadoReserva | ''
  hotelId?: number | ''
  pais?: string
  texto?: string
  alcance?: Alcance
  salidaPendiente?: boolean
  pagina?: number
}

export interface DatosReserva {
  clienteId?: string
  clienteNuevo?: { nombre: string; edad: number; genero: string; ciudad: string; pais: string }
  hotelId: number
  checkIn: string
  checkOut: string
  adultos: number
  ninos: number
  habitaciones: number
  regimen: string
  medioPago: string
  transporte: string
  descuento: number
  costoTotal: number
}
