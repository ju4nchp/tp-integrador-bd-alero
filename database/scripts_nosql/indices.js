// validador e indices de la coleccion resenas

// mongo no tiene fk ni check, por eso el validador
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
  },
  validationLevel: "strict",
  validationAction: "error"
});

// una reseña por reserva y busqueda por hotel
db.resenas.createIndex({ reservaId: 1 }, { unique: true, name: "uq_resenas_reservaId" });
db.resenas.createIndex({ hotelId: 1 }, { name: "idx_resenas_hotelId" });

print("resenas: " + db.resenas.countDocuments() + " documentos, indices " + db.resenas.getIndexes().map(i => i.name));
