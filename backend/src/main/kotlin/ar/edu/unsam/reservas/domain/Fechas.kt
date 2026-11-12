package ar.edu.unsam.reservas.domain

import java.time.LocalDate
import java.time.ZoneId

fun hoy(): LocalDate = LocalDate.now(ZoneId.of("America/Argentina/Buenos_Aires"))
