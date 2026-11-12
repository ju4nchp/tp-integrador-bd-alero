# Alero · TP Integrador de Bases de Datos (TPI, UNSAM)
Docentes: Gabriel Flores y Rodrigo Casco.

**Alero** es una aplicación para que el dueño o encargado de cualquier alojamiento (hotel, hostel, cabañas) maneje sus reservas, cancelaciones y reseñas. El backend es **políglota**: usa dos motores al mismo tiempo, cada uno para lo que mejor le queda. **MySQL** (relacional) guarda las reservas, los huéspedes, los alojamientos y las cancelaciones, y **MongoDB** (documental) guarda las reseñas.

Utilizamos el Dataset `Travel_And_Tourism.csv` con 1000 reservas, 29 columnas, 78 alojamientos en 26 destinos de 10 países.

## Qué se puede hacer

| Pantalla | Qué hace |
|---|---|
| **Reservas** | Listado con filtros (huésped, estado, alojamiento, salidas por registrar), detalle de la reserva (el "voucher"), alta, confirmación, edición, cancelación con motivo, registrar la salida y dar de baja. Tiene una pestaña de **historial** y otra de **dadas de baja**. |
| **Reseñas** | Listado por alojamiento, registrar la reseña que dejó el huésped, responderla y ponerle etiquetas. |
| **Estadísticas** | Facturación y tasa de cancelación por país, y reservas, cancelaciones y puntaje por alojamiento. |

En el menú se ve qué base usa cada sección: Reservas → MySQL, Reseñas → MongoDB y Estadísticas → las dos.

### Reglas de negocio

- **Las reservas no se borran nunca.** "Dar de baja" es para una reserva cargada por error: sale de la lista y de las estadísticas, pero sigue guardada (baja lógica, columna `activa`). Si el huésped no viene, se **cancela** con un motivo.
- Toda reserva nueva arranca **pendiente** y se **confirma** desde su detalle.
- Solo se editan, cancelan o dan de baja reservas **pendientes o confirmadas**.
- Cancelar pide un **motivo** del catálogo `motivo_cancelacion`. Se cambia el estado y se inserta en `cancelacion` en la misma transacción.
- **Registrar la salida** (pasa a completada) solo se puede en reservas confirmadas cuya fecha de salida ya llegó.
- El **total a pagar** es el precio menos el descuento; la facturación usa ese total.
- Validaciones de la reserva: la salida es posterior a la llegada, viaja al menos un adulto, no hay más habitaciones que huéspedes, el precio es mayor a cero, el descuento no lo supera y el titular es mayor de edad.
- **Reseñas**: solo de estadías completadas y una por reserva. No se editan ni se borran, porque son la opinión del huésped. El alojamiento puede responderlas y etiquetarlas.

## Cómo levantarlo (con Docker)

Hace falta Docker Desktop. En la carpeta del proyecto:

```bash
docker compose up --build
```

| Qué | Dónde |
|---|---|
| Aplicación | http://localhost:5173 |
| API | http://localhost:9000/api |
| phpMyAdmin | http://localhost:8081 (usuario `root`, clave `root`) |
| MySQL | `localhost:3306`, base `reservas_viajes` |
| MongoDB | `localhost:27017`, base `reservas_viajes` (usuario `admin`, clave `admin`) |

Al arrancar pasa esto, en orden:

1. **mysql** corre `modelo_relacional.sql` (crea las 11 tablas) y después `datos_iniciales.sql`, que carga el CSV en una tabla plana (`dataset_original.travel_and_tourism`) y llena las tablas con `INSERT ... SELECT`.
2. **mongo** importa `resenas.json` en la colección `resenas` y corre `indices.js`, que crea el validador y los índices.
3. Con las dos bases listas arranca el **backend**, y después el **frontend**.

Las bases se cargan solo la primera vez. Para empezar de cero:

```bash
docker compose down -v
```

Si algún puerto ya está ocupado, se cambia en `.env` (hay un ejemplo en `.env.example`).

## Versión en la nube

También está publicada con planes gratis: la app en https://alero-tp-bd.onrender.com y la API en https://alero-tp-bd-api.onrender.com (las dos en Render), MySQL en Aiven y MongoDB en Atlas. La configuración de Render está en `render.yaml`.

- La API se duerme a los 15 minutos sin uso: el primer pedido después de eso tarda cerca de un minuto. No tiene que ver con la cantidad de datos.
- Los datos de la nube son compartidos: lo que cambia uno lo ven todos. La versión de Docker de cada uno es otra base distinta.
- Para volver a cargar las bases de la nube: levantar la app con Docker, completar `.env.nube` (no se sube al repo) y correr `bash database/cargar_nube.sh`.

## Cómo levantarlo sin Docker

1. **MySQL**: correr `database/modelo_relacional.sql`. Después copiar el CSV en la carpeta que muestra `SHOW VARIABLES LIKE 'secure_file_priv';`, ajustar la ruta del `LOAD DATA` en `database/datos_iniciales.sql` y correrlo. (También se puede importar el CSV con phpMyAdmin en la tabla `dataset_original.travel_and_tourism` y correr desde el paso 2 del script).
2. **MongoDB**: importar las reseñas y crear el validador y los índices:

   ```bash
   mongoimport --db reservas_viajes --collection resenas --drop --file database/scripts_nosql/resenas.json
   ```

   ```bash
   mongosh reservas_viajes database/scripts_nosql/indices.js
   ```

3. **Backend** (Java 21). Por defecto se conecta a MySQL en `localhost:3306` con `root`/`root`. Si cambia algo se pasa por variables de entorno: `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_USER`, `MYSQL_PASSWORD` y `MONGO_URI`.

   ```bash
   cd backend && ./gradlew bootRun
   ```

4. **Frontend** (Node 24):

   ```bash
   cd frontend && npm install && npm run dev
   ```

`resenas.json` sale de la consulta `database/scripts_nosql/resenas.sql`, que arma un documento por reseña a partir del CSV cargado en MySQL. Para volver a generarlo:

```bash
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot --default-character-set=utf8mb4 -N -B -r' < database/scripts_nosql/resenas.sql > database/scripts_nosql/resenas.json
```

## Cómo usa las dos bases el backend

El backend es **políglota**: cada repositorio es una interfaz, y su implementación está en la base que mejor le queda a ese dato.

| Interfaz | Implementación | Base |
|---|---|---|
| `ReservaRepository` | `MySqlReservaRepository` (Spring Data JPA y JdbcTemplate) | MySQL |
| `CatalogoRepository` | `MySqlCatalogoRepository` | MySQL |
| `ResenaRepository` | `MongoResenaRepository` (MongoTemplate) | MongoDB |

Los services no saben qué base hay atrás: usan las interfaces. Dos casos juntan las dos bases en la misma operación:

- **Cargar una reseña**: `ResenaService` busca la reserva en MySQL para controlar que exista, que esté completada y que no tenga otra reseña (MongoDB no tiene claves foráneas), y guarda la reseña en MongoDB con el hotel copiado.
- **Estadística por alojamiento**: `EstadisticaService` trae reservas y cancelaciones de MySQL (JOIN y GROUP BY) y los puntajes promedio de MongoDB (aggregate con `$group`), y los junta por el id del hotel.

## Modelo de datos

**MySQL** (11 tablas en 3FN, `database/modelo_relacional.sql`): `pais`, `ciudad`, `cliente`, `hotel`, `regimen`, `medio_pago`, `transporte`, `estado_reserva`, `motivo_cancelacion`, `reserva` y `cancelacion`. Todas usan el motor InnoDB, que soporta claves foráneas y transacciones. No se guardan los datos que se calculan: viajeros (adultos + niños), noches (salida − llegada), total a pagar (precio − descuento), el estado de cancelación y el puntaje del hotel (sale de las reseñas).

**MongoDB** (`database/scripts_nosql/`): la colección `resenas`, un documento por reseña con el hotel y el destino copiados, el array `etiquetas` y el objeto opcional `respuestaAgencia`. El `hotelId` es el id del hotel en MySQL.

```json
{
  "reservaId": 1,
  "hotelId": 32,
  "hotel": "Indus Valley Retreat",
  "destino": "Leh, India",
  "puntajeViaje": 1.0,
  "puntajeHotel": 4.6,
  "comentario": "Poor service experience",
  "etiquetas": ["atencion"],
  "respuestaAgencia": { "texto": "Gracias por avisarnos", "fecha": "2026-11-04T21:30:00" }
}
```

### Decisiones respecto del documento del TP

- A `reserva` le agregamos `number_of_adults` y `number_of_children` (si no, no se puede calcular la cantidad de viajeros) y `transporte_id` (el catálogo `transporte` no tenía FK).
- Agregamos `activa` en `reserva` para la baja lógica.
- El motivo de cancelación es un catálogo (`motivo_cancelacion`, 7 motivos), igual que régimen, medio de pago, transporte y estado.
- `pais.region` no viene en el CSV: la completa `datos_iniciales.sql`.
- El `Hotel_Rating` del CSV cambia en cada reserva: es el puntaje que dejó el huésped, así que va en la reseña (`puntajeHotel`). El puntaje de un hotel se calcula con el promedio de sus reseñas y no se guarda.
- Las reseñas están solo en MongoDB.

## API

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/reservas?estado=&hotelId=&pais=&texto=&alcance=&salidaPendiente=&pagina=` | Listado paginado (`alcance`: `ACTIVAS`, `TODAS` o `BAJAS`) |
| GET | `/api/reservas/{id}` | Una reserva |
| POST | `/api/reservas` | Alta (con huésped existente o nuevo); arranca pendiente |
| PUT | `/api/reservas/{id}` | Modificación |
| DELETE | `/api/reservas/{id}` | Baja lógica |
| POST | `/api/reservas/{id}/confirmacion` | Confirmar una reserva pendiente |
| POST | `/api/reservas/{id}/cancelacion` | Cancelar con `{ "motivo": "..." }` (un motivo del catálogo) |
| POST | `/api/reservas/{id}/completar` | Registrar la salida |
| GET | `/api/resenas?hotelId=&sinResponder=` | Listado de reseñas |
| GET | `/api/resenas/{reservaId}` | Una reseña |
| POST | `/api/resenas` | Registrar una reseña |
| PUT | `/api/resenas/{reservaId}/respuesta` | Responder con `{ "texto": "..." }` |
| PUT | `/api/resenas/{reservaId}/etiquetas` | Etiquetar con `{ "etiquetas": ["..."] }` |
| GET | `/api/catalogos` | Alojamientos, ciudades, regímenes, medios de pago, transportes y motivos |
| GET | `/api/clientes?texto=` | Buscar huéspedes por nombre |
| GET | `/api/estadisticas/paises` | Facturación y tasa de cancelación por país (MySQL) |
| GET | `/api/estadisticas/hoteles` | Reservas y cancelaciones (MySQL) con el puntaje promedio de las reseñas (MongoDB) |

### Pruebas rápidas con curl

Una reserva (sale de MySQL):

```bash
curl http://localhost:9000/api/reservas/1
```

Su reseña (sale de MongoDB):

```bash
curl http://localhost:9000/api/resenas/1
```

Una reserva con la salida antes que la llegada se rechaza:

```bash
curl -X POST http://localhost:9000/api/reservas -H "Content-Type: application/json" -d "{\"clienteId\":\"cust01\",\"hotelId\":1,\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-10\",\"adultos\":2,\"ninos\":0,\"habitaciones\":1,\"regimen\":\"Room Only\",\"medioPago\":\"UPI\",\"transporte\":\"Car\",\"costoTotal\":50000}"
```

## Estructura

```
backend/        Kotlin + Spring Boot (controller → service → repository)
frontend/       React + Vite + TypeScript
database/
  modelo_relacional.sql    crea la base y las 11 tablas
  datos_iniciales.sql      carga el CSV y llena las tablas con INSERT ... SELECT
  analisis_dataset.sql     consultas del análisis del CSV
  cargar_nube.sh           carga las bases de la nube
  scripts_nosql/           resenas.sql, resenas.json, indices.js, init-mongo.sh
documentacion/  informe (PDF), DER (DER.drawio, para abrir en draw.io), diagrama del modelo relacional y capturas
docker-compose.yml
```

## Tecnologías

MySQL 8.4, MongoDB 7, Kotlin 2.4 con Spring Boot 4.1 (Spring Data JPA y Spring Data MongoDB), React 19 con Vite y TypeScript, y Docker Compose.
