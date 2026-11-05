package ar.edu.unsam.reservas.controller

import ar.edu.unsam.reservas.domain.HEADER_MOTOR
import ar.edu.unsam.reservas.repository.SelectorDeMotor
import ar.edu.unsam.reservas.service.CatalogoService
import ar.edu.unsam.reservas.service.EstadisticaService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class ConsultaController(
    private val catalogoService: CatalogoService,
    private val estadisticaService: EstadisticaService,
    private val selector: SelectorDeMotor,
) {

    @GetMapping("/catalogos")
    fun catalogos(@RequestHeader(HEADER_MOTOR, required = false) motor: String?) =
        catalogoService.catalogos(selector.motor(motor))

    @GetMapping("/clientes")
    fun clientes(@RequestHeader(HEADER_MOTOR, required = false) motor: String?, @RequestParam texto: String) =
        catalogoService.buscarClientes(selector.motor(motor), texto)

    @GetMapping("/estadisticas/paises")
    fun porPais(@RequestHeader(HEADER_MOTOR, required = false) motor: String?) =
        estadisticaService.porPais(selector.motor(motor))

    @GetMapping("/estadisticas/hoteles")
    fun porHotel(@RequestHeader(HEADER_MOTOR, required = false) motor: String?) =
        estadisticaService.porHotel(selector.motor(motor))
}
