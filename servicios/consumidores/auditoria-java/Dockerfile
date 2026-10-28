FROM eclipse-temurin:17-jdk AS compilar
WORKDIR /src
RUN mkdir lib && \
    curl -fsSL -o lib/amqp-client.jar https://repo1.maven.org/maven2/com/rabbitmq/amqp-client/5.21.0/amqp-client-5.21.0.jar && \
    curl -fsSL -o lib/slf4j-api.jar https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar && \
    curl -fsSL -o lib/slf4j-simple.jar https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar
COPY Auditoria.java .
RUN mkdir out && javac -cp "lib/*" -d out Auditoria.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=compilar /src/lib ./lib
COPY --from=compilar /src/out ./out
CMD ["java", "-cp", "out:lib/*", "Auditoria"]
