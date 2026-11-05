import { ChevronLeft, ChevronRight, CircleAlert, Plus, Search } from 'lucide-react'
import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { BarraEstadia, Sello } from '../components/Sello'
import { Voucher } from '../components/Voucher'
import { useMotor } from '../context/MotorContext'
import * as t from '../domain/textos'
import type { Alcance, Catalogos, EstadoReserva, FiltroReservas, Pagina, Reserva } from '../domain/tipos'
import { mensajeDeError } from '../services/api'
import { consultaService, reservaService } from '../services/servicios'
import './ReservasPage.css'

const PESTANIAS: { alcance: Alcance; nombre: string }[] = [
  { alcance: 'ACTIVAS', nombre: 'Activas' },
  { alcance: 'TODAS', nombre: 'Historial completo' },
  { alcance: 'BAJAS', nombre: 'Dadas de baja' },
]

export function ReservasPage() {
  const { motor } = useMotor()
  const navigate = useNavigate()
  const { id } = useParams()
  const location = useLocation()
  const idElegido = id ? Number(id) : null

  const [filtro, setFiltro] = useState<FiltroReservas>({ alcance: 'ACTIVAS', pagina: 0 })
  const [texto, setTexto] = useState('')
  const [pagina, setPagina] = useState<Pagina<Reserva> | null>(null)
  const [catalogos, setCatalogos] = useState<Catalogos | null>(null)
  const [elegida, setElegida] = useState<Reserva | null>(null)
  const [error, setError] = useState('')
  const [mensaje, setMensaje] = useState<string>((location.state as { mensaje?: string } | null)?.mensaje ?? '')
  const [ocupado, setOcupado] = useState(false)
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    consultaService.catalogos().then(setCatalogos).catch((e) => setError(mensajeDeError(e)))
  }, [motor])

  useEffect(() => {
    const espera = setTimeout(() => setFiltro((f) => ((f.texto ?? '') === texto ? f : { ...f, texto, pagina: 0 })), 300)
    return () => clearTimeout(espera)
  }, [texto])

  useEffect(() => {
    setError('')
    reservaService
      .listar(filtro)
      .then(setPagina)
      .catch((e) => setError(mensajeDeError(e)))
  }, [filtro, motor, recarga])

  useEffect(() => {
    if (idElegido === null) {
      setElegida(null)
      return
    }
    reservaService
      .buscar(idElegido)
      .then(setElegida)
      .catch((e) => {
        setElegida(null)
        setError(mensajeDeError(e))
      })
  }, [idElegido, motor, recarga])

  const cambiar = (cambios: Partial<FiltroReservas>) => setFiltro((f) => ({ ...f, ...cambios, pagina: 0 }))

  const ejecutar = async (accion: () => Promise<unknown>, exito: string) => {
    setOcupado(true)
    setError('')
    try {
      await accion()
      setMensaje(exito)
      setRecarga((n) => n + 1)
    } catch (e) {
      setError(mensajeDeError(e))
    } finally {
      setOcupado(false)
    }
  }

  const total = pagina?.totalElementos ?? 0
  const desde = pagina && total > 0 ? pagina.pagina * 20 + 1 : 0
  const hasta = pagina ? Math.min((pagina.pagina + 1) * 20, total) : 0

  return (
    <>
      <header className="cabecera">
        <div>
          <h1>Reservas</h1>
          <p>{pagina ? `${t.numero(total)} ${filtro.alcance === 'ACTIVAS' ? 'activas' : 'en esta vista'}` : error ? 'Sin datos' : 'Cargando…'}</p>
        </div>
        <Link className="boton boton--primario" to="/reservas/nueva">
          <Plus size={18} /> Nueva reserva
        </Link>
      </header>

      <div className="pestanias" role="tablist" aria-label="Qué reservas ver">
        {PESTANIAS.map((p) => (
          <button
            key={p.alcance}
            type="button"
            role="tab"
            aria-selected={filtro.alcance === p.alcance}
            className="pestanias__item"
            onClick={() => cambiar({ alcance: p.alcance })}
          >
            {p.nombre}
          </button>
        ))}
      </div>

      <div className="filtros">
        <label className="campo filtros__buscar">
          <span className="solo-lectores">Buscar por huésped</span>
          <Search size={18} aria-hidden="true" />
          <input className="control" value={texto} onChange={(e) => setTexto(e.target.value)} placeholder="Buscar por nombre del huésped" />
        </label>
        <label className="campo">
          <span className="solo-lectores">Estado</span>
          <select className="control" value={filtro.estado ?? ''} onChange={(e) => cambiar({ estado: e.target.value as EstadoReserva | '' })}>
            <option value="">Todos los estados</option>
            {Object.entries(t.ESTADOS).map(([valor, nombre]) => (
              <option key={valor} value={valor}>
                {nombre}
              </option>
            ))}
          </select>
        </label>
        <label className="campo">
          <span className="solo-lectores">Alojamiento</span>
          <select className="control" value={filtro.hotelId ?? ''} onChange={(e) => cambiar({ hotelId: e.target.value ? Number(e.target.value) : '' })}>
            <option value="">Todos los alojamientos</option>
            {catalogos?.hoteles.map((h) => (
              <option key={h.id} value={h.id}>
                {h.nombre} · {h.ciudad}
              </option>
            ))}
          </select>
        </label>
        <label className="filtros__check">
          <input type="checkbox" checked={!!filtro.salidaPendiente} onChange={(e) => cambiar({ salidaPendiente: e.target.checked })} />
          Salidas por registrar
        </label>
      </div>

      {mensaje && (
        <p className="aviso aviso--info reservas__mensaje" role="status" onClick={() => setMensaje('')}>
          {mensaje}
        </p>
      )}
      {error && (
        <p className="aviso aviso--error reservas__mensaje" role="alert">
          <CircleAlert size={18} /> {error}
        </p>
      )}

      <div className={`reservas${elegida ? ' reservas--con-detalle' : ''}`}>
        <div className="tabla-marco">
          <table className="tabla">
            <thead>
              <tr>
                <th>Reserva</th>
                <th>Huésped</th>
                <th>Alojamiento</th>
                <th>Estadía</th>
                <th>Estado</th>
                <th className="derecha">Total</th>
              </tr>
            </thead>
            <tbody>
              {!pagina &&
                !error &&
                Array.from({ length: 8 }, (_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 6 }, (_, j) => (
                      <td key={j}>
                        <span className="esqueleto" />
                      </td>
                    ))}
                  </tr>
                ))}
              {pagina?.contenido.map((r) => (
                <tr
                  key={r.id}
                  className="fila-clic"
                  tabIndex={0}
                  aria-selected={r.id === idElegido}
                  onClick={() => navigate(`/reservas/${r.id}`)}
                  onKeyDown={(e) => e.key === 'Enter' && navigate(`/reservas/${r.id}`)}
                >
                  <td className="numero">{t.nroReserva(r.id)}</td>
                  <td>
                    <div className="dos-lineas">
                      <strong>{r.cliente.nombre}</strong>
                      <span>{t.personas(r.adultos, r.ninos)}</span>
                    </div>
                  </td>
                  <td>
                    <div className="dos-lineas">
                      <strong>{r.hotel.nombre}</strong>
                      <span>
                        {r.hotel.ciudad}, {t.pais(r.hotel.pais)}
                      </span>
                    </div>
                  </td>
                  <td>
                    <div className="dos-lineas">
                      <strong className="sin-corte">
                        {t.fechaCorta(r.checkIn)} → {t.fecha(r.checkOut)}
                      </strong>
                      <span className="reservas__noches">
                        <BarraEstadia noches={r.noches} /> {t.noches(r.noches)}
                      </span>
                    </div>
                  </td>
                  <td>
                    <Sello estado={r.estado} activa={r.activa} />
                  </td>
                  <td className="derecha numero">{t.plata(r.costoTotal)}</td>
                </tr>
              ))}
              {pagina && pagina.contenido.length === 0 && (
                <tr>
                  <td colSpan={6} className="reservas__vacio">
                    {filtro.alcance === 'BAJAS'
                      ? 'Todavía no se dio de baja ninguna reserva. Cuando des de baja una, aparece acá con su sello.'
                      : 'No hay reservas con estos filtros. Probá sacando alguno.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
          {pagina && total > 0 && (
            <div className="paginador">
              <span className="tenue">
                {t.numero(desde)}–{t.numero(hasta)} de {t.numero(total)}
              </span>
              <div>
                <button
                  type="button"
                  className="boton boton--secundario boton--chico"
                  disabled={pagina.pagina === 0}
                  onClick={() => setFiltro((f) => ({ ...f, pagina: (f.pagina ?? 0) - 1 }))}
                >
                  <ChevronLeft size={16} /> Anterior
                </button>
                <button
                  type="button"
                  className="boton boton--secundario boton--chico"
                  disabled={pagina.pagina + 1 >= pagina.totalPaginas}
                  onClick={() => setFiltro((f) => ({ ...f, pagina: (f.pagina ?? 0) + 1 }))}
                >
                  Siguiente <ChevronRight size={16} />
                </button>
              </div>
            </div>
          )}
        </div>

        {elegida && (
          <Voucher
            key={`${elegida.id}-${elegida.estado}-${elegida.activa}`}
            reserva={elegida}
            motivos={catalogos?.motivosCancelacion ?? []}
            ocupado={ocupado}
            onCerrar={() => navigate('/reservas')}
            onCancelar={(motivo) => ejecutar(() => reservaService.cancelar(elegida.id, motivo), `Se canceló la reserva ${t.nroReserva(elegida.id)}.`)}
            onCompletar={() => ejecutar(() => reservaService.completar(elegida.id), `Se registró la salida de la reserva ${t.nroReserva(elegida.id)}.`)}
            onDarDeBaja={() => ejecutar(() => reservaService.darDeBaja(elegida.id), `La reserva ${t.nroReserva(elegida.id)} se dio de baja y quedó en el historial.`)}
          />
        )}
      </div>
    </>
  )
}
