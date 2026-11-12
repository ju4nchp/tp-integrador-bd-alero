-- Arma los documentos de la colección resenas de MongoDB a partir del CSV cargado en MySQL.
-- Sale un documento JSON por línea (solo reservas completadas, que son las que tienen reseña).
-- El hotelId es el id del hotel en MySQL, así el backend puede cruzar las dos bases.
-- Se corre después de datos_iniciales.sql y la salida se guarda en resenas.json:
--   docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N -B -r' < database/scripts_nosql/resenas.sql > database/scripts_nosql/resenas.json

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
