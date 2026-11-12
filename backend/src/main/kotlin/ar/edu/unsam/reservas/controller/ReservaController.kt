package ar.edu.unsam.reservas.controller

import ar.edu.unsam.reservas.domain.Alcance
import ar.edu.unsam.reservas.domain.EstadoReserva
import ar.edu.unsam.reservas.domain.FiltroReservas
import ar.edu.unsam.reservas.domain.Pagina
import ar.edu.unsam.reservas.domain.hoy
import ar.edu.unsam.reservas.dto.CancelacionRequest
import ar.edu.unsam.reservas.dto.ReservaDTO
import ar.edu.unsam.reservas.dto.ReservaRequest
import ar.edu.unsam.reservas.service.ReservaService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/reservas")
class ReservaController(private val reservaService: ReservaService) {

    @GetMapping
    fun listar(
        @RequestParam(required = false) estado: EstadoReserva?,
        @RequestParam(required = false) hotelId: Int?,
        @RequestParam(required = false) pais: String?,
        @RequestParam(required = false) texto: String?,
        @RequestParam(defaultValue = "ACTIVAS") alcance: Alcance,
        @RequestParam(defaultValue = "false") salidaPendiente: Boolean,
        @RequestParam(defaultValue = "0") pagina: Int,
    ): Pagina<ReservaDTO> {
        val resultado = reservaService.listar(FiltroReservas(estado, hotelId, pais, texto, alcance, salidaPendiente), pagina)
        val hoy = hoy()
        return Pagina(resultado.contenido.map { ReservaDTO.desde(it, hoy) }, resultado.pagina, resultado.totalPaginas, resultado.totalElementos)
    }

    @GetMapping("/{id}")
    fun buscar(@PathVariable id: Int) = ReservaDTO.desde(reservaService.buscar(id), hoy())

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@RequestBody datos: ReservaRequest) = ReservaDTO.desde(reservaService.crear(datos), hoy())

    @PutMapping("/{id}")
    fun modificar(@PathVariable id: Int, @RequestBody datos: ReservaRequest) =
        ReservaDTO.desde(reservaService.modificar(id, datos), hoy())

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun darDeBaja(@PathVariable id: Int) {
        reservaService.darDeBaja(id)
    }

    @PostMapping("/{id}/confirmacion")
    fun confirmar(@PathVariable id: Int) = ReservaDTO.desde(reservaService.confirmar(id), hoy())

    @PostMapping("/{id}/cancelacion")
    fun cancelar(@PathVariable id: Int, @RequestBody datos: CancelacionRequest) =
        ReservaDTO.desde(reservaService.cancelar(id, datos.motivo), hoy())

    @PostMapping("/{id}/completar")
    fun completar(@PathVariable id: Int) = ReservaDTO.desde(reservaService.completar(id), hoy())
}
