package ar.edu.unsam.reservas.repository.mysql

import ar.edu.unsam.reservas.domain.Ciudad
import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.repository.CatalogoRepository
import ar.edu.unsam.reservas.repository.mysql.MySqlReservaRepository.Companion.aCliente
import ar.edu.unsam.reservas.repository.mysql.MySqlReservaRepository.Companion.aHotel
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
@Transactional(readOnly = true)
class MySqlCatalogoRepository(
    private val hoteles: HotelJpaRepository,
    private val ciudades: CiudadJpaRepository,
    private val clientes: ClienteJpaRepository,
    private val regimenes: RegimenJpaRepository,
    private val mediosPago: MedioPagoJpaRepository,
    private val transportes: TransporteJpaRepository,
    private val jdbc: JdbcTemplate,
) : CatalogoRepository {

    override val motor = Motor.MYSQL

    override fun hoteles() = hoteles.todos().map { aHotel(it) }

    override fun hotel(id: Int) = hoteles.findById(id).map { aHotel(it) }.orElse(null)

    override fun ciudades() = ciudades.todas().map { Ciudad(it.nombre, it.pais.nombre) }

    override fun regimenes() = regimenes.findAll().map { it.descripcion }.sorted()

    override fun mediosPago() = mediosPago.findAll().map { it.descripcion }.sorted()

    override fun transportes() = transportes.findAll().map { it.descripcion }.sorted()

    override fun motivosCancelacion(): List<String> =
        jdbc.queryForList("SELECT DISTINCT motivo FROM cancelacion ORDER BY motivo", String::class.java).filterNotNull()

    override fun buscarClientes(texto: String) =
        clientes.findTop10ByNombreContainingIgnoreCaseOrderByNombre(texto).map { aCliente(it) }

    override fun cliente(id: String) = clientes.findById(id).map { aCliente(it) }.orElse(null)

    override fun siguienteIdCliente(): String {
        val ultimo = jdbc.queryForObject(
            "SELECT COALESCE(MAX(CAST(SUBSTRING(customer_id, 5) AS UNSIGNED)), 0) FROM cliente WHERE customer_id LIKE 'cust%'",
            java.lang.Long::class.java,
        )?.toLong() ?: 0L
        return "cust${ultimo + 1}"
    }
}
