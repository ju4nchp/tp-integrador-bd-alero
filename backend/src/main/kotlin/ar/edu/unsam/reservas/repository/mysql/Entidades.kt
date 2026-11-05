package ar.edu.unsam.reservas.repository.mysql

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "pais")
class PaisEntity {
    @Id
    var nombre: String = ""
    var region: String = ""
}

@Entity
@Table(name = "ciudad")
class CiudadEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    var nombre: String = ""

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pais_nombre")
    lateinit var pais: PaisEntity
}

@Entity
@Table(name = "cliente")
class ClienteEntity {
    @Id
    @Column(name = "customer_id")
    var customerId: String = ""
    var nombre: String = ""
    var edad: Int = 0
    var genero: String = ""

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudad_id")
    lateinit var ciudad: CiudadEntity
}

@Entity
@Table(name = "hotel")
class HotelEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null
    var nombre: String = ""
    var categoriaBase: BigDecimal = BigDecimal.ZERO

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciudad_id")
    lateinit var ciudad: CiudadEntity
}

@Entity
@Table(name = "regimen")
class RegimenEntity {
    @Id
    var id: Int? = null
    var descripcion: String = ""
}

@Entity
@Table(name = "medio_pago")
class MedioPagoEntity {
    @Id
    var id: Int? = null
    var descripcion: String = ""
}

@Entity
@Table(name = "transporte")
class TransporteEntity {
    @Id
    var id: Int? = null
    var descripcion: String = ""
}

@Entity
@Table(name = "estado_reserva")
class EstadoReservaEntity {
    @Id
    var id: Int? = null
    var descripcion: String = ""
}

@Entity
@Table(name = "reserva")
class ReservaEntity {
    @Id
    @Column(name = "booking_id")
    var bookingId: Int = 0

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id")
    lateinit var cliente: ClienteEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id")
    lateinit var hotel: HotelEntity

    var bookingDate: LocalDate = LocalDate.now()
    var travelDate: LocalDate = LocalDate.now()
    var returnDate: LocalDate = LocalDate.now()
    var numberOfAdults: Int = 1
    var numberOfChildren: Int = 0
    var numberOfRooms: Int = 1
    var discountAmount: BigDecimal = BigDecimal.ZERO
    var totalTripCost: BigDecimal = BigDecimal.ZERO

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "regimen_id")
    lateinit var regimen: RegimenEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medio_pago_id")
    lateinit var medioPago: MedioPagoEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transporte_id")
    lateinit var transporte: TransporteEntity

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_id")
    lateinit var estado: EstadoReservaEntity

    var activa: Boolean = true
}

@Entity
@Table(name = "cancelacion")
class CancelacionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Int? = null

    @Column(name = "booking_id")
    var bookingId: Int = 0
    var motivo: String = ""
}

@Entity
@Table(name = "resena")
class ResenaEntity {
    @Id
    @Column(name = "booking_id")
    var bookingId: Int? = null

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "booking_id")
    var reserva: ReservaEntity? = null

    var puntajeViaje: BigDecimal = BigDecimal.ONE
    var puntajeHotel: BigDecimal = BigDecimal.ONE
    var comentario: String? = null
    var respuestaAgencia: String? = null
    var fechaRespuesta: LocalDateTime? = null

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "resena_etiqueta", joinColumns = [JoinColumn(name = "booking_id")])
    @Column(name = "etiqueta")
    var etiquetas: MutableSet<String> = mutableSetOf()
}
