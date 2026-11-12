import axios from 'axios'

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:9000'

// El backend decide en qué base va cada dato: reservas en MySQL y reseñas en MongoDB.
export const api = axios.create({ baseURL: `${API_URL}/api` })

export const mensajeDeError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    if (!error.response) return 'No hay conexión con el servidor. Revisá que el backend esté levantado.'
    const datos = error.response.data as { error?: string; detalle?: string } | undefined
    if (datos?.error) return datos.detalle ? `${datos.error}: ${datos.detalle}` : datos.error
  }
  return 'Algo salió mal. Probá de nuevo en un momento.'
}
