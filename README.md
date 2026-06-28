![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Railway](https://img.shields.io/badge/Deploy-Railway-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)
![GitHub Actions](https://img.shields.io/badge/CI-GitHub_Actions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white)
![Swagger](https://img.shields.io/badge/API-Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black)
![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)
# 🛒 Inventory & Order API  
===================================

> **Live API Documentation (Swagger):** [Test the API Here](https://inventory-order-api-production.up.railway.app/docs)
> **Postman Collection:** For advanced testing with pre-configured environments and automated token extraction, download the collection here:
> 📥 [Download Postman Collection](./docs/Inventory-Order-API.postman_collection.json)
> 
> **Test Credentials:**
> 
> *   Email: admin@orderapi.com
>     
> *   Password: admin123456
>     

📖 About
--------

Inventory Order API is a production-oriented backend application built with Java and Spring Boot.

The project follows a **Modular Monolith (MVC)** architecture with a **Feature-Based Package Structure**, focusing on solving real backend challenges such as:

- Concurrent inventory updates
- Secure order processing
- Payment gateway integration
- Caching
- Background jobs
- Production deployment
- Automated testing

✨ Features
----------

*   **Advanced Security:** JWT-based authentication & OAuth2 (Google/GitHub) with Role-Based Access Control (RBAC), fortified with **Rate Limiting** to prevent API abuse.
    
*   **Order & Inventory Management:** Full life-cycle of orders, real-time stock validation using **Optimistic Locking** and **Pessimistic Locking**, and cart management.

*   **Observability:** Structured logging, execution time tracking using Spring AOP, and Spring Data Auditing.

*   **Payment Integration:** Secure payment gateway integration using **Paymob**.
    
*   **Media Management:** Automated product image uploads and cloud storage via **Cloudinary**.
    
*   **High Performance:** API response acceleration using **Redis** caching, alongside optimized **MySQL indexing**, Pagination, and Projection.
    
*   **Scheduled Jobs:** Automated cleanup tasks safely synchronized across multiple instances using **ShedLock**.
    
*   **Reliability:** Covered by **260+ Test Cases** using JUnit 5 and Mockito (Repo, Service, Controller).

*   **Unified API Responses & Global Exception Handling**
          
*   **Automated Data Seeding:** Utilized Spring Boot's CommandLineRunner to automatically seed essential database records on startup (e.g., default Roles and the master Admin account), enabling instant API testing without manual setup.
    

💻 Tech Stack
-------------

*   **Backend:** Java 21, Spring Boot 4, Spring Data JPA (Hibernate)
    
*   **Database & Migrations:** MySQL, Flyway
    
*   **Caching:** Redis
    
*   **Security:** Spring Security, JWT, OAuth2
    
*   **3rd Party APIs:** Paymob, Cloudinary
    
*   **DevOps & Deployment:** Docker, CI/CD (GitHub Actions), Railway
    
*   **Testing:** JUnit 5, Mockito
    
*   **Documentation:** Springdoc OpenAPI (Swagger UI)
    **Developer Tools:**
    MapStruct
    Log4j2
    Spring AOP


## Why these technologies?

### Why Redis for Rate Limiting?

Redis provides atomic increment operations and automatic key expiration, making it more suitable than relational databases.

### Why Pessimistic Locking?

To guarantee that the same stock cannot be reserved by multiple concurrent orders.

### Why Optimistic Locking?

Product updates are read-heavy and write-light, so optimistic locking reduces unnecessary database locks.

### Why ShedLock?

To ensure scheduled jobs execute only once across multiple application instances.

### Why Flyway?

Keeping database schema changes versioned and reproducible across all environments.

### Why Testcontainers?

To validate repository behavior against a real MySQL instance instead of relying only on in-memory databases.

## 🧪 Testing Strategy

The project contains over **260 automated test cases** covering:

- Service Layer (Mockito)

- Controller Layer (MockMvc)

- Repository Layer (Testcontainers + Real MySQL)

This ensures business logic, REST APIs, and SQL queries are all verified independently.

## ☁️ Deployment

The application is deployed on Railway.

Production environment includes:

- Spring Boot Application

- Managed MySQL Database

- Managed Redis Instance

- Docker Container

Database schema is managed automatically using Flyway migrations.
    

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

📊 System Diagrams
------------------

Detailed architectural and database visual representations showcasing the system's robustness and data flow.

### 1. System Architecture & Data Flow
This diagram illustrates the comprehensive cloud architecture, including edge rate-limiting, security filter chains, asynchronous payment webhooks, and managed data services.

<img width="6308" height="3644" alt="Untitled Diagram drawio (8)" src="https://github.com/user-attachments/assets/1ff1d100-5e25-4c22-824a-74eb06ecd394" />




### 2. Entity Relationship Diagram (ERD)
The ERD shows the database schema, illustrating the relationships and constraints between tables.
<img width="1900" height="966" alt="inventory-order-api-ERD" src="https://github.com/user-attachments/assets/654bdfa3-b8f8-4f30-a262-750de248f540" />

The following diagrams provide a high-level overview of the application's architecture and database design.

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



🚀 Getting Started
------------------

### Prerequisites

*   Java 21
    
*   Maven
    
*   MySQL
    
*   Redis
    
*   Docker (Optional for containerized run)
    

### Installation & Run

1.  git clone https://github.com/ahmed-ehab-m/inventory-order-api.git
2.  cd inventory-order-api
3.  mvn clean package
4.  mvn spring-boot:run

*Alternatively, you can run the entire stack locally using Docker Compose:*
docker-compose up -d
    

⚙️ Configuration
---------------


Create an application-prod.properties (or .env file) in your environment and configure the following essential variables:
```
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
REDIS_HOST=your_redis_host
REDIS_PORT=your_redis_port

# Admin
ADMIN_EMAIL
ADMIN_PASSWORD

```

## 🔄 CI/CD

The project uses GitHub Actions to automate the build pipeline.

### Continuous Integration

- Build the application
- Run 260+ automated tests
- Verify code quality

### Continuous Deployment

- Build Docker Image
- Push Image to Docker Hub
- Deploy the latest version on Railway

🤝 How to Contribute
--------------------

Contributions are what make the open source community such an amazing place to learn, inspire, and create. Any contributions you make are greatly appreciated.

1.  Fork the Project
    
2.  Create your Feature Branch (git checkout -b feature/AmazingFeature)
    
3.  Commit your Changes (git commit -m 'Add some AmazingFeature')
    
4.  Push to the Branch (git push origin feature/AmazingFeature)
    
5.  Open a Pull Request
    

📄 License
----------

Distributed under the MIT License. See LICENSE for more information.

👤 Author
---------

*   **Ahmed Ehab** - Backend Software Engineer

*   **LinkedIn:** https://www.linkedin.com/in/ahmed-ehab-72052b21a/
    
*   **GitHub:** https://github.com/ahmed-ehab-m/inventory-order-api
