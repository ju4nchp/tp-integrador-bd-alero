package ar.edu.unsam.reservas.service

import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.dto.CatalogosDTO
import ar.edu.unsam.reservas.repository.CatalogoRepository
import org.springframework.stereotype.Service

@Service
class CatalogoService(private val catalogos: CatalogoRepository) {

    fun catalogos() = CatalogosDTO(
        hoteles = catalogos.hoteles(),
        ciudades = catalogos.ciudades(),
        regimenes = catalogos.regimenes(),
        mediosPago = catalogos.mediosPago(),
        transportes = catalogos.transportes(),
        motivosCancelacion = catalogos.motivosCancelacion(),
        estados = EstadoReserva.entries,
    )

    fun buscarClientes(texto: String) = if (texto.isBlank()) emptyList() else catalogos.buscarClientes(texto.trim())
}
