import { CheckCheck, CircleAlert, Plus, Reply, Tag } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import * as t from '../domain/textos'
import type { Hotel, Resena } from '../domain/tipos'
import { mensajeDeError } from '../services/api'
import { consultaService, resenaService } from '../services/servicios'
import './ResenasPage.css'

const POR_TANDA = 25

export function ResenasPage() {
  const [params, setParams] = useSearchParams()
  const reservaParaCargar = params.get('reserva')
  const reservaParaVer = params.get('ver')

  const [hoteles, setHoteles] = useState<Hotel[]>([])
  const [hotelId, setHotelId] = useState('')
  const [sinResponder, setSinResponder] = useState(false)
  const [resenas, setResenas] = useState<Resena[] | null>(null)
  const [mostradas, setMostradas] = useState(POR_TANDA)
  const [cargando, setCargando] = useState(Boolean(reservaParaCargar))
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState('')
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    consultaService.catalogos().then((c) => setHoteles(c.hoteles)).catch((e) => setError(mensajeDeError(e)))
  }, [])

  useEffect(() => {
    setResenas(null)
    setMostradas(POR_TANDA)
    // Desde el detalle de una reserva se puede venir a ver solo su reseña.
    const pedido = reservaParaVer
      ? resenaService.buscar(Number(reservaParaVer)).then((r) => [r])
      : resenaService.listar(hotelId ? Number(hotelId) : undefined, sinResponder)
    pedido.then(setResenas).catch((e) => setError(mensajeDeError(e)))
  }, [hotelId, sinResponder, reservaParaVer, recarga])

  const actualizar = (nueva: Resena) => {
    setResenas((lista) => lista?.map((r) => (r.reservaId === nueva.reservaId ? nueva : r)) ?? null)
  }

  return (
    <>
      <header className="cabecera">
        <div>
          <h1>Reseñas</h1>
          <p>
            {resenas
              ? `${t.numero(resenas.length)} ${resenas.length === 1 ? 'reseña' : 'reseñas'}${sinResponder ? ' sin responder' : ''}`
              : 'Cargando…'}
            {reservaParaVer && (
              <>
                {' · '}
                <button type="button" className="boton boton--texto boton--chico" onClick={() => setParams({})}>
                  Ver todas las reseñas
                </button>
              </>
            )}
          </p>
        </div>
        {!cargando && (
          <button type="button" className="boton boton--primario" onClick={() => setCargando(true)}>
            <Plus size={18} /> Registrar reseña del huésped
          </button>
        )}
      </header>

      {cargando && (
        <CargarResena
          reservaInicial={reservaParaCargar ?? ''}
          onCerrar={() => {
            setCargando(false)
            setParams({})
          }}
          onGuardada={(r) => {
            setCargando(false)
            setParams({})
            setMensaje(`Se cargó la reseña de la reserva ${t.nroReserva(r.reservaId)}.`)
            setRecarga((n) => n + 1)
          }}
        />
      )}

      <div className="filtros filtros--resenas">
        <label className="campo">
          <span className="solo-lectores">Alojamiento</span>
          <select className="control" value={hotelId} onChange={(e) => setHotelId(e.target.value)}>
            <option value="">Todos los alojamientos</option>
            {hoteles.map((h) => (
              <option key={h.id} value={h.id}>
                {h.nombre} · {h.ciudad}
              </option>
            ))}
          </select>
        </label>
        <label className="filtros__check">
          <input type="checkbox" checked={sinResponder} onChange={(e) => setSinResponder(e.target.checked)} />
          Solo las que falta responder
        </label>
      </div>

      {mensaje && (
        <p className="aviso aviso--info resenas__mensaje" role="status">
          {mensaje}
        </p>
      )}
      {error && (
        <p className="aviso aviso--error resenas__mensaje" role="alert">
          <CircleAlert size={18} /> {error}
        </p>
      )}

      <ol className="resenas">
        {!resenas &&
          !error &&
          Array.from({ length: 4 }, (_, i) => (
            <li key={i} className="resena">
              <span className="esqueleto" style={{ width: '60%' }} />
              <span className="esqueleto" style={{ width: '40%' }} />
            </li>
          ))}
        {resenas?.slice(0, mostradas).map((r) => (
          <ItemResena key={r.reservaId} resena={r} onCambio={actualizar} />
        ))}
      </ol>

      {resenas && resenas.length === 0 && (
        <p className="resenas__vacio">{sinResponder ? 'No queda ninguna reseña sin responder.' : 'Este alojamiento todavía no tiene reseñas.'}</p>
      )}
      {resenas && resenas.length > mostradas && (
        <button type="button" className="boton boton--secundario resenas__mas" onClick={() => setMostradas((n) => n + POR_TANDA)}>
          Ver {Math.min(POR_TANDA, resenas.length - mostradas)} más ({t.numero(resenas.length - mostradas)} restantes)
        </button>
      )}
    </>
  )
}

function Puntaje({ valor, nombre }: { valor: number; nombre: string }) {
  return (
    <div className="puntaje">
      <span className="numero">{t.decimal(valor)}</span>
      <span className="puntaje__barra" aria-hidden="true">
        <span style={{ width: `${(valor / 5) * 100}%` }} />
      </span>
      <span className="puntaje__nombre">{nombre}</span>
    </div>
  )
}

function ItemResena({ resena: r, onCambio }: { resena: Resena; onCambio: (r: Resena) => void }) {
  const [editando, setEditando] = useState<'nada' | 'respuesta' | 'etiquetas'>('nada')
  const [respuesta, setRespuesta] = useState(r.respuestaAgencia?.texto ?? '')
  const [etiquetas, setEtiquetas] = useState(r.etiquetas.join(', '))
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const guardar = async (e: FormEvent, accion: () => Promise<Resena>) => {
    e.preventDefault()
    setGuardando(true)
    setError('')
    try {
      onCambio(await accion())
      setEditando('nada')
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <li className="resena">
      <div className="resena__puntajes">
        <Puntaje valor={r.puntajeViaje} nombre="Estadía" />
        <Puntaje valor={r.puntajeHotel} nombre="Alojamiento" />
      </div>

      <div className="resena__cuerpo">
        <p className="resena__comentario">{r.comentario ? `“${r.comentario}”` : <span className="tenue">El huésped no dejó comentario.</span>}</p>
        <p className="resena__datos">
          <Link to={`/reservas/${r.reservaId}`} className="numero">
            {t.nroReserva(r.reservaId)}
          </Link>{' '}
          · {r.hotel} · {r.destino}
        </p>

        <div className="resena__etiquetas">
          {r.etiquetas.map((e) => (
            <span key={e} className="chip">
              {e}
            </span>
          ))}
          {editando !== 'etiquetas' && (
            <button type="button" className="boton boton--texto boton--chico" onClick={() => setEditando('etiquetas')}>
              <Tag size={15} /> {r.etiquetas.length ? 'Cambiar etiquetas' : 'Etiquetar'}
            </button>
          )}
        </div>
        {editando === 'etiquetas' && (
          <form className="resena__form" onSubmit={(e) => guardar(e, () => resenaService.etiquetar(r.reservaId, etiquetas.split(',')))}>
            <label className="campo">
              <span>Etiquetas separadas por coma</span>
              <input className="control" value={etiquetas} onChange={(e) => setEtiquetas(e.target.value)} placeholder="Temas que toca la reseña" autoFocus />
            </label>
            <div className="voucher__botones">
              <button type="submit" className="boton boton--primario boton--chico" disabled={guardando}>
                Guardar etiquetas
              </button>
              <button type="button" className="boton boton--texto boton--chico" onClick={() => setEditando('nada')}>
                Volver
              </button>
            </div>
          </form>
        )}

        {r.respuestaAgencia && editando !== 'respuesta' && (
          <div className="resena__respuesta">
            <p className="resena__respondida">
              <CheckCheck size={16} /> Respondida el {t.fecha(r.respuestaAgencia.fecha)}
            </p>
            <p>{r.respuestaAgencia.texto}</p>
            <button type="button" className="boton boton--texto boton--chico" onClick={() => setEditando('respuesta')}>
              Corregir la respuesta
            </button>
          </div>
        )}
        {!r.respuestaAgencia && editando !== 'respuesta' && (
          <button type="button" className="boton boton--secundario boton--chico resena__responder" onClick={() => setEditando('respuesta')}>
            <Reply size={16} /> Responder
          </button>
        )}
        {editando === 'respuesta' && (
          <form className="resena__form" onSubmit={(e) => guardar(e, () => resenaService.responder(r.reservaId, respuesta))}>
            <label className="campo">
              <span>Respuesta del alojamiento</span>
              <textarea className="control" value={respuesta} onChange={(e) => setRespuesta(e.target.value)} maxLength={500} required placeholder="Lo que le querés contestar al huésped" autoFocus />
            </label>
            <div className="voucher__botones">
              <button type="submit" className="boton boton--primario boton--chico" disabled={guardando}>
                Guardar respuesta
              </button>
              <button type="button" className="boton boton--texto boton--chico" onClick={() => setEditando('nada')}>
                Volver
              </button>
            </div>
          </form>
        )}
        {error && (
          <p className="aviso aviso--error" role="alert">
            <CircleAlert size={18} /> {error}
          </p>
        )}
      </div>
    </li>
  )
}

function CargarResena({ reservaInicial, onCerrar, onGuardada }: { reservaInicial: string; onCerrar: () => void; onGuardada: (r: Resena) => void }) {
  const [reservaId, setReservaId] = useState(reservaInicial)
  const [puntajeViaje, setPuntajeViaje] = useState('')
  const [puntajeHotel, setPuntajeHotel] = useState('')
  const [comentario, setComentario] = useState('')
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  const guardar = async (e: FormEvent) => {
    e.preventDefault()
    setGuardando(true)
    setError('')
    try {
      onGuardada(
        await resenaService.crear({
          reservaId: Number(reservaId),
          puntajeViaje: Number(puntajeViaje),
          puntajeHotel: Number(puntajeHotel),
          comentario,
        }),
      )
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setGuardando(false)
    }
  }

  return (
    <form className="formulario__bloque cargar-resena" onSubmit={guardar}>
      <div>
        <h2>Registrar la reseña que dejó el huésped</h2>
        <p className="tenue">Pasá acá la opinión que el huésped dejó por mail o en el libro de visitas. Solo para estadías completadas, una por reserva.</p>
      </div>
      <div className="formulario__grilla">
        <label className="campo">
          <span>Número de reserva</span>
          <input className="control" type="number" min={1} value={reservaId} onChange={(e) => setReservaId(e.target.value)} required />
        </label>
        <label className="campo">
          <span>Puntaje de la estadía</span>
          <input className="control" type="number" min={1} max={5} step={0.1} value={puntajeViaje} onChange={(e) => setPuntajeViaje(e.target.value)} required />
          <small>De 1 a 5, con un decimal.</small>
        </label>
        <label className="campo">
          <span>Puntaje del alojamiento</span>
          <input className="control" type="number" min={1} max={5} step={0.1} value={puntajeHotel} onChange={(e) => setPuntajeHotel(e.target.value)} required />
          <small>De 1 a 5, con un decimal.</small>
        </label>
        <label className="campo formulario__ancho">
          <span>Comentario del huésped</span>
          <textarea className="control" value={comentario} onChange={(e) => setComentario(e.target.value)} maxLength={255} placeholder="Lo que escribió el huésped, tal cual" />
        </label>
      </div>
      {error && (
        <p className="aviso aviso--error" role="alert">
          <CircleAlert size={18} /> {error}
        </p>
      )}
      <div className="formulario__pie">
        <button type="submit" className="boton boton--primario" disabled={guardando}>
          Guardar reseña
        </button>
        <button type="button" className="boton boton--secundario" onClick={onCerrar}>
          Cancelar
        </button>
      </div>
    </form>
  )
}
