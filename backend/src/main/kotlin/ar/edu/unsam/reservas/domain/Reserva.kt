package ar.edu.unsam.reservas.domain

import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class EstadoReserva(val valorBase: String, val etiqueta: String) {
    PENDIENTE("Pending", "pendiente"),
    CONFIRMADA("Confirmed", "confirmada"),
    COMPLETADA("Completed", "completada"),
    CANCELADA("Cancelled", "cancelada");

    companion object {
        fun desdeBase(valor: String) = entries.firstOrNull { it.valorBase == valor }
            ?: throw IllegalStateException("Estado desconocido en la base: $valor")
    }
}

data class Cliente(
    val id: String,
    val nombre: String,
    val edad: Int,
    val genero: String,
    val ciudad: String,
    val pais: String,
)

data class Hotel(
    val id: Int,
    val nombre: String,
    val ciudad: String,
    val pais: String,
    val region: String,
)

data class Ciudad(val nombre: String, val pais: String)

class Reserva(
    var id: Int,
    var cliente: Cliente,
    var hotel: Hotel,
    var fechaReserva: LocalDate,
    var checkIn: LocalDate,
    var checkOut: LocalDate,
    var adultos: Int,
    var ninos: Int,
    var habitaciones: Int,
    var regimen: String,
    var medioPago: String,
    var transporte: String,
    var descuento: BigDecimal,
    var costoTotal: BigDecimal,
    var estado: EstadoReserva,
    var motivoCancelacion: String? = null,
    var activa: Boolean = true,
) {
    val viajeros get() = adultos + ninos
    val noches get() = ChronoUnit.DAYS.between(checkIn, checkOut)
    val totalFinal: BigDecimal get() = costoTotal - descuento

    fun esModificable() = activa && (estado == EstadoReserva.PENDIENTE || estado == EstadoReserva.CONFIRMADA)

    fun puedeConfirmarse() = activa && estado == EstadoReserva.PENDIENTE

    fun puedeCompletarse(hoy: LocalDate) = activa && estado == EstadoReserva.CONFIRMADA && !checkOut.isAfter(hoy)

    fun validar() {
        if (!checkOut.isAfter(checkIn)) throw BusinessException("La salida tiene que ser posterior a la llegada")
        if (fechaReserva.isAfter(checkIn)) throw BusinessException("La fecha de reserva no puede ser posterior a la llegada")
        if (adultos < 1) throw BusinessException("Tiene que viajar al menos un adulto")
        if (ninos < 0) throw BusinessException("La cantidad de niños no puede ser negativa")
        if (habitaciones < 1) throw BusinessException("Tiene que haber al menos una habitación")
        if (habitaciones > viajeros) throw BusinessException("No puede haber más habitaciones que huéspedes")
        if (costoTotal <= BigDecimal.ZERO) throw BusinessException("El costo total tiene que ser mayor a cero")
        if (descuento < BigDecimal.ZERO) throw BusinessException("El descuento no puede ser negativo")
        if (descuento > costoTotal) throw BusinessException("El descuento no puede superar el costo total")
        if (cliente.edad < 18) throw BusinessException("El titular de la reserva tiene que ser mayor de edad")
    }

    fun validarQueSeaModificable() {
        if (!activa) throw BusinessException("La reserva $id está dada de baja: solo se puede consultar en el historial")
        if (!esModificable()) {
            throw BusinessException("La reserva $id está ${estado.etiqueta}: solo se modifican reservas pendientes o confirmadas")
        }
    }

    fun cancelar(motivo: String?) {
        validarQueSeaModificable()
        if (motivo.isNullOrBlank()) throw BusinessException("Indicá el motivo de la cancelación")
        estado = EstadoReserva.CANCELADA
        motivoCancelacion = motivo.trim()
    }

    fun confirmar() {
        if (!activa) throw BusinessException("La reserva $id está dada de baja")
        if (estado != EstadoReserva.PENDIENTE) {
            throw BusinessException("Solo se confirman reservas pendientes (esta está ${estado.etiqueta})")
        }
        estado = EstadoReserva.CONFIRMADA
    }

    fun completar(hoy: LocalDate) {
        if (!activa) throw BusinessException("La reserva $id está dada de baja")
        if (estado != EstadoReserva.CONFIRMADA) {
            throw BusinessException("Solo se registra la salida de reservas confirmadas (esta está ${estado.etiqueta})")
        }
        if (checkOut.isAfter(hoy)) throw BusinessException("Todavía no llegó la fecha de salida ($checkOut)")
        estado = EstadoReserva.COMPLETADA
    }

    fun darDeBaja() {
        validarQueSeaModificable()
        activa = false
    }
}
