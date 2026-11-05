package ar.edu.unsam.reservas.controller

import ar.edu.unsam.reservas.domain.BusinessException
import ar.edu.unsam.reservas.domain.NotFoundException
import ar.edu.unsam.reservas.dto.ErrorDTO
import com.mongodb.MongoWriteException
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.sql.SQLException

@RestControllerAdvice
class ErrorHandler {

    private val log = LoggerFactory.getLogger(ErrorHandler::class.java)

    @ExceptionHandler(NotFoundException::class)
    fun noEncontrado(e: NotFoundException) = respuesta(HttpStatus.NOT_FOUND, e.message ?: "No encontrado")

    @ExceptionHandler(BusinessException::class)
    fun negocio(e: BusinessException) = respuesta(HttpStatus.BAD_REQUEST, e.message ?: "Operación inválida")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun metodoNoPermitido(e: HttpRequestMethodNotSupportedException) =
        respuesta(HttpStatus.METHOD_NOT_ALLOWED, "Esa operación no está permitida", e.message)

    @ExceptionHandler(NoResourceFoundException::class)
    fun rutaInexistente(e: NoResourceFoundException) = respuesta(HttpStatus.NOT_FOUND, "No existe esa ruta de la API")

    @ExceptionHandler(HttpMessageNotReadableException::class, MethodArgumentTypeMismatchException::class, MissingServletRequestParameterException::class)
    fun malFormado(e: Exception) = respuesta(HttpStatus.BAD_REQUEST, "Los datos enviados no tienen el formato esperado")

    @ExceptionHandler(DataAccessException::class)
    fun baseDeDatos(e: DataAccessException): ResponseEntity<ErrorDTO> {
        val causa = e.mostSpecificCause
        if (causa is SQLException) {
            return respuesta(HttpStatus.BAD_REQUEST, "MySQL rechazó la operación por una restricción", causa.message)
        }
        log.error("Error de acceso a datos", e)
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error de acceso a datos", causa.message)
    }

    @ExceptionHandler(MongoWriteException::class)
    fun mongo(e: MongoWriteException) = respuesta(HttpStatus.BAD_REQUEST, "MongoDB rechazó la operación", e.error.message)

    @ExceptionHandler(Exception::class)
    fun otro(e: Exception): ResponseEntity<ErrorDTO> {
        log.error("Error no controlado", e)
        return respuesta(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", e.message)
    }

    private fun respuesta(status: HttpStatus, error: String, detalle: String? = null) =
        ResponseEntity.status(status).body(ErrorDTO(status.value(), error, detalle))
}
