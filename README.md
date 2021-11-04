# gridcapa-core-valid
It's provide a full suite to perform CEP 70% validation process on CORE zone 

## Functional overview


## Environment

This application is collaborating with RabbitMQ message broker and MinIO object storage.

## Developer documentation

## Build application

Application is using Maven as base build framework. Application is simply built with following command.

```bash
mvn install
```

## Build docker image

For building Docker image of the application, start by building application.

```bash
mvn install
```

Then build docker image

```bash
docker build -t farao/gridcapa-core-valid .
```


### Running the application locally

For testing the application locally, it is first needed to start a RabbitMQ server.

The easier solution is to start a Docker container.
```bash
docker run --rm --hostname my-rabbit --name my-rabbit -p 5672:5672 -p15672:15672 rabbitmq:3-management
```
Previous command will start a Docker container running a basic RabbitMQ instance with management UI.

For validating that RabbitMQ has been correctly started, it is possible to connect to the management UI on following URL: http://localhost:15672/
Connect using default credentials (guest/guest).

![RabbitMQ management UI connection](./docs/assets/rabbitmq-connection.png)

When connected, management UI appears.

![RabbitMQ management UI overview](./docs/assets/rabbitmq-overview.png)

Then, it is needed to start a MinIO server.

Once again, easiest solution is to start a Docker container.
```bash
docker run --rm --hostname my-minio --name my-minio -p 9000:9000 minio/minio server /data
```
Previous command will start a Docker container running a basic MinIO instance with management UI.

For validating that MinIO has been correctly started, it is possible to connect to the management UI on following URL: http://localhost:9000/
Connect using default credentials (minioadmin/minioadmin).

![MinIO management UI connection](./docs/assets/minio-connection.png)

When connected, management UI appears.

![MinIO management UI overview](./docs/assets/minio-overview.png)

Finally, start the server using any IDE.

Core validation server responds to RabbitMQ message. Messages can be sent using RabbitMQ management UI.
Navigate to the AMQP Exchanges *core-valid-response* management page.

![Exchange management](./docs/assets/rabbitmq-exchange-management.png)

It is then possible to publish a message to that exchange using the dedicated UI.

![Exchange management - publish message](./docs/assets/rabbitmq-publish-message.png)
