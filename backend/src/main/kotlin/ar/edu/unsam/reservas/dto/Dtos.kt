package ar.edu.unsam.reservas.dto

import ar.edu.unsam.reservas.domain.Ciudad
import ar.edu.unsam.reservas.domain.Cliente
import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.domain.Hotel
import ar.edu.unsam.reservas.domain.Reserva
import java.math.BigDecimal
import java.time.LocalDate

data class ReservaDTO(
    val id: Int,
    val cliente: Cliente,
    val hotel: Hotel,
    val fechaReserva: LocalDate,
    val checkIn: LocalDate,
    val checkOut: LocalDate,
    val noches: Long,
    val adultos: Int,
    val ninos: Int,
    val viajeros: Int,
    val habitaciones: Int,
    val regimen: String,
    val medioPago: String,
    val transporte: String,
    val descuento: BigDecimal,
    val costoTotal: BigDecimal,
    val totalFinal: BigDecimal,
    val estado: EstadoReserva,
    val motivoCancelacion: String?,
    val activa: Boolean,
    val modificable: Boolean,
    val confirmable: Boolean,
    val completable: Boolean,
) {
    companion object {
        fun desde(r: Reserva, hoy: LocalDate) = ReservaDTO(
            r.id, r.cliente, r.hotel, r.fechaReserva, r.checkIn, r.checkOut, r.noches, r.adultos, r.ninos,
            r.viajeros, r.habitaciones, r.regimen, r.medioPago, r.transporte, r.descuento, r.costoTotal,
            r.totalFinal, r.estado, r.motivoCancelacion, r.activa, r.esModificable(), r.puedeConfirmarse(),
            r.puedeCompletarse(hoy),
        )
    }
}

data class ClienteNuevoRequest(
    val nombre: String? = null,
    val edad: Int? = null,
    val genero: String? = null,
    val ciudad: String? = null,
    val pais: String? = null,
)

data class ReservaRequest(
    val clienteId: String? = null,
    val clienteNuevo: ClienteNuevoRequest? = null,
    val hotelId: Int? = null,
    val fechaReserva: LocalDate? = null,
    val checkIn: LocalDate? = null,
    val checkOut: LocalDate? = null,
    val adultos: Int? = null,
    val ninos: Int? = null,
    val habitaciones: Int? = null,
    val regimen: String? = null,
    val medioPago: String? = null,
    val transporte: String? = null,
    val descuento: BigDecimal? = null,
    val costoTotal: BigDecimal? = null,
)

data class CancelacionRequest(val motivo: String? = null)

data class ResenaRequest(
    val reservaId: Int? = null,
    val puntajeViaje: BigDecimal? = null,
    val puntajeHotel: BigDecimal? = null,
    val comentario: String? = null,
)

data class RespuestaRequest(val texto: String? = null)

data class EtiquetasRequest(val etiquetas: List<String> = emptyList())

data class CatalogosDTO(
    val hoteles: List<Hotel>,
    val ciudades: List<Ciudad>,
    val regimenes: List<String>,
    val mediosPago: List<String>,
    val transportes: List<String>,
    val motivosCancelacion: List<String>,
    val estados: List<EstadoReserva>,
)

data class ErrorDTO(val status: Int, val error: String, val detalle: String? = null)
