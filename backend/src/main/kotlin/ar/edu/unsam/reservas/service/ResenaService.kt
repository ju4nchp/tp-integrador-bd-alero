package ar.edu.unsam.reservas.service

import ar.edu.unsam.reservas.domain.BusinessException
import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.domain.NotFoundException
import ar.edu.unsam.reservas.domain.Resena
import ar.edu.unsam.reservas.domain.RespuestaAgencia
import ar.edu.unsam.reservas.dto.ResenaRequest
import ar.edu.unsam.reservas.repository.SelectorDeMotor
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class ResenaService(private val selector: SelectorDeMotor) {

    fun listar(motor: Motor, hotelId: Int?, sinResponder: Boolean) = selector.resenas(motor).listar(hotelId, sinResponder)

    fun buscar(motor: Motor, reservaId: Int): Resena =
        selector.resenas(motor).buscar(reservaId) ?: throw NotFoundException("La reserva $reservaId no tiene reseña")

    fun crear(motor: Motor, datos: ResenaRequest): Resena {
        val reservaId = datos.reservaId ?: throw BusinessException("Falta la reserva")
        val reserva = selector.reservas(motor).buscarPorId(reservaId) ?: throw NotFoundException("No existe la reserva $reservaId")
        if (!reserva.activa || reserva.estado != EstadoReserva.COMPLETADA) {
            throw BusinessException("Solo se cargan reseñas de estadías completadas (la reserva $reservaId está ${reserva.estado.etiqueta})")
        }
        if (selector.resenas(motor).buscar(reservaId) != null) throw BusinessException("La reserva $reservaId ya tiene su reseña")
        val comentario = datos.comentario?.trim()?.takeIf { it.isNotEmpty() }
        if ((comentario?.length ?: 0) > 255) throw BusinessException("El comentario puede tener hasta 255 caracteres")
        return selector.resenas(motor).crear(
            Resena(
                reservaId = reservaId,
                hotelId = reserva.hotel.id,
                hotel = reserva.hotel.nombre,
                destino = "${reserva.hotel.ciudad}, ${reserva.hotel.pais}",
                puntajeViaje = Resena.validarPuntaje("puntaje de la estadía", datos.puntajeViaje),
                puntajeHotel = Resena.validarPuntaje("puntaje del alojamiento", datos.puntajeHotel),
                comentario = comentario,
            ),
        )
    }

    fun responder(motor: Motor, reservaId: Int, texto: String?): Resena {
        val respuesta = texto?.trim()?.takeIf { it.isNotEmpty() } ?: throw BusinessException("Escribí la respuesta")
        if (respuesta.length > 500) throw BusinessException("La respuesta puede tener hasta 500 caracteres")
        buscar(motor, reservaId)
        val ahora = LocalDateTime.now(ZoneId.of("America/Argentina/Buenos_Aires")).truncatedTo(ChronoUnit.SECONDS)
        selector.resenas(motor).guardarRespuesta(reservaId, RespuestaAgencia(respuesta, ahora))
        return buscar(motor, reservaId)
    }

    fun etiquetar(motor: Motor, reservaId: Int, etiquetas: List<String>): Resena {
        buscar(motor, reservaId)
        selector.resenas(motor).guardarEtiquetas(reservaId, Resena.normalizarEtiquetas(etiquetas))
        return buscar(motor, reservaId)
    }
}
