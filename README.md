# Alero · TP Integrador de Bases de Datos (TPI, UNSAM)

**Alero** es una aplicación para que el dueño o encargado de cualquier alojamiento (hotel, hostel, cabañas) maneje sus reservas, cancelaciones y reseñas. Atrás tiene dos motores: **MySQL** (relacional) y **MongoDB** (documental)

Docentes: Gabriel Flores y Rodrigo Casco.

Dataset: `Travel_And_Tourism.csv` (1000 reservas, 29 columnas, 78 alojamientos en 26 destinos de 14 países). *(Completar en el informe de dónde se sacó el archivo).*

## Qué se puede hacer

| Pantalla | Qué hace |
|---|---|
| **Reservas** | Listado con filtros (huésped, estado, alojamiento, salidas por registrar), detalle de la reserva (el "voucher"), alta, edición, cancelación con motivo, registrar la salida y dar de baja. Tiene una pestaña de **historial** donde se ven también las reservas dadas de baja. |
| **Reseñas** | Listado por alojamiento, cargar la reseña de una estadía, responderla y ponerle etiquetas. |
| **Estadísticas** | Facturación y tasa de cancelación por país, y reservas, cancelaciones y puntaje por alojamiento. El botón **Comparar los dos motores** corre las mismas consultas en MySQL y en MongoDB, muestra cuánto tardó cada uno y si los números coinciden. |

El selector **Base de datos** (abajo a la izquierda) elige el motor. Debajo se ve qué motor respondió el último pedido y cuánto tardó.

### Reglas de negocio

- **Las reservas no se borran nunca.** "Dar de baja" la saca de la lista de activas, pero sigue en el historial y se puede consultar (baja lógica: columna `activa` en MySQL, campo `activa` en MongoDB).
- Solo se editan, cancelan o dan de baja reservas **pendientes o confirmadas**.
- Cancelar pide un **motivo**. En MySQL se cambia el estado y se inserta en `cancelacion` en la misma transacción.
- **Registrar la salida** (pasa a completada) solo se puede en reservas confirmadas cuya fecha de salida ya llegó.
- Las validaciones de la reserva: la salida tiene que ser posterior a la llegada, tiene que haber al menos un adulto, no puede haber más habitaciones que huéspedes, el costo tiene que ser mayor a cero, el descuento no puede superarlo y el titular tiene que ser mayor de edad.
- **Reseñas**: solo de estadías completadas y una por reserva. **No se editan ni se borran**, porque son la opinión del huésped. El alojamiento puede responderlas y etiquetarlas.

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

1. **loader** corre `database/cargar_datos.py`, que chequea el CSV y genera `datos_iniciales.sql`, `reservas.json` y `resenas.json`.
2. **mysql** corre `modelo_relacional.sql` y después `datos_iniciales.sql`.
3. **mongo** importa los dos `.json` y corre `indices.js`, que crea los validadores y los índices.
4. Recién con las dos bases listas arranca el **backend**, y después el **frontend**.

Las bases se cargan solo la primera vez. Para empezar de cero:

```bash
docker compose down -v
```

Si algún puerto ya está ocupado, se cambia en `.env` (hay un ejemplo en `.env.example`).

## Versión en la nube

También está publicada con planes gratis: la app en https://alero-tp-bd.onrender.com y la API en https://alero-tp-bd-api.onrender.com (las dos en Render), MySQL en Aiven y MongoDB en Atlas. La configuración de Render está en `render.yaml`.

- La API se duerme a los 15 minutos sin uso: el primer pedido después de eso tarda cerca de un minuto.
- Los datos son compartidos: lo que cambia uno lo ven todos.
- Para volver a cargar las bases de la nube: completar `.env.nube` (no se sube al repo) y correr `bash database/cargar_nube.sh`, con Docker abierto.

## Cómo levantarlo sin Docker

1. **MySQL**: correr `database/modelo_relacional.sql` y `database/datos_iniciales.sql`, por ejemplo desde MySQL Workbench.
2. **MongoDB**: importar las colecciones y crear los índices:

   ```bash
   mongoimport --db reservas_viajes --collection reservas --jsonArray --file database/scripts_nosql/reservas.json
   ```

   ```bash
   mongoimport --db reservas_viajes --collection resenas --jsonArray --file database/scripts_nosql/resenas.json
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

Para regenerar los datos desde el CSV (hace falta `pandas`):

```bash
python database/cargar_datos.py
```

## Cómo se elige el motor

El backend es **polimórfico**: cada repositorio es una interfaz con dos implementaciones.

| Interfaz | MySQL | MongoDB |
|---|---|---|
| `ReservaRepository` | `MySqlReservaRepository` (Spring Data JPA) | `MongoReservaRepository` (MongoTemplate) |
| `ResenaRepository` | `MySqlResenaRepository` | `MongoResenaRepository` |
| `CatalogoRepository` | `MySqlCatalogoRepository` | `MongoCatalogoRepository` |

`SelectorDeMotor` elige la implementación **en cada pedido**:

- el frontend manda el header `X-DB-Engine: mysql` o `X-DB-Engine: mongo` según lo que esté elegido en el selector;
- si el pedido no trae el header, se usa `app.motor-por-defecto` de `application.yml`, que se puede cambiar con la variable `MOTOR_POR_DEFECTO` (por defecto `mysql`);
- cada respuesta trae el header `X-DB-Engine-Used` con el motor que se usó.

Los services (`ReservaService`, `ResenaService`, etc.) no saben qué motor hay atrás: las reglas de negocio están en el dominio (`Reserva`, `Resena`) y valen igual para los dos. Cada motor guarda **su propia copia** de los datos. Al arrancar las dos tienen lo mismo, así que las estadísticas dan iguales.

## Modelo de datos

**MySQL** (12 tablas, `database/modelo_relacional.sql`): `pais`, `ciudad`, `cliente`, `hotel`, `regimen`, `medio_pago`, `transporte`, `estado_reserva`, `reserva`, `cancelacion`, `resena` y `resena_etiqueta`. No se guardan los datos que se calculan: viajeros (adultos + niños), noches (salida − llegada) y el estado de cancelación.

**MongoDB** (`database/scripts_nosql/`):

- `reservas`: un documento por reserva, con el cliente, el hotel y las fechas embebidos. La cancelación va adentro solo si existe.
- `resenas`: un documento por reseña, con hotel y destino copiados, el array `etiquetas` y el objeto opcional `respuestaAgencia`.

Ejemplo de documento de `resenas`:

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
- Agregamos `resena` y `resena_etiqueta` para que las reseñas también funcionen con MySQL y se puedan comparar los dos motores.
- `pais.region` no viene en el CSV: la completa el script de carga.
- `hotel.categoria_base` es el promedio del `Hotel_Rating` de cada hotel, porque en el CSV ese valor cambia en cada reserva.
- En MongoDB hay dos colecciones: `reservas` (como en la propuesta) y `resenas` (como en el documento).

## API

Todas las rutas aceptan el header `X-DB-Engine` (`mysql` o `mongo`).

| Método | Ruta | Qué hace |
|---|---|---|
| GET | `/api/reservas?estado=&hotelId=&pais=&texto=&alcance=&salidaPendiente=&pagina=` | Listado paginado (`alcance`: `ACTIVAS`, `TODAS` o `BAJAS`) |
| GET | `/api/reservas/{id}` | Una reserva |
| POST | `/api/reservas` | Alta (con huésped existente o nuevo) |
| PUT | `/api/reservas/{id}` | Modificación |
| DELETE | `/api/reservas/{id}` | Baja lógica |
| POST | `/api/reservas/{id}/cancelacion` | Cancelar con `{ "motivo": "..." }` |
| POST | `/api/reservas/{id}/completar` | Registrar la salida |
| GET | `/api/resenas?hotelId=&sinResponder=` | Listado de reseñas |
| GET | `/api/resenas/{reservaId}` | Una reseña |
| POST | `/api/resenas` | Cargar una reseña |
| PUT | `/api/resenas/{reservaId}/respuesta` | Responder con `{ "texto": "..." }` |
| PUT | `/api/resenas/{reservaId}/etiquetas` | Etiquetar con `{ "etiquetas": ["..."] }` |
| GET | `/api/catalogos` | Alojamientos, ciudades, regímenes, medios de pago, transportes y motivos |
| GET | `/api/clientes?texto=` | Buscar huéspedes por nombre |
| GET | `/api/estadisticas/paises` | Facturación y tasa de cancelación por país (JOIN + GROUP BY / aggregate) |
| GET | `/api/estadisticas/hoteles` | Reservas, cancelaciones y puntaje promedio por alojamiento |

### Pruebas rápidas con curl

La misma reserva, leída de cada motor (fijate el header `X-DB-Engine-Used`):

```bash
curl -i -H "X-DB-Engine: mysql" http://localhost:9000/api/reservas/1
```

```bash
curl -i -H "X-DB-Engine: mongo" http://localhost:9000/api/reservas/1
```

Una reserva con la salida antes que la llegada se rechaza:

```bash
curl -X POST http://localhost:9000/api/reservas -H "Content-Type: application/json" -H "X-DB-Engine: mongo" -d "{\"clienteId\":\"cust01\",\"hotelId\":1,\"checkIn\":\"2026-12-20\",\"checkOut\":\"2026-12-10\",\"adultos\":2,\"ninos\":0,\"habitaciones\":1,\"regimen\":\"Room Only\",\"medioPago\":\"UPI\",\"transporte\":\"Car\",\"costoTotal\":50000}"
```

La consulta por país en MongoDB:

```bash
curl -H "X-DB-Engine: mongo" http://localhost:9000/api/estadisticas/paises
```

## Estructura

```
backend/        Kotlin + Spring Boot (controller → service → repository)
frontend/       React + Vite + TypeScript
database/
  modelo_relacional.sql
  datos_iniciales.sql      generado por cargar_datos.py
  cargar_datos.py
  scripts_nosql/           reservas.json, resenas.json, indices.js, init-mongo.sh
documentacion/  informe y DER
docker-compose.yml
```

## Tecnologías

MySQL 8.4, MongoDB 7, Kotlin 2.4 con Spring Boot 4.1 (Spring Data JPA y Spring Data MongoDB), React 19 con Vite y TypeScript, Python con pandas para la carga, y Docker Compose.
