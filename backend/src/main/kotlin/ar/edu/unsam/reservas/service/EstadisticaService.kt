package ar.edu.unsam.reservas.service

import ar.edu.unsam.reservas.domain.EstadisticaHotel
import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.repository.SelectorDeMotor
import org.springframework.stereotype.Service

@Service
class EstadisticaService(private val selector: SelectorDeMotor) {

    fun porPais(motor: Motor) = selector.reservas(motor).estadisticasPorPais()

    fun porHotel(motor: Motor): List<EstadisticaHotel> {
        val promedios = selector.resenas(motor).promediosPorHotel()
        return selector.reservas(motor).estadisticasPorHotel().map { fila ->
            val promedio = promedios[fila.hotelId]
            fila.copy(
                promedioHotel = promedio?.promedioHotel,
                promedioViaje = promedio?.promedioViaje,
                cantidadResenas = promedio?.cantidadResenas ?: 0,
            )
        }
    }
}
