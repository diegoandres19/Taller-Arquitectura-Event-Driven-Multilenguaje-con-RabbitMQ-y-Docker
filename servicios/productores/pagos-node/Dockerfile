FROM node:20-alpine
WORKDIR /app
RUN npm install amqplib@0.10.4
COPY app.js .
CMD ["node", "app.js"]
