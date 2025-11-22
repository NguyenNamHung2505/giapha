---
phase: implementation
title: Implementation Guide
description: Technical implementation notes, patterns, and code guidelines
---

# Implementation Guide

## Development Setup
**How do we get started?**

### Prerequisites and Dependencies
- **Java**: JDK 17 or higher (recommend Eclipse Temurin or OpenJDK)
- **Maven**: 3.8.x or higher
- **Node.js**: 18.x or higher (for Angular)
- **npm**: 9.x or higher (comes with Node.js)
- **PostgreSQL**: 14.x or higher
- **Redis**: 7.x or higher
- **MinIO**: Latest version
- **Git**: Latest version
- **Docker & Docker Compose** (recommended): For running PostgreSQL, Redis, MinIO
- **IDE**: IntelliJ IDEA (recommended) or VS Code with Java extensions

### Environment Setup Steps

1. **Install Java JDK 17+**
   ```bash
   # macOS
   brew install openjdk@17

   # Ubuntu/Debian
   sudo apt-get install openjdk-17-jdk

   # Windows
   # Download from https://adoptium.net/
   ```

2. **Install Maven**
   ```bash
   # macOS
   brew install maven

   # Ubuntu/Debian
   sudo apt-get install maven

   # Windows
   # Download from https://maven.apache.org/
   ```

3. **Start Infrastructure with Docker Compose**
   ```bash
   # Create docker-compose.yml in project root
   docker-compose up -d
   # This starts: PostgreSQL, Redis, MinIO
   ```

4. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd family-tree-manager
   ```

5. **Backend Setup (Spring Boot)**
   ```bash
   cd backend
   cp src/main/resources/application-dev.properties.example application-dev.properties
   # Edit application-dev.properties with your database credentials

   # Build project
   mvn clean install

   # Run Spring Boot application
   mvn spring-boot:run -Dspring-boot.run.profiles=dev
   ```

6. **Frontend Setup (Angular)**
   ```bash
   cd frontend
   npm install
   cp src/environments/environment.development.ts.example environment.development.ts
   # Edit with API URL if needed

   # Run Angular development server
   ng serve
   ```

7. **Verify Services**
   - Spring Boot API: http://localhost:8080
   - Angular App: http://localhost:4200
   - PostgreSQL: localhost:5432
   - Redis: localhost:6379
   - MinIO Console: http://localhost:9001

### Configuration Needed

**Backend `application-dev.properties`:**
```properties
# Server Configuration
server.port=8080
spring.application.name=family-tree-manager

# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/family_tree_dev
spring.datasource.username=familytree
spring.datasource.password=familytree_password
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.properties.hibernate.format_sql=true

# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.cache.type=redis
spring.cache.redis.time-to-live=3600000

# Session Configuration
spring.session.store-type=redis
spring.session.redis.namespace=spring:session

# MinIO Configuration
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin123
minio.bucket-name=family-tree-media

# JWT Configuration
jwt.secret=your-secret-key-change-in-production-min-256-bits
jwt.expiration=604800000

# File Upload Configuration
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=5MB

# Logging
logging.level.com.familytree=DEBUG
logging.level.org.springframework.security=DEBUG
```

**Frontend `environment.development.ts`:**
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  minioUrl: 'http://localhost:9000'
};
```

## Code Structure
**How is the code organized?**

### Directory Structure

```
family-tree-manager/
├── backend/                  # Spring Boot Application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── familytree/
│   │   │   │           ├── FamilyTreeApplication.java  # Main Spring Boot class
│   │   │   │           ├── config/                     # Configuration classes
│   │   │   │           │   ├── SecurityConfig.java
│   │   │   │           │   ├── RedisConfig.java
│   │   │   │           │   ├── MinioConfig.java
│   │   │   │           │   └── WebConfig.java
│   │   │   │           ├── controller/                 # REST Controllers
│   │   │   │           │   ├── AuthController.java
│   │   │   │           │   ├── TreeController.java
│   │   │   │           │   ├── IndividualController.java
│   │   │   │           │   ├── RelationshipController.java
│   │   │   │           │   ├── MediaController.java
│   │   │   │           │   └── GedcomController.java
│   │   │   │           ├── service/                    # Business logic
│   │   │   │           │   ├── AuthService.java
│   │   │   │           │   ├── TreeService.java
│   │   │   │           │   ├── IndividualService.java
│   │   │   │           │   ├── RelationshipService.java
│   │   │   │           │   ├── MediaService.java
│   │   │   │           │   ├── GedcomService.java
│   │   │   │           │   └── SearchService.java
│   │   │   │           ├── repository/                 # Spring Data JPA Repositories
│   │   │   │           │   ├── UserRepository.java
│   │   │   │           │   ├── TreeRepository.java
│   │   │   │           │   ├── IndividualRepository.java
│   │   │   │           │   ├── RelationshipRepository.java
│   │   │   │           │   ├── MediaRepository.java
│   │   │   │           │   └── TreePermissionRepository.java
│   │   │   │           ├── model/                      # JPA Entities
│   │   │   │           │   ├── User.java
│   │   │   │           │   ├── FamilyTree.java
│   │   │   │           │   ├── Individual.java
│   │   │   │           │   ├── Relationship.java
│   │   │   │           │   ├── Media.java
│   │   │   │           │   ├── Event.java
│   │   │   │           │   └── TreePermission.java
│   │   │   │           ├── dto/                        # Data Transfer Objects
│   │   │   │           │   ├── request/
│   │   │   │           │   │   ├── LoginRequest.java
│   │   │   │           │   │   ├── RegisterRequest.java
│   │   │   │           │   │   ├── CreateTreeRequest.java
│   │   │   │           │   │   ├── CreateIndividualRequest.java
│   │   │   │           │   │   └── CreateRelationshipRequest.java
│   │   │   │           │   └── response/
│   │   │   │           │       ├── AuthResponse.java
│   │   │   │           │       ├── TreeResponse.java
│   │   │   │           │       ├── IndividualResponse.java
│   │   │   │           │       └── ErrorResponse.java
│   │   │   │           ├── security/                   # Security components
│   │   │   │           │   ├── JwtTokenProvider.java
│   │   │   │           │   ├── JwtAuthenticationFilter.java
│   │   │   │           │   └── UserDetailsServiceImpl.java
│   │   │   │           ├── exception/                  # Exception handling
│   │   │   │           │   ├── GlobalExceptionHandler.java
│   │   │   │           │   ├── ResourceNotFoundException.java
│   │   │   │           │   └── UnauthorizedException.java
│   │   │   │           └── util/                       # Utility classes
│   │   │   │               ├── ValidationUtils.java
│   │   │   │               └── DateUtils.java
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── application-dev.properties
│   │   │       ├── application-prod.properties
│   │   │       └── db/
│   │   │           └── migration/                      # Flyway migrations (optional)
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── familytree/
│   │                   ├── service/                    # Service tests
│   │                   ├── controller/                 # Controller tests
│   │                   └── repository/                 # Repository tests
│   ├── pom.xml                                         # Maven dependencies
│   ├── mvnw                                            # Maven wrapper
│   └── mvnw.cmd
│
├── frontend/                 # Angular Application
│   ├── src/
│   │   ├── app/
│   │   │   ├── core/                                   # Core module (singletons)
│   │   │   │   ├── guards/
│   │   │   │   │   └── auth.guard.ts
│   │   │   │   ├── interceptors/
│   │   │   │   │   ├── auth.interceptor.ts
│   │   │   │   │   └── error.interceptor.ts
│   │   │   │   └── services/
│   │   │   │       ├── auth.service.ts
│   │   │   │       └── storage.service.ts
│   │   │   ├── shared/                                 # Shared module (reusables)
│   │   │   │   ├── components/
│   │   │   │   │   ├── button/
│   │   │   │   │   ├── input/
│   │   │   │   │   └── modal/
│   │   │   │   ├── directives/
│   │   │   │   ├── pipes/
│   │   │   │   └── models/
│   │   │   │       └── index.ts
│   │   │   ├── features/                               # Feature modules
│   │   │   │   ├── auth/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── login/
│   │   │   │   │   │   └── register/
│   │   │   │   │   ├── auth.module.ts
│   │   │   │   │   └── auth-routing.module.ts
│   │   │   │   ├── dashboard/
│   │   │   │   │   ├── dashboard.component.ts
│   │   │   │   │   └── dashboard.module.ts
│   │   │   │   ├── tree/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── tree-list/
│   │   │   │   │   │   ├── tree-view/
│   │   │   │   │   │   └── tree-visualization/
│   │   │   │   │   ├── services/
│   │   │   │   │   │   └── tree.service.ts
│   │   │   │   │   ├── tree.module.ts
│   │   │   │   │   └── tree-routing.module.ts
│   │   │   │   ├── individual/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── individual-detail/
│   │   │   │   │   │   ├── individual-form/
│   │   │   │   │   │   └── individual-card/
│   │   │   │   │   ├── services/
│   │   │   │   │   │   └── individual.service.ts
│   │   │   │   │   └── individual.module.ts
│   │   │   │   └── media/
│   │   │   │       ├── components/
│   │   │   │       │   ├── media-gallery/
│   │   │   │       │   └── media-uploader/
│   │   │   │       └── media.module.ts
│   │   │   ├── layout/
│   │   │   │   ├── header/
│   │   │   │   ├── sidebar/
│   │   │   │   └── footer/
│   │   │   ├── app.component.ts
│   │   │   ├── app.module.ts
│   │   │   └── app-routing.module.ts
│   │   ├── assets/                                     # Static assets
│   │   │   ├── images/
│   │   │   └── styles/
│   │   ├── environments/
│   │   │   ├── environment.ts
│   │   │   └── environment.development.ts
│   │   ├── index.html
│   │   ├── main.ts
│   │   └── styles.scss
│   ├── angular.json
│   ├── package.json
│   ├── tsconfig.json
│   └── tsconfig.app.json
│
├── nginx/                    # Nginx configuration
│   ├── nginx.conf
│   └── default.conf
│
├── docker-compose.yml
├── .gitignore
└── README.md
```

### Module Organization

**Backend:**
- **Routes**: Define API endpoints, minimal logic
- **Services**: Business logic, database operations
- **Middleware**: Request processing, auth, validation
- **Models**: Data structures and DTOs
- **Utils**: Shared utility functions

**Frontend:**
- **Pages**: Top-level route components
- **Components**: Reusable UI components organized by domain
- **Hooks**: Custom React hooks for shared logic
- **Services**: API communication layer
- **Store**: Global state management

### Naming Conventions

**Files:**
- Components: `PascalCase.tsx` (e.g., `TreeVisualization.tsx`)
- Services: `camelCase.service.ts` (e.g., `auth.service.ts`)
- Utilities: `camelCase.ts` (e.g., `dateUtils.ts`)
- Types: `PascalCase.ts` or `index.ts`

**Code:**
- Components: `PascalCase`
- Functions: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Interfaces/Types: `PascalCase` with `I` prefix for interfaces (optional)

## Implementation Notes
**Key technical details to remember:**

### Core Features

#### Feature 1: Authentication & Authorization
**Approach:**
- Use JWT (JSON Web Tokens) for stateless authentication
- Store tokens in HTTP-only cookies (secure) or localStorage (convenient)
- Implement refresh token mechanism for long-lived sessions
- Hash passwords with bcrypt (10 rounds)

**Key Implementation Points:**
```typescript
// Middleware to verify JWT
export const authenticateJWT = (req: Request, res: Response, next: NextFunction) => {
  const token = req.cookies.token || req.headers.authorization?.split(' ')[1];
  if (!token) return res.status(401).json({ error: 'Unauthorized' });

  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET!);
    req.user = payload;
    next();
  } catch (error) {
    return res.status(401).json({ error: 'Invalid token' });
  }
};

// Middleware to check tree permissions
export const checkTreePermission = (requiredRole: 'viewer' | 'editor' | 'owner') => {
  return async (req: Request, res: Response, next: NextFunction) => {
    const treeId = req.params.treeId || req.body.treeId;
    const userId = req.user.id;

    const permission = await prisma.treePermission.findFirst({
      where: { treeId, userId }
    });

    if (!permission || !hasPermission(permission.role, requiredRole)) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    next();
  };
};
```

#### Feature 2: Relationship Management
**Approach:**
- Store relationships as directed edges (individual1 -> individual2)
- Use relationship type to determine directionality
- Implement validation to prevent circular relationships (person cannot be their own ancestor)

**Key Implementation Points:**
```typescript
// Validate no circular relationships
async function validateNoCircularRelationship(
  individual1Id: string,
  individual2Id: string,
  type: string
): Promise<boolean> {
  if (type === 'parent-child') {
    // Check if individual2 is already an ancestor of individual1
    const ancestors = await getAncestors(individual1Id);
    return !ancestors.some(a => a.id === individual2Id);
  }
  return true;
}

// Get all ancestors using recursive query
async function getAncestors(individualId: string) {
  return prisma.$queryRaw`
    WITH RECURSIVE ancestors AS (
      SELECT i.*, r.individual1_id as parent_id
      FROM individuals i
      LEFT JOIN relationships r ON i.id = r.individual2_id AND r.type = 'parent-child'
      WHERE i.id = ${individualId}

      UNION ALL

      SELECT i.*, r.individual1_id as parent_id
      FROM individuals i
      JOIN relationships r ON i.id = r.individual2_id AND r.type = 'parent-child'
      JOIN ancestors a ON r.individual2_id = a.parent_id
    )
    SELECT * FROM ancestors WHERE id != ${individualId}
  `;
}
```

#### Feature 3: Tree Visualization
**Approach:**
- Use D3.js for flexible, data-driven visualization
- Implement hierarchical tree layout (d3.tree() or d3.cluster())
- Render individuals as SVG nodes with hover/click interactions
- Optimize rendering with virtualization for large trees

**Key Implementation Points:**
```typescript
// Example D3.js tree rendering
import * as d3 from 'd3';

interface TreeNode {
  id: string;
  name: string;
  children?: TreeNode[];
}

function renderTree(data: TreeNode, containerId: string) {
  const width = 800;
  const height = 600;

  const svg = d3.select(`#${containerId}`)
    .append('svg')
    .attr('width', width)
    .attr('height', height);

  const treeLayout = d3.tree<TreeNode>()
    .size([width - 100, height - 100]);

  const root = d3.hierarchy(data);
  const treeData = treeLayout(root);

  // Draw links
  svg.selectAll('.link')
    .data(treeData.links())
    .enter()
    .append('path')
    .attr('class', 'link')
    .attr('d', d3.linkVertical()
      .x((d: any) => d.x)
      .y((d: any) => d.y)
    );

  // Draw nodes
  const nodes = svg.selectAll('.node')
    .data(treeData.descendants())
    .enter()
    .append('g')
    .attr('class', 'node')
    .attr('transform', d => `translate(${d.x},${d.y})`);

  nodes.append('circle')
    .attr('r', 5)
    .on('click', (event, d) => handleNodeClick(d.data.id));

  nodes.append('text')
    .text(d => d.data.name)
    .attr('dy', -10);
}
```

#### Feature 4: GEDCOM Import/Export
**Approach:**
- Use existing GEDCOM parsing library (e.g., `parse-gedcom`, `gedcom.js`)
- Map GEDCOM tags to database models
- Handle common GEDCOM variations and errors gracefully

**Key GEDCOM Mappings:**
- `0 @I1@ INDI` → Individual record
- `1 NAME John /Smith/` → Given name: John, Surname: Smith
- `1 SEX M` → Gender: male
- `1 BIRT` / `2 DATE` / `2 PLAC` → Birth event
- `1 FAMS @F1@` → Spouse in family F1
- `1 FAMC @F2@` → Child in family F2
- `0 @F1@ FAM` → Family record
- `1 HUSB @I1@` → Husband reference
- `1 WIFE @I2@` → Wife reference
- `1 CHIL @I3@` → Child reference

**Key Implementation Points:**
```typescript
import { parseGedcom } from 'parse-gedcom';

async function importGedcom(filePath: string, treeId: string) {
  const gedcomData = await fs.readFile(filePath, 'utf-8');
  const parsed = parseGedcom(gedcomData);

  const individualMap = new Map();

  // First pass: Create all individuals
  for (const record of parsed.filter(r => r.tag === 'INDI')) {
    const individual = {
      treeId,
      givenName: extractGivenName(record),
      surname: extractSurname(record),
      gender: extractGender(record),
      birthDate: extractBirthDate(record),
      birthPlace: extractBirthPlace(record),
      // ... etc
    };

    const created = await prisma.individual.create({ data: individual });
    individualMap.set(record.pointer, created.id);
  }

  // Second pass: Create relationships
  for (const record of parsed.filter(r => r.tag === 'FAM')) {
    const husbandId = individualMap.get(extractHusband(record));
    const wifeId = individualMap.get(extractWife(record));
    const childIds = extractChildren(record).map(c => individualMap.get(c));

    // Create spouse relationship
    if (husbandId && wifeId) {
      await prisma.relationship.create({
        data: {
          treeId,
          individual1Id: husbandId,
          individual2Id: wifeId,
          type: 'spouse'
        }
      });
    }

    // Create parent-child relationships
    for (const childId of childIds) {
      if (husbandId) {
        await prisma.relationship.create({
          data: { treeId, individual1Id: husbandId, individual2Id: childId, type: 'parent-child' }
        });
      }
      if (wifeId) {
        await prisma.relationship.create({
          data: { treeId, individual1Id: wifeId, individual2Id: childId, type: 'parent-child' }
        });
      }
    }
  }
}
```

### Patterns & Best Practices

**1. Service Layer Pattern:**
- All business logic in services, not controllers
- Services are testable, reusable, and composable

**2. DTO (Data Transfer Objects):**
- Define explicit types for API requests/responses
- Validate with Zod schemas

```typescript
import { z } from 'zod';

export const createIndividualSchema = z.object({
  treeId: z.string().uuid(),
  givenName: z.string().min(1).max(100),
  surname: z.string().min(1).max(100),
  birthDate: z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
  gender: z.enum(['male', 'female', 'other', 'unknown']).optional(),
});

export type CreateIndividualDTO = z.infer<typeof createIndividualSchema>;
```

**3. Repository Pattern (via Prisma):**
- Use Prisma client as abstraction over database
- Encapsulate complex queries in service methods

**4. Error Handling:**
- Use custom error classes
- Centralized error handler middleware

```typescript
export class AppError extends Error {
  constructor(
    public statusCode: number,
    public message: string,
    public isOperational = true
  ) {
    super(message);
  }
}

export const errorHandler = (err: Error, req: Request, res: Response, next: NextFunction) => {
  if (err instanceof AppError) {
    return res.status(err.statusCode).json({ error: err.message });
  }

  console.error(err);
  return res.status(500).json({ error: 'Internal server error' });
};
```

## Integration Points
**How do pieces connect?**

### API Integration Details
- **Base URL**: `http://localhost:3000` (dev) or configured production URL
- **Authentication**: JWT token in Authorization header: `Bearer <token>`
- **Content Type**: `application/json` for most endpoints, `multipart/form-data` for file uploads

### Database Connections
- **Connection String**: Defined in `DATABASE_URL` environment variable
- **Connection Pooling**: Managed by Prisma (default pool size: 10)
- **Migrations**: Run with `npx prisma migrate dev`

### Third-Party Service Setup
- **Storage**: Local filesystem (uploads directory) or S3 (configure AWS credentials)
- **Email** (future): SendGrid, Mailgun, or SMTP

## Error Handling
**How do we handle failures?**

### Error Handling Strategy
- **Validation Errors**: Return 400 with descriptive message
- **Authentication Errors**: Return 401
- **Authorization Errors**: Return 403
- **Not Found**: Return 404
- **Server Errors**: Return 500, log error details

### Logging Approach
- Use Winston or Pino for structured logging
- Log levels: error, warn, info, debug
- Log format: JSON with timestamp, level, message, context

```typescript
import winston from 'winston';

export const logger = winston.createLogger({
  level: process.env.LOG_LEVEL || 'info',
  format: winston.format.json(),
  transports: [
    new winston.transports.File({ filename: 'error.log', level: 'error' }),
    new winston.transports.File({ filename: 'combined.log' }),
  ],
});

if (process.env.NODE_ENV !== 'production') {
  logger.add(new winston.transports.Console({
    format: winston.format.simple(),
  }));
}
```

### Retry/Fallback Mechanisms
- **Database**: Prisma has built-in retry logic
- **File Uploads**: Client-side retry with exponential backoff
- **GEDCOM Import**: Fail gracefully, provide detailed error report

## Performance Considerations
**How do we keep it fast?**

### Optimization Strategies
1. **Database Indexing**: Add indexes on frequently queried fields
   ```sql
   CREATE INDEX idx_individuals_tree_id ON individuals(tree_id);
   CREATE INDEX idx_relationships_individuals ON relationships(individual1_id, individual2_id);
   ```

2. **Query Optimization**: Use Prisma's `select` and `include` to fetch only needed data

3. **Pagination**: Implement cursor-based pagination for large lists

4. **Lazy Loading**: Load tree visualization data on-demand

### Caching Approach
- **Tree Data**: Cache frequently accessed trees in Redis (future)
- **Static Assets**: Use CDN for media files
- **API Responses**: Cache GET requests with short TTL

### Query Optimization
```typescript
// Efficient: Only fetch needed fields
const individuals = await prisma.individual.findMany({
  where: { treeId },
  select: {
    id: true,
    givenName: true,
    surname: true,
    birthDate: true,
  },
});

// Avoid N+1 queries: Use include for relationships
const tree = await prisma.familyTree.findUnique({
  where: { id: treeId },
  include: {
    individuals: {
      include: {
        relationships: true,
      },
    },
  },
});
```

### Resource Management
- **Connection Pooling**: Configure Prisma pool size based on expected load
- **File Upload Limits**: Enforce max file size (5MB)
- **Rate Limiting**: Implement with express-rate-limit

## Security Notes
**What security measures are in place?**

### Authentication/Authorization
- **Password Hashing**: bcrypt with 10 rounds
- **JWT**: HS256 algorithm, short expiration (7 days)
- **Permission Checks**: Every tree operation checks user permission

### Input Validation
- **Schema Validation**: Use Zod to validate all inputs
- **Sanitization**: Escape HTML in user-generated content

### Data Encryption
- **In Transit**: HTTPS in production (TLS 1.2+)
- **At Rest**: Database encryption (platform-dependent)

### Secrets Management
- **Environment Variables**: Never commit secrets to Git
- **Production**: Use secret management service (AWS Secrets Manager, etc.)

### Security Headers
```typescript
import helmet from 'helmet';
app.use(helmet());
```

### File Upload Security
- **Validation**: Check file type (magic numbers, not just extension)
- **Size Limits**: Max 5MB per file
- **Storage**: Store outside web root, serve through API
- **Malware Scanning**: (Future) Integrate ClamAV or similar

### SQL Injection Prevention
- **Parameterized Queries**: Prisma handles this automatically
- **No Raw SQL**: Avoid `$executeRaw` unless absolutely necessary

### XSS Prevention
- **Output Encoding**: React handles this automatically for JSX
- **CSP Headers**: Set Content-Security-Policy headers
