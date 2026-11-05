"""
cargar_datos.py - TP Integrador Bases de Datos (TPI UNSAM)

Lee Travel_And_Tourism.csv, chequea los datos y arma los archivos de carga:

  - datos_iniciales.sql            INSERTs para MySQL, en orden de FK
  - scripts_nosql/reservas.json    coleccion reservas (un documento por reserva)
  - scripts_nosql/resenas.json     coleccion resenas (una por reserva completada)

Uso:  python cargar_datos.py [--csv RUTA] [--out CARPETA]
"""

import argparse
import json
import sys
from pathlib import Path

import pandas as pd

AQUI = Path(__file__).resolve().parent

REGIONES = {
    "India": "Asia del Sur", "Sri Lanka": "Asia del Sur", "Nepal": "Asia del Sur", "Maldives": "Asia del Sur",
    "Indonesia": "Sudeste Asiático", "Malaysia": "Sudeste Asiático", "Singapore": "Sudeste Asiático",
    "Thailand": "Sudeste Asiático", "United Arab Emirates": "Medio Oriente",
    "France": "Europa", "United Kingdom": "Europa",
    "Canada": "América del Norte", "United States": "América del Norte", "Australia": "Oceanía",
}


def texto(valor):
    return "'" + str(valor).replace("\\", "\\\\").replace("'", "''") + "'"


def insert(tabla, columnas, filas, lote=250):
    if not filas:
        return f"-- {tabla}: sin filas\n"
    partes = [f"-- {tabla}: {len(filas)} filas"]
    for i in range(0, len(filas), lote):
        valores = ",\n  ".join("(" + ", ".join(f) + ")" for f in filas[i:i + lote])
        partes.append(f"INSERT INTO {tabla} ({', '.join(columnas)}) VALUES\n  {valores};")
    return "\n".join(partes) + "\n"


def chequear(descripcion, malas, total, fallas):
    if malas:
        fallas.append(descripcion)
    print(f"  [{'OK' if not malas else 'ERROR'}] {descripcion}: {total - malas}/{total}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--csv", default=str(AQUI.parent / "Travel_And_Tourism.csv"))
    ap.add_argument("--out", default=str(AQUI))
    args = ap.parse_args()

    df = pd.read_csv(args.csv, encoding="utf-8-sig")
    df.columns = [c.strip() for c in df.columns]
    for c in df.columns:
        if df[c].dtype == object or str(df[c].dtype).startswith("str"):
            df[c] = df[c].astype(str).str.strip()
    for c in ("Booking_Date", "Travel_Date", "Return_Date"):
        df[c] = pd.to_datetime(df[c], format="%Y-%m-%d")
    df = df.sort_values("Booking_ID")
    n = len(df)
    print(f"Leido {args.csv}: {n} filas, {len(df.columns)} columnas\n")

    fallas = []
    print("Columnas que no se guardan porque se calculan:")
    chequear("Number_of_Travellers = adultos + ninos",
             int((df.Number_of_Travellers != df.Number_of_Adults + df.Number_of_Children).sum()), n, fallas)
    chequear("Number_of_Nights = Return_Date - Travel_Date",
             int((df.Number_of_Nights != (df.Return_Date - df.Travel_Date).dt.days).sum()), n, fallas)
    esperado = df.Booking_Status.map(lambda s: "Cancelled" if s == "Cancelled" else "Not Cancelled")
    chequear("Cancellation_Status sale de Booking_Status", int((df.Cancellation_Status != esperado).sum()), n, fallas)

    print("\nDependencias y reglas del modelo:")
    chequear("Booking_ID unico", int(df.Booking_ID.duplicated().sum()), n, fallas)
    cli = df.groupby("Customer_ID")[["Customer_Name", "Customer_Age", "Gender", "Customer_City"]].nunique()
    chequear("Customer_ID -> nombre, edad, genero, ciudad", int((cli > 1).any(axis=1).sum()), len(cli), fallas)
    hot = df.groupby("Hotel_Name")[["Destination_City", "Destination_Country"]].nunique()
    chequear("Hotel -> ciudad", int((hot > 1).any(axis=1).sum()), len(hot), fallas)
    ciudades = pd.concat([
        df[["Customer_City", "Customer_Country"]].set_axis(["c", "p"], axis=1),
        df[["Destination_City", "Destination_Country"]].set_axis(["c", "p"], axis=1),
    ]).drop_duplicates()
    chequear("Ciudad -> pais", int(ciudades.c.duplicated().sum()), len(ciudades), fallas)
    chequear("Todos los paises tienen region", int((~ciudades.p.isin(REGIONES.keys())).sum()), len(ciudades), fallas)
    chequear("reserva <= salida < regreso",
             int(((df.Booking_Date > df.Travel_Date) | (df.Travel_Date >= df.Return_Date)).sum()), n, fallas)
    chequear("adultos >= 1 y habitaciones entre 1 y la cantidad de viajeros",
             int(((df.Number_of_Adults < 1) | (df.Number_of_Rooms < 1) | (df.Number_of_Rooms > df.Number_of_Travellers)).sum()), n, fallas)
    chequear("0 <= descuento <= costo y costo > 0",
             int(((df.Discount_Amount < 0) | (df.Discount_Amount > df.Total_Trip_Cost) | (df.Total_Trip_Cost <= 0)).sum()), n, fallas)
    chequear("edad >= 18", int((df.Customer_Age < 18).sum()), n, fallas)

    completas = df[df.Booking_Status == "Completed"]
    canceladas = df[df.Booking_Status == "Cancelled"]
    otras = df[df.Booking_Status != "Completed"]
    chequear("Resenas (Completed) con puntajes entre 1 y 5",
             int((~completas.Customer_Rating.between(1, 5) | ~completas.Hotel_Rating.between(1, 5)).sum()), len(completas), fallas)
    chequear("Sin completar: rating 0 (se descarta)", int((otras.Customer_Rating != 0).sum()), len(otras), fallas)
    chequear("Canceladas con motivo", int((canceladas.Cancellation_Reason == "Not Applicable").sum()), len(canceladas), fallas)

    if fallas:
        print(f"\nFallaron {len(fallas)} chequeos, no se generan archivos.")
        return 1

    paises = sorted(set(ciudades.p))
    lista_ciudades = sorted({(r.c, r.p) for r in ciudades.itertuples()}, key=lambda x: (x[1], x[0]))
    ciudad_id = {cp: i for i, cp in enumerate(lista_ciudades, start=1)}
    hoteles = sorted({(r.Hotel_Name, r.Destination_City, r.Destination_Country) for r in df.itertuples()})
    hotel_id = {h[0]: i for i, h in enumerate(hoteles, start=1)}
    categoria = df.groupby("Hotel_Name").Hotel_Rating.mean().round(1).to_dict()

    def catalogo(col, excluir=()):
        return {v: i for i, v in enumerate(sorted(x for x in df[col].unique() if x not in excluir), start=1)}

    regimen = catalogo("Meal_Plan")
    medio_pago = catalogo("Payment_Method")
    transporte = catalogo("Transportation_Type")
    estado = catalogo("Booking_Status")
    clientes = df.drop_duplicates("Customer_ID")

    sql = [
        "-- ============================================================",
        "-- datos_iniciales.sql - generado por database/cargar_datos.py",
        "-- ============================================================",
        "SET NAMES utf8mb4;",
        "USE reservas_viajes;",
        "START TRANSACTION;",
        "",
        insert("pais", ["nombre", "region"], [[texto(p), texto(REGIONES[p])] for p in paises]),
        insert("ciudad", ["id", "nombre", "pais_nombre"],
               [[str(i), texto(c), texto(p)] for (c, p), i in ciudad_id.items()]),
        insert("hotel", ["id", "nombre", "categoria_base", "ciudad_id"],
               [[str(hotel_id[h]), texto(h), f"{categoria[h]:.1f}", str(ciudad_id[(c, p)])] for h, c, p in hoteles]),
    ]
    for tabla, cat in (("regimen", regimen), ("medio_pago", medio_pago),
                       ("transporte", transporte), ("estado_reserva", estado)):
        sql.append(insert(tabla, ["id", "descripcion"], [[str(i), texto(v)] for v, i in cat.items()]))
    sql.append(insert("cliente", ["customer_id", "nombre", "edad", "genero", "ciudad_id"],
                      [[texto(r.Customer_ID), texto(r.Customer_Name), str(r.Customer_Age), texto(r.Gender),
                        str(ciudad_id[(r.Customer_City, r.Customer_Country)])] for r in clientes.itertuples()]))
    sql.append(insert(
        "reserva",
        ["booking_id", "customer_id", "hotel_id", "booking_date", "travel_date", "return_date",
         "number_of_adults", "number_of_children", "number_of_rooms", "discount_amount", "total_trip_cost",
         "regimen_id", "medio_pago_id", "transporte_id", "estado_id", "activa"],
        [[str(r.Booking_ID), texto(r.Customer_ID), str(hotel_id[r.Hotel_Name]),
          texto(r.Booking_Date.date()), texto(r.Travel_Date.date()), texto(r.Return_Date.date()),
          str(r.Number_of_Adults), str(r.Number_of_Children), str(r.Number_of_Rooms),
          f"{r.Discount_Amount:.2f}", f"{r.Total_Trip_Cost:.2f}",
          str(regimen[r.Meal_Plan]), str(medio_pago[r.Payment_Method]), str(transporte[r.Transportation_Type]),
          str(estado[r.Booking_Status]), "TRUE"] for r in df.itertuples()]))
    sql.append(insert("cancelacion", ["booking_id", "motivo"],
                      [[str(r.Booking_ID), texto(r.Cancellation_Reason)] for r in canceladas.itertuples()]))
    sql.append(insert("resena", ["booking_id", "puntaje_viaje", "puntaje_hotel", "comentario"],
                      [[str(r.Booking_ID), f"{r.Customer_Rating:.1f}", f"{r.Hotel_Rating:.1f}",
                        texto(r.Customer_Review_Comment)] for r in completas.itertuples()]))
    sql.append("COMMIT;\n")

    out = Path(args.out)
    (out / "scripts_nosql").mkdir(parents=True, exist_ok=True)
    with open(out / "datos_iniciales.sql", "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(sql))

    reservas = []
    for r in df.itertuples():
        doc = {
            "_id": int(r.Booking_ID),
            "fechas": {"reserva": str(r.Booking_Date.date()), "salida": str(r.Travel_Date.date()),
                       "regreso": str(r.Return_Date.date())},
            "cliente": {"id": r.Customer_ID, "nombre": r.Customer_Name, "edad": int(r.Customer_Age),
                        "genero": r.Gender, "ciudad": r.Customer_City, "pais": r.Customer_Country},
            "hotel": {"id": hotel_id[r.Hotel_Name], "nombre": r.Hotel_Name, "categoriaBase": float(categoria[r.Hotel_Name]),
                      "ciudad": r.Destination_City, "pais": r.Destination_Country,
                      "region": REGIONES[r.Destination_Country]},
            "regimen": r.Meal_Plan,
            "medioPago": r.Payment_Method,
            "transporte": r.Transportation_Type,
            "estado": r.Booking_Status,
            "adultos": int(r.Number_of_Adults),
            "ninos": int(r.Number_of_Children),
            "habitaciones": int(r.Number_of_Rooms),
            "descuento": float(r.Discount_Amount),
            "costoTotal": float(r.Total_Trip_Cost),
            "activa": True,
        }
        if r.Booking_Status == "Cancelled":
            doc["cancelacion"] = {"motivo": r.Cancellation_Reason}
        reservas.append(doc)

    resenas = [{
        "reservaId": int(r.Booking_ID),
        "hotelId": hotel_id[r.Hotel_Name],
        "hotel": r.Hotel_Name,
        "destino": f"{r.Destination_City}, {r.Destination_Country}",
        "puntajeViaje": round(float(r.Customer_Rating), 1),
        "puntajeHotel": round(float(r.Hotel_Rating), 1),
        "comentario": r.Customer_Review_Comment,
        "etiquetas": [],
        "respuestaAgencia": None,
    } for r in completas.itertuples()]

    for nombre, docs in (("reservas.json", reservas), ("resenas.json", resenas)):
        with open(out / "scripts_nosql" / nombre, "w", encoding="utf-8", newline="\n") as f:
            json.dump(docs, f, ensure_ascii=False, indent=1)
            f.write("\n")

    print("\nFilas por tabla (MySQL):")
    for tabla, cant in (("pais", len(paises)), ("ciudad", len(ciudad_id)), ("hotel", len(hotel_id)),
                        ("cliente", len(clientes)), ("regimen", len(regimen)), ("medio_pago", len(medio_pago)),
                        ("transporte", len(transporte)), ("estado_reserva", len(estado)), ("reserva", n),
                        ("cancelacion", len(canceladas)), ("resena", len(completas)), ("resena_etiqueta", 0)):
        print(f"  {tabla:<16} {cant:>5}")
    print(f"\nDocumentos MongoDB: reservas={len(reservas)}  resenas={len(resenas)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
