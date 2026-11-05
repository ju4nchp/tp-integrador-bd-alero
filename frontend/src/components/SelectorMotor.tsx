import { Database } from 'lucide-react'
import { useMotor } from '../context/MotorContext'
import { NOMBRE_MOTOR } from '../domain/textos'
import type { Motor } from '../domain/tipos'

const MOTORES: Motor[] = ['mysql', 'mongo']

export function SelectorMotor() {
  const { motor, cambiarMotor, ultimaRespuesta } = useMotor()

  return (
    <div className="selector-motor">
      <div className="selector-motor__titulo" id="titulo-motor">
        <Database size={16} strokeWidth={2.2} />
        Base de datos
      </div>
      <div className="selector-motor__opciones" role="radiogroup" aria-labelledby="titulo-motor">
        {MOTORES.map((opcion) => (
          <button
            key={opcion}
            type="button"
            role="radio"
            aria-checked={motor === opcion}
            className="selector-motor__opcion"
            onClick={() => cambiarMotor(opcion)}
          >
            {NOMBRE_MOTOR[opcion]}
          </button>
        ))}
      </div>
      <p className="selector-motor__estado" aria-live="polite">
        {ultimaRespuesta
          ? `Respondió ${NOMBRE_MOTOR[ultimaRespuesta.motor]} en ${ultimaRespuesta.ms} ms`
          : 'Esperando la primera respuesta'}
      </p>
    </div>
  )
}
