-- analisis del csv antes de normalizar (tabla dataset_original.travel_and_tourism)

USE dataset_original;

-- 1. el hotel se repite en cada reserva
SELECT Hotel_Name, Destination_City, Destination_Country, COUNT(*) AS reservas
  FROM travel_and_tourism
 GROUP BY Hotel_Name, Destination_City, Destination_Country
 ORDER BY reservas DESC
 LIMIT 10;

-- 2. cada ciudad tiene un solo pais
SELECT Destination_City, COUNT(DISTINCT Destination_Country) AS paises, COUNT(*) AS filas
  FROM travel_and_tourism
 GROUP BY Destination_City
 ORDER BY filas DESC;

-- 3. el puntaje del hotel cambia en cada reserva
SELECT Hotel_Name, COUNT(*) AS reservas, MIN(Hotel_Rating) AS minimo, MAX(Hotel_Rating) AS maximo,
       ROUND(AVG(Hotel_Rating), 1) AS promedio
  FROM travel_and_tourism
 GROUP BY Hotel_Name
 ORDER BY reservas DESC
 LIMIT 10;

-- 4. columnas que se calculan (si da 0 se pueden sacar)
SELECT SUM(Number_of_Travellers <> Number_of_Adults + Number_of_Children)       AS viajeros_distintos,
       SUM(Number_of_Nights <> DATEDIFF(Return_Date, Travel_Date))              AS noches_distintas,
       SUM((Booking_Status = 'Cancelled') <> (Cancellation_Status = 'Cancelled')) AS estado_distinto
  FROM travel_and_tourism;

-- 5. valores vacios (not applicable y puntajes en 0)
SELECT SUM(Cancellation_Reason = 'Not Applicable') AS sin_motivo_de_cancelacion,
       SUM(Customer_Rating = 0)                    AS sin_resena,
       COUNT(*)                                    AS total
  FROM travel_and_tourism;
