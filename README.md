
# Kafka streams based store

The implementation of microservice architecture using kafka as event-streaming platform.
Most of the business logic implemented by Kafka Streams stateful operations. All components
communicate via events in kafka topics.

Order service provides a REST API for creating and getting orders with CQRS style.
It has local kafka-streams based local store and two type or get queries: 
get current order state by id and long-pooling get order, which will wait until order 
will be validated by other services.
 
Orders are validated in parallel by 3 validation services: Fraud, Order details and Warehouse services. 
After validation has been completed Validations-aggregatos-service aggregate the result and decide
if order pass or not validation.

## Architecture

Architecture is described in the great book - Designing event-driven systems by Ben Stopford

System Architecture:
![alt text](./architecture.png "System Architecture")
[image source](https://cdn.confluent.io/wp-content/uploads/Screenshot-2017-11-09-12.34.26-1024x761.png)

## Services

- order-service
- fraud-service
- order-details-service
- order-validations-aggregate-service

## How to run

For local development run 
[LocalRunner](https://github.com/StepanovNickolay/kafka-streams-store-demo/blob/master/src/test/java/ru/step/store/LocalRunner.java)
 which will start kafka and kafka ui via testcontainers.

To simulate store customers run 
[ClientSimulation](https://github.com/StepanovNickolay/kafka-streams-store-demo/blob/master/src/main/java/ru/step/store/ClientSimulation.java)