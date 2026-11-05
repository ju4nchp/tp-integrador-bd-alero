import { ArrowLeft, CircleAlert, Search } from 'lucide-react'
import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { BarraEstadia } from '../components/Sello'
import { useMotor } from '../context/MotorContext'
import * as t from '../domain/textos'
import type { Catalogos, Cliente, DatosReserva, EstadoReserva } from '../domain/tipos'
import { mensajeDeError } from '../services/api'
import { consultaService, reservaService } from '../services/servicios'
import './ReservaFormPage.css'

const HOY = new Date().toISOString().slice(0, 10)

interface Formulario {
  hotelId: string
  checkIn: string
  checkOut: string
  adultos: string
  ninos: string
  habitaciones: string
  regimen: string
  medioPago: string
  transporte: string
  costoTotal: string
  descuento: string
  estado: EstadoReserva
}

const VACIO: Formulario = {
  hotelId: '',
  checkIn: '',
  checkOut: '',
  adultos: '2',
  ninos: '0',
  habitaciones: '1',
  regimen: '',
  medioPago: '',
  transporte: '',
  costoTotal: '',
  descuento: '0',
  estado: 'CONFIRMADA',
}

const nochesEntre = (desde: string, hasta: string) =>
  desde && hasta ? Math.round((Date.parse(hasta) - Date.parse(desde)) / 86_400_000) : 0

export function ReservaFormPage() {
  const { id } = useParams()
  const editando = id !== undefined
  const navigate = useNavigate()
  const { motor } = useMotor()

  const [catalogos, setCatalogos] = useState<Catalogos | null>(null)
  const [form, setForm] = useState<Formulario>(VACIO)
  const [huesped, setHuesped] = useState<Cliente | null>(null)
  const [esNuevo, setEsNuevo] = useState(false)
  const [busqueda, setBusqueda] = useState('')
  const [encontrados, setEncontrados] = useState<Cliente[]>([])
  const [nuevo, setNuevo] = useState({ nombre: '', edad: '', genero: '', ciudad: '' })
  const [error, setError] = useState('')
  const [guardando, setGuardando] = useState(false)

  useEffect(() => {
    consultaService.catalogos().then(setCatalogos).catch((e) => setError(mensajeDeError(e)))
  }, [motor])

  useEffect(() => {
    if (!editando) return
    reservaService
      .buscar(Number(id))
      .then((r) => {
        setHuesped(r.cliente)
        setForm({
          hotelId: String(r.hotel.id),
          checkIn: r.checkIn,
          checkOut: r.checkOut,
          adultos: String(r.adultos),
          ninos: String(r.ninos),
          habitaciones: String(r.habitaciones),
          regimen: r.regimen,
          medioPago: r.medioPago,
          transporte: r.transporte,
          costoTotal: String(r.costoTotal),
          descuento: String(r.descuento),
          estado: r.estado,
        })
      })
      .catch((e) => setError(mensajeDeError(e)))
  }, [editando, id, motor])

  useEffect(() => {
    if (editando || esNuevo || busqueda.trim().length < 2) {
      setEncontrados([])
      return
    }
    const espera = setTimeout(() => {
      consultaService.clientes(busqueda).then(setEncontrados).catch(() => setEncontrados([]))
    }, 250)
    return () => clearTimeout(espera)
  }, [busqueda, editando, esNuevo, motor])

  const cambiar = (campo: keyof Formulario) => (e: { target: { value: string } }) =>
    setForm((f) => ({ ...f, [campo]: e.target.value }))

  const guardar = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    const ciudad = catalogos?.ciudades.find((c) => `${c.nombre}|${c.pais}` === nuevo.ciudad)
    if (!editando && !esNuevo && !huesped) {
      setError('Elegí al huésped de la lista o marcá que es un huésped nuevo.')
      return
    }
    const datos: DatosReserva = {
      ...(editando ? {} : esNuevo
        ? { clienteNuevo: { nombre: nuevo.nombre, edad: Number(nuevo.edad), genero: nuevo.genero, ciudad: ciudad?.nombre ?? '', pais: ciudad?.pais ?? '' } }
        : { clienteId: huesped?.id }),
      hotelId: Number(form.hotelId),
      checkIn: form.checkIn,
      checkOut: form.checkOut,
      adultos: Number(form.adultos),
      ninos: Number(form.ninos),
      habitaciones: Number(form.habitaciones),
      regimen: form.regimen,
      medioPago: form.medioPago,
      transporte: form.transporte,
      costoTotal: Number(form.costoTotal),
      descuento: Number(form.descuento || 0),
      estado: form.estado,
    }
    setGuardando(true)
    try {
      const guardada = editando ? await reservaService.modificar(Number(id), datos) : await reservaService.crear(datos)
      navigate(`/reservas/${guardada.id}`, {
        state: { mensaje: editando ? `Se guardaron los cambios de la reserva ${t.nroReserva(guardada.id)}.` : `Se creó la reserva ${t.nroReserva(guardada.id)}.` },
      })
    } catch (err) {
      setError(mensajeDeError(err))
    } finally {
      setGuardando(false)
    }
  }

  const noches = nochesEntre(form.checkIn, form.checkOut)

  return (
    <form className="formulario" onSubmit={guardar}>
      <header className="cabecera">
        <div>
          <Link className="boton boton--texto boton--chico formulario__volver" to={editando ? `/reservas/${id}` : '/reservas'}>
            <ArrowLeft size={16} /> Volver a reservas
          </Link>
          <h1>{editando ? `Editar la reserva ${t.nroReserva(Number(id))}` : 'Nueva reserva'}</h1>
        </div>
      </header>

      <section className="formulario__bloque">
        <h2>Huésped titular</h2>
        {editando ? (
          <p className="formulario__fijo">
            <strong>{huesped?.nombre}</strong>
            <span className="tenue"> · el titular no se cambia; si es otra persona, cargá una reserva nueva.</span>
          </p>
        ) : (
          <>
            <div className="pestanias" role="radiogroup" aria-label="Tipo de huésped">
              <button type="button" role="radio" aria-checked={!esNuevo} className="pestanias__item" onClick={() => setEsNuevo(false)}>
                Ya se alojó antes
              </button>
              <button type="button" role="radio" aria-checked={esNuevo} className="pestanias__item" onClick={() => setEsNuevo(true)}>
                Es un huésped nuevo
              </button>
            </div>

            {!esNuevo && (
              <div className="formulario__huesped">
                {huesped ? (
                  <p className="formulario__fijo">
                    <strong>{huesped.nombre}</strong>
                    <span className="tenue">
                      {' '}
                      · {huesped.edad} años · {huesped.ciudad}, {t.pais(huesped.pais)}
                    </span>
                    <button type="button" className="boton boton--texto boton--chico" onClick={() => setHuesped(null)}>
                      Cambiar
                    </button>
                  </p>
                ) : (
                  <label className="campo filtros__buscar">
                    <span className="solo-lectores">Buscar huésped</span>
                    <Search size={18} aria-hidden="true" />
                    <input className="control" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} placeholder="Nombre o apellido del huésped" />
                  </label>
                )}
                {!huesped && encontrados.length > 0 && (
                  <ul className="formulario__resultados">
                    {encontrados.map((c) => (
                      <li key={c.id}>
                        <button type="button" onClick={() => setHuesped(c)}>
                          <strong>{c.nombre}</strong>
                          <span className="tenue">
                            {c.edad} años · {c.ciudad}, {t.pais(c.pais)}
                          </span>
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}

            {esNuevo && (
              <div className="formulario__grilla">
                <label className="campo formulario__ancho">
                  <span>Nombre y apellido</span>
                  <input className="control" value={nuevo.nombre} onChange={(e) => setNuevo({ ...nuevo, nombre: e.target.value })} required maxLength={150} />
                </label>
                <label className="campo">
                  <span>Edad</span>
                  <input className="control" type="number" min={18} value={nuevo.edad} onChange={(e) => setNuevo({ ...nuevo, edad: e.target.value })} required />
                  <small>El titular tiene que ser mayor de edad.</small>
                </label>
                <label className="campo">
                  <span>Género</span>
                  <select className="control" value={nuevo.genero} onChange={(e) => setNuevo({ ...nuevo, genero: e.target.value })} required>
                    <option value="">Elegí una opción</option>
                    <option value="Female">Femenino</option>
                    <option value="Male">Masculino</option>
                  </select>
                </label>
                <label className="campo formulario__ancho">
                  <span>De dónde viene</span>
                  <select className="control" value={nuevo.ciudad} onChange={(e) => setNuevo({ ...nuevo, ciudad: e.target.value })} required>
                    <option value="">Elegí la ciudad de origen</option>
                    {catalogos?.ciudades.map((c) => (
                      <option key={`${c.nombre}|${c.pais}`} value={`${c.nombre}|${c.pais}`}>
                        {c.nombre}, {t.pais(c.pais)}
                      </option>
                    ))}
                  </select>
                </label>
              </div>
            )}
          </>
        )}
      </section>

      <section className="formulario__bloque">
        <h2>Estadía</h2>
        <div className="formulario__grilla">
          <label className="campo formulario__ancho">
            <span>Alojamiento</span>
            <select className="control" value={form.hotelId} onChange={cambiar('hotelId')} required>
              <option value="">Elegí el alojamiento</option>
              {catalogos?.hoteles.map((h) => (
                <option key={h.id} value={h.id}>
                  {h.nombre} · {h.ciudad}, {t.pais(h.pais)}
                </option>
              ))}
            </select>
          </label>
          <label className="campo">
            <span>Llegada (check-in)</span>
            <input className="control" type="date" value={form.checkIn} min={editando ? undefined : HOY} onChange={cambiar('checkIn')} required />
          </label>
          <label className="campo">
            <span>Salida (check-out)</span>
            <input className="control" type="date" value={form.checkOut} onChange={cambiar('checkOut')} required />
          </label>
          <p className="formulario__noches formulario__ancho" aria-live="polite">
            {noches > 0 ? (
              <>
                <BarraEstadia noches={noches} /> {t.noches(noches)}
              </>
            ) : (
              <span className="tenue">Las noches se calculan solas con las dos fechas.</span>
            )}
          </p>
          <label className="campo">
            <span>Adultos</span>
            <input className="control" type="number" min={1} value={form.adultos} onChange={cambiar('adultos')} required />
          </label>
          <label className="campo">
            <span>Niños</span>
            <input className="control" type="number" min={0} value={form.ninos} onChange={cambiar('ninos')} required />
          </label>
          <label className="campo">
            <span>Habitaciones</span>
            <input className="control" type="number" min={1} value={form.habitaciones} onChange={cambiar('habitaciones')} required />
            <small>No pueden ser más que los huéspedes.</small>
          </label>
          <label className="campo">
            <span>Régimen</span>
            <select className="control" value={form.regimen} onChange={cambiar('regimen')} required>
              <option value="">Elegí el régimen</option>
              {catalogos?.regimenes.map((r) => (
                <option key={r} value={r}>
                  {t.regimen(r)}
                </option>
              ))}
            </select>
          </label>
          <label className="campo">
            <span>Cómo llega</span>
            <select className="control" value={form.transporte} onChange={cambiar('transporte')} required>
              <option value="">Elegí el transporte</option>
              {catalogos?.transportes.map((x) => (
                <option key={x} value={x}>
                  {t.transporte(x)}
                </option>
              ))}
            </select>
          </label>
        </div>
      </section>

      <section className="formulario__bloque">
        <h2>Pago</h2>
        <div className="formulario__grilla">
          <label className="campo">
            <span>Medio de pago</span>
            <select className="control" value={form.medioPago} onChange={cambiar('medioPago')} required>
              <option value="">Elegí el medio de pago</option>
              {catalogos?.mediosPago.map((m) => (
                <option key={m} value={m}>
                  {t.medioPago(m)}
                </option>
              ))}
            </select>
          </label>
          <label className="campo">
            <span>Costo total</span>
            <input className="control" type="number" min={1} step="0.01" value={form.costoTotal} onChange={cambiar('costoTotal')} required />
          </label>
          <label className="campo">
            <span>Descuento</span>
            <input className="control" type="number" min={0} step="0.01" value={form.descuento} onChange={cambiar('descuento')} />
            <small>No puede superar el costo total.</small>
          </label>
          <label className="campo">
            <span>Estado</span>
            <select className="control" value={form.estado} onChange={cambiar('estado')}>
              <option value="CONFIRMADA">Confirmada</option>
              <option value="PENDIENTE">Pendiente de confirmar</option>
            </select>
          </label>
        </div>
      </section>

      {error && (
        <p className="aviso aviso--error" role="alert">
          <CircleAlert size={18} /> {error}
        </p>
      )}

      <div className="formulario__pie">
        <button type="submit" className="boton boton--primario" disabled={guardando}>
          {guardando ? 'Guardando…' : editando ? 'Guardar cambios' : 'Crear reserva'}
        </button>
        <Link className="boton boton--secundario" to={editando ? `/reservas/${id}` : '/reservas'}>
          Cancelar
        </Link>
      </div>
    </form>
  )
}
