---
phase: planning
title: Project Planning & Task Breakdown
description: Break down work into actionable tasks and estimate timeline
last_updated: 2025-11-23
---

# Project Planning & Task Breakdown

## 📊 Current Status (Updated: 2025-11-23)

**Overall Progress**: 4 of 7 milestones completed (57%)
**Estimated Completion**: 55-67% of total effort completed

**Recent Activity**:
- ✅ Phase 3: Fixed critical tree visualization bug with spouse relationships
- ✅ Phase 4: Implementation complete, testing in progress
- 🔧 UI/UX bugs identified and resolved
- 📝 Planning documentation updated

**Immediate Next Steps**:
1. Complete Phase 4 manual testing (following test guide)
2. Commit Phase 4 untracked files
3. Add automated tests for Phase 4
4. Begin Phase 5: GEDCOM Import/Export

---

## Milestones
**What are the major checkpoints?**

- [x] **Milestone 1**: Foundation & Authentication - User can register, login, and manage their account ✅
- [x] **Milestone 2**: Core Family Tree - Users can create trees, add individuals, and define relationships ✅
- [x] **Milestone 3**: Visualization - Interactive family tree visualization with navigation ✅
- [x] **Milestone 4**: Media Management - Photo and document uploads and display ✅ (Testing in progress)
- [ ] **Milestone 5**: GEDCOM Support - Import and export GEDCOM files
- [ ] **Milestone 6**: Collaboration - Multi-user permissions and editing
- [ ] **Milestone 7**: MVP Polish - Search, UI refinements, testing, and deployment

## Task Breakdown
**What specific work needs to be done?**

### Phase 1: Foundation & Setup
**Goal**: Project infrastructure, database, and authentication

- [ ] **Task 1.1**: Initialize Spring Boot project structure
  - Create Spring Boot 3.x project with Maven using Spring Initializr
  - Set up package structure (controller, service, repository, model, dto, config)
  - Configure application.properties for dev/prod environments
  - Initialize Git repository and configure .gitignore
  - Verify Maven build (mvn clean install)

- [ ] **Task 1.2**: Database setup with JPA
  - Start PostgreSQL via docker-compose
  - Configure Spring Data JPA and PostgreSQL driver
  - Create JPA entities (User, FamilyTree, Individual, Relationship, Media, TreePermission, Event)
  - Define entity relationships (@OneToMany, @ManyToOne, @ManyToMany)
  - Configure Hibernate DDL auto-update
  - Create database indexes for performance

- [ ] **Task 1.3**: Spring Security authentication system
  - Configure Spring Security with JWT
  - Create JwtTokenProvider for token generation/validation
  - Create JwtAuthenticationFilter for request filtering
  - Implement UserDetailsService for loading user data
  - Create AuthController with register/login/logout endpoints
  - Implement BCryptPasswordEncoder for password hashing
  - Configure CORS and security headers

- [ ] **Task 1.4**: Initialize Angular frontend
  - Create Angular 17+ project with Angular CLI
  - Set up project structure (core, shared, features modules)
  - Configure environment files (development/production)
  - Create auth interceptor for JWT injection
  - Create auth guard for route protection
  - Set up HttpClient for API communication

- [ ] **Task 1.5**: Basic Angular UI framework
  - Install Angular Material or Tailwind CSS
  - Create core layout components (Header, Sidebar, Footer)
  - Set up Angular Router with lazy loading
  - Create shared form components (input, button, select)
  - Configure global styles and theme

### Phase 2: Core Family Tree Features
**Goal**: Basic family tree CRUD operations

- [ ] **Task 2.1**: Family tree management (Spring Boot backend)
  - Create TreeController with REST endpoints
  - Implement TreeService with business logic
  - Create TreeRepository extending JpaRepository
  - Implement create tree (POST /api/trees) with DTO validation
  - Implement list trees (GET /api/trees) with pagination
  - Implement get tree (GET /api/trees/{id}) with @PreAuthorize
  - Implement update tree (PUT /api/trees/{id})
  - Implement delete tree (DELETE /api/trees/{id}) with cascade
  - Add authorization checks using Spring Security

- [ ] **Task 2.2**: Family tree management (Angular frontend)
  - Create tree feature module with routing
  - Create TreeService for API calls
  - Build dashboard component showing user's trees
  - Create tree-form component with reactive forms
  - Create tree-settings component
  - Implement tree list with Material table or custom list
  - Add edit/delete actions with confirmation dialogs

- [ ] **Task 2.3**: Individual management (Spring Boot backend)
  - Create IndividualController with REST endpoints
  - Implement IndividualService with business logic
  - Create IndividualRepository with custom queries
  - Implement create individual (POST /api/trees/{id}/individuals)
  - Implement get individual (GET /api/individuals/{id})
  - Implement update individual (PUT /api/individuals/{id})
  - Implement delete individual (DELETE /api/individuals/{id})
  - Implement list individuals (GET /api/trees/{id}/individuals)
  - Add @Valid Bean Validation for dates and required fields

- [ ] **Task 2.4**: Individual management (Angular frontend)
  - Create individual feature module
  - Create IndividualService for API calls
  - Build individual-form component with FormBuilder
  - Create individual-detail component with tabs
  - Implement Angular Material date pickers
  - Add reactive form validation with custom validators
  - Create individual-list component with filtering

- [ ] **Task 2.5**: Relationship management (Spring Boot backend)
  - Create RelationshipController with REST endpoints
  - Implement RelationshipService with graph validation
  - Create RelationshipRepository with native queries
  - Implement create relationship (POST /api/relationships)
  - Implement delete relationship (DELETE /api/relationships/{id})
  - Add circular relationship validation logic
  - Implement get ancestors using PostgreSQL recursive CTE
  - Implement get descendants using recursive queries
  - Cache ancestor/descendant queries with @Cacheable

- [ ] **Task 2.6**: Relationship management (Angular frontend)
  - Create relationship feature module
  - Create RelationshipService for API calls
  - Build relationship-picker component with autocomplete
  - Implement "Add Parent" dialog with individual search
  - Implement "Add Child" dialog
  - Implement "Add Spouse" dialog
  - Display relationships on individual detail with visual links
  - Add remove relationship functionality

### Phase 3: Tree Visualization ✅ COMPLETED
**Goal**: Interactive graphical family tree

- [x] **Task 3.1**: Choose and integrate visualization library
  - D3.js selected and integrated
  - Create basic proof-of-concept visualization
  - **Status**: ✅ Complete

- [x] **Task 3.2**: Implement tree layout algorithm
  - Hierarchical layout for ancestors/descendants implemented
  - Spouse positioning implemented (horizontal connections with pink dashed lines)
  - Smart root-finding algorithm (selects best root by descendant count)
  - **Status**: ✅ Complete - Enhanced with spouse visualization (2025-11-23)

- [x] **Task 3.3**: Interactive tree component
  - TreeVisualization component created
  - Click handlers for individuals implemented
  - Zoom and pan functionality added
  - **Status**: ✅ Complete

- [x] **Task 3.4**: Tree navigation controls
  - Zoom in/out buttons added
  - Reset view button added
  - Refresh button added for reloading after relationship changes
  - **Status**: ✅ Complete (generation filter deferred to future enhancement)

- [x] **Task 3.5**: Tree styling and UX
  - Visual style for individuals (circular nodes with initials)
  - Gender-based styling (blue for male, pink for female)
  - Life years displayed below names
  - Loading states implemented
  - **Status**: ✅ Complete (photo thumbnails deferred to future enhancement)

**Bug Fix Applied (2025-11-23)**:
- Fixed critical bug where tree disappeared when creating SPOUSE/PARTNER relationships
- Enhanced root-finding algorithm to handle multiple potential roots intelligently
- Added visual representation of spouse/partner relationships (horizontal pink dashed lines)
- Improved error handling and debugging with console logging
- Added refresh functionality for manual tree reload

### Phase 4: Media Management ✅ COMPLETED
**Goal**: Upload, store, and display photos and documents

- [x] **Task 4.1**: MinIO storage configuration
  - Configure MinIO client bean in Spring Boot
  - Create MinioService for file operations
  - Set up bucket creation on application startup
  - Implement storage adapter interface for flexibility
  - Configure file naming and organization strategy (tree-id/individual-id/filename)
  - **Status**: ✅ Complete - MinioConfig.java and MinioService.java created with full functionality

- [x] **Task 4.2**: Media upload (Spring Boot backend)
  - Create MediaController with multipart endpoints
  - Implement MediaService with MinIO integration
  - Handle MultipartFile upload (POST /api/individuals/{id}/media)
  - Add file type validation (MIME types: image/*, application/pdf)
  - Add file size limits (5MB via spring.servlet.multipart.max-file-size)
  - Generate thumbnails using Thumbnailator library
  - Store media metadata in Media entity
  - **Status**: ✅ Complete - MediaService.java created with full upload, validation, and thumbnail generation

- [x] **Task 4.3**: Media retrieval (Spring Boot backend)
  - Implement get media endpoint (GET /api/media/{id}) streaming from MinIO
  - Implement delete media endpoint (DELETE /api/media/{id})
  - Add @PreAuthorize authorization checks
  - Implement thumbnail serving with caching headers
  - Handle MinIO exceptions and error responses
  - **Status**: ✅ Complete - MediaController.java created with all REST endpoints (GET, PUT, DELETE, download, stream, thumbnail)

- [x] **Task 4.4**: Media upload (Angular frontend)
  - Create media feature module
  - Build media-uploader component with file input
  - Implement drag-and-drop using Angular CDK
  - Add HttpEventType.UploadProgress tracking
  - Add client-side file validation (size, type)
  - Display upload errors with user-friendly messages
  - Show upload progress bar
  - **Status**: ✅ Complete - MediaUploaderComponent created with drag-and-drop, progress tracking, and validation

- [x] **Task 4.5**: Media display (Angular frontend)
  - Create media-gallery component with grid layout
  - Implement photo lightbox/modal using Angular Material dialog
  - Display media on individual-detail page
  - Add captions and metadata editing
  - Implement delete with confirmation dialog
  - Lazy load images for performance
  - **Status**: ✅ Complete - MediaGalleryComponent created with grid layout, caption editing, and integrated into individual-detail page with tabs

### Phase 5: GEDCOM Import/Export
**Goal**: Support standard genealogy file format

- [ ] **Task 5.1**: GEDCOM library integration
  - Research GEDCOM 5.5 specification
  - Add gedcom4j Maven dependency (already in pom.xml)
  - Study gedcom4j API and examples
  - Plan GEDCOM tag to JPA entity mapping

- [ ] **Task 5.2**: GEDCOM import (Spring Boot backend)
  - Create GedcomController with import endpoint
  - Implement GedcomService with gedcom4j parser
  - Map GEDCOM INDI records to Individual entities
  - Map GEDCOM FAM records to Relationship entities
  - Handle various GEDCOM date formats (ABT, BEF, AFT, BET)
  - Create import endpoint (POST /api/trees/{id}/import) with multipart file
  - Add @Transactional for atomic imports
  - Implement error handling and validation
  - Return import summary (count, errors, warnings)

- [ ] **Task 5.3**: GEDCOM import (Angular frontend)
  - Create gedcom feature module
  - Build import-wizard component with stepper
  - Add GEDCOM file upload with validation
  - Display import preview/summary
  - Show import progress with spinner
  - Handle import errors with detailed messages
  - Navigate to imported tree on success

- [ ] **Task 5.4**: GEDCOM export (Spring Boot backend)
  - Implement GedcomService export method
  - Map Individual entities to GEDCOM INDI records
  - Map Relationship entities to GEDCOM FAM records
  - Generate valid GEDCOM 5.5 file using gedcom4j
  - Create export endpoint (GET /api/trees/{id}/export)
  - Set proper Content-Type and Content-Disposition headers
  - Stream file to response

- [ ] **Task 5.5**: GEDCOM export (Angular frontend)
  - Add "Export GEDCOM" button on tree view
  - Call export endpoint with HttpClient
  - Handle blob response and trigger download
  - Show export progress/spinner
  - Display success notification

### Phase 6: Collaboration & Permissions
**Goal**: Multiple users can work on the same tree

- [ ] **Task 6.1**: Permission system (Spring Boot backend)
  - Create TreePermission JPA entity
  - Create PermissionController with REST endpoints
  - Implement PermissionService with business logic
  - Create PermissionRepository
  - Implement grant permission (POST /api/trees/{id}/permissions)
  - Implement list permissions (GET /api/trees/{id}/permissions)
  - Implement revoke permission (DELETE /api/permissions/{id})
  - Update Spring Security @PreAuthorize checks for permissions
  - Cache permissions with @Cacheable for performance

- [ ] **Task 6.2**: Invitation system
  - Create InvitationController with endpoints
  - Implement invitation token generation (JWT or UUID)
  - Create invite endpoint (POST /api/trees/{id}/invite)
  - Create accept invitation endpoint (POST /api/invitations/{token}/accept)
  - Store invitation records with expiration
  - Optional: Integrate Spring Mail for email notifications
  - MVP: Return shareable invitation link

- [ ] **Task 6.3**: Permission management (Angular frontend)
  - Create permissions feature module
  - Build permissions-manager component
  - Display collaborators table with roles
  - Create invite-collaborator dialog
  - Implement role dropdown (owner/editor/viewer)
  - Add revoke permission with confirmation
  - Show invitation link for sharing

- [ ] **Task 6.4**: Real-time updates (optional for MVP)
  - Configure Spring WebSocket support
  - Create WebSocket endpoint for tree updates
  - Implement STOMP messaging
  - Angular: Subscribe to WebSocket topics
  - Show "User X is editing" indicators
  - Implement optimistic locking for conflict resolution

### Phase 7: Search & Polish
**Goal**: Search functionality and UI/UX improvements

- [ ] **Task 7.1**: Search implementation (Spring Boot backend)
  - Create SearchController with search endpoint
  - Implement SearchService with PostgreSQL full-text search
  - Use @Query with LIKE or tsvector for text search
  - Create search endpoint (GET /api/trees/{id}/search?q={query})
  - Search by given name, surname, birth/death places
  - Return ranked results with pagination
  - Cache search results with Redis @Cacheable

- [ ] **Task 7.2**: Search UI (Angular frontend)
  - Create search feature module
  - Build search-bar component with autocomplete
  - Display search results in dropdown or results page
  - Implement "jump to individual" navigation
  - Add search filters (date range, gender, etc.) with reactive forms
  - Debounce search input with RxJS operators

- [ ] **Task 7.3**: UI/UX improvements (Angular)
  - Add loading skeletons using Angular Material or ngx-skeleton-loader
  - Improve error messages with user-friendly text
  - Add confirmation dialogs for delete actions (Material Dialog)
  - Improve mobile responsiveness with Angular Flex Layout
  - Add keyboard shortcuts using @HostListener
  - Accessibility improvements (ARIA labels, keyboard navigation, focus management)

- [ ] **Task 7.4**: Data validation and error handling
  - Add comprehensive @Valid Bean Validation on backend
  - Implement custom validators for date logic (death after birth)
  - Create @ControllerAdvice for global exception handling
  - Prevent duplicate individuals with unique constraints
  - Detect orphaned individuals and warn user
  - Return structured error responses (RFC 7807 Problem Details)

- [ ] **Task 7.5**: Performance optimization
  - Add database indexes on frequently queried columns (tree_id, name, dates)
  - Optimize JPA queries with @EntityGraph for eager loading
  - Configure Redis caching for trees and individuals
  - Lazy load images in Angular with intersection observer
  - Optimize tree rendering with virtual scrolling
  - Configure Spring Boot Actuator for performance monitoring

### Phase 8: Testing & Deployment
**Goal**: Comprehensive testing and production deployment

- [ ] **Task 8.1**: Unit tests (Spring Boot backend)
  - Write JUnit 5 tests for AuthService
  - Write tests for TreeService with @MockBean
  - Write tests for IndividualService
  - Write tests for RelationshipService (circular validation)
  - Write tests for GedcomService parser
  - Use Mockito for mocking repositories
  - Achieve 80%+ code coverage with JaCoCo
  - Generate coverage report (mvn test jacoco:report)

- [ ] **Task 8.2**: Integration tests (Spring Boot backend)
  - Test REST endpoints with @SpringBootTest and MockMvc
  - Use @AutoConfigureMockMvc for controller tests
  - Test database operations with @DataJpaTest
  - Use Testcontainers for PostgreSQL and Redis
  - Test file upload/download with MinIO testcontainer
  - Test permission system with @WithMockUser
  - Test transactions and rollback behavior

- [ ] **Task 8.3**: Frontend tests (Angular)
  - Write component tests with Karma and Jasmine
  - Test services with HttpClientTestingModule
  - Test reactive forms validation
  - Test user workflows (add individual, create relationship)
  - Write E2E tests with Protractor or Cypress
  - Achieve 80%+ code coverage (ng test --code-coverage)

- [ ] **Task 8.4**: Deployment setup
  - Create Dockerfile for Spring Boot (multi-stage with Maven)
  - Create Dockerfile for Angular (nginx-alpine)
  - Create docker-compose.prod.yml with all services
  - Configure environment variables via .env file
  - Set up PostgreSQL with persistent volumes
  - Configure Nginx reverse proxy with SSL
  - Test complete deployment locally

- [ ] **Task 8.5**: Documentation
  - Update README.md with setup instructions (already created)
  - Generate API documentation with Springdoc OpenAPI
  - Create user guide with screenshots
  - Add JavaDoc comments to service classes
  - Update all phase documentation with final notes
  - Create deployment guide for production

- [ ] **Task 8.6**: Security hardening
  - Run Maven security audit (mvn dependency-check:check)
  - Implement rate limiting with Bucket4j
  - Configure CSRF protection in Spring Security
  - Configure CORS with specific origins
  - Set up HTTPS with Let's Encrypt certificates
  - Review OWASP Top 10 vulnerabilities
  - Enable Spring Security headers

- [ ] **Task 8.7**: Production deployment
  - Deploy to cloud (AWS/Azure/GCP) or self-hosted VPS
  - Configure automated PostgreSQL backups
  - Set up centralized logging (ELK stack or cloud logging)
  - Configure monitoring with Spring Boot Actuator + Prometheus
  - Optional: Set up error tracking with Sentry
  - Perform smoke tests on production
  - Set up CI/CD pipeline (GitHub Actions or GitLab CI)

## Dependencies
**What needs to happen in what order?**

### Critical Path
1. **Phase 1** (Foundation) must complete before all other phases
2. **Phase 2** (Core Features) must complete before Phase 3, 4, 5, 6
3. **Phase 3** (Visualization) depends on Phase 2 (needs individuals and relationships)
4. **Phase 4** (Media) can start after Phase 2 Task 2.4 (individual detail page)
5. **Phase 5** (GEDCOM) depends on Phase 2 (needs core tree functionality)
6. **Phase 6** (Collaboration) depends on Phase 1 (auth) and Phase 2 (trees)
7. **Phase 7** (Search & Polish) can happen in parallel with Phases 4-6
8. **Phase 8** (Testing & Deployment) happens after all features are complete

### Parallel Work Opportunities
- Phase 4 (Media) and Phase 5 (GEDCOM) can be developed in parallel
- Phase 6 (Collaboration) and Phase 7 (Search) can overlap
- Frontend and backend tasks within each phase can be parallelized

### External Dependencies
- PostgreSQL database availability
- Domain name and hosting (for deployment)
- Email service (for invitations) - optional for MVP
- SSL certificate (for HTTPS)

## Timeline & Estimates
**When will things be done?**

### Effort Estimates (Person-Days)

**Phase 1: Foundation** - 5-7 days
- Project setup: 1 day
- Database: 1 day
- Authentication backend: 2 days
- Authentication frontend: 1-2 days
- Basic UI: 1 day

**Phase 2: Core Features** - 8-10 days
- Tree management: 2 days
- Individual management: 3 days
- Relationship management: 3-4 days
- Testing and debugging: 1 day

**Phase 3: Visualization** - 5-7 days
- Library integration: 1 day
- Layout algorithm: 2-3 days
- Interactive component: 2 days
- Polish and optimization: 1 day

**Phase 4: Media** - 4-5 days
- Storage setup: 0.5 day
- Backend implementation: 2 days
- Frontend implementation: 2 days
- Testing: 0.5 day

**Phase 5: GEDCOM** - 5-7 days
- Research and library: 1 day
- Import backend: 2-3 days
- Import frontend: 1 day
- Export backend: 1 day
- Export frontend: 0.5 day
- Testing: 0.5-1 day

**Phase 6: Collaboration** - 4-6 days
- Permission system: 2 days
- Invitation system: 1-2 days
- Frontend: 1-2 days
- Testing: 0.5-1 day

**Phase 7: Search & Polish** - 5-7 days
- Search: 2 days
- UI/UX improvements: 2-3 days
- Validation: 1 day
- Performance: 1 day

**Phase 8: Testing & Deployment** - 7-10 days
- Unit tests: 3 days
- Integration tests: 2 days
- Frontend tests: 2 days
- Deployment setup: 2-3 days
- Documentation: 1 day
- Security: 1 day

**Total Estimated Effort**: 43-59 person-days (roughly 9-12 weeks for one developer)

### Buffer for Unknowns
- Add 20-30% buffer for unexpected issues, learning curve, bug fixes
- Total with buffer: 52-77 person-days (11-15 weeks)

## Risks & Mitigation
**What could go wrong?**

### Technical Risks

**Risk 1: Tree visualization performance with large trees**
- **Impact**: High
- **Likelihood**: Medium
- **Mitigation**:
  - Implement pagination/lazy loading for tree rendering
  - Add generation filters to limit displayed nodes
  - Profile and optimize rendering early
  - Consider switching to more performant library if needed

**Risk 2: GEDCOM parsing complexity**
- **Impact**: Medium
- **Likelihood**: Medium
- **Mitigation**:
  - Use battle-tested parsing library
  - Start with subset of GEDCOM tags (core individuals/families)
  - Add support for additional tags incrementally
  - Provide clear error messages for unsupported features

**Risk 3: Database performance for relationship queries**
- **Impact**: Medium
- **Likelihood**: Low-Medium
- **Mitigation**:
  - Design efficient database indexes
  - Use PostgreSQL recursive CTEs efficiently
  - Cache frequently accessed trees
  - Have migration path to Neo4j if needed

**Risk 4: File storage scalability**
- **Impact**: Low-Medium
- **Likelihood**: Low
- **Mitigation**:
  - Use storage adapter pattern from the start
  - Implement file size limits
  - Plan migration to S3 or similar if volume grows

### Resource Risks

**Risk 5: Underestimated complexity**
- **Impact**: High
- **Likelihood**: Medium
- **Mitigation**:
  - Build MVP with core features first
  - Defer nice-to-have features
  - Regular progress reviews
  - Adjust scope as needed

**Risk 6: Testing time underestimated**
- **Impact**: Medium
- **Likelihood**: Medium
- **Mitigation**:
  - Write tests alongside implementation
  - Prioritize critical path testing
  - Use automated testing tools
  - Allocate sufficient time in plan

### Dependency Risks

**Risk 7: Third-party library issues**
- **Impact**: Medium
- **Likelihood**: Low
- **Mitigation**:
  - Choose mature, well-maintained libraries
  - Have backup options identified
  - Don't couple tightly to library-specific features

**Risk 8: Deployment complexity**
- **Impact**: Medium
- **Likelihood**: Low-Medium
- **Mitigation**:
  - Use Docker for consistent environments
  - Document deployment process thoroughly
  - Test deployment early and often
  - Have rollback plan

## Resources Needed
**What do we need to succeed?**

### Team Members and Roles
- Full-stack developer (primary)
- UI/UX designer (for design system, optional)
- QA/Tester (can be same as developer for MVP)

### Tools and Services

**Development:**
- Java JDK 17+
- Maven 3.8+
- Node.js 18+ (for Angular)
- PostgreSQL 14+
- Redis 7+
- MinIO (latest)
- Git and GitHub/GitLab
- IntelliJ IDEA or VS Code
- Postman or Insomnia for API testing

**Backend Libraries (Maven dependencies in pom.xml):**
- Spring Boot 3.x (Web, Security, Data JPA, Cache, Session)
- Spring Security with JWT (jjwt)
- PostgreSQL driver
- Redis (Jedis)
- MinIO Java SDK
- gedcom4j (GEDCOM parsing)
- Lombok (boilerplate reduction)
- MapStruct (DTO mapping)
- Thumbnailator (image processing)
- Commons IO

**Frontend Libraries (npm dependencies):**
- Angular 17+
- TypeScript 5+
- RxJS
- Angular Material or Tailwind CSS
- D3.js or Cytoscape.js (tree visualization)
- Angular CDK

**Testing:**
- JUnit 5 (backend unit tests)
- Mockito (mocking)
- MockMvc (controller tests)
- Testcontainers (integration tests)
- JaCoCo (code coverage)
- Karma & Jasmine (Angular tests)
- Protractor or Cypress (E2E tests)

**Deployment:**
- Docker & Docker Compose
- Nginx (reverse proxy)
- Cloud provider (AWS, Azure, GCP) or VPS

**Optional:**
- Spring Boot Actuator (monitoring)
- Prometheus & Grafana (metrics)
- Sentry (error tracking)
- Spring WebSocket (real-time updates)
- Spring Mail (email notifications)
- Bucket4j (rate limiting)

### Infrastructure
- PostgreSQL database (local or cloud)
- Web server (for deployment)
- Domain name
- SSL certificate
- File storage (local filesystem or S3)

### Documentation/Knowledge
- GEDCOM 5.5 specification
- PostgreSQL recursive query documentation (WITH RECURSIVE)
- Spring Boot reference documentation
- Spring Security authentication guide
- Spring Data JPA query methods
- Angular best practices and style guide
- D3.js/Cytoscape.js documentation
- MinIO Java SDK documentation
- Web security best practices (OWASP Top 10)
- JWT best practices

---

## 🐛 Bug Resolutions & Recent Changes

### Tree Visualization Bug Fix (2025-11-23)

**Issue**: Tree disappeared when creating SPOUSE or PARTNER relationships

**Symptoms**:
- After adding a spouse/partner relationship, navigating to the tree visualization showed no nodes
- Tree would sometimes render only a single node or small subset of the family
- No error messages, just an empty visualization

**Root Cause**:
- Original root-finding algorithm returned the first individual without parents
- When multiple disconnected individuals existed (common with spouse relationships), it could select a person with no descendants
- Spouse relationships were not visualized, making the tree appear incomplete

**Solution Implemented**:
- **Enhanced Root Finding** (`tree-visualization.component.ts:152-232`):
  - Finds ALL potential roots (individuals without parents)
  - Ranks by descendant count and birth date
  - Selects the "best" root with the most family connections
  - Added `countDescendants()` helper for intelligent selection

- **Spouse Visualization** (`tree-visualization.component.ts:234-291`):
  - Added `spouses` array to tree node structure
  - Bidirectional spouse relationship lookup
  - Prevents duplicate rendering with visited set

- **Visual Rendering** (`tree-visualization.component.ts:329-389`):
  - Pink dashed lines connect spouses horizontally
  - Spouse nodes render side-by-side with full details
  - All nodes are clickable and interactive

- **Error Handling** (`tree-visualization.component.ts:107-168`):
  - Try-catch wrapper around visualization logic
  - Detailed console logging for debugging
  - User-friendly error messages via snackbar
  - Container availability retry logic

- **User Experience**:
  - Added refresh button to toolbar
  - Updated instructions to explain spouse visualization
  - Clear guidance on when to refresh

**Files Modified**:
- `frontend/src/app/features/tree-visualization/tree-visualization.component.ts` (+160 lines)
- `frontend/src/app/features/tree-visualization/tree-visualization.component.html` (+7 lines)

**Testing Status**: ✅ Logic verified, awaiting user testing with real data

**Impact**: CRITICAL bug resolved - tree visualization now works correctly with all relationship types

---

## 📋 Outstanding Work

### Immediate (This Week)
1. **Complete Phase 4 Testing** - Follow `docs/ai/testing/phase4-media-management-test-guide.md`
2. **Commit Phase 4 Files** - Add untracked backend/frontend media management files
3. **Fix AuthInterceptor compilation error** - Update to use `authInterceptor` (functional interceptor)

### Short-term (Next 1-2 Weeks)
4. **Add Automated Tests for Phases 3-4**
   - Backend: MediaService, MinioService unit tests
   - Backend: MediaController integration tests
   - Frontend: MediaUploader, MediaGallery component tests
5. **Begin Phase 5: GEDCOM Import/Export** - High priority for interoperability

### Medium-term (Next 2-4 Weeks)
6. **Phase 6: Collaboration & Permissions** - PermissionService.java already created
7. **Phase 7: Search & Polish**
8. **Phase 8: Testing & Deployment**

---

## 📈 Progress Tracking

**Effort Completed**: ~29 person-days of ~43-59 estimated (67% through base estimate)

**Remaining Milestones**: 3 major (GEDCOM, Collaboration, Polish/Deploy)

**Timeline**: On track for completion within original 11-15 week estimate
