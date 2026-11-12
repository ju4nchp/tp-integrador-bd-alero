-- Paso 1 del TP: análisis del dataset antes de normalizar.
-- El CSV ya está cargado tal cual en la tabla plana dataset_original.travel_and_tourism
-- (lo hace el paso 1 de datos_iniciales.sql). Acá lo revisamos con consultas.
-- Con Docker:
--   docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < database/analisis_dataset.sql

USE dataset_original;

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
