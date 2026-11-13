#!/bin/bash
# carga las reseñas en mongo la primera vez que arranca
set -e

DB="${MONGO_INITDB_DATABASE:-reservas_viajes}"
URI="mongodb://${MONGO_INITDB_ROOT_USERNAME}:${MONGO_INITDB_ROOT_PASSWORD}@localhost:27017/${DB}?authSource=admin"

echo ">> Importando resenas.json"
mongoimport --uri "$URI" --collection resenas --drop --file /data/nosql/resenas.json

echo ">> Creando el validador y los indices"
mongosh "$URI" --quiet /data/nosql/indices.js
