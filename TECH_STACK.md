# Family Tree Manager - Technology Stack

## Overview
Enterprise-grade family tree management system built with Java Spring Boot and Angular.

## Backend Stack

### Core Framework
- **Java 17 (LTS)** - Modern Java with latest features
- **Spring Boot 3.2.x** - Enterprise application framework
- **Maven 3.8+** - Build and dependency management

### Spring Modules
- **Spring Web** - RESTful API endpoints
- **Spring Data JPA** - Database ORM (Hibernate)
- **Spring Security** - Authentication and authorization
- **Spring Cache** - Redis-based caching
- **Spring Session** - Redis-backed session management
- **Spring Validation** - Bean validation

### Database & Storage
- **PostgreSQL 14+** - Primary relational database
  - Recursive CTEs for ancestor/descendant queries
  - JSON support for flexible data
  - Full-text search capabilities
- **Redis 7+** - Caching and session store
  - Tree data caching (performance boost)
  - Session storage
  - Rate limiting
- **MinIO** - S3-compatible object storage
  - Photo and document storage
  - Self-hosted, can migrate to AWS S3
  - High performance

### Key Libraries

**Authentication & Security:**
- `io.jsonwebtoken:jjwt` (0.12.3) - JWT token generation and validation
- Spring Security - RBAC (Role-Based Access Control)

**GEDCOM Processing:**
- `org.gedcom4j:gedcom4j` (4.2.0) - Parse and generate GEDCOM files

**File Storage:**
- `io.minio:minio` (8.5.7) - MinIO Java client

**Code Quality:**
- `org.projectlombok:lombok` (1.18.30) - Reduce boilerplate code
- `org.mapstruct:mapstruct` (1.5.5) - DTO mapping

**Image Processing:**
- `net.coobird:thumbnailator` (0.4.20) - Generate thumbnails

**Utilities:**
- `commons-io:commons-io` - File utilities
- `commons-lang3` - Common utilities

### Testing
- **JUnit 5** - Unit testing framework
- **Mockito** - Mocking framework
- **Spring Boot Test** - Integration testing
- **Testcontainers** - PostgreSQL/Redis for integration tests
- **JaCoCo** - Code coverage reporting

## Frontend Stack

### Core Framework
- **Angular 17+** - Modern TypeScript framework
- **TypeScript 5+** - Type-safe JavaScript
- **RxJS** - Reactive programming

### UI & Visualization
- **Angular Material** or **Tailwind CSS** - UI components and styling
- **D3.js** or **Cytoscape.js** - Tree visualization library
- Responsive design (mobile, tablet, desktop)

### Angular Features
- **Angular Router** - Client-side routing
- **HttpClient** - API communication
- **Reactive Forms** - Form handling
- **Interceptors** - Auth token injection, error handling
- **Guards** - Route protection

### Development Tools
- **Angular CLI** - Project scaffolding and build
- **npm** - Package management
- **Karma & Jasmine** - Unit testing
- **Protractor/Cypress** - E2E testing

## Infrastructure

### Reverse Proxy & Web Server
- **Nginx** - Production-grade web server
  - Serve Angular static files
  - Reverse proxy to Spring Boot API
  - SSL/TLS termination
  - Load balancing ready

### Containerization
- **Docker** - Container platform
- **Docker Compose** - Multi-container orchestration
  - PostgreSQL container
  - Redis container
  - MinIO container
  - Spring Boot container
  - Nginx container

## Development Workflow

### Backend Development
```bash
# Build project
mvn clean install

# Run tests
mvn test

# Run with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package for production
mvn clean package
```

### Frontend Development
```bash
# Install dependencies
npm install

# Run dev server
ng serve

# Build for production
ng build --configuration production

# Run tests
ng test

# E2E tests
ng e2e
```

### Docker Development
```bash
# Start all infrastructure services
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

## Production Deployment

### Option 1: Docker Compose (Recommended for self-hosted)
```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Option 2: Cloud Deployment
- **AWS**: EC2 + RDS PostgreSQL + ElastiCache Redis + S3
- **Azure**: App Service + Azure Database for PostgreSQL + Azure Cache for Redis + Blob Storage
- **GCP**: Cloud Run + Cloud SQL + Memorystore + Cloud Storage

### Option 3: Kubernetes
- Deploy to any Kubernetes cluster (AKS, EKS, GKE, on-prem)

## Architecture Benefits

### Performance
- ✅ **Java multi-threading** - Better concurrent user handling
- ✅ **Redis caching** - Fast tree data retrieval
- ✅ **PostgreSQL** - Efficient recursive queries
- ✅ **Connection pooling** - HikariCP (Spring Boot default)

### Scalability
- ✅ **Stateless API** - Horizontal scaling
- ✅ **Redis sessions** - Shared session store
- ✅ **MinIO** - Distributed object storage
- ✅ **Load balancing** - Nginx ready

### Security
- ✅ **Spring Security** - Industry-standard security
- ✅ **JWT authentication** - Stateless auth
- ✅ **HTTPS** - Encrypted communication
- ✅ **CORS** - Cross-origin protection
- ✅ **Rate limiting** - DoS protection

### Maintainability
- ✅ **Strong typing** - Java + TypeScript
- ✅ **Clean architecture** - Controller → Service → Repository
- ✅ **Test coverage** - JUnit + Jasmine
- ✅ **Code quality** - Lombok + MapStruct

### Data Ownership
- ✅ **Self-hosted** - Full data control
- ✅ **MinIO** - No cloud vendor lock-in
- ✅ **PostgreSQL** - Standard SQL database
- ✅ **GEDCOM export** - Data portability

## Migration Path

### From Development to Production
1. **Database**: Switch from local PostgreSQL to managed PostgreSQL (AWS RDS, Azure Database)
2. **Redis**: Switch to managed Redis (AWS ElastiCache, Azure Cache)
3. **Storage**: Switch MinIO to AWS S3 (same API, minimal code changes)
4. **Deployment**: Containerize with Docker and deploy to Kubernetes or cloud

### Scaling Strategy
1. **Phase 1** (0-100 users): Single server with Docker Compose
2. **Phase 2** (100-1000 users): Add Redis caching, CDN for media
3. **Phase 3** (1000+ users): Kubernetes with multiple replicas, managed databases
4. **Phase 4** (10,000+ users): Microservices, separate read/write databases, event-driven architecture

## Estimated Costs (Self-Hosted)

**Development Environment:** $0 (local Docker containers)

**Small Production (< 100 users):**
- VPS (4 CPU, 8GB RAM): $40/month
- Storage (100GB): $10/month
- **Total: ~$50/month**

**Medium Production (< 1000 users):**
- VPS (8 CPU, 16GB RAM): $80/month
- Managed PostgreSQL: $50/month
- Managed Redis: $30/month
- Storage (500GB): $50/month
- **Total: ~$210/month**

## Next Steps

1. **Start Infrastructure**: Run `docker-compose up -d`
2. **Initialize Backend**: Follow setup in `docs/ai/implementation/feature-family-tree-manager.md`
3. **Initialize Frontend**: Set up Angular project
4. **Follow Planning**: Implement phases in `docs/ai/planning/feature-family-tree-manager.md`

---

**Documentation:**
- Requirements: `docs/ai/requirements/feature-family-tree-manager.md`
- Design: `docs/ai/design/feature-family-tree-manager.md`
- Planning: `docs/ai/planning/feature-family-tree-manager.md`
- Implementation: `docs/ai/implementation/feature-family-tree-manager.md`
- Testing: `docs/ai/testing/feature-family-tree-manager.md`
