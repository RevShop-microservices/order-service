# Order Service

The **Order Service** is a Spring Boot microservice responsible for the full lifecycle of order management in the e-commerce platform. It handles checkout flows, Razorpay payment processing, order status management, return workflows, coupon validation, PDF invoice generation, and seller payout calculations.

---

## Features

- **Checkout & Order Creation**: Supports both cart-based checkout and single-item "Buy Now" flows.
- **Razorpay Payment Integration**: Creates Razorpay payment orders and verifies webhook signatures for secure payment confirmation.
- **Order Status Management**: Tracks orders through full lifecycle states (`PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`, `RETURNED`).
- **Cancellations & Returns**: Granular item-level cancellation and full-order return request/processing workflows.
- **Coupon Management**: Admin-managed coupon CRUD with code validation and usage-limit enforcement.
- **Seller Payouts**: Calculates and tracks commission-based seller payouts with PENDING/PAID lifecycle management.
- **PDF Invoice Generation**: Generates downloadable PDF invoices for orders using OpenPDF.
- **Asynchronous Messaging**: Publishes and consumes order-related events via **RabbitMQ** (e.g., stock deduction notifications to Product Service).
- **Cross-Service Communication**: Uses **OpenFeign** clients to call the Product Service for stock operations.
- **JWT Security**: All endpoints are secured with stateless JWT-based authentication forwarded from the API Gateway.
- **Distributed Tracing**: Integrated with Zipkin via Micrometer Brave for end-to-end request tracing.
- **OpenAPI Docs**: Swagger UI available via SpringDoc at `http://localhost:8083/swagger-ui.html`.

---

## Tech Stack

| Category | Technology |
| :--- | :--- |
| **Core** | Spring Boot 3.3.2, Java 17 |
| **Data - Relational** | Spring Data JPA, Hibernate, MySQL 8 |
| **Data - Document** | Spring Data MongoDB |
| **Payment** | Razorpay Java SDK 1.4.3 |
| **Messaging** | Spring AMQP, RabbitMQ |
| **Service Discovery** | Spring Cloud Netflix Eureka Client |
| **Centralized Config** | Spring Cloud Config Client |
| **HTTP Client** | Spring Cloud OpenFeign |
| **Security** | Spring Security, JJWT 0.11.5 |
| **PDF Generation** | OpenPDF (librepdf) 1.3.30 |
| **Tracing** | Zipkin, Micrometer Brave |
| **API Docs** | SpringDoc OpenAPI 2.5.0 |
| **Build & Coverage** | Maven, Jacoco, SonarQube |

---

## Configuration

The service runs on port **`8083`** and fetches its full configuration from the centralized Spring Cloud Config Server.

### Key Properties (`order-service.properties` in Config Server):

| Property | Default Value | Description |
| :--- | :--- | :--- |
| `spring.datasource.url` | `jdbc:mysql://localhost:3307/EcommerceDB` | MySQL database URL |
| `spring.datasource.username` | `root` | MySQL username |
| `spring.datasource.password` | `root123` | MySQL password |
| `spring.data.mongodb.uri` | Atlas connection string | MongoDB URI |
| `spring.rabbitmq.host` | `localhost` | RabbitMQ host |
| `spring.rabbitmq.port` | `5672` | RabbitMQ port |
| `spring.rabbitmq.username` | `guest` | RabbitMQ username |
| `spring.rabbitmq.password` | `guest` | RabbitMQ password |
| `razorpay.key` | `rzp_test_xxxxxxxxx` | Razorpay API Key |
| `razorpay.secret` | `xxxxxxxxxxxxxxxxxx` | Razorpay API Secret |

### Environment Variables (Docker / K8s override):

```
SPRING_DATASOURCE_URL
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
MONGO_URI
SPRING_RABBITMQ_HOST
SPRING_RABBITMQ_PORT
SPRING_RABBITMQ_USERNAME
SPRING_RABBITMQ_PASSWORD
RAZORPAY_KEY
RAZORPAY_SECRET
EUREKA_SERVER_URL
CONFIG_SERVER_URL
```

---

## REST API Documentation

### 1. Order Endpoints (`/api/orders`)

| Method | Path | Description |
| :--- | :--- | :--- |
| **POST** | `/api/orders/checkout` | Place an order from the user's cart. |
| **POST** | `/api/orders/buy-now` | Instantly place a single-item "Buy Now" order. |
| **GET** | `/api/orders/user` | Retrieve all orders belonging to the authenticated user. |
| **GET** | `/api/orders/check-purchase` | Check if a user has purchased a specific product (for review eligibility). |
| **GET** | `/api/orders/{orderId}` | Retrieve a specific order by its numeric ID. |
| **PUT** | `/api/orders/{orderId}/cancel` | Cancel an entire order. |
| **PUT** | `/api/orders/{orderId}/cancel-item` | Cancel a specific item within an order. |
| **POST** | `/api/orders/{orderId}/return` | Submit a return request for a delivered order. |
| **POST** | `/api/orders/{orderId}/process-return` | Process and approve/reject a return request (Admin). |

### 2. Payment Endpoints (`/api/payment`)

| Method | Path | Description |
| :--- | :--- | :--- |
| **POST** | `/api/payment/create-order` | Create a Razorpay payment order and receive the `order_id`. |
| **POST** | `/api/payment/verify` | Verify Razorpay payment signature and confirm the order. |

### 3. Coupon Endpoints (`/api/coupons`)

| Method | Path | Description |
| :--- | :--- | :--- |
| **POST** | `/api/coupons` | Create a new discount coupon (Admin only). |
| **GET** | `/api/coupons/validate` | Validate a coupon code and return discount details. |
| **GET** | `/api/coupons` | List all available coupons. |
| **DELETE** | `/api/coupons/{id}` | Delete a coupon by ID (Admin only). |

### 4. Payout Endpoints (`/api/payouts`)

| Method | Path | Description |
| :--- | :--- | :--- |
| **POST** | `/api/payouts/generate` | Generate payout records for all sellers for a given period (Admin). |
| **PUT** | `/api/payouts/{payoutId}/mark-paid` | Mark a payout as paid (Admin). |
| **GET** | `/api/payouts/pending` | Retrieve all payouts with `PENDING` status (Admin). |
| **GET** | `/api/payouts/seller` | Retrieve payouts for the authenticated seller. |
| **GET** | `/api/payouts` | List all payouts (Admin). |
| **GET** | `/api/payouts/commission-rate` | Get the current platform commission rate. |

---

## Data Models

| Table / Collection | Database | Description |
| :--- | :--- | :--- |
| `orders1` | MySQL | Core order records with status, totals, and address. |
| `order_items1` | MySQL | Line items for each order (product, qty, price, seller). |
| `payments` | MySQL | Razorpay payment transaction records. |
| `coupons` | MySQL | Discount coupon codes and usage rules. |
| `payouts` | MySQL | Seller payout records with commission calculations. |
| `products` | MongoDB | Product catalog read-replica (used for product lookups). |

---

## Build, Test, and Run

### 1. Compile and Package
```bash
./mvnw clean package
```
> Tests are skipped by default (`skipTests=true` in `pom.xml`).

### 2. Run Tests
```bash
./mvnw test -DskipTests=false
```

### 3. Run Locally
```bash
./mvnw spring-boot:run
```
> Ensure the following services are up before starting: Config Server (`8888`), Eureka Server (`8761`), MySQL (`3307`), MongoDB (`27017`), and RabbitMQ (`5672`).

### 4. Build Docker Image
```bash
docker build -t order-service .
```
> The `Dockerfile` uses `eclipse-temurin:17-jre-alpine` and copies the pre-built JAR from `target/`.

### 5. Run via Docker Compose
```bash
docker compose up order-service
```
> Refer to the root `docker-compose.yml` for full environment variable overrides.

---

## CI/CD

The GitHub Actions workflow at [`.github/workflows/ci-cd.yml`](.github/workflows/ci-cd.yml) automatically:
1. Checks out the code.
2. Sets up JDK 17 (Temurin) with Maven dependency caching.
3. Builds and tests the service with `./mvnw clean package -DskipTests=false`.
4. Builds and pushes the Docker image to **GitHub Container Registry (GHCR)** on every push to `main`, `master`, or `develop`.
