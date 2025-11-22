# Family Tree Manager

A comprehensive, enterprise-grade web application for managing family trees, genealogy research, and preserving family history.

## 🌟 Features

- **Visual Family Tree** - Interactive graphical representation with zoom, pan, and navigation
- **Individual Management** - Add family members with detailed biographical information
- **Relationship Tracking** - Define parent-child, spouse, sibling, and other relationships
- **Photo & Document Management** - Attach photos and documents to individuals
- **GEDCOM Import/Export** - Standard genealogy file format for data portability
- **Collaborative Editing** - Multiple family members can work on the same tree
- **Search & Filter** - Quickly find individuals by name, date, or place
- **Privacy Controls** - Permission system with owner, editor, and viewer roles
- **Responsive Design** - Works on desktop, tablet, and mobile browsers

## 🛠️ Technology Stack

### Backend
- **Java 17** + **Spring Boot 3.x**
- **PostgreSQL 14+** (Database)
- **Redis 7+** (Cache & Sessions)
- **MinIO** (S3-compatible object storage)
- **Maven** (Build tool)

### Frontend
- **Angular 17+** + **TypeScript**
- **D3.js** or **Cytoscape.js** (Tree visualization)
- **Angular Material** or **Tailwind CSS** (UI)

### Infrastructure
- **Docker** & **Docker Compose**
- **Nginx** (Reverse proxy)

See [TECH_STACK.md](TECH_STACK.md) for complete details.

## 📋 Prerequisites

- **Java JDK 17+** ([Download](https://adoptium.net/))
- **Maven 3.8+** ([Download](https://maven.apache.org/))
- **Node.js 18+** and **npm** ([Download](https://nodejs.org/))
- **Docker** & **Docker Compose** ([Download](https://www.docker.com/))
- **Git** ([Download](https://git-scm.com/))

## 🚀 Quick Start

### 1. Clone Repository
```bash
git clone <repository-url>
cd family-tree-manager
```

### 2. Start Infrastructure (PostgreSQL, Redis, MinIO)
```bash
docker-compose up -d
```

This starts:
- PostgreSQL on port 5432
- Redis on port 6379
- MinIO API on port 9000, Console on port 9001

### 3. Start Backend (Spring Boot)
```bash
cd backend
mvn clean install
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Backend API will be available at: http://localhost:8080

### 4. Start Frontend (Angular)
```bash
cd frontend
npm install
ng serve
```

Frontend will be available at: http://localhost:4200

### 5. Access Application
- **Frontend:** http://localhost:4200
- **Backend API:** http://localhost:8080/api
- **MinIO Console:** http://localhost:9001 (login: minioadmin/minioadmin123)

## 📁 Project Structure

```
family-tree-manager/
├── backend/                 # Spring Boot application
│   ├── src/main/java/      # Java source code
│   ├── src/main/resources/ # Configuration files
│   ├── src/test/           # Tests
│   └── pom.xml             # Maven dependencies
├── frontend/               # Angular application
│   ├── src/app/            # Angular modules and components
│   ├── src/environments/   # Environment configs
│   └── package.json        # npm dependencies
├── nginx/                  # Nginx configuration
├── docs/ai/                # Project documentation
│   ├── requirements/       # Requirements documentation
│   ├── design/             # System design & architecture
│   ├── planning/           # Project planning & tasks
│   ├── implementation/     # Implementation guide
│   └── testing/            # Testing strategy
├── docker-compose.yml      # Docker services
└── README.md
```

## 🧪 Testing

### Backend Tests
```bash
cd backend

# Run unit tests
mvn test

# Run integration tests
mvn verify

# Generate coverage report
mvn test jacoco:report
# View report: target/site/jacoco/index.html
```

### Frontend Tests
```bash
cd frontend

# Run unit tests
ng test

# Run E2E tests
ng e2e

# Generate coverage
ng test --code-coverage
# View report: coverage/index.html
```

## 📦 Building for Production

### Backend
```bash
cd backend
mvn clean package
# JAR file: target/family-tree-manager-1.0.0-SNAPSHOT.jar
```

### Frontend
```bash
cd frontend
ng build --configuration production
# Build output: dist/
```

## 🐳 Docker Deployment

### Build Docker Images
```bash
# Backend
cd backend
docker build -t family-tree-backend .

# Frontend
cd frontend
docker build -t family-tree-frontend .
```

### Deploy with Docker Compose
```bash
docker-compose -f docker-compose.prod.yml up -d
```

## 🔧 Configuration

### Backend Configuration
Edit `backend/src/main/resources/application-dev.properties`:

```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/family_tree_dev
spring.datasource.username=familytree
spring.datasource.password=familytree_password

# Redis
spring.redis.host=localhost
spring.redis.port=6379

# MinIO
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin123

# JWT
jwt.secret=your-secret-key-change-in-production
jwt.expiration=604800000
```

### Frontend Configuration
Edit `frontend/src/environments/environment.development.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  minioUrl: 'http://localhost:9000'
};
```

## 📖 Documentation

Complete project documentation is available in the `docs/ai/` directory:

- **[Requirements](docs/ai/requirements/feature-family-tree-manager.md)** - Problem statement, user stories, success criteria
- **[Design](docs/ai/design/feature-family-tree-manager.md)** - System architecture, data models, API design
- **[Planning](docs/ai/planning/feature-family-tree-manager.md)** - Task breakdown, timeline, risks
- **[Implementation](docs/ai/implementation/feature-family-tree-manager.md)** - Setup guide, code structure, patterns
- **[Testing](docs/ai/testing/feature-family-tree-manager.md)** - Test strategy, test cases, coverage goals

## 🗺️ Roadmap

### Phase 1: Foundation ✅
- [x] Project setup
- [x] Documentation
- [ ] Database schema
- [ ] Authentication

### Phase 2: Core Features
- [ ] Family tree CRUD
- [ ] Individual management
- [ ] Relationship management

### Phase 3: Visualization
- [ ] Tree rendering
- [ ] Interactive navigation
- [ ] Layout algorithms

### Phase 4: Media
- [ ] Photo uploads
- [ ] Document storage
- [ ] Thumbnail generation

### Phase 5: GEDCOM
- [ ] Import GEDCOM files
- [ ] Export GEDCOM files

### Phase 6: Collaboration
- [ ] User permissions
- [ ] Invite system
- [ ] Real-time updates

### Phase 7: Polish
- [ ] Search functionality
- [ ] UI/UX improvements
- [ ] Performance optimization

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- **GEDCOM4J** - GEDCOM parsing library
- **Spring Boot** - Application framework
- **Angular** - Frontend framework
- **D3.js** - Visualization library
- **MinIO** - Object storage
- **PostgreSQL** - Database

## 💬 Support

For questions, issues, or feature requests, please:
- Open an issue on GitHub
- Check the documentation in `docs/ai/`
- Review the planning guide for implementation details

## 🎯 Key Commands

```bash
# Start development environment
docker-compose up -d                    # Infrastructure
cd backend && mvn spring-boot:run       # Backend
cd frontend && ng serve                 # Frontend

# Run tests
mvn test                                # Backend tests
ng test                                 # Frontend tests

# Build for production
mvn clean package                       # Backend
ng build --configuration production     # Frontend

# View services
http://localhost:4200                   # Angular app
http://localhost:8080                   # Spring Boot API
http://localhost:9001                   # MinIO Console
```

---

**Built with ❤️ using Java Spring Boot + Angular**
