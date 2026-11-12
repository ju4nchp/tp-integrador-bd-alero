package ar.edu.unsam.reservas.repository.mysql

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ReservaJpaRepository : JpaRepository<ReservaEntity, Int> {

    @EntityGraph(attributePaths = ["cliente", "cliente.ciudad", "cliente.ciudad.pais", "hotel", "hotel.ciudad",
        "hotel.ciudad.pais", "regimen", "medioPago", "transporte", "estado"])
    @Query(
        value = """
            select r from ReservaEntity r
            where (:estado is null or r.estado.descripcion = :estado)
              and (:hotelId is null or r.hotel.id = :hotelId)
              and (:pais is null or r.hotel.ciudad.pais.nombre = :pais)
              and (:texto is null or lower(r.cliente.nombre) like lower(concat('%', :texto, '%')))
              and (:activa is null or r.activa = :activa)
              and (:hasta is null or (r.estado.descripcion = 'Confirmed' and r.returnDate <= :hasta))
        """,
        countQuery = """
            select count(r) from ReservaEntity r
            where (:estado is null or r.estado.descripcion = :estado)
              and (:hotelId is null or r.hotel.id = :hotelId)
              and (:pais is null or r.hotel.ciudad.pais.nombre = :pais)
              and (:texto is null or lower(r.cliente.nombre) like lower(concat('%', :texto, '%')))
              and (:activa is null or r.activa = :activa)
              and (:hasta is null or (r.estado.descripcion = 'Confirmed' and r.returnDate <= :hasta))
        """,
    )
    fun buscar(
        @Param("estado") estado: String?,
        @Param("hotelId") hotelId: Int?,
        @Param("pais") pais: String?,
        @Param("texto") texto: String?,
        @Param("activa") activa: Boolean?,
        @Param("hasta") hasta: java.time.LocalDate?,
        pageable: Pageable,
    ): Page<ReservaEntity>

    @Query("select coalesce(max(r.bookingId), 0) from ReservaEntity r")
    fun ultimoId(): Int
}

interface ClienteJpaRepository : JpaRepository<ClienteEntity, String> {
    @EntityGraph(attributePaths = ["ciudad", "ciudad.pais"])
    fun findTop10ByNombreContainingIgnoreCaseOrderByNombre(texto: String): List<ClienteEntity>
}

interface HotelJpaRepository : JpaRepository<HotelEntity, Int> {
    @EntityGraph(attributePaths = ["ciudad", "ciudad.pais"])
    @Query("select h from HotelEntity h order by h.nombre")
    fun todos(): List<HotelEntity>
}

interface CiudadJpaRepository : JpaRepository<CiudadEntity, Int> {
    @EntityGraph(attributePaths = ["pais"])
    @Query("select c from CiudadEntity c order by c.pais.nombre, c.nombre")
    fun todas(): List<CiudadEntity>

    fun findByNombreAndPaisNombre(nombre: String, pais: String): CiudadEntity?
}

interface RegimenJpaRepository : JpaRepository<RegimenEntity, Int> {
    fun findByDescripcion(descripcion: String): RegimenEntity?
}

interface MedioPagoJpaRepository : JpaRepository<MedioPagoEntity, Int> {
    fun findByDescripcion(descripcion: String): MedioPagoEntity?
}

interface TransporteJpaRepository : JpaRepository<TransporteEntity, Int> {
    fun findByDescripcion(descripcion: String): TransporteEntity?
}

interface EstadoReservaJpaRepository : JpaRepository<EstadoReservaEntity, Int> {
    fun findByDescripcion(descripcion: String): EstadoReservaEntity?
}

interface MotivoCancelacionJpaRepository : JpaRepository<MotivoCancelacionEntity, Int> {
    fun findByDescripcion(descripcion: String): MotivoCancelacionEntity?
}

interface CancelacionJpaRepository : JpaRepository<CancelacionEntity, Int> {
    @EntityGraph(attributePaths = ["motivo"])
    fun findByBookingId(bookingId: Int): CancelacionEntity?
}
