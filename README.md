# SYOS - Store Your Own Store

A comprehensive web-based inventory and billing management system designed for retail stores with both in-store and online operations. Built with Java EE, this enterprise-grade application provides a complete solution for managing inventory, processing transactions, and generating detailed business reports.

## 🚀 Features

### Core Functionality
- **Dual Portal System**
  - Employee Portal (Cashier, Store Manager, Admin)
  - Customer Portal (Online Store)
- **Inventory Management**
  - Real-time stock tracking
  - Expiry date monitoring
  - Automatic reorder alerts
  - Shelf management system
- **Billing System**
  - In-store and online billing
  - Receipt generation
  - Transaction history
  - Multi-item purchase processing
- **Advanced Reporting**
  - Daily sales reports
  - Stock status reports
  - Reorder recommendations
  - Custom report generation

### Technical Highlights
- **Design Patterns Implementation**
  - Factory Pattern (Bill generation)
  - Builder Pattern (Complex object construction)
  - Visitor Pattern (Report generation)
  - Dependency Injection
  - State Pattern
- **Robust Architecture**
  - Layered architecture (Presentation, Business, Persistence)
  - Service-oriented design
  - Custom dependency injection container
- **Concurrency Support**
  - Thread-safe request queue management
  - High-load handling
  - Multi-user support
- **Data Validation**
  - Comprehensive input validation
  - Customer data verification
  - Transaction integrity checks

## 🛠️ Technology Stack

- **Backend**: Java 22, Java EE (Servlets, JSP)
- **Database**: MySQL
- **Build Tool**: Maven
- **Testing**: JUnit 5, Mockito
- **Server**: Compatible with Apache Tomcat/Jakarta EE servers

## 📋 Prerequisites

- Java Development Kit (JDK) 22 or higher
- Apache Maven 3.6+
- MySQL Server 8.0+
- Apache Tomcat 10.1+ or compatible Jakarta EE server

## 🔧 Installation & Setup

### 1. Clone the Repository
```bash
git clone https://github.com/tahir-packeer/Webstore-SYOS.git
cd Webstore-SYOS
```

### 2. Database Setup
```bash
# Login to MySQL
mysql -u root -p

# Run the schema file
source src/main/resources/database_schema.sql
```

### 3. Configure Database Connection
Update database credentials in the application configuration if needed.

### 4. Build the Project
```bash
mvn clean install
```

### 5. Deploy
- Copy the generated WAR file from `target/SYOS-CB009900-1.0.war` to your Tomcat `webapps` directory
- Or use your IDE's deployment configuration

### 6. Access the Application
- Employee Portal: `http://localhost:8080/SYOS-CB009900/`
- Online Store: Select Customer Portal from the main menu

## 🏗️ Project Structure

```
SYOS/
├── src/
│   ├── main/
│   │   ├── java/org/example/
│   │   │   ├── business/          # Business logic layer
│   │   │   │   ├── facades/       # Facade pattern implementations
│   │   │   │   ├── managers/      # Business managers
│   │   │   │   ├── services/      # Service layer
│   │   │   │   └── validators/    # Input validators
│   │   │   ├── core/              # Core utilities and DI container
│   │   │   ├── persistence/       # Data access layer
│   │   │   │   ├── database/      # Database connection
│   │   │   │   ├── models/        # Entity models
│   │   │   │   └── repositories/  # Data repositories
│   │   │   ├── presentation/      # Presentation layer
│   │   │   │   ├── controllers/   # Controllers & Servlets
│   │   │   │   └── views/         # Console views
│   │   │   └── shared/            # Shared utilities & patterns
│   │   │       ├── dto/           # Data transfer objects
│   │   │       └── patterns/      # Design pattern implementations
│   │   ├── resources/
│   │   │   └── database_schema.sql
│   │   └── webapp/                # Web resources (JSP, CSS, JS)
│   └── test/                      # Comprehensive test suite
└── pom.xml                        # Maven configuration
```

## 🧪 Testing

The project includes extensive test coverage:

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CustomerValidatorTest

# Generate test reports
mvn surefire-report:report
```

Test categories include:
- Unit tests for business logic
- Validator tests
- Concurrency tests
- Design pattern implementation tests
- Database integration tests

## 👥 User Roles

- **Admin**: Full system access, user management, all reports
- **Store Manager**: Inventory management, reporting, store operations
- **Cashier**: Point of sale, billing, basic inventory queries
- **Customer**: Browse products, online shopping, order tracking

## 📊 Database Schema

The application uses a normalized MySQL database with the following main entities:
- Users (Authentication & Authorization)
- Items (Product catalog)
- Stock (Inventory with expiry tracking)
- Customers (Customer information)
- Bills & Bill Items (Transaction records)
- Shelves (Physical inventory management)

## 🔐 Security Features

- Role-based access control
- Password authentication
- Session management
- Input validation and sanitization
- SQL injection prevention

## 📈 Performance

- Optimized for high-load scenarios
- Concurrent request handling
- Efficient database queries
- Connection pooling support

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is part of an academic assignment (CB009900) and is provided as-is for educational purposes.

## 📧 Contact

**Developer**: Tahir Packeer  
**GitHub**: [@tahir-packeer](https://github.com/tahir-packeer)  
**Repository**: [Webstore-SYOS](https://github.com/tahir-packeer/Webstore-SYOS)

## 🙏 Acknowledgments

- Built as part of the SYOS (Store Your Own Store) academic project
- Implements industry-standard design patterns and best practices
- Designed with scalability and maintainability in mind

---

**Note**: This is an enterprise-level retail management system showcasing modern Java development practices, design patterns, and full-stack web application architecture.
