package ar.edu.unsam.reservas.repository

import ar.edu.unsam.reservas.domain.BusinessException
import ar.edu.unsam.reservas.domain.Motor
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SelectorDeMotor(
    private val reservas: List<ReservaRepository>,
    private val resenas: List<ResenaRepository>,
    private val catalogos: List<CatalogoRepository>,
    @Value("\${app.motor-por-defecto}") motorPorDefecto: String,
) {
    val porDefecto: Motor = Motor.desde(motorPorDefecto)
        ?: throw IllegalStateException("app.motor-por-defecto tiene que ser mysql o mongo, no '$motorPorDefecto'")

    fun motor(header: String?): Motor =
        if (header.isNullOrBlank()) porDefecto
        else Motor.desde(header) ?: throw BusinessException("Motor desconocido: '$header'. Los valores posibles son mysql y mongo")

    fun reservas(motor: Motor) = reservas.first { it.motor == motor }

    fun resenas(motor: Motor) = resenas.first { it.motor == motor }

    fun catalogos(motor: Motor) = catalogos.first { it.motor == motor }
}
