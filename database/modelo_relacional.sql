-- ============================================================
-- BASE DE DATOS: reservas_viajes
-- TP Integrador - Bases de Datos - TPI UNSAM
-- Modelo relacional en 3FN (11 tablas) para MySQL 8.4
-- ============================================================
-- Las reseñas NO están acá: se guardan en MongoDB (colección resenas).
-- No se guardan columnas que se pueden calcular:
--   viajeros    = number_of_adults + number_of_children
--   noches      = return_date - travel_date
--   total final = total_trip_cost - discount_amount
--   el estado de cancelación sale de estado_reserva
--   el puntaje de un hotel sale del promedio de sus reseñas
-- Las reservas no se borran: la baja es lógica (columna activa).
-- Todas las tablas usan el motor InnoDB (el de MySQL por defecto),
-- que es el que soporta claves foráneas y transacciones.
-- ============================================================

DROP DATABASE IF EXISTS reservas_viajes;

CREATE DATABASE reservas_viajes CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

USE reservas_viajes;


-- ============================================================
-- 1. PAIS
-- ============================================================

CREATE TABLE pais (
    nombre VARCHAR(100) PRIMARY KEY,
    region VARCHAR(100) NOT NULL
);


-- ============================================================
-- 2. CIUDAD (sirve para la ciudad del cliente y la del hotel)
-- ============================================================

CREATE TABLE ciudad (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    pais_nombre VARCHAR(100) NOT NULL,

    CONSTRAINT uq_ciudad_nombre_pais UNIQUE (nombre, pais_nombre),

    CONSTRAINT fk_ciudad_pais
        FOREIGN KEY (pais_nombre)
        REFERENCES pais(nombre)
);


-- ============================================================
-- 3. CLIENTE
-- ============================================================

CREATE TABLE cliente (
    customer_id VARCHAR(50) PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    edad INT NOT NULL,
    genero VARCHAR(20) NOT NULL,
    ciudad_id INT NOT NULL,

    CONSTRAINT chk_cliente_edad CHECK (edad >= 18),
    CONSTRAINT chk_cliente_genero CHECK (genero IN ('Male', 'Female')),

    CONSTRAINT fk_cliente_ciudad
        FOREIGN KEY (ciudad_id)
        REFERENCES ciudad(id)
);


-- ============================================================
-- 4. HOTEL
-- ============================================================

CREATE TABLE hotel (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    ciudad_id INT NOT NULL,

    CONSTRAINT uq_hotel_nombre_ciudad UNIQUE (nombre, ciudad_id),

    CONSTRAINT fk_hotel_ciudad
        FOREIGN KEY (ciudad_id)
        REFERENCES ciudad(id)
);


-- ============================================================
-- 5. CATALOGOS: regimen, medio_pago, transporte, estado_reserva,
--    motivo_cancelacion
-- ============================================================

CREATE TABLE regimen (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE medio_pago (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE transporte (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE estado_reserva (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE motivo_cancelacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    descripcion VARCHAR(100) NOT NULL UNIQUE
);


-- ============================================================
-- 6. RESERVA (tabla transaccional central)
-- ============================================================

CREATE TABLE reserva (
    booking_id INT PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    hotel_id INT NOT NULL,
    booking_date DATE NOT NULL,
    travel_date DATE NOT NULL,
    return_date DATE NOT NULL,
    number_of_adults INT NOT NULL,
    number_of_children INT NOT NULL DEFAULT 0,
    number_of_rooms INT NOT NULL,
    discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_trip_cost DECIMAL(10,2) NOT NULL,
    regimen_id INT NOT NULL,
    medio_pago_id INT NOT NULL,
    transporte_id INT NOT NULL,
    estado_id INT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT chk_reserva_fechas CHECK (booking_date <= travel_date AND travel_date < return_date),
    CONSTRAINT chk_reserva_adultos CHECK (number_of_adults >= 1),
    CONSTRAINT chk_reserva_ninos CHECK (number_of_children >= 0),
    CONSTRAINT chk_reserva_habitaciones CHECK (number_of_rooms >= 1 AND number_of_rooms <= number_of_adults + number_of_children),
    CONSTRAINT chk_reserva_costo CHECK (total_trip_cost > 0),
    CONSTRAINT chk_reserva_descuento CHECK (discount_amount >= 0 AND discount_amount <= total_trip_cost),

    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (customer_id)
        REFERENCES cliente(customer_id),

    CONSTRAINT fk_reserva_hotel
        FOREIGN KEY (hotel_id)
        REFERENCES hotel(id),

    CONSTRAINT fk_reserva_regimen
        FOREIGN KEY (regimen_id)
        REFERENCES regimen(id),

    CONSTRAINT fk_reserva_medio_pago
        FOREIGN KEY (medio_pago_id)
        REFERENCES medio_pago(id),

    CONSTRAINT fk_reserva_transporte
        FOREIGN KEY (transporte_id)
        REFERENCES transporte(id),

    CONSTRAINT fk_reserva_estado
        FOREIGN KEY (estado_id)
        REFERENCES estado_reserva(id)
);


-- ============================================================
-- 7. CANCELACION (entidad opcional 1:0..1)
-- ============================================================

CREATE TABLE cancelacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL UNIQUE,
    motivo_id INT NOT NULL,

    CONSTRAINT fk_cancelacion_reserva
        FOREIGN KEY (booking_id)
        REFERENCES reserva(booking_id),

    CONSTRAINT fk_cancelacion_motivo
        FOREIGN KEY (motivo_id)
        REFERENCES motivo_cancelacion(id)
);
