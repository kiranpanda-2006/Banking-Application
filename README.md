# Banking Application – Microservices Architecture

A **secure and scalable Banking Application** built using **Java, Spring Boot, and Microservices Architecture**.

The application is designed to demonstrate real-world backend development concepts such as **API Gateway, service-to-service communication, authentication, distributed transactions, Kafka messaging, Redis caching, MySQL persistence, email notifications, Docker containerization, and online payment integration using Razorpay**.
### Backend

* Java
* Spring Boot
* Spring Data JPA
* Spring Security
* REST APIs
* Maven

### Microservices & Communication

* Spring Cloud API Gateway
* OpenFeign
* Apache Kafka
* Service-to-Service Communication
* Distributed Microservices Architecture

### Database & Caching

* MySQL
* Redis
* Hibernate / JPA

### Authentication & Security

* Spring Security
* JWT Authentication
* Role-Based Access Control

### Payment

* Razorpay Payment Gateway

### Email

* Spring Boot Mail
* JavaMailSender
* Email Notifications

### DevOps & Deployment

* Docker
* Docker Compose
* Git & GitHub

---

# Architecture

The application follows a **Microservices Architecture**, where different business functionalities are separated into independent services.

```text
                         ┌──────────────────────┐
                         │      Frontend        │
                         │   Web / Mobile UI    │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │      API Gateway     │
                         │   Spring Cloud       │
                         └──────────┬───────────┘
                                    │
          ┌─────────────────────────┼─────────────────────────┐
          │                         │                         │
          ▼                         ▼                         ▼
 ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
 │ Auth Service    │       │ Account Service │       │ Transaction     │
 │                 │       │                 │       │ Service         │
 └─────────────────┘       └─────────────────┘       └─────────────────┘
          │                         │                         │
          │                         │                         │
          ▼                         ▼                         ▼
 ┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
 │     MySQL       │       │     MySQL       │       │     MySQL       │
 └─────────────────┘       └─────────────────┘       └─────────────────┘

                                    │
                                    ▼
                           ┌─────────────────┐
                           │     Redis       │
                           │     Cache       │
                           └─────────────────┘

                                    │
                                    ▼
                           ┌─────────────────┐
                           │  Apache Kafka   │
                           │ Event Messaging │
                           └────────┬────────┘
                                    │
                     ┌──────────────┴──────────────┐
                     ▼                             ▼
             ┌─────────────────┐          ┌─────────────────┐
             │ Notification    │          │ Payment Service │
             │ Service         │          │    Razorpay     │
             └────────┬────────┘          └─────────────────┘
                      │
                      ▼
             ┌─────────────────┐
             │ JavaMailSender  │
             │ Email Service   │
             └─────────────────┘
```

---

# Microservices

The project is divided into multiple independent services.

###  Authentication Service

Responsible for:

* User registration
* User login
* JWT generation
* Authentication
* Authorization
* Role-based access control
* Password security

---

### User / Customer Service

Responsible for managing customer information.

Features include:

* Customer registration
* Customer profile
* Customer information
* Account ownership
* Customer management

---

###  Account Service

Responsible for bank account operations.

Features include:

* Create bank account
* View account details
* Account balance
* Account status
* Account management

---

### Transaction Service

Responsible for financial transactions.

Features include:

* Money transfer
* Deposit
* Withdrawal
* Transaction history
* Transaction validation
* Transaction status

The Transaction Service communicates with other services using **OpenFeign** and asynchronous messaging through **Apache Kafka** where appropriate.

---

### Payment Service

Handles online payment functionality using **Razorpay Payment Gateway**.

Features include:

* Create payment order
* Payment processing
* Payment verification
* Payment status
* Payment-related events

> Razorpay credentials should be stored securely using environment variables or a secrets manager and should never be committed to GitHub.

---

### Notification Service

Responsible for sending notifications to customers.

Technology:

* Spring Boot Mail
* JavaMailSender
* Apache Kafka

Examples:

* Account creation email
* Transaction notification
* Payment confirmation
* Password-related notifications

---

###  API Gateway

The **API Gateway** acts as the single entry point for clients.

Responsibilities include:

* Routing requests
* Authentication
* Authorization
* Request filtering
* Service routing
* Centralized API access

Example:

```text
Client
   │
   ▼
API Gateway
   │
   ├── /pai/v1/account**
   ├── /pai/v1/payment**
   ├── /pai/v1/transaction**
```

---

#  Service-to-Service Communication

The application uses **OpenFeign** for synchronous communication between microservices.

Example:

```text
Transaction Service
        │
        │ OpenFeign
        ▼
Account Service
```

This allows one microservice to communicate with another through REST APIs without manually creating HTTP clients.

---

#  Apache Kafka

Apache Kafka is used for **asynchronous event-driven communication**.

Example transaction flow:

```text
Transaction Service
        │
        ▼
   Kafka Topic
        │
        ├──────────────► Notification Service
        │
        └──────────────► Other Consumers
```

For example, after a successful transaction:

```text
Transaction Completed
        ↓
Kafka Event Published
        ↓
Notification Service
        ↓
Email Sent
```

This helps reduce tight coupling between services and improves scalability.

---

#  Redis

Redis is used as a caching layer.

Possible use cases:

* Frequently accessed account information
* Session-related data
* Temporary data
* Frequently requested information
* Performance optimization

Example:

```text
Client
  │
  ▼
Account Service
  │
  ├── Redis → Cache Hit → Return Data
  │
  └── MySQL → Cache Miss → Fetch Data
                         ↓
                       Redis
```

---

#  MySQL

Each microservice can maintain its own database/schema depending on the service boundary.

Example:

```text
Auth Service        → MySQL
User Service        → MySQL
Account Service     → MySQL
Transaction Service → MySQL
Payment Service     → MySQL
```

This follows the **database-per-service** principle and helps maintain service independence.

---


#  Razorpay Integration

The application integrates **Razorpay** for online payments.

General flow:

```text
Customer
   ↓
Payment Request
   ↓
Payment Service
   ↓
Razorpay
   ↓
Payment Processing
   ↓
Payment Verification
   ↓
Kafka Event
   ↓
Notification Service
   ↓
Email Confirmation
```

---

#  Docker

The application is containerized using Docker.

The project can use Docker containers for:

* Microservices
* MySQL
* Redis
* Kafka
* Supporting infrastructure

Example:

```text
Docker Environment
│
├── API Gateway
├── Auth Service
├── User Service
├── Account Service
├── Transaction Service
├── Payment Service
├── Notification Service
├── MySQL
├── Redis
└── Kafka
```

Docker Compose can be used to start the required infrastructure together.

---

#  Project Structure

Example structure:

```text
Banking-Application/
│
├── api-gateway/
│
├── auth-service/
│
├── user-service/
│
├── account-service/
│
├── transaction-service/
│
├── payment-service/
│
├── notification-service/
│
├── docker-compose.yml
│
└── README.md
```

---

# ✨ Key Features

* 🔐 JWT-based authentication
* 👤 Customer management
* 🏦 Bank account management
* 💰 Money transactions
* 📜 Transaction history
* 💳 Razorpay payment integration
* 📧 Email notifications
* ⚡ Redis caching
* 📨 Kafka event-driven communication
* 🔗 OpenFeign service communication
* 🚪 Spring Cloud API Gateway
* 🗄️ MySQL database
* 🐳 Docker containerization
* 🔒 Role-based authorization
* 📱 RESTful APIs
* ☁️ Microservices architecture

---

# 🔄 Example Transaction Flow

A typical transaction can follow this architecture:

```text
                    Client
                      │
                      ▼
                API Gateway
                      │
                      ▼
              Transaction Service
                      │
                 OpenFeign
                      │
                      ▼
                Account Service
                      │
                      ▼
                    MySQL
                      │
                      ▼
              Transaction Success
                      │
                      ▼
                 Apache Kafka
                      │
             ┌────────┴────────┐
             ▼                 ▼
      Notification       Other Services
        Service
             │
             ▼
       JavaMailSender
             │
             ▼
       Customer Email
```

---

# 🧪 Testing

The APIs can be tested using tools such as:

* Postman
* Swagger / OpenAPI
* REST clients

Example API categories:

```text
Authentication
    POST /auth/register
    POST /auth/login

Accounts
    POST /accounts
    GET  /accounts/{id}

Transactions
    POST /transactions
    GET  /transactions/{id}

Payments
    POST /payments/create
    POST /payments/verify
```

> The exact endpoints may vary depending on the implementation of each microservice.

---

# ⚙️ Configuration

Sensitive configuration should **not** be committed to GitHub.

Use environment variables for values such as:

```text
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
REDIS_PASSWORD
RAZORPAY_KEY_ID
RAZORPAY_KEY_SECRET
MAIL_USERNAME
MAIL_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
```

Create your own local environment configuration rather than uploading credentials.

---

# 🚀 Running the Project

### Clone the repository

```bash
git clone https://github.com/kiranpanda-2006/Banking-Application.git
```

### Navigate into the project

```bash
cd Banking-Application
```

### Start infrastructure with Docker

```bash
docker compose up -d
```

### Build the services

```bash
mvn clean install
```

### Run the individual Spring Boot microservices

Start the required services according to their dependencies and configuration.

---

# 🌐 Future Improvements

Planned improvements may include:

* Service discovery with Eureka
* Centralized configuration using Spring Cloud Config
* Circuit breaker using Resilience4j
* Distributed tracing
* Prometheus monitoring
* Grafana dashboards
* CI/CD with GitHub Actions
* Kubernetes deployment
* Cloud deployment
* Improved fraud detection
* Advanced transaction monitoring

---

# 📌 Learning Objectives

This project demonstrates practical knowledge of:

* Java backend development
* Spring Boot
* Spring Security
* Microservices architecture
* REST API development
* API Gateway
* JWT authentication
* OpenFeign
* Apache Kafka
* Redis
* MySQL
* Docker
* Payment Gateway integration
* Email services
* Distributed system concepts

---

# 👨‍💻 Author

**Kiran Panda**

GitHub:
https://github.com/kiranpanda-2006

---

# ⭐ Support

If you find this project useful for learning **Java, Spring Boot, and Microservices**, consider giving the repository a ⭐ on GitHub.

---

## 📄 License

This project is created for educational and demonstration purposes.
