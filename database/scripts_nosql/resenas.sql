-- arma las reseñas para mongo, un json por linea (solo las completadas)
-- la salida se guarda en resenas.json

SELECT JSON_OBJECT(
         'reservaId', t.Booking_ID,
         'hotelId', h.id,
         'hotel', h.nombre,
         'destino', CONCAT(c.nombre, ', ', c.pais_nombre),
         'puntajeViaje', t.Customer_Rating,
         'puntajeHotel', t.Hotel_Rating,
         'comentario', t.Customer_Review_Comment,
         'etiquetas', JSON_ARRAY(),
         'respuestaAgencia', NULL
       )
  FROM dataset_original.travel_and_tourism t
  JOIN reservas_viajes.hotel h  ON h.nombre = t.Hotel_Name
  JOIN reservas_viajes.ciudad c ON c.id = h.ciudad_id
 WHERE t.Booking_Status = 'Completed'
 ORDER BY t.Booking_ID;
