package ar.edu.unsam.reservas

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@SpringBootApplication
@EnableJpaRepositories(basePackages = ["ar.edu.unsam.reservas.repository.mysql"])
@EnableMongoRepositories(basePackages = ["ar.edu.unsam.reservas.repository.mongo"])
class ReservasApplication

fun main(args: Array<String>) {
    runApplication<ReservasApplication>(*args)
}
