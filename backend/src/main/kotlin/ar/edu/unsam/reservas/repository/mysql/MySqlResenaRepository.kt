package ar.edu.unsam.reservas.repository.mysql

import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.domain.NotFoundException
import ar.edu.unsam.reservas.domain.PromedioHotel
import ar.edu.unsam.reservas.domain.Resena
import ar.edu.unsam.reservas.domain.RespuestaAgencia
import ar.edu.unsam.reservas.repository.ResenaRepository
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode

@Repository
class MySqlResenaRepository(
    private val resenas: ResenaJpaRepository,
    private val reservas: ReservaJpaRepository,
    private val jdbc: JdbcTemplate,
) : ResenaRepository {

    override val motor = Motor.MYSQL

    @Transactional(readOnly = true)
    override fun listar(hotelId: Int?, sinResponder: Boolean) = resenas.listar(hotelId, sinResponder).map { aDominio(it) }

    @Transactional(readOnly = true)
    override fun buscar(reservaId: Int) = resenas.buscar(reservaId)?.let { aDominio(it) }

    @Transactional
    override fun crear(resena: Resena): Resena {
        resenas.save(ResenaEntity().apply {
            reserva = reservas.getReferenceById(resena.reservaId)
            puntajeViaje = resena.puntajeViaje
            puntajeHotel = resena.puntajeHotel
            comentario = resena.comentario
        })
        return resena
    }

    @Transactional
    override fun guardarRespuesta(reservaId: Int, respuesta: RespuestaAgencia) {
        val entidad = resenas.findById(reservaId).orElseThrow { NotFoundException("La reserva $reservaId no tiene reseña") }
        entidad.respuestaAgencia = respuesta.texto
        entidad.fechaRespuesta = respuesta.fecha
    }

    @Transactional
    override fun guardarEtiquetas(reservaId: Int, etiquetas: List<String>) {
        val entidad = resenas.findById(reservaId).orElseThrow { NotFoundException("La reserva $reservaId no tiene reseña") }
        entidad.etiquetas.clear()
        entidad.etiquetas.addAll(etiquetas)
    }

    override fun promediosPorHotel(): Map<Int, PromedioHotel> = jdbc.query(
        """
        SELECT r.hotel_id, AVG(rs.puntaje_hotel) AS prom_hotel, AVG(rs.puntaje_viaje) AS prom_viaje, COUNT(*) AS cantidad
          FROM resena rs
          JOIN reserva r ON r.booking_id = rs.booking_id
         WHERE r.activa = TRUE
         GROUP BY r.hotel_id
        """,
    ) { rs, _ ->
        PromedioHotel(
            rs.getInt("hotel_id"),
            rs.getBigDecimal("prom_hotel").setScale(2, RoundingMode.HALF_UP),
            rs.getBigDecimal("prom_viaje").setScale(2, RoundingMode.HALF_UP),
            rs.getLong("cantidad"),
        )
    }.associateBy { it.hotelId }

    private fun aDominio(e: ResenaEntity): Resena {
        val hotel = e.reserva!!.hotel
        return Resena(
            reservaId = e.bookingId!!,
            hotelId = hotel.id!!,
            hotel = hotel.nombre,
            destino = "${hotel.ciudad.nombre}, ${hotel.ciudad.pais.nombre}",
            puntajeViaje = e.puntajeViaje,
            puntajeHotel = e.puntajeHotel,
            comentario = e.comentario,
            etiquetas = e.etiquetas.sorted(),
            respuestaAgencia = e.respuestaAgencia?.let { RespuestaAgencia(it, e.fechaRespuesta!!) },
        )
    }
}
