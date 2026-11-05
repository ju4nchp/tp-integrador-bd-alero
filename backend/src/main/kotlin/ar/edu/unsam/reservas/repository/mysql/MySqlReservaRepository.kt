package ar.edu.unsam.reservas.repository.mysql

import ar.edu.unsam.reservas.domain.Alcance
import ar.edu.unsam.reservas.domain.BusinessException
import ar.edu.unsam.reservas.domain.Cliente
import ar.edu.unsam.reservas.domain.EstadisticaHotel
import ar.edu.unsam.reservas.domain.EstadisticaPais
import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.domain.FiltroReservas
import ar.edu.unsam.reservas.domain.Hotel
import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.domain.NotFoundException
import ar.edu.unsam.reservas.domain.Pagina
import ar.edu.unsam.reservas.domain.Reserva
import ar.edu.unsam.reservas.domain.hoy
import ar.edu.unsam.reservas.repository.ReservaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class MySqlReservaRepository(
    private val reservas: ReservaJpaRepository,
    private val clientes: ClienteJpaRepository,
    private val hoteles: HotelJpaRepository,
    private val ciudades: CiudadJpaRepository,
    private val regimenes: RegimenJpaRepository,
    private val mediosPago: MedioPagoJpaRepository,
    private val transportes: TransporteJpaRepository,
    private val estados: EstadoReservaJpaRepository,
    private val cancelaciones: CancelacionJpaRepository,
    private val jdbc: JdbcTemplate,
) : ReservaRepository {

    override val motor = Motor.MYSQL

    @Transactional(readOnly = true)
    override fun buscar(filtro: FiltroReservas, pagina: Int, tamanio: Int): Pagina<Reserva> {
        val activa = when (filtro.alcance) {
            Alcance.ACTIVAS -> true
            Alcance.BAJAS -> false
            Alcance.TODAS -> null
        }
        val resultado = reservas.buscar(
            filtro.estado?.valorBase, filtro.hotelId, filtro.pais, filtro.texto?.takeIf { it.isNotBlank() }, activa,
            if (filtro.salidaPendiente) hoy() else null,
            PageRequest.of(pagina, tamanio, Sort.by("bookingId")),
        )
        return Pagina(resultado.content.map { aDominio(it) }, resultado.number, resultado.totalPages, resultado.totalElements)
    }

    @Transactional(readOnly = true)
    override fun buscarPorId(id: Int): Reserva? =
        reservas.findById(id).map { aDominio(it, cancelaciones.findByBookingId(id)?.motivo) }.orElse(null)

    override fun siguienteId() = reservas.ultimoId() + 1

    @Transactional
    override fun guardar(reserva: Reserva): Reserva {
        val entidad = reservas.findById(reserva.id).orElseGet { ReservaEntity().apply { bookingId = reserva.id } }
        entidad.cliente = clientes.findById(reserva.cliente.id).orElseGet { clientes.save(nuevoCliente(reserva.cliente)) }
        entidad.hotel = hoteles.findById(reserva.hotel.id).orElseThrow { NotFoundException("No existe el alojamiento ${reserva.hotel.id}") }
        entidad.bookingDate = reserva.fechaReserva
        entidad.travelDate = reserva.checkIn
        entidad.returnDate = reserva.checkOut
        entidad.numberOfAdults = reserva.adultos
        entidad.numberOfChildren = reserva.ninos
        entidad.numberOfRooms = reserva.habitaciones
        entidad.discountAmount = reserva.descuento
        entidad.totalTripCost = reserva.costoTotal
        entidad.regimen = regimenes.findByDescripcion(reserva.regimen) ?: throw BusinessException("Régimen desconocido: ${reserva.regimen}")
        entidad.medioPago = mediosPago.findByDescripcion(reserva.medioPago) ?: throw BusinessException("Medio de pago desconocido: ${reserva.medioPago}")
        entidad.transporte = transportes.findByDescripcion(reserva.transporte) ?: throw BusinessException("Transporte desconocido: ${reserva.transporte}")
        entidad.estado = estados.findByDescripcion(reserva.estado.valorBase) ?: throw IllegalStateException("Falta el estado ${reserva.estado.valorBase}")
        entidad.activa = reserva.activa
        reservas.save(entidad)

        if (reserva.estado == EstadoReserva.CANCELADA && cancelaciones.findByBookingId(reserva.id) == null) {
            cancelaciones.save(CancelacionEntity().apply {
                bookingId = reserva.id
                motivo = reserva.motivoCancelacion ?: throw BusinessException("Falta el motivo de la cancelación")
            })
        }
        return reserva
    }

    override fun estadisticasPorPais(): List<EstadisticaPais> = jdbc.query(
        """
        SELECT p.nombre AS pais, p.region,
               COUNT(*) AS reservas,
               COUNT(c.booking_id) AS canceladas,
               SUM(CASE WHEN c.booking_id IS NULL THEN r.total_trip_cost ELSE 0 END) AS facturacion
          FROM reserva r
          JOIN hotel h   ON h.id = r.hotel_id
          JOIN ciudad ci ON ci.id = h.ciudad_id
          JOIN pais p    ON p.nombre = ci.pais_nombre
          LEFT JOIN cancelacion c ON c.booking_id = r.booking_id
         WHERE r.activa = TRUE
         GROUP BY p.nombre, p.region
         ORDER BY facturacion DESC
        """,
    ) { rs, _ ->
        EstadisticaPais(rs.getString("pais"), rs.getString("region"), rs.getLong("reservas"),
            rs.getLong("canceladas"), rs.getBigDecimal("facturacion"))
    }

    override fun estadisticasPorHotel(): List<EstadisticaHotel> = jdbc.query(
        """
        SELECT h.id, h.nombre, ci.nombre AS ciudad, ci.pais_nombre AS pais,
               COUNT(r.booking_id) AS reservas,
               COUNT(c.booking_id) AS canceladas,
               COALESCE(SUM(CASE WHEN c.booking_id IS NULL THEN r.total_trip_cost END), 0) AS facturacion
          FROM hotel h
          JOIN ciudad ci ON ci.id = h.ciudad_id
          LEFT JOIN reserva r ON r.hotel_id = h.id AND r.activa = TRUE
          LEFT JOIN cancelacion c ON c.booking_id = r.booking_id
         GROUP BY h.id, h.nombre, ci.nombre, ci.pais_nombre
         ORDER BY h.nombre
        """,
    ) { rs, _ ->
        EstadisticaHotel(rs.getInt("id"), rs.getString("nombre"), rs.getString("ciudad"), rs.getString("pais"),
            rs.getLong("reservas"), rs.getLong("canceladas"), rs.getBigDecimal("facturacion"))
    }

    private fun nuevoCliente(cliente: Cliente) = ClienteEntity().apply {
        customerId = cliente.id
        nombre = cliente.nombre
        edad = cliente.edad
        genero = cliente.genero
        ciudad = ciudades.findByNombreAndPaisNombre(cliente.ciudad, cliente.pais)
            ?: throw BusinessException("No existe la ciudad ${cliente.ciudad} (${cliente.pais})")
    }

    private fun aDominio(e: ReservaEntity, motivo: String? = null) = Reserva(
        id = e.bookingId,
        cliente = aCliente(e.cliente),
        hotel = aHotel(e.hotel),
        fechaReserva = e.bookingDate,
        checkIn = e.travelDate,
        checkOut = e.returnDate,
        adultos = e.numberOfAdults,
        ninos = e.numberOfChildren,
        habitaciones = e.numberOfRooms,
        regimen = e.regimen.descripcion,
        medioPago = e.medioPago.descripcion,
        transporte = e.transporte.descripcion,
        descuento = e.discountAmount,
        costoTotal = e.totalTripCost,
        estado = EstadoReserva.desdeBase(e.estado.descripcion),
        motivoCancelacion = motivo,
        activa = e.activa,
    )

    companion object {
        fun aCliente(c: ClienteEntity) = Cliente(c.customerId, c.nombre, c.edad, c.genero, c.ciudad.nombre, c.ciudad.pais.nombre)

        fun aHotel(h: HotelEntity) = Hotel(h.id!!, h.nombre, h.categoriaBase, h.ciudad.nombre, h.ciudad.pais.nombre, h.ciudad.pais.region)
    }
}
