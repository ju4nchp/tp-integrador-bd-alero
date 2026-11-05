# Imagen del servicio "loader": regenera datos_iniciales.sql y resenas.json desde el CSV
FROM python:3.12-slim
RUN pip install --no-cache-dir pandas==2.2.3
WORKDIR /app
CMD ["python", "/app/database/cargar_datos.py", "--csv", "/data/Travel_And_Tourism.csv", "--out", "/app/database"]
