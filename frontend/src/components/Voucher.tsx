import { ArchiveX, Ban, CircleCheck, DoorOpen, MessageSquareText, Pencil, X } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import * as t from '../domain/textos'
import type { Reserva } from '../domain/tipos'
import { Sello } from './Sello'
import './Voucher.css'

interface Props {
  reserva: Reserva
  motivos: string[]
  tieneResena: boolean | null
  ocupado: boolean
  onCerrar: () => void
  onConfirmar: () => void
  onCancelar: (motivo: string) => void
  onCompletar: () => void
  onDarDeBaja: () => void
}

type Paso = 'nada' | 'cancelar' | 'baja'

export function Voucher({ reserva: r, motivos, tieneResena, ocupado, onCerrar, onConfirmar, onCancelar, onCompletar, onDarDeBaja }: Props) {
  const [paso, setPaso] = useState<Paso>('nada')
  const [motivo, setMotivo] = useState('')

  return (
    <article className="voucher" aria-label={`Reserva ${t.nroReserva(r.id)}`}>
      <header className="voucher__talon">
        <div>
          <h2 className="voucher__nro numero">{t.nroReserva(r.id)}</h2>
          <span className="tenue">Reservada el {t.fecha(r.fechaReserva)}</span>
        </div>
        <Sello estado={r.estado} activa={r.activa} grande />
        <button type="button" className="voucher__cerrar" onClick={onCerrar} aria-label="Cerrar el voucher">
          <X size={18} />
        </button>
      </header>

      <div className="voucher__troquel" aria-hidden="true" />

      <div className="voucher__cuerpo">
        <section className="voucher__estadia">
          <div>
            <span className="voucher__rotulo">Llegada</span>
            <strong>{t.fecha(r.checkIn)}</strong>
          </div>
          <div className="voucher__noches">
            <span>{t.noches(r.noches)}</span>
          </div>
          <div>
            <span className="voucher__rotulo">Salida</span>
            <strong>{t.fecha(r.checkOut)}</strong>
          </div>
        </section>

        <dl className="voucher__datos">
          <div>
            <dt>Huésped</dt>
            <dd>
              {r.cliente.nombre}
              <span>
                {r.cliente.edad} años · {t.genero(r.cliente.genero)} · viene de {r.cliente.ciudad}, {t.pais(r.cliente.pais)}
              </span>
            </dd>
          </div>
          <div>
            <dt>Alojamiento</dt>
            <dd>
              {r.hotel.nombre}
              <span>
                {r.hotel.ciudad}, {t.pais(r.hotel.pais)}
              </span>
            </dd>
          </div>
          <div>
            <dt>Huéspedes</dt>
            <dd>
              {t.personas(r.adultos, r.ninos)}
              <span>
                {r.habitaciones} {r.habitaciones === 1 ? 'habitación' : 'habitaciones'}
              </span>
            </dd>
          </div>
          <div>
            <dt>Régimen</dt>
            <dd>{t.regimen(r.regimen)}</dd>
          </div>
          <div>
            <dt>Pago</dt>
            <dd>{t.medioPago(r.medioPago)}</dd>
          </div>
          <div>
            <dt>Llega en</dt>
            <dd>{t.transporte(r.transporte)}</dd>
          </div>
        </dl>

        <div className="voucher__importe">
          <div>
            <span className="voucher__rotulo">Total a pagar</span>
            <span className="numero voucher__total">{t.plata(r.totalFinal)}</span>
          </div>
          <div>
            <span className="voucher__rotulo">Precio</span>
            <span className="numero">{t.plata(r.costoTotal)}</span>
          </div>
          <div>
            <span className="voucher__rotulo">Descuento</span>
            <span className="numero">− {t.plata(r.descuento)}</span>
          </div>
        </div>

        {r.motivoCancelacion && (
          <p className="aviso aviso--error">
            <Ban size={18} />
            Cancelada por: {t.motivo(r.motivoCancelacion)}
          </p>
        )}
        {!r.activa && (
          <p className="aviso aviso--info">Reserva dada de baja: no aparece en la lista de activas ni cuenta en las estadísticas.</p>
        )}
      </div>

      <footer className="voucher__acciones">
        {r.modificable && paso === 'nada' && (
          <>
            {r.confirmable && (
              <button type="button" className="boton boton--primario boton--chico" disabled={ocupado} onClick={onConfirmar}>
                <CircleCheck size={16} /> Confirmar
              </button>
            )}
            <Link className="boton boton--secundario boton--chico" to={`/reservas/${r.id}/editar`}>
              <Pencil size={16} /> Editar
            </Link>
            {r.completable && (
              <button type="button" className="boton boton--primario boton--chico" disabled={ocupado} onClick={onCompletar}>
                <DoorOpen size={16} /> Registrar salida
              </button>
            )}
            <button type="button" className="boton boton--peligro boton--chico" onClick={() => setPaso('cancelar')}>
              <Ban size={16} /> Cancelar reserva
            </button>
            <button type="button" className="boton boton--texto boton--chico voucher__baja" onClick={() => setPaso('baja')}>
              <ArchiveX size={16} /> Dar de baja
            </button>
          </>
        )}

        {paso === 'cancelar' && (
          <form
            className="voucher__confirmar"
            onSubmit={(e) => {
              e.preventDefault()
              if (motivo) onCancelar(motivo)
            }}
          >
            <label className="campo">
              <span>Motivo de la cancelación</span>
              <select className="control" value={motivo} onChange={(e) => setMotivo(e.target.value)} required>
                <option value="">Elegí el motivo</option>
                {motivos.map((m) => (
                  <option key={m} value={m}>
                    {t.motivo(m)}
                  </option>
                ))}
              </select>
            </label>
            <p className="tenue">Usalo cuando el huésped no viene: la reserva queda cancelada y cuenta en la tasa de cancelación.</p>
            <div className="voucher__botones">
              <button type="submit" className="boton boton--peligro boton--chico" disabled={!motivo || ocupado}>
                Confirmar cancelación
              </button>
              <button type="button" className="boton boton--texto boton--chico" onClick={() => setPaso('nada')}>
                Volver
              </button>
            </div>
          </form>
        )}

        {paso === 'baja' && (
          <div className="voucher__confirmar">
            <p>
              Usalo solo si la reserva se cargó por error. Deja de aparecer en la lista y en las estadísticas, y la vas a encontrar
              en la pestaña <strong>Dadas de baja</strong>. Si el huésped no viene, usá <strong>Cancelar reserva</strong>.
            </p>
            <div className="voucher__botones">
              <button type="button" className="boton boton--primario boton--chico" disabled={ocupado} onClick={onDarDeBaja}>
                Sí, dar de baja
              </button>
              <button type="button" className="boton boton--texto boton--chico" onClick={() => setPaso('nada')}>
                Volver
              </button>
            </div>
          </div>
        )}

        {r.activa && r.estado === 'COMPLETADA' && tieneResena !== null && (
          <Link className="boton boton--secundario boton--chico" to={tieneResena ? `/resenas?ver=${r.id}` : `/resenas?reserva=${r.id}`}>
            <MessageSquareText size={16} /> {tieneResena ? 'Ver la reseña' : 'Registrar la reseña del huésped'}
          </Link>
        )}
      </footer>
    </article>
  )
}
