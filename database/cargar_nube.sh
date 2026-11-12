#!/bin/bash
# Carga MySQL (Aiven) y MongoDB (Atlas) con los mismos datos que la versión de Docker.
# Uso: con Docker abierto y la app levantada (docker compose up -d), completar .env.nube
# en la raíz del proyecto y correr:  bash database/cargar_nube.sh
#  - MySQL: copia la base reservas_viajes de Docker a Aiven (mysqldump), así no hace falta
#    subir el CSV a la nube.
#  - MongoDB: importa resenas.json en Atlas y crea el validador y los índices.
set -e
cd "$(dirname "$0")/.."
# Se lee .env.nube linea por linea (sin source) para que los & de las URIs no se interpreten
while IFS='=' read -r clave valor || [ -n "$clave" ]; do
  case "$clave" in NUBE_*) export "$clave=${valor%$'\r'}" ;; esac
done < .env.nube
export MSYS_NO_PATHCONV=1
DATOS="$(pwd -W 2>/dev/null || pwd)/database"

echo ">> MySQL: copiando reservas_viajes de Docker a la nube"
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysqldump -uroot --default-character-set=utf8mb4 --databases reservas_viajes --add-drop-database --no-tablespaces --set-gtid-purged=OFF' \
  | docker run --rm -i -e MYSQL_PWD="$NUBE_MYSQL_PASSWORD" mysql:8.4 \
    mysql -h "$NUBE_MYSQL_HOST" -P "$NUBE_MYSQL_PORT" -u "$NUBE_MYSQL_USER" --ssl-mode=REQUIRED --default-character-set=utf8mb4

echo ">> MongoDB: resenas"
docker run --rm -v "$DATOS/scripts_nosql:/datos:ro" mongo:7 \
  mongoimport --uri "$NUBE_MONGO_URI" --collection resenas --drop --file /datos/resenas.json

echo ">> MongoDB: validador e indices (y se borra la coleccion reservas de la version anterior)"
docker run --rm -v "$DATOS/scripts_nosql:/datos:ro" mongo:7 mongosh "$NUBE_MONGO_URI" --quiet --eval 'db.reservas.drop()' /datos/indices.js

echo ">> Listo"
