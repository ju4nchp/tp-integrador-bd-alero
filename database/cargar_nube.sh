#!/bin/bash
# Carga MySQL (Aiven) y MongoDB (Atlas) con los mismos datos que usa Docker.
# Uso: completar .env.nube en la raiz del proyecto y correr:  bash database/cargar_nube.sh
# Hace falta Docker abierto: los clientes mysql y mongoimport corren en contenedores.
set -e
cd "$(dirname "$0")/.."
# Se lee .env.nube linea por linea (sin source) para que los & de las URIs no se interpreten
while IFS='=' read -r clave valor || [ -n "$clave" ]; do
  case "$clave" in NUBE_*) export "$clave=${valor%$'\r'}" ;; esac
done < .env.nube
export MSYS_NO_PATHCONV=1
DATOS="$(pwd -W 2>/dev/null || pwd)/database"

echo ">> MySQL: modelo relacional"
docker run --rm -i -e MYSQL_PWD="$NUBE_MYSQL_PASSWORD" mysql:8.4 \
  mysql -h "$NUBE_MYSQL_HOST" -P "$NUBE_MYSQL_PORT" -u "$NUBE_MYSQL_USER" --ssl-mode=REQUIRED < database/modelo_relacional.sql

echo ">> MySQL: datos iniciales"
docker run --rm -i -e MYSQL_PWD="$NUBE_MYSQL_PASSWORD" mysql:8.4 \
  mysql -h "$NUBE_MYSQL_HOST" -P "$NUBE_MYSQL_PORT" -u "$NUBE_MYSQL_USER" --ssl-mode=REQUIRED < database/datos_iniciales.sql

for coleccion in reservas resenas; do
  echo ">> MongoDB: $coleccion"
  docker run --rm -v "$DATOS/scripts_nosql:/datos:ro" mongo:7 \
    mongoimport --uri "$NUBE_MONGO_URI" --collection "$coleccion" --drop --jsonArray --file "/datos/$coleccion.json"
done

echo ">> MongoDB: validadores e indices"
docker run --rm -v "$DATOS/scripts_nosql:/datos:ro" mongo:7 mongosh "$NUBE_MONGO_URI" --quiet /datos/indices.js

echo ">> Listo"
