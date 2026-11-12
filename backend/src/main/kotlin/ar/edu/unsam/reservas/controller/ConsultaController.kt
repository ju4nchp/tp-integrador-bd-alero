package ar.edu.unsam.reservas.controller

import ar.edu.unsam.reservas.service.CatalogoService
import ar.edu.unsam.reservas.service.EstadisticaService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ConsultaController(
    private val catalogoService: CatalogoService,
    private val estadisticaService: EstadisticaService,
) {

    @GetMapping("/catalogos")
    fun catalogos() = catalogoService.catalogos()

    @GetMapping("/clientes")
    fun clientes(@RequestParam texto: String) = catalogoService.buscarClientes(texto)

    @GetMapping("/estadisticas/paises")
    fun porPais() = estadisticaService.porPais()

    @GetMapping("/estadisticas/hoteles")
    fun porHotel() = estadisticaService.porHotel()
}
