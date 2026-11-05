package ar.edu.unsam.reservas.repository.mongo

import ar.edu.unsam.reservas.domain.Alcance
import ar.edu.unsam.reservas.domain.Cliente
import ar.edu.unsam.reservas.domain.EstadisticaHotel
import ar.edu.unsam.reservas.domain.EstadisticaPais
import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.domain.FiltroReservas
import ar.edu.unsam.reservas.domain.Hotel
import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.domain.Pagina
import ar.edu.unsam.reservas.domain.Reserva
import ar.edu.unsam.reservas.domain.hoy
import ar.edu.unsam.reservas.repository.ReservaRepository
import org.bson.Document
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class MongoReservaRepository(private val mongo: MongoTemplate) : ReservaRepository {

    override val motor = Motor.MONGO

    override fun buscar(filtro: FiltroReservas, pagina: Int, tamanio: Int): Pagina<Reserva> {
        val criterios = mutableListOf<Criteria>()
        when (filtro.alcance) {
            Alcance.ACTIVAS -> criterios += Criteria.where("activa").`is`(true)
            Alcance.BAJAS -> criterios += Criteria.where("activa").`is`(false)
            Alcance.TODAS -> {}
        }
        filtro.estado?.let { criterios += Criteria.where("estado").`is`(it.valorBase) }
        filtro.hotelId?.let { criterios += Criteria.where("hotel.id").`is`(it) }
        filtro.pais?.let { criterios += Criteria.where("hotel.pais").`is`(it) }
        filtro.texto?.takeIf { it.isNotBlank() }?.let { criterios += Criteria.where("cliente.nombre").regex(escapar(it), "i") }
        if (filtro.salidaPendiente) {
            criterios += Criteria.where("estado").`is`(EstadoReserva.CONFIRMADA.valorBase)
            criterios += Criteria.where("fechas.regreso").lte(hoy().toString())
        }

        val query = if (criterios.isEmpty()) Query() else Query(Criteria().andOperator(criterios))
        val total = mongo.count(query, ReservaDocument::class.java)
        val documentos = mongo.find(query.with(PageRequest.of(pagina, tamanio, Sort.by("id"))), ReservaDocument::class.java)
        val paginas = ((total + tamanio - 1) / tamanio).toInt()
        return Pagina(documentos.map { aDominio(it) }, pagina, paginas, total)
    }

    override fun buscarPorId(id: Int) = mongo.findById(id, ReservaDocument::class.java)?.let { aDominio(it) }

    override fun siguienteId(): Int {
        val ultima = mongo.findOne(Query().with(Sort.by(Sort.Direction.DESC, "id")).limit(1), ReservaDocument::class.java)
        return (ultima?.id ?: 0) + 1
    }

    override fun guardar(reserva: Reserva): Reserva {
        mongo.save(aDocumento(reserva))
        return reserva
    }

    override fun estadisticasPorPais(): List<EstadisticaPais> {
        val pipeline = listOf(
            Document("\$match", Document("activa", true)),
            Document("\$group", Document("_id", "\$hotel.pais")
                .append("region", Document("\$first", "\$hotel.region"))
                .append("reservas", Document("\$sum", 1))
                .append("canceladas", Document("\$sum", siEsCancelada(1, 0)))
                .append("facturacion", Document("\$sum", siEsCancelada(0, "\$costoTotal")))),
            Document("\$sort", Document("facturacion", -1)),
        )
        return mongo.getCollection("reservas").aggregate(pipeline).map {
            EstadisticaPais(it.getString("_id"), it.getString("region"), numero(it["reservas"]).toLong(),
                numero(it["canceladas"]).toLong(), plata(it["facturacion"]))
        }.toList()
    }

    override fun estadisticasPorHotel(): List<EstadisticaHotel> {
        val pipeline = listOf(
            Document("\$match", Document("activa", true)),
            Document("\$group", Document("_id", "\$hotel.id")
                .append("hotel", Document("\$first", "\$hotel"))
                .append("reservas", Document("\$sum", 1))
                .append("canceladas", Document("\$sum", siEsCancelada(1, 0)))
                .append("facturacion", Document("\$sum", siEsCancelada(0, "\$costoTotal")))),
            Document("\$sort", Document("hotel.nombre", 1)),
        )
        return mongo.getCollection("reservas").aggregate(pipeline).map {
            val hotel = it.get("hotel", Document::class.java)
            EstadisticaHotel(numero(it["_id"]).toInt(), hotel.getString("nombre"), hotel.getString("ciudad"),
                hotel.getString("pais"), numero(it["reservas"]).toLong(), numero(it["canceladas"]).toLong(),
                plata(it["facturacion"]))
        }.toList()
    }

    private fun siEsCancelada(entonces: Any, sino: Any) =
        Document("\$cond", listOf(Document("\$eq", listOf("\$estado", EstadoReserva.CANCELADA.valorBase)), entonces, sino))

    private fun aDocumento(r: Reserva) = ReservaDocument(
        id = r.id,
        fechas = FechasDocument(r.fechaReserva.toString(), r.checkIn.toString(), r.checkOut.toString()),
        cliente = ClienteDocument(r.cliente.id, r.cliente.nombre, r.cliente.edad, r.cliente.genero, r.cliente.ciudad, r.cliente.pais),
        hotel = HotelDocument(r.hotel.id, r.hotel.nombre, r.hotel.categoriaBase.toDouble(), r.hotel.ciudad, r.hotel.pais, r.hotel.region),
        regimen = r.regimen,
        medioPago = r.medioPago,
        transporte = r.transporte,
        estado = r.estado.valorBase,
        adultos = r.adultos,
        ninos = r.ninos,
        habitaciones = r.habitaciones,
        descuento = r.descuento.toDouble(),
        costoTotal = r.costoTotal.toDouble(),
        cancelacion = r.motivoCancelacion?.let { CancelacionDocument(it) },
        activa = r.activa,
    )

    companion object {
        fun escapar(texto: String) = texto.replace(Regex("[.*+?^\${}()|\\[\\]\\\\]")) { "\\" + it.value }

        fun numero(valor: Any?) = (valor as Number)

        fun plata(valor: Any?): BigDecimal = BigDecimal.valueOf(numero(valor).toDouble()).setScale(2, java.math.RoundingMode.HALF_UP)

        fun aCliente(c: ClienteDocument) = Cliente(c.codigo, c.nombre, c.edad, c.genero, c.ciudad, c.pais)

        fun aHotel(h: HotelDocument) = Hotel(h.codigo, h.nombre, BigDecimal.valueOf(h.categoriaBase).setScale(1, java.math.RoundingMode.HALF_UP), h.ciudad, h.pais, h.region)

        fun aDominio(d: ReservaDocument) = Reserva(
            id = d.id,
            cliente = aCliente(d.cliente),
            hotel = aHotel(d.hotel),
            fechaReserva = LocalDate.parse(d.fechas.reserva),
            checkIn = LocalDate.parse(d.fechas.salida),
            checkOut = LocalDate.parse(d.fechas.regreso),
            adultos = d.adultos,
            ninos = d.ninos,
            habitaciones = d.habitaciones,
            regimen = d.regimen,
            medioPago = d.medioPago,
            transporte = d.transporte,
            descuento = BigDecimal.valueOf(d.descuento).setScale(2, java.math.RoundingMode.HALF_UP),
            costoTotal = BigDecimal.valueOf(d.costoTotal).setScale(2, java.math.RoundingMode.HALF_UP),
            estado = EstadoReserva.desdeBase(d.estado),
            motivoCancelacion = d.cancelacion?.motivo,
            activa = d.activa,
        )
    }
}
