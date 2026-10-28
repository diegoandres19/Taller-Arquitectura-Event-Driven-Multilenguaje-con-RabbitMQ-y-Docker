package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log"
	"math/rand"
	"os"
	"strconv"
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
	url := fmt.Sprintf("amqp://%s:%s@%s:5672/",
		variable("RABBITMQ_USER", "admin"),
		variable("RABBITMQ_PASS", "admin"),
		variable("RABBITMQ_HOST", "rabbitmq"))
	segundos, _ := strconv.ParseFloat(variable("INTERVALO", "5"), 64)

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

	contador := 0
	for {
		contador++
		evento := map[string]any{
			"productor": "inventario",
			"lenguaje":  "Go",
			"mensaje":   "Hola, soy el productor inventario, hecho en Go",
			"hora":      time.Now().Format("15:04:05"),
			"datos": map[string]any{
				"movimiento_id": contador,
				"sku":           fmt.Sprintf("SKU-%03d", rand.Intn(200)),
				"cantidad":      rand.Intn(40) - 20,
			},
		}
		cuerpo, _ := json.Marshal(evento)
		err := canal.PublishWithContext(context.Background(), "eventos", "inventario.actualizado",
			false, false, amqp.Publishing{
				ContentType:  "application/json",
				DeliveryMode: amqp.Persistent,
				Body:         cuerpo,
			})
		if err != nil {
			return err
		}
		log.Printf("[PRODUCTOR inventario - Go] envie inventario.actualizado: %v", evento["datos"])
		time.Sleep(time.Duration(segundos * float64(time.Second)))
	}
}

func main() {
	for {
		if err := ejecutar(); err != nil {
			log.Printf("[inventario] no hay conexion con RabbitMQ (%v). Reintentando...", err)
			time.Sleep(3 * time.Second)
		}
	}
}
