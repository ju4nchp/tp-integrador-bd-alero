import type {
  Catalogos,
  Cliente,
  DatosReserva,
  EstadisticaHotel,
  EstadisticaPais,
  FiltroReservas,
  Motor,
  Pagina,
  Resena,
  Reserva,
} from '../domain/tipos'
import { api } from './api'

const parametros = (filtro: FiltroReservas) => {
  const p: Record<string, string> = {}
  Object.entries(filtro).forEach(([clave, valor]) => {
    if (valor !== undefined && valor !== '' && valor !== false) p[clave] = String(valor)
  })
  return p
}

export const reservaService = {
  listar: async (filtro: FiltroReservas) =>
    (await api.get<Pagina<Reserva>>('/reservas', { params: parametros(filtro) })).data,
  buscar: async (id: number) => (await api.get<Reserva>(`/reservas/${id}`)).data,
  crear: async (datos: DatosReserva) => (await api.post<Reserva>('/reservas', datos)).data,
  modificar: async (id: number, datos: Partial<DatosReserva>) => (await api.put<Reserva>(`/reservas/${id}`, datos)).data,
  cancelar: async (id: number, motivo: string) => (await api.post<Reserva>(`/reservas/${id}/cancelacion`, { motivo })).data,
  completar: async (id: number) => (await api.post<Reserva>(`/reservas/${id}/completar`)).data,
  darDeBaja: async (id: number) => {
    await api.delete(`/reservas/${id}`)
  },
}

export const resenaService = {
  listar: async (hotelId?: number, sinResponder = false) =>
    (await api.get<Resena[]>('/resenas', { params: { ...(hotelId ? { hotelId } : {}), sinResponder } })).data,
  buscar: async (reservaId: number) => (await api.get<Resena>(`/resenas/${reservaId}`)).data,
  crear: async (datos: { reservaId: number; puntajeViaje: number; puntajeHotel: number; comentario: string }) =>
    (await api.post<Resena>('/resenas', datos)).data,
  responder: async (reservaId: number, texto: string) =>
    (await api.put<Resena>(`/resenas/${reservaId}/respuesta`, { texto })).data,
  etiquetar: async (reservaId: number, etiquetas: string[]) =>
    (await api.put<Resena>(`/resenas/${reservaId}/etiquetas`, { etiquetas })).data,
}

export const consultaService = {
  catalogos: async () => (await api.get<Catalogos>('/catalogos')).data,
  clientes: async (texto: string) => (await api.get<Cliente[]>('/clientes', { params: { texto } })).data,
  porPais: async (motor?: Motor) =>
    (await api.get<EstadisticaPais[]>('/estadisticas/paises', motor ? { headers: { 'X-DB-Engine': motor } } : {})).data,
  porHotel: async (motor?: Motor) =>
    (await api.get<EstadisticaHotel[]>('/estadisticas/hoteles', motor ? { headers: { 'X-DB-Engine': motor } } : {})).data,
}
