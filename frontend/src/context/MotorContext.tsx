import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'
import type { Motor } from '../domain/tipos'
import { alResponder, usarMotor, type Respuesta } from '../services/api'

interface MotorContexto {
  motor: Motor
  cambiarMotor: (motor: Motor) => void
  ultimaRespuesta: Respuesta | null
}

const Contexto = createContext<MotorContexto | null>(null)

const motorGuardado = (): Motor => {
  try {
    return localStorage.getItem('alero.motor') === 'mongo' ? 'mongo' : 'mysql'
  } catch {
    return 'mysql'
  }
}

export function MotorProvider({ children }: { children: ReactNode }) {
  const [motor, setMotor] = useState<Motor>(() => {
    const inicial = motorGuardado()
    usarMotor(inicial)
    return inicial
  })
  const [ultimaRespuesta, setUltimaRespuesta] = useState<Respuesta | null>(null)

  useEffect(() => alResponder(setUltimaRespuesta), [])

  const cambiarMotor = (nuevo: Motor) => {
    usarMotor(nuevo)
    setMotor(nuevo)
    try {
      localStorage.setItem('alero.motor', nuevo)
    } catch {
      /* sin almacenamiento local: el motor vuelve a MySQL al recargar */
    }
  }

  return <Contexto.Provider value={{ motor, cambiarMotor, ultimaRespuesta }}>{children}</Contexto.Provider>
}

export const useMotor = () => {
  const contexto = useContext(Contexto)
  if (!contexto) throw new Error('useMotor tiene que usarse dentro de MotorProvider')
  return contexto
}
