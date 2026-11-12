-- ============================================================
-- Carga de datos de reservas_viajes desde el CSV
-- TP Integrador - Bases de Datos - TPI UNSAM
-- ============================================================
-- Paso 1: el CSV se carga tal cual en una tabla plana (base dataset_original).
-- Paso 2: cada tabla del modelo se llena con INSERT ... SELECT desde esa tabla.
--         Es la normalización hecha en SQL: SELECT DISTINCT saca los repetidos
--         y los JOIN cambian los textos por los id de cada tabla.
-- Hay que correrlo después de modelo_relacional.sql.
--
-- Con Docker se corre solo (el CSV está montado en /var/lib/mysql-files).
-- Sin Docker: copiar el CSV en la carpeta que muestra
--   SHOW VARIABLES LIKE 'secure_file_priv';
-- y cambiar la ruta del LOAD DATA, o importar el CSV con phpMyAdmin
-- en la tabla dataset_original.travel_and_tourism y correr desde el paso 2.
-- ============================================================


SET NAMES utf8mb4;

-- ============================================================
-- PASO 1: el CSV en una tabla plana (29 columnas, una fila por reserva)
-- ============================================================

DROP DATABASE IF EXISTS dataset_original;
CREATE DATABASE dataset_original CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE TABLE dataset_original.travel_and_tourism (
  Booking_ID              INT PRIMARY KEY,
  Booking_Date            DATE,
  Travel_Date             DATE,
  Return_Date             DATE,
  Customer_ID             VARCHAR(20),
  Customer_Name           VARCHAR(100),
  Customer_Age            INT,
  Gender                  VARCHAR(10),
  Customer_City           VARCHAR(60),
  Customer_Country        VARCHAR(60),
  Destination_City        VARCHAR(60),
  Destination_Country     VARCHAR(60),
  Number_of_Travellers    INT,
  Number_of_Adults        INT,
  Number_of_Children      INT,
  Hotel_Name              VARCHAR(100),
  Hotel_Rating            DECIMAL(2,1),
  Number_of_Rooms         INT,
  Number_of_Nights        INT,
  Meal_Plan               VARCHAR(40),
  Payment_Method          VARCHAR(40),
  Booking_Status          VARCHAR(20),
  Cancellation_Status     VARCHAR(20),
  Cancellation_Reason     VARCHAR(100),
  Discount_Amount         DECIMAL(10,2),
  Customer_Rating         DECIMAL(2,1),
  Customer_Review_Comment VARCHAR(255),
  Total_Trip_Cost         DECIMAL(10,2),
  Transportation_Type     VARCHAR(20)
);

LOAD DATA INFILE '/var/lib/mysql-files/Travel_And_Tourism.csv'
  INTO TABLE dataset_original.travel_and_tourism
  CHARACTER SET utf8mb4
  FIELDS TERMINATED BY ','
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;


-- ============================================================
-- PASO 2: llenar las tablas normalizadas
-- ============================================================

USE reservas_viajes;

-- Países de clientes y de destinos, sin repetir. La región no viene en el CSV.
INSERT INTO pais (nombre, region)
SELECT p.nombre,
       CASE
         WHEN p.nombre IN ('India', 'Sri Lanka', 'Nepal', 'Maldives') THEN 'Asia del Sur'
         WHEN p.nombre IN ('Indonesia', 'Malaysia', 'Singapore', 'Thailand') THEN 'Sudeste Asiático'
         WHEN p.nombre = 'United Arab Emirates' THEN 'Medio Oriente'
         WHEN p.nombre IN ('France', 'United Kingdom') THEN 'Europa'
         WHEN p.nombre IN ('Canada', 'United States') THEN 'América del Norte'
         WHEN p.nombre = 'Australia' THEN 'Oceanía'
       END
  FROM (SELECT Customer_Country AS nombre FROM dataset_original.travel_and_tourism
        UNION
        SELECT Destination_Country FROM dataset_original.travel_and_tourism) p
 ORDER BY p.nombre;

-- Ciudades de clientes y de destinos, cada una con su país (ciudad -> país).
INSERT INTO ciudad (nombre, pais_nombre)
SELECT c.nombre, c.pais
  FROM (SELECT Customer_City AS nombre, Customer_Country AS pais FROM dataset_original.travel_and_tourism
        UNION
        SELECT Destination_City, Destination_Country FROM dataset_original.travel_and_tourism) c
 ORDER BY c.pais, c.nombre;

-- Catálogos: los valores distintos de cada columna.
INSERT INTO regimen (descripcion)
SELECT DISTINCT Meal_Plan FROM dataset_original.travel_and_tourism ORDER BY Meal_Plan;

INSERT INTO medio_pago (descripcion)
SELECT DISTINCT Payment_Method FROM dataset_original.travel_and_tourism ORDER BY Payment_Method;

INSERT INTO transporte (descripcion)
SELECT DISTINCT Transportation_Type FROM dataset_original.travel_and_tourism ORDER BY Transportation_Type;

INSERT INTO estado_reserva (descripcion)
SELECT DISTINCT Booking_Status FROM dataset_original.travel_and_tourism ORDER BY Booking_Status;

-- "Not Applicable" no es un motivo: es la falta de cancelación.
INSERT INTO motivo_cancelacion (descripcion)
SELECT DISTINCT Cancellation_Reason FROM dataset_original.travel_and_tourism
 WHERE Cancellation_Reason <> 'Not Applicable'
 ORDER BY Cancellation_Reason;

-- Hoteles: el nombre se repite en cada reserva, acá queda una sola vez con su ciudad.
INSERT INTO hotel (nombre, ciudad_id)
SELECT DISTINCT t.Hotel_Name, c.id
  FROM dataset_original.travel_and_tourism t
  JOIN ciudad c ON c.nombre = t.Destination_City AND c.pais_nombre = t.Destination_Country
 ORDER BY t.Hotel_Name;

-- Clientes con su ciudad de residencia.
INSERT INTO cliente (customer_id, nombre, edad, genero, ciudad_id)
SELECT DISTINCT t.Customer_ID, t.Customer_Name, t.Customer_Age, t.Gender, c.id
  FROM dataset_original.travel_and_tourism t
  JOIN ciudad c ON c.nombre = t.Customer_City AND c.pais_nombre = t.Customer_Country
 ORDER BY t.Customer_ID;

-- Reservas: los textos se cambian por los id de cada tabla. No se cargan
-- Number_of_Travellers, Number_of_Nights ni Cancellation_Status porque se calculan.
INSERT INTO reserva (booking_id, customer_id, hotel_id, booking_date, travel_date, return_date,
                     number_of_adults, number_of_children, number_of_rooms, discount_amount,
                     total_trip_cost, regimen_id, medio_pago_id, transporte_id, estado_id, activa)
SELECT t.Booking_ID, t.Customer_ID, h.id, t.Booking_Date, t.Travel_Date, t.Return_Date,
       t.Number_of_Adults, t.Number_of_Children, t.Number_of_Rooms, t.Discount_Amount,
       t.Total_Trip_Cost, r.id, m.id, tr.id, e.id, TRUE
  FROM dataset_original.travel_and_tourism t
  JOIN hotel h           ON h.nombre = t.Hotel_Name
  JOIN regimen r         ON r.descripcion = t.Meal_Plan
  JOIN medio_pago m      ON m.descripcion = t.Payment_Method
  JOIN transporte tr     ON tr.descripcion = t.Transportation_Type
  JOIN estado_reserva e  ON e.descripcion = t.Booking_Status
 ORDER BY t.Booking_ID;

-- Cancelaciones: solo las reservas canceladas, con su motivo del catálogo.
INSERT INTO cancelacion (booking_id, motivo_id)
SELECT t.Booking_ID, mc.id
  FROM dataset_original.travel_and_tourism t
  JOIN motivo_cancelacion mc ON mc.descripcion = t.Cancellation_Reason
 WHERE t.Booking_Status = 'Cancelled'
 ORDER BY t.Booking_ID;

-- Control: cuántas filas quedaron en cada tabla.
SELECT 'pais' AS tabla, COUNT(*) AS filas FROM pais
UNION ALL SELECT 'ciudad', COUNT(*) FROM ciudad
UNION ALL SELECT 'cliente', COUNT(*) FROM cliente
UNION ALL SELECT 'hotel', COUNT(*) FROM hotel
UNION ALL SELECT 'regimen', COUNT(*) FROM regimen
UNION ALL SELECT 'medio_pago', COUNT(*) FROM medio_pago
UNION ALL SELECT 'transporte', COUNT(*) FROM transporte
UNION ALL SELECT 'estado_reserva', COUNT(*) FROM estado_reserva
UNION ALL SELECT 'motivo_cancelacion', COUNT(*) FROM motivo_cancelacion
UNION ALL SELECT 'reserva', COUNT(*) FROM reserva
UNION ALL SELECT 'cancelacion', COUNT(*) FROM cancelacion;
