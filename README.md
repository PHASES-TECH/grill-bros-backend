# Grill Bros Restaurant Management System Backend

A scalable backend API powering the Grill Bros Restaurant Management and Point of Sale (POS) platform.

The system provides a complete restaurant operations backend, enabling menu management, order processing, payment handling, customer notifications, analytics, and administrative workflows through secure and scalable REST APIs.

Built with Java and Spring Boot, the backend was designed using industry-standard practices with a focus on performance, maintainability, security, and future scalability.

Repository:
https://github.com/PHASES-TECH/grill-bros-backend/

---

# Overview

The Grill Bros Backend provides the core infrastructure for managing restaurant operations digitally.

The platform supports:

- Customer online ordering
- POS order management
- Menu and modifier management
- Payment processing
- Order tracking
- SMS notifications
- Sales analytics
- Role-based administration
- Popular item and recommendation features

The system was built to solve operational challenges faced by restaurants by replacing manual processes with an integrated digital management platform.

---

# Features

## Authentication & Authorization

- Secure authentication using JWT
- Refresh token based session management
- Role-based access control (RBAC)
- Protected API endpoints
- User and admin management
- Secure cookie-based authentication flow

Supported roles include:
- Super Admin
- Restaurant Administrators
- Staff users

---

# Menu Management

Complete restaurant menu administration system.

Features include:

- Create, update, and delete menu items
- Manage menu categories
- Upload and manage food images
- Control item availability
- Configure item sorting order
- Add menu tags
- Create reusable modifier groups
- Support menu customization through modifiers and add-ons

Examples:
- Size selection
- Extra toppings
- Add-ons
- Custom meal options

---

# Order Management

Handles the complete restaurant order lifecycle.

Features include:

- Create customer orders
- POS order processing
- Online ordering workflow
- Order status tracking
- Order history management
- Unique order tracking tokens
- Customer order status lookup

Supported order states:

- Pending
- Confirmed
- Preparing
- Ready
- Completed
- Cancelled

---

# Payment Integration

Integrated payment processing supporting multiple payment methods.

Features:

- Paystack online payment integration
- Cash payment workflow
- Payment verification
- Payment status management
- Order confirmation after successful payment

---

# Notifications

Integrated communication system for customer updates.

Features:

- SMS order notifications
- Order confirmation messages
- Order status updates
- Customer tracking notifications

---

# Analytics & Reporting

Provides restaurant owners with operational insights.

Features include:

- Revenue analytics
- Order statistics
- Average order value calculation
- Payment method breakdown
- Admin sales performance tracking
- Popular menu item tracking

---

# Recommendation System

The backend includes food recommendation capabilities using order analytics.

Features:

- Identify frequently ordered items
- Recommend related menu items
- Track item popularity
- Category-based recommendations

This improves customer discovery and helps restaurants understand customer preferences.

---

# Technology Stack

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- Hibernate
- Maven

## Database

- PostgreSQL

## Caching

- Redis

Used for:
- Menu caching
- Performance optimization
- Reducing database load

## Security

- JWT Authentication
- Refresh Tokens
- Role-Based Authorization
- Spring Security

## External Integrations

- Paystack Payments
- SMS Gateway Integration
- Image Upload Service

## Deployment & Tools

- Docker
- Railway
- AWS compatible deployment
- Git
- Postman

---

# Architecture

The application follows a layered architecture to ensure separation of concerns and maintainability.


---

## Controller Layer

Responsible for:

- Handling HTTP requests
- Request validation
- Returning API responses

Examples:

- AuthController
- MenuController
- OrderController
- PaymentController
- AnalyticsController

---

## Service Layer

Contains:

- Business logic
- Transaction management
- Data processing
- External service integration

Examples:

- MenuService
- OrderService
- PaymentService
- NotificationService

---

## Repository Layer

Handles database operations using Spring Data JPA.

Responsible for:

- Entity persistence
- Database queries
- Custom repository methods

---

## Security Layer

Contains:

- JWT authentication filter
- Security configuration
- Authentication providers
- Authorization rules

---

# Project Structure

├── controller
│ ├── AuthController
│ ├── MenuController
│ ├── OrderController
│ ├── PaymentController
│ └── AnalyticsController
│
├── service
│ ├── AuthService
│ ├── MenuService
│ ├── OrderService
│ ├── PaymentService
│ └── NotificationService
│
├── repository
│
├── model
│
├── dto
│
├── security
│ ├── JwtFilter
│ └── SecurityConfig
│
├── exception


---

# Database Design

The application uses PostgreSQL with relational data modeling.

Main entities include:

### Users
Handles authentication and user management.

### Menu Items
Stores restaurant food items.

### Categories
Groups menu items.

### Modifier Groups
Reusable customization groups assigned to multiple menu items.

Example:

Burger:
- Size
- Extra Cheese
- Add Bacon


### Orders

Stores customer purchases.

### Order Items

Stores individual items within an order.

### Payments

Tracks payment transactions.

### Notifications

Stores communication records.

Relationships are managed using JPA/Hibernate mappings.

---

# API Documentation

The backend exposes RESTful APIs for:

- Authentication
- Menu management
- Orders
- Payments
- Analytics
- Notifications

All endpoints include:

- Request validation
- Exception handling
- Standard API responses

---

# Getting Started

## Prerequisites

Install:

- Java 21+
- Maven
- PostgreSQL
- Redis


---

## Run Application

Using Maven:

./mvnw spring-boot:run

The API will start on:

http://localhost:8080

## Clone Repository

```bash
git clone https://github.com/PHASES-TECH/grill-bros-backend.git

cd grill-bros-backend

```

## Author

Built by PHASES TECH

A full-stack restaurant management platform designed to help food businesses manage operations efficiently through modern software solutions.
