import { CircleAlert, Scale } from 'lucide-react'
import { useEffect, useState } from 'react'
import { useMotor } from '../context/MotorContext'
import * as t from '../domain/textos'
import type { EstadisticaHotel, EstadisticaPais, Motor } from '../domain/tipos'
import { mensajeDeError } from '../services/api'
import { consultaService } from '../services/servicios'
import './EstadisticasPage.css'

const COMO_SE_CALCULA: Record<Motor, string> = {
  mysql: 'MySQL las calcula con JOIN entre reserva, hotel, ciudad y país, y GROUP BY.',
  mongo: 'MongoDB las calcula con un aggregate y $group sobre la colección reservas, sin JOIN.',
}

interface Comparacion {
  ms: Record<Motor, number>
  iguales: boolean
}

function Barra({ valor, maximo }: { valor: number; maximo: number }) {
  return (
    <span className="barra" aria-hidden="true">
      <span style={{ width: `${maximo > 0 ? (valor / maximo) * 100 : 0}%` }} />
    </span>
  )
}

export function EstadisticasPage() {
  const { motor } = useMotor()
  const [paises, setPaises] = useState<EstadisticaPais[] | null>(null)
  const [hoteles, setHoteles] = useState<EstadisticaHotel[] | null>(null)
  const [comparacion, setComparacion] = useState<Comparacion | null>(null)
  const [comparando, setComparando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setPaises(null)
    setHoteles(null)
    setComparacion(null)
    Promise.all([consultaService.porPais(), consultaService.porHotel()])
      .then(([p, h]) => {
        setPaises(p)
        setHoteles(h)
      })
      .catch((e) => setError(mensajeDeError(e)))
  }, [motor])

  const comparar = async () => {
    setComparando(true)
    setError('')
    try {
      const medir = async (m: Motor) => {
        const inicio = performance.now()
        const datos = await Promise.all([consultaService.porPais(m), consultaService.porHotel(m)])
        return { datos: JSON.stringify(datos), ms: Math.round(performance.now() - inicio) }
      }
      const mysql = await medir('mysql')
      const mongo = await medir('mongo')
      setComparacion({ ms: { mysql: mysql.ms, mongo: mongo.ms }, iguales: mysql.datos === mongo.datos })
    } catch (e) {
      setError(mensajeDeError(e))
    } finally {
      setComparando(false)
    }
  }

  const maxFacturacion = Math.max(0, ...(paises ?? []).map((p) => p.facturacion))

  return (
    <>
      <header className="cabecera">
        <div>
          <h1>Estadísticas</h1>
          <p>{COMO_SE_CALCULA[motor]}</p>
        </div>
        <button type="button" className="boton boton--secundario" onClick={comparar} disabled={comparando}>
          <Scale size={18} /> {comparando ? 'Comparando…' : 'Comparar los dos motores'}
        </button>
      </header>

      {comparacion && (
        <p className={`aviso ${comparacion.iguales ? 'aviso--info' : 'aviso--ambar'} estadisticas__comparacion`} role="status">
          Las mismas dos consultas tardaron {comparacion.ms.mysql} ms en MySQL y {comparacion.ms.mongo} ms en MongoDB.{' '}
          {comparacion.iguales
            ? 'Los dos motores devuelven exactamente los mismos números.'
            : 'Los números difieren: cada motor guarda su propia copia y hubo cambios en uno solo.'}
        </p>
      )}
      {error && (
        <p className="aviso aviso--error estadisticas__comparacion" role="alert">
          <CircleAlert size={18} /> {error}
        </p>
      )}

      <section className="estadisticas__seccion">
        <h2>Facturación y cancelaciones por país de destino</h2>
        <p className="tenue">La facturación no cuenta las reservas canceladas. Las dadas de baja no entran en ninguna cuenta.</p>
        <div className="tabla-marco">
          <table className="tabla">
            <thead>
              <tr>
                <th>País</th>
                <th className="derecha">Reservas</th>
                <th className="derecha">Canceladas</th>
                <th className="derecha">Tasa de cancelación</th>
                <th className="derecha">Facturación</th>
              </tr>
            </thead>
            <tbody>
              {!paises && !error && <FilasEsqueleto columnas={5} />}
              {paises?.map((p) => (
                <tr key={p.pais}>
                  <td>
                    <div className="dos-lineas">
                      <strong>{t.pais(p.pais)}</strong>
                      <span>{p.region}</span>
                    </div>
                  </td>
                  <td className="derecha">{t.numero(p.reservas)}</td>
                  <td className="derecha">{t.numero(p.canceladas)}</td>
                  <td className="derecha">{t.decimal(p.tasaCancelacion)} %</td>
                  <td className="derecha">
                    <span className="estadisticas__plata">
                      <Barra valor={p.facturacion} maximo={maxFacturacion} />
                      <span className="numero">{t.plata(p.facturacion)}</span>
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="estadisticas__seccion">
        <h2>Cómo le va a cada alojamiento</h2>
        <p className="tenue">Reservas y cancelaciones salen de las reservas; los puntajes salen de las reseñas del mismo motor.</p>
        <div className="tabla-marco">
          <table className="tabla">
            <thead>
              <tr>
                <th>Alojamiento</th>
                <th className="derecha">Reservas</th>
                <th className="derecha">Canceladas</th>
                <th className="derecha">Reseñas</th>
                <th className="derecha">Puntaje del alojamiento</th>
                <th className="derecha">Puntaje de la estadía</th>
              </tr>
            </thead>
            <tbody>
              {!hoteles && !error && <FilasEsqueleto columnas={6} />}
              {hoteles?.map((h) => (
                <tr key={h.hotelId}>
                  <td>
                    <div className="dos-lineas">
                      <strong>{h.hotel}</strong>
                      <span>
                        {h.ciudad}, {t.pais(h.pais)}
                      </span>
                    </div>
                  </td>
                  <td className="derecha">{t.numero(h.reservas)}</td>
                  <td className="derecha">{t.numero(h.canceladas)}</td>
                  <td className="derecha">{t.numero(h.cantidadResenas)}</td>
                  <td className="derecha numero">{h.promedioHotel !== null ? t.decimal(h.promedioHotel) : '—'}</td>
                  <td className="derecha numero">{h.promedioViaje !== null ? t.decimal(h.promedioViaje) : '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>
    </>
  )
}

function FilasEsqueleto({ columnas }: { columnas: number }) {
  return (
    <>
      {Array.from({ length: 6 }, (_, i) => (
        <tr key={i}>
          {Array.from({ length: columnas }, (_, j) => (
            <td key={j}>
              <span className="esqueleto" />
            </td>
          ))}
        </tr>
      ))}
    </>
  )
}
