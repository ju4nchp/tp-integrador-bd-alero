package ar.edu.unsam.reservas.repository.mongo

import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.domain.NotFoundException
import ar.edu.unsam.reservas.domain.PromedioHotel
import ar.edu.unsam.reservas.domain.Resena
import ar.edu.unsam.reservas.domain.RespuestaAgencia
import ar.edu.unsam.reservas.repository.ResenaRepository
import ar.edu.unsam.reservas.repository.mongo.MongoReservaRepository.Companion.numero
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

@Repository
class MongoResenaRepository(private val mongo: MongoTemplate) : ResenaRepository {

    override val motor = Motor.MONGO

    override fun listar(hotelId: Int?, sinResponder: Boolean): List<Resena> {
        val criterios = mutableListOf<Criteria>()
        hotelId?.let { criterios += Criteria.where("hotelId").`is`(it) }
        if (sinResponder) criterios += Criteria.where("respuestaAgencia").`is`(null)
        val query = if (criterios.isEmpty()) Query() else Query(Criteria().andOperator(criterios))
        return mongo.find(query.with(Sort.by("reservaId")), ResenaDocument::class.java).map { aDominio(it) }
    }

    override fun buscar(reservaId: Int) = mongo.findOne(porReserva(reservaId), ResenaDocument::class.java)?.let { aDominio(it) }

    override fun crear(resena: Resena): Resena {
        mongo.insert(
            ResenaDocument(
                reservaId = resena.reservaId,
                hotelId = resena.hotelId,
                hotel = resena.hotel,
                destino = resena.destino,
                puntajeViaje = resena.puntajeViaje.toDouble(),
                puntajeHotel = resena.puntajeHotel.toDouble(),
                comentario = resena.comentario,
                etiquetas = resena.etiquetas,
            ),
        )
        return resena
    }

    override fun guardarRespuesta(reservaId: Int, respuesta: RespuestaAgencia) {
        val cambio = Update().set("respuestaAgencia", RespuestaDocument(respuesta.texto, respuesta.fecha.toString()))
        actualizar(reservaId, cambio)
    }

    override fun guardarEtiquetas(reservaId: Int, etiquetas: List<String>) {
        actualizar(reservaId, Update().set("etiquetas", etiquetas))
    }

    override fun promediosPorHotel(): Map<Int, PromedioHotel> {
        val pipeline = listOf(
            Document("\$group", Document("_id", "\$hotelId")
                .append("sumaHotel", Document("\$sum", "\$puntajeHotel"))
                .append("sumaViaje", Document("\$sum", "\$puntajeViaje"))
                .append("cantidad", Document("\$sum", 1))),
        )
        return mongo.getCollection("resenas").aggregate(pipeline).map {
            val cantidad = numero(it["cantidad"]).toLong()
            PromedioHotel(
                numero(it["_id"]).toInt(),
                promedio(numero(it["sumaHotel"]).toDouble(), cantidad),
                promedio(numero(it["sumaViaje"]).toDouble(), cantidad),
                cantidad,
            )
        }.toList().associateBy { it.hotelId }
    }

    private fun actualizar(reservaId: Int, cambio: Update) {
        val resultado = mongo.updateFirst(porReserva(reservaId), cambio, ResenaDocument::class.java)
        if (resultado.matchedCount == 0L) throw NotFoundException("La reserva $reservaId no tiene reseña")
    }

    private fun porReserva(reservaId: Int) = Query(Criteria.where("reservaId").`is`(reservaId))

    private fun promedio(suma: Double, cantidad: Long): BigDecimal =
        BigDecimal.valueOf(suma).setScale(1, RoundingMode.HALF_UP).divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP)

    private fun aDominio(d: ResenaDocument) = Resena(
        reservaId = d.reservaId,
        hotelId = d.hotelId,
        hotel = d.hotel,
        destino = d.destino,
        puntajeViaje = BigDecimal.valueOf(d.puntajeViaje).setScale(1, RoundingMode.HALF_UP),
        puntajeHotel = BigDecimal.valueOf(d.puntajeHotel).setScale(1, RoundingMode.HALF_UP),
        comentario = d.comentario,
        etiquetas = d.etiquetas.sorted(),
        respuestaAgencia = d.respuestaAgencia?.let { RespuestaAgencia(it.texto, LocalDateTime.parse(it.fecha)) },
    )
}
