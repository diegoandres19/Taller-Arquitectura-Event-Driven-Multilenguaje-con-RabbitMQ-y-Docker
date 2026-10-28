import json, os, random, time
from datetime import datetime
import pika

HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
USUARIO = os.getenv("RABBITMQ_USER", "admin")
CLAVE = os.getenv("RABBITMQ_PASS", "admin")
INTERVALO = float(os.getenv("INTERVALO", "3"))
EXCHANGE = "eventos"
CLAVE_RUTA = "pedido.creado"

def ejecutar():
    credenciales = pika.PlainCredentials(USUARIO, CLAVE)
    conexion = pika.BlockingConnection(
        pika.ConnectionParameters(host=HOST, credentials=credenciales, heartbeat=30))
    canal = conexion.channel()
    canal.exchange_declare(exchange=EXCHANGE, exchange_type="topic", durable=True)
    contador = 0
    while True:
        contador += 1
        evento = {
            "productor": "pedidos",
            "lenguaje": "Python",
            "mensaje": "Hola, soy el productor pedidos, hecho en Python",
            "hora": datetime.now().strftime("%H:%M:%S"),
            "datos": {"pedido_id": contador, "total": round(random.uniform(10, 500), 2)},
        }
        canal.basic_publish(exchange=EXCHANGE, routing_key=CLAVE_RUTA,
                            body=json.dumps(evento),
                            properties=pika.BasicProperties(delivery_mode=2))
        print(f"[PRODUCTOR pedidos - Python] envie {CLAVE_RUTA}: {evento['datos']}", flush=True)
        time.sleep(INTERVALO)

while True:
    try:
        ejecutar()
    except Exception as e:
        print(f"[pedidos] no hay conexion con RabbitMQ en {HOST} ({e}). Reintentando...", flush=True)
        time.sleep(3)
