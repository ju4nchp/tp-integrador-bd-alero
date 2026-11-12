package ar.edu.unsam.reservas.service

import ar.edu.unsam.reservas.domain.BusinessException
import ar.edu.unsam.reservas.domain.Cliente
import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.domain.FiltroReservas
import ar.edu.unsam.reservas.domain.NotFoundException
import ar.edu.unsam.reservas.domain.Pagina
import ar.edu.unsam.reservas.domain.Reserva
import ar.edu.unsam.reservas.domain.hoy
import ar.edu.unsam.reservas.dto.ClienteNuevoRequest
import ar.edu.unsam.reservas.dto.ReservaRequest
import ar.edu.unsam.reservas.repository.CatalogoRepository
import ar.edu.unsam.reservas.repository.ReservaRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

// Las reservas, los huéspedes y los catálogos están en MySQL.
@Service
class ReservaService(
    private val reservas: ReservaRepository,
    private val catalogos: CatalogoRepository,
) {

    companion object {
        const val TAMANIO_PAGINA = 20
    }

    fun listar(filtro: FiltroReservas, pagina: Int): Pagina<Reserva> =
        reservas.buscar(filtro, maxOf(pagina, 0), TAMANIO_PAGINA)

    fun buscar(id: Int): Reserva =
        reservas.buscarPorId(id) ?: throw NotFoundException("No existe la reserva $id")

    fun crear(datos: ReservaRequest): Reserva {
        val cliente = when {
            datos.clienteId != null -> catalogos.cliente(datos.clienteId)
                ?: throw NotFoundException("No existe el huésped ${datos.clienteId}")
            datos.clienteNuevo != null -> nuevoCliente(datos.clienteNuevo)
            else -> throw BusinessException("Elegí un huésped existente o cargá uno nuevo")
        }
        // Toda reserva nueva arranca pendiente; después se confirma con su propia acción.
        val reserva = Reserva(
            id = reservas.siguienteId(),
            cliente = cliente,
            hotel = hotel(datos.hotelId),
            fechaReserva = datos.fechaReserva ?: hoy(),
            checkIn = obligatorio(datos.checkIn, "la fecha de llegada"),
            checkOut = obligatorio(datos.checkOut, "la fecha de salida"),
            adultos = obligatorio(datos.adultos, "la cantidad de adultos"),
            ninos = datos.ninos ?: 0,
            habitaciones = obligatorio(datos.habitaciones, "la cantidad de habitaciones"),
            regimen = obligatorio(datos.regimen, "el régimen"),
            medioPago = obligatorio(datos.medioPago, "el medio de pago"),
            transporte = obligatorio(datos.transporte, "el transporte"),
            descuento = plata(datos.descuento ?: BigDecimal.ZERO),
            costoTotal = plata(obligatorio(datos.costoTotal, "el costo total")),
            estado = EstadoReserva.PENDIENTE,
        )
        validarCatalogos(reserva)
        reserva.validar()
        return reservas.guardar(reserva)
    }

    fun modificar(id: Int, datos: ReservaRequest): Reserva {
        val reserva = buscar(id)
        reserva.validarQueSeaModificable()
        datos.hotelId?.let { reserva.hotel = hotel(it) }
        datos.fechaReserva?.let { reserva.fechaReserva = it }
        datos.checkIn?.let { reserva.checkIn = it }
        datos.checkOut?.let { reserva.checkOut = it }
        datos.adultos?.let { reserva.adultos = it }
        datos.ninos?.let { reserva.ninos = it }
        datos.habitaciones?.let { reserva.habitaciones = it }
        datos.regimen?.let { reserva.regimen = it }
        datos.medioPago?.let { reserva.medioPago = it }
        datos.transporte?.let { reserva.transporte = it }
        datos.descuento?.let { reserva.descuento = plata(it) }
        datos.costoTotal?.let { reserva.costoTotal = plata(it) }
        validarCatalogos(reserva)
        reserva.validar()
        return reservas.guardar(reserva)
    }

    fun confirmar(id: Int): Reserva {
        val reserva = buscar(id)
        reserva.confirmar()
        return reservas.guardar(reserva)
    }

    fun cancelar(id: Int, motivo: String?): Reserva {
        val reserva = buscar(id)
        if (motivo != null && motivo !in catalogos.motivosCancelacion()) {
            throw BusinessException("Elegí un motivo de la lista")
        }
        reserva.cancelar(motivo)
        return reservas.guardar(reserva)
    }

    fun completar(id: Int): Reserva {
        val reserva = buscar(id)
        reserva.completar(hoy())
        return reservas.guardar(reserva)
    }

    fun darDeBaja(id: Int) {
        val reserva = buscar(id)
        reserva.darDeBaja()
        reservas.guardar(reserva)
    }

    private fun nuevoCliente(datos: ClienteNuevoRequest): Cliente {
        val nombre = obligatorio(datos.nombre?.trim()?.takeIf { it.isNotEmpty() }, "el nombre del huésped")
        val genero = obligatorio(datos.genero, "el género del huésped")
        if (genero != "Male" && genero != "Female") throw BusinessException("Género inválido: $genero")
        val ciudad = obligatorio(datos.ciudad, "la ciudad del huésped")
        val pais = obligatorio(datos.pais, "el país del huésped")
        if (catalogos.ciudades().none { it.nombre == ciudad && it.pais == pais }) {
            throw BusinessException("No existe la ciudad $ciudad ($pais)")
        }
        return Cliente(catalogos.siguienteIdCliente(), nombre, obligatorio(datos.edad, "la edad del huésped"), genero, ciudad, pais)
    }

    private fun hotel(id: Int?) =
        catalogos.hotel(obligatorio(id, "el alojamiento")) ?: throw NotFoundException("No existe el alojamiento $id")

    private fun validarCatalogos(reserva: Reserva) {
        if (reserva.regimen !in catalogos.regimenes()) throw BusinessException("Régimen desconocido: ${reserva.regimen}")
        if (reserva.medioPago !in catalogos.mediosPago()) throw BusinessException("Medio de pago desconocido: ${reserva.medioPago}")
        if (reserva.transporte !in catalogos.transportes()) throw BusinessException("Transporte desconocido: ${reserva.transporte}")
    }

    private fun plata(valor: BigDecimal) = valor.setScale(2, RoundingMode.HALF_UP)

    private fun <T> obligatorio(valor: T?, campo: String): T = valor ?: throw BusinessException("Falta $campo")
}
