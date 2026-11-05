import axios, { AxiosError, type InternalAxiosRequestConfig } from 'axios'
import type { Motor } from '../domain/tipos'

export const API_URL: string = import.meta.env.VITE_API_URL ?? 'http://localhost:9000'

export interface Respuesta {
  motor: Motor
  ms: number
}

let motorActual: Motor = 'mysql'
let avisar: (respuesta: Respuesta) => void = () => {}

export const usarMotor = (motor: Motor) => {
  motorActual = motor
}

export const alResponder = (callback: (respuesta: Respuesta) => void) => {
  avisar = callback
}

type ConInicio = InternalAxiosRequestConfig & { inicio?: number }

export const api = axios.create({ baseURL: `${API_URL}/api` })

api.interceptors.request.use((config: ConInicio) => {
  if (!config.headers.has('X-DB-Engine')) config.headers.set('X-DB-Engine', motorActual)
  config.inicio = performance.now()
  return config
})

const registrar = (config: ConInicio | undefined, motor: unknown) => {
  if (config?.inicio !== undefined && (motor === 'mysql' || motor === 'mongo')) {
    avisar({ motor, ms: Math.round(performance.now() - config.inicio) })
  }
}

api.interceptors.response.use(
  (respuesta) => {
    registrar(respuesta.config, respuesta.headers['x-db-engine-used'])
    return respuesta
  },
  (error: AxiosError) => {
    registrar(error.config, error.response?.headers['x-db-engine-used'])
    return Promise.reject(error)
  },
)

export const mensajeDeError = (error: unknown): string => {
  if (axios.isAxiosError(error)) {
    if (!error.response) return 'No hay conexión con el servidor. Revisá que el backend esté levantado.'
    const datos = error.response.data as { error?: string; detalle?: string } | undefined
    if (datos?.error) return datos.detalle ? `${datos.error}: ${datos.detalle}` : datos.error
  }
  return 'Algo salió mal. Probá de nuevo en un momento.'
}
