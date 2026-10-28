import json, os, time
import pika

HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
USUARIO = os.getenv("RABBITMQ_USER", "admin")
CLAVE = os.getenv("RABBITMQ_PASS", "admin")
EXCHANGE = "eventos"
COLA = "cola.notificaciones"
BINDINGS = os.getenv("BINDINGS", "usuario.registrado,pago.procesado").split(",")

def al_recibir(canal, metodo, propiedades, cuerpo):
    evento = json.loads(cuerpo)
    print(f"[CONSUMIDOR notificaciones - Python] recibi {metodo.routing_key} "
          f"de {evento['productor']} ({evento['lenguaje']}) a las {evento['hora']}: {evento['datos']}",
          flush=True)
    canal.basic_ack(delivery_tag=metodo.delivery_tag)

def ejecutar():
    credenciales = pika.PlainCredentials(USUARIO, CLAVE)
    conexion = pika.BlockingConnection(
        pika.ConnectionParameters(host=HOST, credentials=credenciales, heartbeat=30))
    canal = conexion.channel()
    canal.exchange_declare(exchange=EXCHANGE, exchange_type="topic", durable=True)
    canal.queue_declare(queue=COLA, durable=True)
    for clave in BINDINGS:
        canal.queue_bind(queue=COLA, exchange=EXCHANGE, routing_key=clave.strip())
    canal.basic_qos(prefetch_count=10)
    canal.basic_consume(queue=COLA, on_message_callback=al_recibir)
    print(f"[notificaciones] escuchando {COLA} con bindings {BINDINGS}", flush=True)
    canal.start_consuming()

while True:
    try:
        ejecutar()
    except Exception as e:
        print(f"[notificaciones] no hay conexion con RabbitMQ en {HOST} ({e}). Reintentando...", flush=True)
        time.sleep(3)
