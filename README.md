# 🛒 E-Commerce Inventory & Order API

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue)
![JWT](https://img.shields.io/badge/Security-JWT%20%7C%20OAuth2-red)

## 📖 About The Project
A robust, secure, and scalable backend RESTful API built for managing an e-commerce ecosystem. It handles user authentication, shopping carts, product inventory, and order processing. The system is designed with a clean architecture and ensures data integrity, security, and seamless integration with external services like Cloudinary for image management.

**Key Features:**
* **Security:** JWT-based authentication & OAuth2 (Google/GitHub) with Role-Based Access Control (RBAC).
* **Order Management:** Full life-cycle of orders including soft/hard deletes with business rules.
* **Inventory & Cart:** Real-time stock validation and cart management.
* **Database Versioning:** Managed via Flyway migrations.
* **API Documentation:** Interactive UI provided by Swagger/OpenAPI.

---

## 💻 Tech Stack
* **Backend:** Java, Spring Boot, Spring Data JPA (Hibernate), Spring Web.
* **Security:** Spring Security, JWT (JSON Web Tokens), OAuth2.
* **Database:** MySQL.
* **Database Migrations:** Flyway.
* **External Services:** Cloudinary API (for multipart image uploads).
* **Tools & Utilities:** MapStruct (Object mapping), Lombok, Maven.
* **Documentation:** Springdoc OpenAPI (Swagger UI).

---

## 📊 System Diagrams

Here are the visual representations of the system's architecture, database design, and user interactions.

### 1. High-Level System Architecture
This diagram illustrates the overall data flow, including the interaction between the client, the Business Logic Server, the Security Layer, the MySQL database, and the external Cloudinary service.

<img width="601" height="512" alt="Untitled Diagram drawio" src="https://github.com/user-attachments/assets/b9a54e17-0c0a-4b6e-94e0-5cfc4b3c2936" />

### 2. Low-Level System Architecture
This detailed layer diagram illustrates the internal architecture of the Spring Boot application. It maps the complete request-response lifecycle, showcasing the role of the Security Filter Chain (JWT & OAuth2), business logic execution, data transfer mechanisms (DTOs & JPA Entities), and the precise protocols (e.g., JDBC, Multipart/form-data) used for database operations and external API integrations.

<img width="1531" height="725" alt="Untitled Diagram drawio (3)" src="https://github.com/user-attachments/assets/4b3dae46-020a-4e9b-80c7-c87a0617ca45" />



### 3. Entity Relationship Diagram (ERD)
The ERD shows the database schema, illustrating the relationships between Users, Products, Carts, Cart Items, Orders, and Order Items.

<img width="1500" height="1016" alt="inventory-order-api-ERD" src="https://github.com/user-attachments/assets/eb3e7ce4-d638-4406-9066-550b1525634e" />


### 4. Use Case Diagram
This diagram outlines the system's core functionalities from the perspective of different actors (e.g., Customer, Admin).
<img width="664" height="581" alt="inventory-order-api-UseCase" src="https://github.com/user-attachments/assets/3eda0995-a37c-462c-8e46-d7a94d7e5052" />
