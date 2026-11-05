package ar.edu.unsam.reservas.domain

class NotFoundException(mensaje: String) : RuntimeException(mensaje)

class BusinessException(mensaje: String) : RuntimeException(mensaje)
