FROM golang:1.22-alpine AS compilar
WORKDIR /src
COPY main.go .
RUN go mod init inventario && go get github.com/rabbitmq/amqp091-go@v1.10.0 && CGO_ENABLED=0 go build -o /app .

FROM alpine:3.20
COPY --from=compilar /app /app
CMD ["/app"]
