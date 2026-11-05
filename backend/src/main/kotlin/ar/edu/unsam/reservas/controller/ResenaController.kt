package ar.edu.unsam.reservas.controller

import ar.edu.unsam.reservas.domain.HEADER_MOTOR
import ar.edu.unsam.reservas.dto.EtiquetasRequest
import ar.edu.unsam.reservas.dto.ResenaRequest
import ar.edu.unsam.reservas.dto.RespuestaRequest
import ar.edu.unsam.reservas.repository.SelectorDeMotor
import ar.edu.unsam.reservas.service.ResenaService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/resenas")
class ResenaController(private val resenaService: ResenaService, private val selector: SelectorDeMotor) {

    @GetMapping
    fun listar(
        @RequestHeader(HEADER_MOTOR, required = false) motor: String?,
        @RequestParam(required = false) hotelId: Int?,
        @RequestParam(defaultValue = "false") sinResponder: Boolean,
    ) = resenaService.listar(selector.motor(motor), hotelId, sinResponder)

    @GetMapping("/{reservaId}")
    fun buscar(@RequestHeader(HEADER_MOTOR, required = false) motor: String?, @PathVariable reservaId: Int) =
        resenaService.buscar(selector.motor(motor), reservaId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@RequestHeader(HEADER_MOTOR, required = false) motor: String?, @RequestBody datos: ResenaRequest) =
        resenaService.crear(selector.motor(motor), datos)

    @PutMapping("/{reservaId}/respuesta")
    fun responder(
        @RequestHeader(HEADER_MOTOR, required = false) motor: String?,
        @PathVariable reservaId: Int,
        @RequestBody datos: RespuestaRequest,
    ) = resenaService.responder(selector.motor(motor), reservaId, datos.texto)

    @PutMapping("/{reservaId}/etiquetas")
    fun etiquetar(
        @RequestHeader(HEADER_MOTOR, required = false) motor: String?,
        @PathVariable reservaId: Int,
        @RequestBody datos: EtiquetasRequest,
    ) = resenaService.etiquetar(selector.motor(motor), reservaId, datos.etiquetas)
}
