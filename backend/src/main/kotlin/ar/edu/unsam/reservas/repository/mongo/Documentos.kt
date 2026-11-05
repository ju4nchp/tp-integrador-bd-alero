package ar.edu.unsam.reservas.repository.mongo

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document("reservas")
data class ReservaDocument(
    @Id val id: Int,
    val fechas: FechasDocument,
    val cliente: ClienteDocument,
    val hotel: HotelDocument,
    val regimen: String,
    val medioPago: String,
    val transporte: String,
    val estado: String,
    val adultos: Int,
    val ninos: Int,
    val habitaciones: Int,
    val descuento: Double,
    val costoTotal: Double,
    val cancelacion: CancelacionDocument? = null,
    val activa: Boolean = true,
)

data class FechasDocument(val reserva: String, val salida: String, val regreso: String)

data class ClienteDocument(
    @Field("id") val codigo: String,
    val nombre: String,
    val edad: Int,
    val genero: String,
    val ciudad: String,
    val pais: String,
)

data class HotelDocument(
    @Field("id") val codigo: Int,
    val nombre: String,
    val categoriaBase: Double,
    val ciudad: String,
    val pais: String,
    val region: String,
)

data class CancelacionDocument(val motivo: String)

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
