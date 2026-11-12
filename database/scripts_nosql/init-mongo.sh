#!/bin/bash
# Corre una sola vez, cuando el volumen de Mongo esta vacio (docker-entrypoint-initdb.d)
# En MongoDB solo van las resenas: el resto de los datos esta en MySQL.
set -e

DB="${MONGO_INITDB_DATABASE:-reservas_viajes}"
URI="mongodb://${MONGO_INITDB_ROOT_USERNAME}:${MONGO_INITDB_ROOT_PASSWORD}@localhost:27017/${DB}?authSource=admin"

echo ">> Importando resenas.json"
mongoimport --uri "$URI" --collection resenas --drop --file /data/nosql/resenas.json

echo ">> Creando el validador y los indices"
mongosh "$URI" --quiet /data/nosql/indices.js
