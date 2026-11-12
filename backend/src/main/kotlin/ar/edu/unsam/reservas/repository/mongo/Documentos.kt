package ar.edu.unsam.reservas.repository.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

// En MongoDB solo está la colección resenas. El hotelId es el id del hotel en MySQL.
@Document("resenas")
data class ResenaDocument(
    @Id val id: String? = null,
    val reservaId: Int,
    val hotelId: Int,
    val hotel: String,
    val destino: String,
    val puntajeViaje: Double,
    val puntajeHotel: Double,
    val comentario: String?,
    val etiquetas: List<String> = emptyList(),
    val respuestaAgencia: RespuestaDocument? = null,
)

data class RespuestaDocument(val texto: String, val fecha: String)
