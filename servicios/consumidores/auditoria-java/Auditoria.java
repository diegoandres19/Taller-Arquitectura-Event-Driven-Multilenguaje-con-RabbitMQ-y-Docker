import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public class Auditoria {

    static String variable(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isEmpty()) ? porDefecto : valor;
    }

    static void ejecutar() throws Exception {
        String cola = "cola.auditoria";
        String bindings = variable("BINDINGS", "#");

        ConnectionFactory fabrica = new ConnectionFactory();
        fabrica.setHost(variable("RABBITMQ_HOST", "rabbitmq"));
        fabrica.setUsername(variable("RABBITMQ_USER", "admin"));
        fabrica.setPassword(variable("RABBITMQ_PASS", "admin"));

        try (Connection conexion = fabrica.newConnection();
             Channel canal = conexion.createChannel()) {

            canal.exchangeDeclare("eventos", "topic", true);
            canal.queueDeclare(cola, true, false, false, null);
            for (String clave : bindings.split(",")) {
                canal.queueBind(cola, "eventos", clave.trim());
            }
            canal.basicQos(10);

            System.out.println("[auditoria] escuchando " + cola + " con bindings " + bindings);

            CountDownLatch cierre = new CountDownLatch(1);
            conexion.addShutdownListener(motivo -> cierre.countDown());

            DeliverCallback alRecibir = (etiqueta, entrega) -> {
                String cuerpo = new String(entrega.getBody(), StandardCharsets.UTF_8);
                System.out.println("[CONSUMIDOR auditoria - Java] recibi "
                        + entrega.getEnvelope().getRoutingKey() + ": " + cuerpo);
                canal.basicAck(entrega.getEnvelope().getDeliveryTag(), false);
            };
            canal.basicConsume(cola, false, alRecibir, etiqueta -> { });

            cierre.await();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            try {
                ejecutar();
            } catch (Exception e) {
                System.out.println("[auditoria] no hay conexion con RabbitMQ (" + e.getMessage() + "). Reintentando...");
                Thread.sleep(3000);
            }
        }
    }
}
