-- Paso 1 del TP: análisis del dataset antes de normalizar.
-- Cargamos el CSV tal cual viene, en una sola tabla de 29 columnas, y lo revisamos con consultas.
-- Con Docker:
--   docker cp Travel_And_Tourism.csv alero-mysql-1:/var/lib/mysql-files/
--   docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < database/analisis_dataset.sql
-- No es parte del modelo final: la base del sistema es reservas_viajes (modelo_relacional.sql).

DROP DATABASE IF EXISTS dataset_original;
CREATE DATABASE dataset_original CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE dataset_original;

CREATE TABLE travel_and_tourism (
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
  INTO TABLE travel_and_tourism
  CHARACTER SET utf8mb4
  FIELDS TERMINATED BY ','
  LINES TERMINATED BY '\n'
  IGNORE 1 LINES;

-- 1. Los datos del hotel se repiten en cada reserva (nombre, ciudad y país).
SELECT Hotel_Name, Destination_City, Destination_Country, COUNT(*) AS reservas
  FROM travel_and_tourism
 GROUP BY Hotel_Name, Destination_City, Destination_Country
 ORDER BY reservas DESC
 LIMIT 10;

-- 2. Dependencia transitiva: cada ciudad tiene un solo país (ciudad -> país).
SELECT Destination_City, COUNT(DISTINCT Destination_Country) AS paises, COUNT(*) AS filas
  FROM travel_and_tourism
 GROUP BY Destination_City
 ORDER BY filas DESC;

-- 3. El puntaje del hotel cambia en cada reserva: no es un dato fijo del hotel.
SELECT Hotel_Name, COUNT(*) AS reservas, MIN(Hotel_Rating) AS minimo, MAX(Hotel_Rating) AS maximo,
       ROUND(AVG(Hotel_Rating), 1) AS promedio
  FROM travel_and_tourism
 GROUP BY Hotel_Name
 ORDER BY reservas DESC
 LIMIT 10;

-- 4. Columnas que se pueden calcular con otras (si da 0 en las tres, se pueden sacar).
SELECT SUM(Number_of_Travellers <> Number_of_Adults + Number_of_Children)       AS viajeros_distintos,
       SUM(Number_of_Nights <> DATEDIFF(Return_Date, Travel_Date))              AS noches_distintas,
       SUM((Booking_Status = 'Cancelled') <> (Cancellation_Status = 'Cancelled')) AS estado_distinto
  FROM travel_and_tourism;

-- 5. Valores "vacíos disfrazados": reservas sin cancelación y sin reseña.
SELECT SUM(Cancellation_Reason = 'Not Applicable') AS sin_motivo_de_cancelacion,
       SUM(Customer_Rating = 0)                    AS sin_resena,
       COUNT(*)                                    AS total
  FROM travel_and_tourism;
