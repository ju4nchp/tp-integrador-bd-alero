package ar.edu.unsam.reservas.service

import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.domain.Motor
import ar.edu.unsam.reservas.dto.CatalogosDTO
import ar.edu.unsam.reservas.repository.SelectorDeMotor
import org.springframework.stereotype.Service

@Service
class CatalogoService(private val selector: SelectorDeMotor) {

    fun catalogos(motor: Motor): CatalogosDTO {
        val catalogos = selector.catalogos(motor)
        return CatalogosDTO(
            hoteles = catalogos.hoteles(),
            ciudades = catalogos.ciudades(),
            regimenes = catalogos.regimenes(),
            mediosPago = catalogos.mediosPago(),
            transportes = catalogos.transportes(),
            motivosCancelacion = catalogos.motivosCancelacion(),
            estados = EstadoReserva.entries,
        )
    }

    fun buscarClientes(motor: Motor, texto: String) =
        if (texto.isBlank()) emptyList() else selector.catalogos(motor).buscarClientes(texto.trim())
}
