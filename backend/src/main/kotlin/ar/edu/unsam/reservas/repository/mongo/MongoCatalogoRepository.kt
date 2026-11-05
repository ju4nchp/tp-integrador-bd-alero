package ar.edu.unsam.reservas.repository.mongo

import ar.edu.unsam.reservas.domain.Ciudad
import ar.edu.unsam.reservas.domain.Cliente
import ar.edu.unsam.reservas.domain.Hotel
import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.repository.CatalogoRepository
import ar.edu.unsam.reservas.repository.mongo.MongoReservaRepository.Companion.aCliente
import ar.edu.unsam.reservas.repository.mongo.MongoReservaRepository.Companion.aHotel
import ar.edu.unsam.reservas.repository.mongo.MongoReservaRepository.Companion.escapar
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.math.RoundingMode

@Repository
class MongoCatalogoRepository(private val mongo: MongoTemplate) : CatalogoRepository {

    override val motor = Motor.MONGO

    private val reservas get() = mongo.getCollection("reservas")

    override fun hoteles(): List<Hotel> = reservas.aggregate(
        listOf(
            Document("\$group", Document("_id", "\$hotel.id").append("hotel", Document("\$first", "\$hotel"))),
            Document("\$sort", Document("hotel.nombre", 1)),
        ),
    ).map { aHotelDesde(it.get("hotel", Document::class.java)) }.toList()

    override fun hotel(id: Int) =
        mongo.findOne(Query(Criteria.where("hotel.id").`is`(id)), ReservaDocument::class.java)?.let { aHotel(it.hotel) }

    override fun ciudades(): List<Ciudad> = reservas.aggregate(
        listOf(
            Document("\$project", Document("lugares", listOf(
                Document("ciudad", "\$cliente.ciudad").append("pais", "\$cliente.pais"),
                Document("ciudad", "\$hotel.ciudad").append("pais", "\$hotel.pais"),
            ))),
            Document("\$unwind", "\$lugares"),
            Document("\$group", Document("_id", "\$lugares")),
            Document("\$sort", Document("_id.pais", 1).append("_id.ciudad", 1)),
        ),
    ).map {
        val lugar = it.get("_id", Document::class.java)
        Ciudad(lugar.getString("ciudad"), lugar.getString("pais"))
    }.toList()

    override fun regimenes() = distintos("regimen")

    override fun mediosPago() = distintos("medioPago")

    override fun transportes() = distintos("transporte")

    override fun motivosCancelacion() = distintos("cancelacion.motivo")

    override fun buscarClientes(texto: String): List<Cliente> = reservas.aggregate(
        listOf(
            Document("\$match", Document("cliente.nombre", Document("\$regex", escapar(texto)).append("\$options", "i"))),
            Document("\$group", Document("_id", "\$cliente.id").append("cliente", Document("\$first", "\$cliente"))),
            Document("\$sort", Document("cliente.nombre", 1)),
            Document("\$limit", 10),
        ),
    ).map { aClienteDesde(it.get("cliente", Document::class.java)) }.toList()

    override fun cliente(id: String) =
        mongo.findOne(Query(Criteria.where("cliente.id").`is`(id)), ReservaDocument::class.java)?.let { aCliente(it.cliente) }

    override fun siguienteIdCliente(): String {
        val ultimo = distintos("cliente.id")
            .mapNotNull { it.removePrefix("cust").toLongOrNull() }
            .maxOrNull() ?: 0L
        return "cust${ultimo + 1}"
    }

    private fun distintos(campo: String): List<String> =
        mongo.findDistinct(Query(), campo, "reservas", String::class.java).sorted()

    private fun aHotelDesde(h: Document) = Hotel(
        (h["id"] as Number).toInt(),
        h.getString("nombre"),
        BigDecimal.valueOf((h["categoriaBase"] as Number).toDouble()).setScale(1, RoundingMode.HALF_UP),
        h.getString("ciudad"),
        h.getString("pais"),
        h.getString("region"),
    )

    private fun aClienteDesde(c: Document) = Cliente(
        c.getString("id"),
        c.getString("nombre"),
        (c["edad"] as Number).toInt(),
        c.getString("genero"),
        c.getString("ciudad"),
        c.getString("pais"),
    )
}
