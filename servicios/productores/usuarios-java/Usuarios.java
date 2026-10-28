import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

public class Usuarios {

    static String variable(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return (valor == null || valor.isEmpty()) ? porDefecto : valor;
    }

    static void ejecutar() throws Exception {
        ConnectionFactory fabrica = new ConnectionFactory();
        fabrica.setHost(variable("RABBITMQ_HOST", "rabbitmq"));
        fabrica.setUsername(variable("RABBITMQ_USER", "admin"));
        fabrica.setPassword(variable("RABBITMQ_PASS", "admin"));
        long intervalo = (long) (Double.parseDouble(variable("INTERVALO", "6")) * 1000);
        Random azar = new Random();

        try (Connection conexion = fabrica.newConnection();
             Channel canal = conexion.createChannel()) {

            canal.exchangeDeclare("eventos", "topic", true);

            int contador = 0;
            while (true) {
                contador++;
                String hora = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                String datos = "{\"usuario_id\":" + contador
                        + ",\"correo\":\"usuario" + azar.nextInt(999) + "@correo.com\"}";
                String evento = "{\"productor\":\"usuarios\","
                        + "\"lenguaje\":\"Java\","
                        + "\"mensaje\":\"Hola, soy el productor usuarios, hecho en Java\","
                        + "\"hora\":\"" + hora + "\","
                        + "\"datos\":" + datos + "}";

                canal.basicPublish("eventos", "usuario.registrado",
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        evento.getBytes(StandardCharsets.UTF_8));

                System.out.println("[PRODUCTOR usuarios - Java] envie usuario.registrado: " + datos);
                Thread.sleep(intervalo);
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        while (true) {
            try {
                ejecutar();
            } catch (Exception e) {
                System.out.println("[usuarios] no hay conexion con RabbitMQ (" + e.getMessage() + "). Reintentando...");
                Thread.sleep(3000);
            }
        }
    }
}
