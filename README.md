# 🔔 Spring Boot Notification Service

A microservice-based notification system built with Spring Boot and RabbitMQ that demonstrates asynchronous order processing and multi-channel notification delivery.

## 📋 Table of Contents

- [🔔 Spring Boot Notification Service](#-spring-boot-notification-service)
  - [📋 Table of Contents](#-table-of-contents)
  - [🎯 Overview](#-overview)
  - [✨ Features](#-features)
  - [🛠 Technology Stack](#-technology-stack)
  - [🏗 Architecture](#-architecture)
  - [📋 Prerequisites](#-prerequisites)
  - [🚀 Installation](#-installation)
    - [1. Clone the Repository](#1-clone-the-repository)
    - [2. Install RabbitMQ](#2-install-rabbitmq)
    - [3. Build the Project](#3-build-the-project)
    - [4. Run the Application](#4-run-the-application)
  - [⚙ Configuration](#-configuration)
  - [📖 Usage](#-usage)
    - [Creating an Order](#creating-an-order)
  - [🔌 API Endpoints](#-api-endpoints)
  - [📁 Project Structure](#-project-structure)
  - [📸 Screenshots](#-screenshots)
  - [🔮 Future Enhancements](#-future-enhancements)
  - [🤝 Contributing](#-contributing)
  - [📝 License](#-license)
  - [👤 Author](#-author)
  - [🙏 Acknowledgments](#-acknowledgments)

## 🎯 Overview

This project implements an event-driven notification system where order creation triggers asynchronous notifications through multiple channels (Email and SMS). The system uses RabbitMQ as a message broker to decouple order processing from notification delivery, ensuring scalability and reliability.

## ✨ Features

- **RESTful API** for order creation
- **Event-Driven Architecture** using RabbitMQ
- **Asynchronous Processing** of notifications
- **Multi-Channel Notifications**:
  - Email notifications
  - SMS notifications
- **H2 In-Memory Database** for development and testing
- **JPA/Hibernate** for data persistence
- **Lombok** integration for cleaner code
- **Spring Boot 3.5.6** with Java 21

## 🛠 Technology Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Spring Boot | 3.5.6 | Application Framework |
| Java | 21 | Programming Language |
| Spring AMQP | Latest | RabbitMQ Integration |
| RabbitMQ | - | Message Broker |
| Spring Data JPA | Latest | Data Access Layer |
| H2 Database | Latest | In-Memory Database |
| Lombok | Latest | Code Generation |
| Maven | - | Build Tool |

## 🏗 Architecture

```
┌─────────────┐      ┌──────────────┐      ┌─────────────┐
│   Client    │─────▶│ OrderService │─────▶│  RabbitMQ   │
└─────────────┘      └──────────────┘      │    Queue    │
                            │               └──────┬──────┘
                            ▼                      │
                     ┌─────────────┐               │
                     │ H2 Database │               │
                     └─────────────┘               │
                                                   ▼
                                        ┌──────────────────────┐
                                        │ NotificationService  │
                                        └──────────────────────┘
                                                   │
                                    ┌──────────────┴──────────────┐
                                    ▼                             ▼
                            ┌──────────────┐            ┌──────────────┐
                            │    Email     │            │     SMS      │
                            └──────────────┘            └──────────────┘
```

**Flow:**
1. Client sends POST request to create an order
2. `OrderService` saves the order to H2 database
3. Order creation event is published to RabbitMQ queue
4. `NotificationService` listens to the queue and processes events asynchronously
5. Multi-channel notifications (Email & SMS) are sent to the user

## 📋 Prerequisites

Before running this application, ensure you have the following installed:

- **Java Development Kit (JDK)** 21 or higher
- **Maven** 3.6+
- **RabbitMQ** Server
- **Git**
- An IDE (IntelliJ IDEA, Eclipse, or VS Code with Java extensions)

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/SB-project.git
cd SB-project/notification
```

### 2. Install RabbitMQ

**Windows (using Chocolatey):**
```bash
choco install rabbitmq
```

**macOS (using Homebrew):**
```bash
brew install rabbitmq
brew services start rabbitmq
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install rabbitmq-server
sudo systemctl start rabbitmq-server
```

**Using Docker:**
```bash
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## ⚙ Configuration

Configuration is managed through `application.properties`:

```properties
# H2 Database Configuration
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driverClassName=org.h2.Driver
spring.h2.console.enabled=true
spring.jpa.show-sql=true

# RabbitMQ Configuration
spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest
```

**H2 Console Access:**
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Username: `sa`
- Password: (leave empty)

**RabbitMQ Management Console:**
- URL: `http://localhost:15672`
- Username: `guest`
- Password: `guest`

## 📖 Usage

### Creating an Order

Use any REST client (Postman, cURL, etc.) to create an order:

**Using cURL:**
```bash
curl -X POST "http://localhost:8080/api/orders?userId=123"
```

**Using Postman:**
- Method: `POST`
- URL: `http://localhost:8080/api/orders?userId=123`
- No body required

**Response:**
```json
{
  "id": 1,
  "userId": 123,
  "status": "CREATED",
  "createdAt": "2025-10-14T13:30:00"
}
```

**Console Output:**
```
Email sent to user 123 with message: Your order has been created!
SMS sent to user 123 with message: Your order has been created!
```

## 🔌 API Endpoints

| Method | Endpoint | Description | Parameters |
|--------|----------|-------------|------------|
| POST | `/api/orders` | Create a new order | `userId` (Long, required) |

## 📁 Project Structure

```
notification/
├── src/
│   ├── main/
│   │   ├── java/com/notification/notification/
│   │   │   ├── config/
│   │   │   │   └── RabbitMQConfig.java          # RabbitMQ configuration
│   │   │   ├── controller/
│   │   │   │   └── OrderController.java         # REST API endpoints
│   │   │   ├── entity/
│   │   │   │   └── Order.java                   # Order entity
│   │   │   ├── event/
│   │   │   │   └── OrderCreatedEvent.java       # Event object
│   │   │   ├── repository/
│   │   │   │   └── OrderRepository.java         # Data access layer
│   │   │   ├── service/
│   │   │   │   ├── OrderService.java            # Order business logic
│   │   │   │   └── NotificationService.java     # Notification logic
│   │   │   └── NotificationApplication.java     # Main application
│   │   └── resources/
│   │       └── application.properties            # Application configuration
│   └── test/
│       └── java/com/notification/notification/
│           └── NotificationApplicationTests.java
├── pom.xml                                       # Maven dependencies
└── README.md                                     # This file
```

## 📸 Screenshots

Project demonstration screenshots are available in the [`Images/`](./Images/) directory:

- **Scenario 1:** Basic order creation and notification
- **Scenario 2:** Order service response
- **Scenario 3:** Notification service processing
- **Scenario 4:** Complete flow with RabbitMQ

## 🔮 Future Enhancements

- [ ] Implement real email service integration (SendGrid, AWS SES)
- [ ] Implement real SMS service integration (Twilio, SNS)
- [ ] Add user management and authentication
- [ ] Implement notification preferences per user
- [ ] Add retry mechanism for failed notifications
- [ ] Support for additional notification channels (Push, WhatsApp)
- [ ] Add comprehensive unit and integration tests
- [ ] Implement monitoring and logging (ELK Stack)
- [ ] Add API documentation (Swagger/OpenAPI)
- [ ] Containerize the application (Docker)
- [ ] Deploy to cloud platform (AWS, Azure, GCP)
- [ ] Add notification templates
- [ ] Implement dead letter queue for failed messages

## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request



## 👤 Author

**Your Name**

- GitHub: https://github.com/hameda9795
- LinkedIn:https://linkedin.com/in/hameda9795

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- RabbitMQ for reliable message queuing
- The open-source community

---

⭐ If you found this project helpful, please give it a star!

**Built with ❤️ using Spring Boot**


