
# 💸 Inter-bank Fund Transfer (IBFT) Microservices Application

This is a learning-oriented IBFT (Inter-bank Fund Transfer) system built using Spring Boot Microservices. It simulates core banking operations across distributed services using RESTful communication and dependencies containerization via Docker.


## Tech Stack

* ☕ Java 24 
* 🍃 Spring Boot
* ☁️ Spring Cloud (Eureka, Gateway)
* 🐋 Docker & Docker Compose
* 🐘 PostgreSQL (Account, User) & 🌿 MongoDB (Payment)
* 💌 Kafka & 🗃️ Redis
* 🧰 Lombok, Spring Validation, Mapstruct, Flyway, etc

## Project Structure

Each microservice is located in its own folder with an independent `docker-compose.yml`

```
ibft-microservice/
│
├── account-service/
│   ├── src/
│   └── docker-compose.yml
│
├── auth-service/
│   ├── src/
│   └── docker-compose.yml
│
├── payment-service/
│   ├── src/
│   └── docker-compose.yml
│
├── service-discovery/
├── api-gateway/
└── ...
```
## Running the application

> ⚠️ IMPORTANT: You must run the `docker-compose.yml` file in each microservice folder and the `docker-compose.yml` in the root folder before starting the microservices.

#### Step-by-step:

##### 1. Clone the repository

```bash
git clone https://github.com/S0lux/IBFT-Microservice-Application
cd IBFT-Microservice-Applicatio
```

##### 2. Start service infrastructures

```bash
cd account-service
docker-compose up -d

cd ../auth-service
docker-compose up -d

cd ../payment-service
docker-compose up -d
```

##### 3. Start each microservice

##### 4. The gateway is exposed at port 8080

## [Diagram] Payment Flow

![Test](https://res.cloudinary.com/drzvajzd4/image/upload/v1749442751/readmes/pgjer04nfibune0k4nnx.png)
