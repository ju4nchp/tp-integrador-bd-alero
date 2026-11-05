package ar.edu.unsam.reservas.config

import ar.edu.unsam.reservas.domain.HEADER_MOTOR
import ar.edu.unsam.reservas.domain.HEADER_MOTOR_USADO
import ar.edu.unsam.reservas.repository.SelectorDeMotor
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    private val selector: SelectorDeMotor,
    @Value("\${app.cors}") private val origenes: String,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/api/**")
            .allowedOrigins(*origenes.split(",").map { it.trim() }.toTypedArray())
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .exposedHeaders(HEADER_MOTOR_USADO)
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(object : HandlerInterceptor {
            override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
                runCatching { selector.motor(request.getHeader(HEADER_MOTOR)) }
                    .onSuccess { response.setHeader(HEADER_MOTOR_USADO, it.clave) }
                return true
            }
        }).addPathPatterns("/api/**")
    }
}
