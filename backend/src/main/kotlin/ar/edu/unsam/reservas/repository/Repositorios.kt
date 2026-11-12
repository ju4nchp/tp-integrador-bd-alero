package ar.edu.unsam.reservas.repository

import ar.edu.unsam.reservas.domain.Ciudad
import ar.edu.unsam.reservas.domain.Cliente
import ar.edu.unsam.reservas.domain.EstadisticaHotel
import ar.edu.unsam.reservas.domain.EstadisticaPais
import ar.edu.unsam.reservas.domain.FiltroReservas
import ar.edu.unsam.reservas.domain.Hotel
import ar.edu.unsam.reservas.domain.Pagina
import ar.edu.unsam.reservas.domain.PromedioHotel
import ar.edu.unsam.reservas.domain.Resena
import ar.edu.unsam.reservas.domain.RespuestaAgencia
import ar.edu.unsam.reservas.domain.Reserva

// Backend políglota: cada interfaz tiene una implementación en la base que mejor le queda.
// Reservas y catálogos -> MySQL (repository/mysql). Reseñas -> MongoDB (repository/mongo).

interface ReservaRepository {
    fun buscar(filtro: FiltroReservas, pagina: Int, tamanio: Int): Pagina<Reserva>
    fun buscarPorId(id: Int): Reserva?
    fun siguienteId(): Int
    fun guardar(reserva: Reserva): Reserva
    fun estadisticasPorPais(): List<EstadisticaPais>
    fun estadisticasPorHotel(): List<EstadisticaHotel>
}

interface ResenaRepository {
    fun listar(hotelId: Int?, sinResponder: Boolean): List<Resena>
    fun buscar(reservaId: Int): Resena?
    fun crear(resena: Resena): Resena
    fun guardarRespuesta(reservaId: Int, respuesta: RespuestaAgencia)
    fun guardarEtiquetas(reservaId: Int, etiquetas: List<String>)
    fun promediosPorHotel(): Map<Int, PromedioHotel>
}

interface CatalogoRepository {
    fun hoteles(): List<Hotel>
    fun hotel(id: Int): Hotel?
    fun ciudades(): List<Ciudad>
    fun regimenes(): List<String>
    fun mediosPago(): List<String>
    fun transportes(): List<String>
    fun motivosCancelacion(): List<String>
    fun buscarClientes(texto: String): List<Cliente>
    fun cliente(id: String): Cliente?
    fun siguienteIdCliente(): String
}
