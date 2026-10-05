# Banking Application

A microservices-based banking application built using Spring Boot. The application provides core banking functionality such as user registration, account creation, account details, balance checking, and transaction processing.

The project is designed to demonstrate a real-world distributed banking system using multiple Spring Boot services and supporting technologies.

## Technologies Used

- Java 21
- Spring Boot
- Spring Cloud
- Spring Cloud Gateway
- Spring Cloud OpenFeign
- Spring Data JPA
- Spring Security
- MySQL
- Apache Kafka
- Redis
- Thymeleaf
- Maven
- REST APIs

## Architecture

The application follows a microservices architecture.

```text
                    Client
                      |
                      v
              +---------------+
              |  API Gateway  |
              +---------------+
                 /     |     \
                /      |      \
               v       v       v
        +---------+ +---------+ +----------------+
        |  User   | | Account | |  Transaction   |
        | Service | | Service | |    Service     |
        +---------+ +---------+ +----------------+
                         |
                         v
                  +-------------+
                  |    MySQL    |
                  +-------------+

              Supporting Services
              -------------------
              Kafka
              Redis
```

## Services

### API Gateway

The API Gateway acts as the entry point for the banking application.

Responsibilities:

- Handle client requests
- Route requests to appropriate microservices
- Provide the web interface using Thymeleaf
- Communicate with backend services using OpenFeign
- Handle authentication and authorization

### User Service

Responsible for managing users.

Responsibilities:

- User registration
- User login
- User information
- Password management
- User authentication

### Account Service

Responsible for managing bank accounts.

Responsibilities:

- Create bank accounts
- Retrieve account details
- Check account balance
- Manage account types
- Maintain account status

Supported account types:

```text
SAVINGS
CURRENT
FIXED_DEPOSIT
```

### Transaction Service

Responsible for handling banking transactions.

Responsibilities:

- Initiate transactions
- Validate transaction amount
- Validate sender account
- Validate receiver account
- Process transactions
- Publish transaction events through Kafka

### Fraud Detection Service

The Fraud Detection Service analyzes transactions and identifies suspicious activities.

Responsibilities:

- Monitor transactions
- Check transaction limits
- Detect suspicious transaction amounts
- Validate transaction against account balance
- Publish fraud verification events

Redis is used for maintaining temporary transaction-related information.

## API Gateway Endpoints

### Account

Open account creation page:

```text
GET /account/create
```

Open account details page:

```text
GET /account/details
```

Search account by account number:

```text
GET /account/get/details?accountNumber=67838437846
```

Check account balance:

```text
GET /account/check-balance?accountNumber=67838437846
```

## Account Service APIs

### Get Account Balance

```text
GET /api/v1/account/{accountNumber}/balance
```

Example:

```text
GET /api/v1/account/67838437846/balance
```

### Account Number

Account numbers are treated as identifiers and are represented as `String` values.

Example:

```text
67838437846
```

This avoids issues with leading zeros and prevents account numbers from being treated as arithmetic values.

## Account Creation Flow

The account creation flow works through the API Gateway and Account Service.

```text
User
 |
 | Submit account form
 v
API Gateway
 |
 | Feign Client
 v
Account Service
 |
 | Save Account
 v
MySQL
 |
 | Account Response
 v
API Gateway
 |
 v
Success Page
```

## Account Details Flow

```text
User
 |
 | Enter Account Number
 v
API Gateway
 |
 | GET /account/get/details
 v
Account Service
 |
 | Find account
 v
MySQL
 |
 | AccountResponseDto
 v
API Gateway
 |
 v
Account Details Page
```

## Balance Checking Flow

The user enters an account number through the Gateway.

```text
GET /account/check-balance?accountNumber=67838437846
```

The Gateway communicates with the Account Service using Feign.

```text
GET /api/v1/account/67838437846/balance
```

The Account Service retrieves the balance and returns it to the Gateway.

```text
Account Service
       |
       v
     MySQL
       |
       v
   BigDecimal
       |
       v
 API Gateway
       |
       v
 Thymeleaf Page
```

## Transaction Flow

Transactions are processed using the Transaction Service.

```text
Client
  |
  v
API Gateway
  |
  v
Transaction Service
  |
  +----> Account Service
  |
  +----> Kafka
           |
           v
    Fraud Detection Service
           |
           +----> Redis
           |
           v
     Fraud Result
```

## Kafka

Kafka is used for asynchronous communication between services.

Example transaction topic:

```text
transaction.initiated
```

Fraud-related topics include:

```text
verification.required
fraud.check.clean
```

Kafka allows transaction processing and fraud detection to work independently.

## Redis

Redis is used by the Fraud Detection Service for temporary transaction-related data and fraud detection checks.

Example configuration:

```text
Redis Host: localhost
Redis Port: 6379
```

## Database

The application uses MySQL for persistent data storage.

Each service can maintain its own database depending on the service boundary.

Example databases:

```text
user_db
account_db
transaction_db
```

The Fraud Detection Service uses:

```text
transaction_db
```

## Project Structure

A simplified project structure:

```text
BankingSystem
|
+-- api-gateway
|   +-- controller
|   +-- client
|   +-- dto
|   +-- service
|   +-- templates
|
+-- account-service
|   +-- controller
|   +-- service
|   +-- repository
|   +-- entity
|   +-- dto
|
+-- transaction-service
|   +-- controller
|   +-- service
|   +-- repository
|   +-- client
|   +-- dto
|
+-- fraud-detection-service
|   +-- consumer
|   +-- service
|   +-- configuration
|
+-- user-service
|   +-- controller
|   +-- service
|   +-- repository
|   +-- entity
|   +-- dto
|
+-- README.md
```

## Communication Between Services

The project uses Spring Cloud OpenFeign for synchronous communication between services.

Example:

```java
@FeignClient(
    name = "account-service",
    url = "${account.service.url}"
)
public interface AccountserviceClient {

    @GetMapping("/api/v1/account/{accountNumber}/balance")
    BigDecimal getAccountBalance(
            @PathVariable("accountNumber") String accountNumber);
}
```

## Validation

The application performs validation at different service boundaries.

Examples:

- Account number validation
- Transaction amount validation
- User registration validation
- Password confirmation
- Account existence validation
- Sender account validation
- Receiver account validation
- Balance validation

Transaction amounts must be greater than zero.

```java
if (amount.compareTo(BigDecimal.ZERO) <= 0) {
    throw new IllegalArgumentException("Amount must be greater than zero");
}
```

## Security

Spring Security is used to secure application endpoints.

Planned security responsibilities include:

- User authentication
- User authorization
- Role-based access
- Secure account access
- Transaction authorization
- JWT-based authentication

Account ownership can also be validated so that a user can access only their own account.

The Account Service maintains a unique relationship between a user and an account.

```text
One User -> One Account
```

## Running the Application

### Prerequisites

Install the following:

```text
Java 21
Maven
MySQL
Redis
Apache Kafka
```

Verify Java:

```bash
java -version
```

Verify Maven:

```bash
mvn -version
```

### Build

From each service directory:

```bash
mvn clean install
```

### Run

Start the services individually.

Example:

```bash
mvn spring-boot:run
```

The services should be started in an appropriate order based on their dependencies.

## Configuration

Service-specific configuration is maintained in `application.properties` or `application.yml`.

Example Account Service URL:

```properties
account.service.url=http://localhost:8081
```

Example Redis configuration:

```properties
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

Example Kafka configuration:

```properties
spring.kafka.bootstrap-servers=localhost:9092
```

## Development Goals

This project is being developed as a real-world Spring Boot microservices project to demonstrate:

- Microservices architecture
- REST API development
- Spring Cloud Gateway
- OpenFeign communication
- Spring Security
- Database design
- Kafka event-driven communication
- Redis
- Transaction processing
- Fraud detection
- Exception handling
- Validation
- Thymeleaf-based frontend
- Service-to-service communication

## Future Improvements

Planned improvements include:

- JWT authentication
- Role-based authorization
- User-account ownership validation
- Transaction history
- Money transfer
- Deposit and withdrawal
- Kafka-based transaction processing
- Advanced fraud detection
- Centralized exception handling
- API documentation using Swagger/OpenAPI
- Docker containerization
- Service discovery
- Centralized configuration
- Monitoring and logging

## Author

Kiran Sekhar Panda
