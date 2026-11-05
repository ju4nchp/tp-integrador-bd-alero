// indices.js - MongoDB 7, base reservas_viajes
// mongosh "mongodb://usuario:clave@localhost:27017/reservas_viajes?authSource=admin" indices.js

const fecha = { bsonType: "string", pattern: "^\\d{4}-\\d{2}-\\d{2}$" };

db.runCommand({
  collMod: "reservas",
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["_id", "fechas", "cliente", "hotel", "regimen", "medioPago", "transporte", "estado",
                 "adultos", "ninos", "habitaciones", "descuento", "costoTotal", "activa"],
      properties: {
        _id: { bsonType: ["int", "long"], minimum: 1 },
        fechas: {
          bsonType: "object",
          required: ["reserva", "salida", "regreso"],
          properties: { reserva: fecha, salida: fecha, regreso: fecha }
        },
        cliente: { bsonType: "object", required: ["id", "nombre", "edad", "genero", "ciudad", "pais"],
                   properties: { edad: { bsonType: ["int", "long"], minimum: 18 },
                                 genero: { enum: ["Male", "Female"] } } },
        hotel: { bsonType: "object", required: ["id", "nombre", "ciudad", "pais"] },
        estado: { enum: ["Pending", "Confirmed", "Completed", "Cancelled"] },
        adultos: { bsonType: ["int", "long"], minimum: 1 },
        ninos: { bsonType: ["int", "long"], minimum: 0 },
        habitaciones: { bsonType: ["int", "long"], minimum: 1 },
        descuento: { bsonType: ["double", "int", "long", "decimal"], minimum: 0 },
        costoTotal: { bsonType: ["double", "int", "long", "decimal"], minimum: 0, exclusiveMinimum: true },
        activa: { bsonType: "bool" },
        cancelacion: { bsonType: "object", required: ["motivo"] }
      }
    }
  }
});

db.runCommand({
  collMod: "resenas",
  validator: {
    $jsonSchema: {
      bsonType: "object",
      required: ["reservaId", "hotelId", "hotel", "destino", "puntajeViaje", "puntajeHotel"],
      properties: {
        reservaId: { bsonType: ["int", "long"], minimum: 1 },
        hotelId: { bsonType: ["int", "long"], minimum: 1 },
        puntajeViaje: { bsonType: ["double", "int", "decimal"], minimum: 1, maximum: 5 },
        puntajeHotel: { bsonType: ["double", "int", "decimal"], minimum: 1, maximum: 5 },
        comentario: { bsonType: ["string", "null"], maxLength: 255 },
        etiquetas: { bsonType: "array", items: { bsonType: "string", maxLength: 40 } },
        respuestaAgencia: { bsonType: ["object", "null"] }
      }
    }
  }
});

db.reservas.createIndex({ estado: 1 }, { name: "idx_reservas_estado" });
db.reservas.createIndex({ "hotel.id": 1 }, { name: "idx_reservas_hotel" });
db.reservas.createIndex({ "hotel.pais": 1 }, { name: "idx_reservas_pais" });

db.resenas.createIndex({ reservaId: 1 }, { unique: true, name: "uq_resenas_reservaId" });
db.resenas.createIndex({ hotelId: 1 }, { name: "idx_resenas_hotelId" });

print("reservas: " + db.reservas.countDocuments() + " documentos, indices " + db.reservas.getIndexes().map(i => i.name));
print("resenas: " + db.resenas.countDocuments() + " documentos, indices " + db.resenas.getIndexes().map(i => i.name));
