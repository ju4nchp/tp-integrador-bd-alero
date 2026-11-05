package ar.edu.unsam.reservas.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class Alcance { ACTIVAS, TODAS, BAJAS }

data class FiltroReservas(
    val estado: EstadoReserva? = null,
    val hotelId: Int? = null,
    val pais: String? = null,
    val texto: String? = null,
    val alcance: Alcance = Alcance.ACTIVAS,
    val salidaPendiente: Boolean = false,
)

data class Pagina<T>(
    val contenido: List<T>,
    val pagina: Int,
    val totalPaginas: Int,
    val totalElementos: Long,
)

data class EstadisticaPais(
    val pais: String,
    val region: String,
    val reservas: Long,
    val canceladas: Long,
    val facturacion: BigDecimal,
) {
    val tasaCancelacion: BigDecimal
        get() = if (reservas == 0L) BigDecimal.ZERO
        else BigDecimal(canceladas * 100).divide(BigDecimal(reservas), 2, RoundingMode.HALF_UP)
}

data class EstadisticaHotel(
    val hotelId: Int,
    val hotel: String,
    val ciudad: String,
    val pais: String,
    val reservas: Long,
    val canceladas: Long,
    val facturacion: BigDecimal,
    val promedioHotel: BigDecimal? = null,
    val promedioViaje: BigDecimal? = null,
    val cantidadResenas: Long = 0,
)
