![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Railway](https://img.shields.io/badge/Deploy-Railway-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/CI%2FCD-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)![Swagger](https://img.shields.io/badge/API-Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
# 🛒 Inventory & Order API  
===================================

### Production-ready Spring Boot REST API for inventory management, order processing, and secure online payments.

## 🚀 Live Demo

- 🌐 Live API (Swagger): https://inventory-order-api-production.up.railway.app/docs
- 📬 Postman Collection: [Download Collection](./docs/Inventory-Order-API.postman_collection.json)
  
## Demo Account

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@orderapi.com | admin123456 |

> The default admin account is automatically seeded during application startup.

# 📑 Table of Contents

- [📖 About](#about)

- [✨ Features](#features)

- [💻 Tech Stack](#tech-stack)

- [❓ Why These Technologies?](#why-these-technologies)

- [🏗️ Project Structure](#️project-structure)

- [🧪 Testing Strategy](#testing-strategy)

- [☁️ Deployment](#️deployment)

- [📊 System Diagrams](#system-diagrams)

- [🖼️ Screenshots](#️screenshots--previews)

- [🚀 Getting Started](#getting-started)

- [⚙️ Configuration](#️configuration)

- [🔄 CI/CD](#cicd)

- [🤝 Contributing](#contributing)

- [📄 License](#license)

- [👤 Author](#author)

  
<a id="about"></a>
## 📖 About

#### Inventory Order API is a production-oriented backend application built with Java and Spring Boot.

#### The project is fully dockerized and deployed on Railway with managed MySQL and Redis services.


#### The project follows a Modular Monolith (MVC) architecture with a Feature-Based Package Structure, focusing on building production-ready backend systems rather than simple CRUD applications.

It covers:

- Concurrent inventory management
- Secure authentication & authorization
- Payment processing (Paymob)
- Distributed caching & IP Rate Limiting
- Background job synchronization
- Dockerized deployment on Railway
- Automated testing and CI/CD

<a id="features"></a>
## ✨ Features

* **Authentication & Authorization:** JWT Authentication (Access & Refresh Tokens), OAuth2 Login (Google/GitHub), and Role-Based Access Control (RBAC).

* **Order & Inventory Management:** Shopping cart, order lifecycle, stock validation, and concurrency handling using Optimistic & Pessimistic Locking.

* **Payment Integration:** Secure Paymob integration with Card, Wallet, Kiosk, HMAC verification, Idempotent webhooks, and Refund support.

* **Caching & Performance:** Redis-based feature caching, MySQL indexing (Full-Text & Composite Indexes), Pagination, Projection, and JPA Specifications.

* **API Protection:** IP Rate Limiting using Redis (Fixed Window Counter strategy).

* **Media Management:** Product image upload, validation, and cloud storage with Cloudinary.

* **Background Jobs:** Automated scheduled tasks synchronized across multiple instances using Spring Scheduler and ShedLock.

* **Observability:** Centralized Log4j2 configuration, structured logging, Spring AOP execution time tracking, and Spring Data Auditing.
  
* **API Design:** DTO Pattern, Unified API Responses, Global Exception Handling, and Localization (i18n).

* **Testing:** 260+ Unit, Controller, and Repository tests using JUnit 5, Mockito, MockMvc, and Testcontainers (Real MySQL).

* **Developer Experience:** Automatic database migrations with Flyway and startup data seeding using CommandLineRunner.
  
<a id="tech-stack"></a>
## 💻 Tech Stack

* **Backend:** Java 21, Spring Boot 4, Spring MVC, Spring Data JPA (Hibernate)

* **Database:** MySQL, Flyway

* **Caching & Rate Limiting:** Redis

* **Security:** Spring Security, JWT, OAuth2

* **Payments & Storage:** Paymob, Cloudinary

* **Testing:** JUnit 5, Mockito, MockMvc, Testcontainers

* **Documentation:** SpringDoc OpenAPI (Swagger UI)

* **Developer Tools:** MapStruct, Log4j2, Spring AOP

* **DevOps:** Docker, Docker Compose, GitHub Actions (CI/CD), Railway


<a id="why"></a>
## ❓ Why These Technologies?

### Why Redis for Rate Limiting?

Redis provides atomic increment operations with automatic key expiration, making it ideal for implementing efficient IP-based rate limiting.

### Why Feature-Based Caching?

Different resources have different read/write patterns, so each feature uses a caching strategy and TTL that best fits its usage.

### Why Pessimistic Locking?

To guarantee that the same stock cannot be reserved by multiple concurrent orders during checkout.

### Why Optimistic Locking?

Product updates are read-heavy and write-light, reducing unnecessary database locks while preventing conflicting updates.

### Why ShedLock?

To ensure scheduled jobs execute only once across multiple application instances.

### Why Flyway?

To keep database schema changes versioned, repeatable, and consistent across all environments.

### Why Testcontainers?

To validate repository behavior against a real MySQL instance instead of relying solely on in-memory databases.

### Why MapStruct?

To generate type-safe object mappings at compile time and eliminate repetitive mapping code.

### Why DTO Pattern?

To decouple the API contract from the persistence layer and avoid exposing internal entities.


<a id="testing"></a>
## 🧪 Testing Strategy

The project contains **260+ automated test cases**, with each layer tested independently using the most appropriate approach:

* **Service Layer:** Mockito-based unit tests with mocked dependencies.
* **Controller Layer:** MockMvc tests for REST endpoints without starting the server.
* **Repository Layer:** Testcontainers running a real MySQL instance inside Docker.

This testing strategy ensures business logic, REST APIs, and database queries are validated independently while keeping tests fast and reliable.

<a id="deployment"></a>
## ☁️ Deployment

The application is containerized with Docker and deployed on **Railway**.

The production environment includes:

* Spring Boot Application
* Managed MySQL Database
* Managed Redis Instance

Database schema is managed automatically using **Flyway** migrations.

Every push to the `main` branch triggers a GitHub Actions pipeline that:

1. Runs the automated test suite.
2. Builds a new Docker image.
3. Pushes the image to Docker Hub.
4. Deploys the latest version to Railway.

    
<a id="project-structure"></a>
## 🗂️ Project Structure
---------------------

The project follows a Feature-based modules architecture to maintain separation of concerns:

```  
src/
├── main/
│   ├── java/com/global/order_api/
│   │   ├── InventoryOrderApiApplication.java  # Root entry point
│   │   ├── core/                              # Core configurations & utilities
│   │   │   ├── annotation/                    # Custom annotations (e.g. @TrackExecutionTime)
│   │   │   ├── aspect/                        # Aspects for AOP monitoring
│   │   │   ├── base/                          # Base generic entities/services/repos
│   │   │   ├── config/                        # Configuration beans (Redis, ShedLock, Cloudinary, OpenApi)
│   │   │   ├── exception/                     # Global exception handler & custom exceptions
│   │   │   ├── rate_limiting/                 # RateLimitInterceptor configuration
│   │   │   ├── response/                      # Standard API response model
│   │   │   ├── security/                      # Security filters, JWT providers, OAuth2 configs
│   │   │   ├── service/                       # FileUploadService (Cloudinary integration)
│   │   │   └── utils/                         # AppTranslator for translation bundles
│   │   └── feature/                           # Feature-oriented modular packages
│   │       ├── auth/                          # Controller, Service, DTOs for authentication
│   │       ├── cart/                          # Shopping cart business layers
│   │       ├── category/                      # Category management modules
│   │       ├── order/                         # Order operations & cron cleanups
│   │       ├── payment/                       # Paymob integrations & webhooks
│   │       ├── product/                       # Products and inventory management
│   │       └── user/                          # Users profiles & permissions
│   └── resources/
│       ├── application.properties             # Main settings (database, cache, keys)
│       ├── log4j2-spring.xml                  # Rich custom Log4j2 configuration
│       ├── db/migration/                      # Flyway SQL migration scripts (V1__ to V19__)
│       ├── i18n/                              # Arabic and English translation bundles
│       ├── static/                            # Static templates (if any)
│       └── templates/                         # HTML templates (if any)
└── test/
    └── java/com/global/order_api/
        ├── InventoryOrderApiApplicationTests.java # Sanity load test
        ├── BaseRepoTest.java                      # Base Repository test helpers
        └── feature/                               # Feature tests (Mockito, MockMvc, H2, Testcontainers)
            ├── auth/                              # AuthControllerTest, AuthServiceTest
            ├── cart/                              # CartControllerTest, CartServiceTest
            ├── category/                          # Category tests
            ├── order/                             # OrderControllerTest, OrderServiceTest (cancellation & scheduling)
            ├── payment/                           # Payment integration mocks and webhook verification tests
            ├── product/                           # ProductRepoTest, AdminProductControllerTest, ProductServiceTest
            └── user/                              # User tests 
```

<a id="system-diagrams"></a>
📊 System Diagrams
------------------

Detailed architectural and database visual representations showcasing the system's robustness and data flow.

### 1. System Architecture & Data Flow
This diagram illustrates the comprehensive cloud architecture, including edge rate-limiting, security filter chains, asynchronous payment webhooks, and managed data services.


<img width="8928" height="4224" alt="Untitled Diagram (8)" src="https://github.com/user-attachments/assets/ee45d00c-e0d4-43dc-8750-e875f7ae9f76" />

### 2. Entity Relationship Diagram (ERD)
The ERD shows the database schema, illustrating the relationships and constraints between tables.
<img width="1900" height="966" alt="inventory-order-api-ERD" src="https://github.com/user-attachments/assets/654bdfa3-b8f8-4f30-a262-750de248f540" />

The following diagrams provide a high-level overview of the application's architecture and database design.

<a id="screenshots"></a>
🖼️ Screenshots & Previews
-------------------------

Visual evidence of the fully functional ecosystem, showcasing deployment environments and external service integrations.

### 🐳 1. Dockerized Environment (Local Stack)
This screenshot shows the entire development stack running seamlessly as decoupled containers via `docker-compose` (Application, MySQL, and Redis Cache).

/><img width="1336" height="724" alt="Screenshot (188)" src="https://github.com/user-attachments/assets/7107b196-2c24-4e16-ad1a-8e5fda0d158e" />

<img width="1366" height="673" alt="Screenshot (190)" src="https://github.com/user-attachments/assets/174f1781-c286-42bf-baf4-60a77aa605a5" />

### 💳 2. Paymob Payment Gateway & Webhooks
A preview of the Paymob merchant dashboard showing processed transactions, payment status, and successful server-to-server webhook callbacks handling order confirmations.

<img width="1366" height="677" alt="Screenshot (206)" src="https://github.com/user-attachments/assets/abdec220-bd48-40ad-8bd3-33d734137264" />
<img width="1366" height="686" alt="Screenshot (183)" src="https://github.com/user-attachments/assets/42b51e8c-5c56-4261-8601-c0e480fd50a7" />
<img width="1366" height="649" alt="Screenshot (202)" src="https://github.com/user-attachments/assets/4a0ace9a-94b3-40fc-a959-9f83bb64801c" />


### ☁️ 3. Cloud Deployment & Managed Services (Railway)
A live view of the production environment hosted on Railway. The first preview showcases the fully integrated cloud stack (Spring Boot Application, Managed MySQL, and Redis) running seamlessly. The database view highlights the successful execution of **Flyway migrations** and the active **ShedLock** table managing distributed cron jobs in production.

<img width="1366" height="686" alt="Screenshot (197)" src="https://github.com/user-attachments/assets/2aa6d01a-d6b7-4ccf-a651-30b397e90dff" />
<img width="1366" height="673" alt="Screenshot (196)" src="https://github.com/user-attachments/assets/5bed36b2-8376-4b72-b32c-99ca1af1dc4e" />


<a id="getting-started"></a>
🚀 Getting Started
Prerequisites
Java 21
Maven
MySQL
Redis
Docker (Optional)
Run Locally

git clone https://github.com/ahmed-ehab-m/inventory-order-api.git


cd inventory-order-api

mvn clean package

mvn spring-boot:run
Run with Docker Compose
docker compose up -d

> This starts the complete local environment (Spring Boot, MySQL, and Redis).
    
<a id="configuration"></a>
## ⚙️ Configuration

Create an `application-prod.properties` file (or configure environment variables) and provide the required values.

```properties
# Database
DB_URL=jdbc:mysql://localhost:3306/inventory_order_db
DB_USER=root
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_super_secret_jwt_key

# Cloudinary
CLOUDINARY_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Paymob
PAYMOB_API_KEY=your_paymob_api_key
PAYMOB_HMAC_SECRET=your_hmac_secret

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Default Admin
ADMIN_EMAIL=admin@orderapi.com
ADMIN_PASSWORD=admin123456
```

> **Note:** Only configure the integrations you plan to use (e.g., Cloudinary or Paymob). The remaining environment variables can be omitted if those features are disabled.


```
<a id="ci-cd"></a>
## 🔄 CI/CD

The project uses **GitHub Actions** to automate building, testing, and deployment.

### Continuous Integration

On every push or pull request, the pipeline:

* Builds the application
* Runs all **260+ automated tests**
* Ensures the project is ready for deployment

### Continuous Deployment

When changes are merged into the main branch, the pipeline:

* Builds a new Docker image
* Pushes the image to Docker Hub
* Deploys the latest version to Railway automatically

<a id="contributing"></a>
## 🤝 Contributing

Contributions are welcome and greatly appreciated.

If you'd like to contribute:

1. Fork the repository.
2. Create a new branch (`feature/your-feature`).
3. Commit your changes.
4. Push your branch.
5. Open a Pull Request.

    
<a id="license"></a>
📄 License

This project is licensed under the MIT License. See the LICENSE file for more details.

<a id="author"></a>
👤 Author
---------

*   **Ahmed Ehab** - Backend Software Engineer

*   **LinkedIn:** https://www.linkedin.com/in/ahmed-ehab-72052b21a/
    
*   **GitHub:** https://github.com/ahmed-ehab-m/inventory-order-api
