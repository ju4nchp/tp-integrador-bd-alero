package ar.edu.unsam.reservas.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

data class RespuestaAgencia(val texto: String, val fecha: LocalDateTime)

data class Resena(
    val reservaId: Int,
    val hotelId: Int,
    val hotel: String,
    val destino: String,
    val puntajeViaje: BigDecimal,
    val puntajeHotel: BigDecimal,
    val comentario: String?,
    val etiquetas: List<String> = emptyList(),
    val respuestaAgencia: RespuestaAgencia? = null,
) {
    companion object {
        private val MINIMO = BigDecimal.ONE
        private val MAXIMO = BigDecimal(5)

        fun validarPuntaje(campo: String, puntaje: BigDecimal?): BigDecimal {
            if (puntaje == null) throw BusinessException("Falta el $campo")
            if (puntaje < MINIMO || puntaje > MAXIMO) throw BusinessException("El $campo va de 1 a 5")
            if (puntaje.stripTrailingZeros().scale() > 1) throw BusinessException("El $campo admite un solo decimal")
            return puntaje.setScale(1, RoundingMode.HALF_UP)
        }

        fun normalizarEtiquetas(etiquetas: List<String>): List<String> {
            val limpias = etiquetas.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
            if (limpias.size > 8) throw BusinessException("Una reseña puede tener hasta 8 etiquetas")
            limpias.firstOrNull { it.length > 40 }?.let { throw BusinessException("La etiqueta '$it' es muy larga (máximo 40 letras)") }
            return limpias
        }
    }
}

data class PromedioHotel(
    val hotelId: Int,
    val promedioHotel: BigDecimal,
    val promedioViaje: BigDecimal,
    val cantidadResenas: Long,
)
