const amqp = require('amqplib');

const HOST = process.env.RABBITMQ_HOST || 'rabbitmq';
const USUARIO = process.env.RABBITMQ_USER || 'admin';
const CLAVE = process.env.RABBITMQ_PASS || 'admin';
const EXCHANGE = 'eventos';
const COLA = 'cola.facturacion';
const BINDINGS = (process.env.BINDINGS || 'pedido.*,pago.*').split(',').map((s) => s.trim());

const esperar = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function ejecutar() {
  const conexion = await amqp.connect(`amqp://${USUARIO}:${CLAVE}@${HOST}`);
  const canal = await conexion.createChannel();

  await canal.assertExchange(EXCHANGE, 'topic', { durable: true });
  await canal.assertQueue(COLA, { durable: true });
  for (const clave of BINDINGS) {
    await canal.bindQueue(COLA, EXCHANGE, clave);
  }
  canal.prefetch(10);

  console.log(`[facturacion] escuchando ${COLA} con bindings ${BINDINGS}`);

  await canal.consume(COLA, (mensaje) => {
    if (!mensaje) return;
    const evento = JSON.parse(mensaje.content.toString());
    console.log(
      `[CONSUMIDOR facturacion - Node.js] recibi ${mensaje.fields.routingKey} ` +
      `de ${evento.productor} (${evento.lenguaje}) a las ${evento.hora}:`,
      JSON.stringify(evento.datos)
    );
    canal.ack(mensaje);
  });

  await new Promise((resolve) => {
    conexion.on('close', resolve);
    conexion.on('error', () => {});
  });
}

(async () => {
  while (true) {
    try {
      await ejecutar();
    } catch (e) {
      console.log(`[facturacion] no hay conexion con RabbitMQ en ${HOST} (${e.message}). Reintentando...`);
    }
    await esperar(3000);
  }
})();
