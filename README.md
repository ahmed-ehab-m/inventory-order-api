🛒 E-Commerce Inventory & Order API
===================================

> **Live API Documentation (Swagger):** [Test the API Here](https://inventory-order-api-production.up.railway.app/docs)
> 
> **Test Credentials:**
> 
> *   Email: admin@orderapi.com
>     
> *   Password: admin123456
>     

📖 About
--------

A robust, secure, and scalable **Modular Monolith** RESTful API built for managing an e-commerce ecosystem. Designed utilizing the **MVC** pattern and structured with a **Package-by-Feature** architecture, this project aims to solve the complex challenges of concurrent inventory tracking and secure order processing. This clean, modular approach ensures high code maintainability, data integrity, robust security, and seamless integration with external services.

✨ Features
----------

*   **Advanced Security:** JWT-based authentication & OAuth2 (Google/GitHub) with Role-Based Access Control (RBAC), fortified with **Rate Limiting** to prevent API abuse.
    
*   **Order & Inventory Management:** Full life-cycle of orders, real-time stock validation using **Optimistic Locking** and **Pessimistic Locking**, and cart management.
    
*   **Payment Integration:** Secure payment gateway integration using **Paymob**.
    
*   **Media Management:** Automated product image uploads and cloud storage via **Cloudinary**.
    
*   **High Performance:** API response acceleration using **Redis** caching, alongside optimized **MySQL indexing**, Pagination, and Projection.
    
*   **Scheduled Jobs:** Automated cleanup tasks safely synchronized across multiple instances using **ShedLock**.
    
*   **Reliability:** Covered by **260+ Test Cases** using JUnit 5 and Mockito.
    

💻 Tech Stack
-------------

*   **Backend:** Java 17, Spring Boot 3, Spring Data JPA (Hibernate)
    
*   **Database & Migrations:** MySQL, Flyway
    
*   **Caching:** Redis
    
*   **Security:** Spring Security, JWT, OAuth2
    
*   **3rd Party APIs:** Paymob, Cloudinary
    
*   **DevOps & Deployment:** Docker, CI/CD (GitHub Actions), Railway
    
*   **Testing:** JUnit 5, Mockito
    
*   **Documentation:** Springdoc OpenAPI (Swagger UI)
    

🗂️ Project Structure
---------------------

The project follows a standard layered architecture to maintain separation of concerns:

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

<img width="1574" height="942" alt="SystemArchitecture" src="https://github.com/user-attachments/assets/5a9d05a1-f852-4ae2-b11c-62241bf72ac6" />



### 2. Entity Relationship Diagram (ERD)
The ERD shows the database schema, illustrating the relationships and constraints between tables.
<img width="1900" height="966" alt="inventory-order-api-ERD" src="https://github.com/user-attachments/assets/654bdfa3-b8f8-4f30-a262-750de248f540" />


🚀 Getting Started
------------------

### Prerequisites

*   Java 17+
    
*   Maven
    
*   MySQL
    
*   Redis
    
*   Docker (Optional for containerized run)
    

### Installation & Run

1.  git clone https://github.com/ahmed-ehab-m/inventory-order-api.git
2.  cd inventory-order-api
3.  mvn clean install
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

```

## 🔄 CI/CD Pipeline

The project utilizes **GitHub Actions** for continuous integration and deployment, ensuring reliable and automated delivery:

1. **Continuous Integration (CI):** On every push or pull request to the `main` branch, the workflow automatically builds the application and runs all **260+ unit and integration tests** to ensure code quality and prevent regressions.
2. **Continuous Deployment (CD):** Once the build and tests pass successfully, the application is containerized using **Docker** and seamlessly deployed to the **Railway** cloud platform.-

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
