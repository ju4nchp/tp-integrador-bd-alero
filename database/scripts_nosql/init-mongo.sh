#!/bin/bash
# Corre una sola vez, cuando el volumen de Mongo esta vacio (docker-entrypoint-initdb.d)
set -e

DB="${MONGO_INITDB_DATABASE:-reservas_viajes}"
URI="mongodb://${MONGO_INITDB_ROOT_USERNAME}:${MONGO_INITDB_ROOT_PASSWORD}@localhost:27017/${DB}?authSource=admin"

for coleccion in reservas resenas; do
  echo ">> Importando ${coleccion}.json"
  mongoimport --uri "$URI" --collection "$coleccion" --drop --jsonArray --file "/data/nosql/${coleccion}.json"
done

echo ">> Creando validadores e indices"
mongosh "$URI" --quiet /data/nosql/indices.js
