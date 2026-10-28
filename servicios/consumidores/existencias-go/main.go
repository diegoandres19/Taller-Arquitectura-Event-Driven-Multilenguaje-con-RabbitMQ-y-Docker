package main

import (
	"encoding/json"
	"fmt"
	"log"
	"os"
	"strings"
	"time"

	amqp "github.com/rabbitmq/amqp091-go"
)

func variable(nombre, porDefecto string) string {
	if valor := os.Getenv(nombre); valor != "" {
		return valor
	}
	return porDefecto
}

func ejecutar() error {
	cola := "cola.existencias"
	bindings := variable("BINDINGS", "pedido.creado,inventario.*")
	url := fmt.Sprintf("amqp://%s:%s@%s:5672/",
		variable("RABBITMQ_USER", "admin"),
		variable("RABBITMQ_PASS", "admin"),
		variable("RABBITMQ_HOST", "rabbitmq"))

	conexion, err := amqp.Dial(url)
	if err != nil {
		return err
	}
	defer conexion.Close()

	canal, err := conexion.Channel()
	if err != nil {
		return err
	}
	if err := canal.ExchangeDeclare("eventos", "topic", true, false, false, false, nil); err != nil {
		return err
	}
	if _, err := canal.QueueDeclare(cola, true, false, false, false, nil); err != nil {
		return err
	}
	for _, clave := range strings.Split(bindings, ",") {
		if err := canal.QueueBind(cola, strings.TrimSpace(clave), "eventos", false, nil); err != nil {
			return err
		}
	}
	canal.Qos(10, 0, false)

	mensajes, err := canal.Consume(cola, "", false, false, false, false, nil)
	if err != nil {
		return err
	}
	log.Printf("[existencias] escuchando %s con bindings %s", cola, bindings)

	for m := range mensajes {
		var evento map[string]any
		json.Unmarshal(m.Body, &evento)
		log.Printf("[CONSUMIDOR existencias - Go] recibi %s de %v (%v) a las %v: %v",
			m.RoutingKey, evento["productor"], evento["lenguaje"], evento["hora"], evento["datos"])
		m.Ack(false)
	}
	return fmt.Errorf("el canal se cerro")
}

func main() {
	for {
		if err := ejecutar(); err != nil {
			log.Printf("[existencias] no hay conexion con RabbitMQ (%v). Reintentando...", err)
			time.Sleep(3 * time.Second)
		}
	}
}
