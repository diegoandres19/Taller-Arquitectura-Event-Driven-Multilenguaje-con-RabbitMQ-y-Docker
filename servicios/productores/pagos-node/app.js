const amqp = require('amqplib');

const HOST = process.env.RABBITMQ_HOST || 'rabbitmq';
const USUARIO = process.env.RABBITMQ_USER || 'admin';
const CLAVE = process.env.RABBITMQ_PASS || 'admin';
const INTERVALO = parseFloat(process.env.INTERVALO || '4') * 1000;
const EXCHANGE = 'eventos';
const CLAVE_RUTA = 'pago.procesado';

const esperar = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

async function ejecutar() {
  const conexion = await amqp.connect(`amqp://${USUARIO}:${CLAVE}@${HOST}`);
  let abierta = true;
  conexion.on('close', () => (abierta = false));
  conexion.on('error', () => (abierta = false));

  const canal = await conexion.createChannel();
  await canal.assertExchange(EXCHANGE, 'topic', { durable: true });

  let contador = 0;
  while (abierta) {
    contador += 1;
    const evento = {
      productor: 'pagos',
      lenguaje: 'Node.js',
      mensaje: 'Hola, soy el productor pagos, hecho en Node.js',
      hora: new Date().toTimeString().slice(0, 8),
      datos: {
        pago_id: contador,
        monto: +(Math.random() * 400 + 5).toFixed(2),
        metodo: ['tarjeta', 'pse', 'efectivo'][Math.floor(Math.random() * 3)],
      },
    };
    canal.publish(EXCHANGE, CLAVE_RUTA, Buffer.from(JSON.stringify(evento)), { persistent: true });
    console.log(`[PRODUCTOR pagos - Node.js] envie ${CLAVE_RUTA}:`, JSON.stringify(evento.datos));
    await esperar(INTERVALO);
  }
}

(async () => {
  while (true) {
    try {
      await ejecutar();
    } catch (e) {
      console.log(`[pagos] no hay conexion con RabbitMQ en ${HOST} (${e.message}). Reintentando...`);
    }
    await esperar(3000);
  }
})();
