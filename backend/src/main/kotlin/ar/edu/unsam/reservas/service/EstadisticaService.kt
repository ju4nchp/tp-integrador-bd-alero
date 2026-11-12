package ar.edu.unsam.reservas.service

import ar.edu.unsam.reservas.domain.EstadisticaHotel
import ar.edu.unsam.reservas.repository.ResenaRepository
import ar.edu.unsam.reservas.repository.ReservaRepository
import org.springframework.stereotype.Service

@Service
class EstadisticaService(
    private val reservas: ReservaRepository,
    private val resenas: ResenaRepository,
) {

    // Sale entera de MySQL (JOIN y GROUP BY).
    fun porPais() = reservas.estadisticasPorPais()

    // Junta las dos bases: reservas y cancelaciones de MySQL, puntajes de las reseñas de MongoDB.
    fun porHotel(): List<EstadisticaHotel> {
        val promedios = resenas.promediosPorHotel()
        return reservas.estadisticasPorHotel().map { fila ->
            val promedio = promedios[fila.hotelId]
            fila.copy(
                promedioHotel = promedio?.promedioHotel,
                promedioViaje = promedio?.promedioViaje,
                cantidadResenas = promedio?.cantidadResenas ?: 0,
            )
        }
    }
}
