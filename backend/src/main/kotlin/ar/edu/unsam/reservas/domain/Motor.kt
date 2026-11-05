package ar.edu.unsam.reservas.domain

import java.time.LocalDate
import java.time.ZoneId

const val HEADER_MOTOR = "X-DB-Engine"
const val HEADER_MOTOR_USADO = "X-DB-Engine-Used"

enum class Motor(val nombre: String) {
    MYSQL("MySQL"),
    MONGO("MongoDB");

    val clave get() = name.lowercase()

    companion object {
        fun desde(valor: String): Motor? = when (valor.trim().lowercase()) {
            "mysql" -> MYSQL
            "mongo", "mongodb" -> MONGO
            else -> null
        }
    }
}

fun hoy(): LocalDate = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
