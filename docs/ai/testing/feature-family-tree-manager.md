---
phase: testing
title: Testing Strategy
description: Define testing approach, test cases, and quality assurance
---

# Testing Strategy

## Test Coverage Goals
**What level of testing do we aim for?**

- **Unit test coverage target**: 100% of new/changed code in services, utilities, and business logic
- **Integration test scope**: All API endpoints, critical database operations, file upload/download, permission checks
- **End-to-end test scenarios**: Key user journeys including registration, tree creation, adding individuals, visualization, GEDCOM import/export
- **Alignment**: All test cases map to requirements and acceptance criteria in `feature-family-tree-manager.md` requirements doc

## Unit Tests
**What individual components need testing?**

### Authentication Service
- [ ] **Test: User registration with valid data** - Creates user with hashed password, returns user object
- [ ] **Test: User registration with duplicate email** - Returns error, does not create user
- [ ] **Test: Login with correct credentials** - Returns JWT token, valid for 7 days
- [ ] **Test: Login with incorrect password** - Returns authentication error
- [ ] **Test: Login with non-existent email** - Returns authentication error
- [ ] **Test: JWT token verification with valid token** - Decodes and returns payload
- [ ] **Test: JWT token verification with expired token** - Returns error
- [ ] **Test: JWT token verification with invalid signature** - Returns error
- [ ] **Test: Password hashing** - bcrypt hash is generated, can be verified

### Tree Service
- [ ] **Test: Create tree with valid data** - Creates tree with owner permission
- [ ] **Test: Create tree without authentication** - Returns unauthorized error
- [ ] **Test: List trees for user** - Returns only trees user owns or has permission to
- [ ] **Test: Get tree by ID with permission** - Returns tree with individuals
- [ ] **Test: Get tree by ID without permission** - Returns forbidden error
- [ ] **Test: Update tree with owner permission** - Updates name/description
- [ ] **Test: Update tree with editor permission** - Returns forbidden (editors can't update tree metadata)
- [ ] **Test: Delete tree with owner permission** - Deletes tree and cascades to individuals/relationships
- [ ] **Test: Delete tree without owner permission** - Returns forbidden error

### Individual Service
- [ ] **Test: Create individual with valid data** - Creates individual in specified tree
- [ ] **Test: Create individual with invalid date format** - Returns validation error
- [ ] **Test: Create individual without tree permission** - Returns forbidden error
- [ ] **Test: Get individual by ID** - Returns individual with relationships and media
- [ ] **Test: Update individual with editor permission** - Updates fields successfully
- [ ] **Test: Update individual without permission** - Returns forbidden error
- [ ] **Test: Delete individual** - Deletes individual and associated relationships
- [ ] **Test: List individuals in tree** - Returns all individuals with pagination
- [ ] **Test: Validate birth date before death date** - Rejects if death before birth

### Relationship Service
- [ ] **Test: Create parent-child relationship** - Creates directed relationship
- [ ] **Test: Create spouse relationship** - Creates bidirectional relationship
- [ ] **Test: Prevent circular relationships** - Rejects if child is ancestor of parent
- [ ] **Test: Prevent duplicate relationships** - Rejects same relationship twice
- [ ] **Test: Delete relationship** - Removes relationship, doesn't delete individuals
- [ ] **Test: Get ancestors** - Returns all ancestors using recursive query
- [ ] **Test: Get descendants** - Returns all descendants using recursive query
- [ ] **Test: Get ancestors for individual with no parents** - Returns empty array
- [ ] **Test: Detect circular relationship** - Returns error when attempting to create cycle

### Media Service
- [ ] **Test: Upload photo with valid file** - Stores file, creates media record with metadata
- [ ] **Test: Upload with file size exceeding limit** - Returns error before storing
- [ ] **Test: Upload with invalid file type** - Returns error, does not store file
- [ ] **Test: Get media file with permission** - Returns file stream
- [ ] **Test: Get media file without permission** - Returns forbidden error
- [ ] **Test: Delete media** - Removes file from storage and database record
- [ ] **Test: Generate thumbnail for image** - Creates resized thumbnail

### GEDCOM Service
- [ ] **Test: Parse valid GEDCOM file** - Extracts individuals and families
- [ ] **Test: Parse GEDCOM with missing required tags** - Handles gracefully, imports what's possible
- [ ] **Test: Import GEDCOM creates individuals** - All INDI records become individuals
- [ ] **Test: Import GEDCOM creates relationships** - FAM records create parent-child and spouse relationships
- [ ] **Test: Import GEDCOM handles dates** - Various date formats parsed correctly
- [ ] **Test: Export tree as GEDCOM** - Generates valid GEDCOM 5.5 file
- [ ] **Test: Export includes all individuals** - All individuals in tree present in export
- [ ] **Test: Export includes all relationships** - Parent-child and spouse relationships preserved
- [ ] **Test: Import-export roundtrip** - Imported data exports to equivalent GEDCOM

### Search Service
- [ ] **Test: Search by given name** - Returns matching individuals
- [ ] **Test: Search by surname** - Returns matching individuals
- [ ] **Test: Search by partial name** - Returns partial matches
- [ ] **Test: Search case-insensitive** - Matches regardless of case
- [ ] **Test: Search with no results** - Returns empty array
- [ ] **Test: Search filters by tree ID** - Only returns individuals from specified tree

### Permission Service
- [ ] **Test: Grant viewer permission** - Creates permission record
- [ ] **Test: Grant editor permission** - Creates permission record
- [ ] **Test: Check owner has full permissions** - Owner can perform all actions
- [ ] **Test: Check editor can modify individuals** - Editor permission allows edits
- [ ] **Test: Check viewer cannot modify** - Viewer permission denies edits
- [ ] **Test: Revoke permission** - Removes permission record
- [ ] **Test: List permissions for tree** - Returns all users with permissions

### Validation Utilities
- [ ] **Test: Validate UUID format** - Accepts valid UUIDs, rejects invalid
- [ ] **Test: Validate date format (YYYY-MM-DD)** - Accepts valid dates, rejects invalid
- [ ] **Test: Validate email format** - Accepts valid emails, rejects invalid
- [ ] **Test: Sanitize HTML input** - Escapes HTML tags to prevent XSS

## Integration Tests
**How do we test component interactions?**

### API Endpoint Tests

#### Authentication Endpoints
- [ ] **POST /api/auth/register** - Register new user, returns 201 with user object
- [ ] **POST /api/auth/login** - Login with credentials, returns 200 with JWT token
- [ ] **POST /api/auth/logout** - Logout, clears token, returns 200
- [ ] **GET /api/auth/me** - Get current user, returns 200 with user object

#### Tree Endpoints
- [ ] **POST /api/trees** - Create tree, requires auth, returns 201
- [ ] **GET /api/trees** - List trees, requires auth, returns 200 with array
- [ ] **GET /api/trees/:id** - Get tree, requires permission, returns 200
- [ ] **PUT /api/trees/:id** - Update tree, requires owner, returns 200
- [ ] **DELETE /api/trees/:id** - Delete tree, requires owner, returns 204

#### Individual Endpoints
- [ ] **POST /api/trees/:id/individuals** - Create individual, requires editor permission
- [ ] **GET /api/individuals/:id** - Get individual, requires viewer permission
- [ ] **PUT /api/individuals/:id** - Update individual, requires editor permission
- [ ] **DELETE /api/individuals/:id** - Delete individual, requires editor permission
- [ ] **GET /api/individuals/:id/ancestors** - Get ancestors, requires viewer permission
- [ ] **GET /api/individuals/:id/descendants** - Get descendants, requires viewer permission

#### Relationship Endpoints
- [ ] **POST /api/relationships** - Create relationship, requires editor permission
- [ ] **DELETE /api/relationships/:id** - Delete relationship, requires editor permission

#### Media Endpoints
- [ ] **POST /api/individuals/:id/media** - Upload media, multipart/form-data, requires editor
- [ ] **GET /api/media/:id** - Get media file, requires viewer permission
- [ ] **DELETE /api/media/:id** - Delete media, requires editor permission

#### Import/Export Endpoints
- [ ] **POST /api/trees/:id/import** - Import GEDCOM, multipart/form-data, requires owner
- [ ] **GET /api/trees/:id/export** - Export GEDCOM, requires viewer permission, returns .ged file

#### Search Endpoints
- [ ] **GET /api/trees/:id/search?q=query** - Search individuals, requires viewer permission

#### Permission Endpoints
- [ ] **GET /api/trees/:id/permissions** - List permissions, requires owner
- [ ] **POST /api/trees/:id/permissions** - Grant permission, requires owner
- [ ] **DELETE /api/permissions/:id** - Revoke permission, requires owner

### Database Integration Tests
- [ ] **Integration: User creation with cascading tree permissions** - Creating user allows tree ownership
- [ ] **Integration: Tree deletion cascades to individuals** - Deleting tree removes all individuals
- [ ] **Integration: Individual deletion removes relationships** - Deleting person removes all their relationships
- [ ] **Integration: Recursive ancestor query returns full lineage** - Multi-generation ancestor query works
- [ ] **Integration: Concurrent edits to same tree** - Two users editing simultaneously doesn't corrupt data

### File Upload/Download Integration
- [ ] **Integration: Upload photo and retrieve via URL** - End-to-end file upload and download
- [ ] **Integration: Delete individual removes associated media files** - File cleanup on deletion
- [ ] **Integration: Large file upload (5MB)** - Boundary test for file size limit

### Permission Check Integration
- [ ] **Integration: Viewer cannot create individuals** - Permission check prevents unauthorized action
- [ ] **Integration: Editor can create individuals** - Permission check allows authorized action
- [ ] **Integration: Owner can delete tree** - Highest permission level works
- [ ] **Integration: Revoked permission denies access** - Permission removal takes effect immediately

## End-to-End Tests
**What user flows need validation?**

### User Journey 1: New User Building First Tree
- [ ] **E2E: User registers account** - Fill registration form, submit, redirect to dashboard
- [ ] **E2E: User creates first tree** - Click "Create Tree", enter name, save
- [ ] **E2E: User adds themselves** - Click "Add Individual", fill own info, save
- [ ] **E2E: User adds parents** - Select self, click "Add Parent", fill info, repeat for second parent
- [ ] **E2E: User views tree visualization** - Navigate to tree view, see graphical tree with 3 people
- [ ] **E2E: User uploads photo to individual** - Open individual detail, upload photo, see thumbnail

### User Journey 2: Importing Existing GEDCOM Data
- [ ] **E2E: User creates tree** - Create new tree for import
- [ ] **E2E: User uploads GEDCOM file** - Navigate to import, select file, upload
- [ ] **E2E: User previews import** - See summary of individuals and families to be imported
- [ ] **E2E: User confirms import** - Confirm import, wait for processing
- [ ] **E2E: User navigates imported tree** - View tree visualization with imported data

### User Journey 3: Collaborative Editing
- [ ] **E2E: Owner invites collaborator** - Navigate to permissions, enter email, send invite
- [ ] **E2E: Collaborator accepts and views tree** - Receive invite, accept, view tree
- [ ] **E2E: Collaborator adds individual** - Add new family member to tree
- [ ] **E2E: Owner sees changes** - Owner views tree, sees collaborator's addition

### User Journey 4: Searching and Editing
- [ ] **E2E: User searches for individual** - Enter name in search bar, see results
- [ ] **E2E: User clicks search result** - Navigate to individual detail page
- [ ] **E2E: User edits individual** - Update birth date, add biography, save changes
- [ ] **E2E: User views updated tree** - Return to tree view, changes reflected

### Critical Path Testing
- [ ] **Critical Path: Full tree building workflow** - Register → Create tree → Add 10 individuals → Define relationships → View visualization
- [ ] **Critical Path: GEDCOM roundtrip** - Import GEDCOM → Edit individuals → Export GEDCOM → Verify data integrity
- [ ] **Critical Path: Media workflow** - Add individual → Upload 3 photos → View gallery → Delete photo

### Regression Testing
- [ ] **Regression: Existing relationships preserved after edit** - Edit individual, ensure relationships still exist
- [ ] **Regression: Tree visualization updates after adding individual** - Add person, refresh, person appears
- [ ] **Regression: Search finds newly added individuals** - Add person, search, person found

## Test Data
**What data do we use for testing?**

### Test Fixtures and Mocks

**Test Users:**
```typescript
const testUsers = {
  alice: { email: 'alice@example.com', password: 'password123', name: 'Alice Smith' },
  bob: { email: 'bob@example.com', password: 'password123', name: 'Bob Jones' },
};
```

**Test Tree:**
```typescript
const testTree = {
  name: 'Smith Family Tree',
  description: 'Test family tree for integration tests',
};
```

**Test Individuals:**
```typescript
const testIndividuals = [
  { givenName: 'John', surname: 'Smith', gender: 'male', birthDate: '1950-01-15' },
  { givenName: 'Jane', surname: 'Doe', gender: 'female', birthDate: '1952-03-20' },
  { givenName: 'Emily', surname: 'Smith', gender: 'female', birthDate: '1975-06-10' },
];
```

**Test GEDCOM File:**
- Prepare sample GEDCOM file with 10 individuals and 3 families
- Include various date formats, missing data, multiple marriages

### Seed Data Requirements
- **Database**: Reset database before each test suite
- **Files**: Clear upload directory after each test
- **Test Isolation**: Each test should create and clean up its own data

### Test Database Setup
```bash
# Create test database
createdb family_tree_test

# Run migrations
DATABASE_URL="postgresql://user:password@localhost:5432/family_tree_test" npx prisma migrate deploy

# Reset database before tests
beforeEach(async () => {
  await prisma.$executeRaw`TRUNCATE TABLE users, family_trees, individuals, relationships, media, tree_permissions RESTART IDENTITY CASCADE`;
});
```

## Test Reporting & Coverage
**How do we verify and communicate test results?**

### Coverage Commands and Thresholds
```bash
# Run all tests with coverage
npm run test -- --coverage

# Backend unit tests
cd backend && npm run test:unit -- --coverage

# Backend integration tests
cd backend && npm run test:integration

# Frontend component tests
cd frontend && npm run test -- --coverage

# E2E tests
cd frontend && npm run test:e2e
```

**Coverage Thresholds (Jest configuration):**
```json
{
  "coverageThreshold": {
    "global": {
      "branches": 90,
      "functions": 90,
      "lines": 90,
      "statements": 90
    }
  }
}
```

### Coverage Gaps
- **Files below 100%**: Document any files with < 100% coverage and justification
  - Example: `gedcom.service.ts` - Some edge cases for obscure GEDCOM tags not covered (90% coverage acceptable)
- **Uncovered branches**: Complex error handling branches in file upload (plan to add in future)
- **Integration vs. unit**: Some complex database queries covered by integration tests rather than unit tests

### Test Reports
- **Generate HTML report**: `npm run test -- --coverage --coverageReporters=html`
- **View report**: Open `coverage/index.html` in browser
- **CI/CD integration**: Upload coverage to Codecov or similar service

### Manual Testing Outcomes
- [ ] **Manual Test 1**: Visual tree renders correctly in Chrome, Firefox, Safari
- [ ] **Manual Test 2**: Photo upload works for JPG, PNG, HEIC formats
- [ ] **Manual Test 3**: Mobile responsive design works on iOS and Android
- [ ] **Manual Test 4**: Keyboard navigation works for all forms
- [ ] **Manual Test 5**: Screen reader compatibility (basic ARIA labels)

## Manual Testing
**What requires human validation?**

### UI/UX Testing Checklist
- [ ] **Visual consistency**: All components use consistent styling (colors, fonts, spacing)
- [ ] **Form validation**: Error messages are clear and helpful
- [ ] **Loading states**: Spinners/skeletons shown during data fetching
- [ ] **Empty states**: Appropriate messages when no data exists
- [ ] **Error handling**: User-friendly error messages, not technical stack traces
- [ ] **Hover states**: Interactive elements show hover effects
- [ ] **Button states**: Disabled buttons clearly distinguished
- [ ] **Modal behavior**: Modals close with X, Escape key, clicking outside
- [ ] **Tree navigation**: Zoom and pan feel smooth and intuitive
- [ ] **Photo gallery**: Lightbox works correctly, images load properly

### Accessibility Testing
- [ ] **Keyboard navigation**: All interactive elements reachable via Tab
- [ ] **Focus indicators**: Clear visual focus on active element
- [ ] **ARIA labels**: Screen reader announces elements correctly
- [ ] **Color contrast**: Text meets WCAG AA standards (4.5:1 ratio)
- [ ] **Alt text**: All images have descriptive alt attributes
- [ ] **Form labels**: All form inputs have associated labels
- [ ] **Error announcements**: Form errors announced to screen readers

### Browser/Device Compatibility
- [ ] **Chrome (latest)**: Desktop and mobile
- [ ] **Firefox (latest)**: Desktop and mobile
- [ ] **Safari (latest)**: macOS and iOS
- [ ] **Edge (latest)**: Desktop
- [ ] **Responsive breakpoints**: 320px (mobile), 768px (tablet), 1024px (desktop)

### Smoke Tests After Deployment
- [ ] **Home page loads**: No console errors
- [ ] **User can register**: Registration form works
- [ ] **User can login**: Login flow works
- [ ] **User can create tree**: Tree creation works
- [ ] **User can add individual**: Individual creation works
- [ ] **Tree visualization renders**: No blank screen or errors

## Performance Testing
**How do we validate performance?**

### Load Testing Scenarios
- [ ] **Scenario 1: 50 concurrent users creating individuals** - Response time < 500ms
- [ ] **Scenario 2: 100 concurrent users viewing trees** - Response time < 200ms
- [ ] **Scenario 3: 10 concurrent GEDCOM imports (500 individuals each)** - Complete within 10 seconds
- [ ] **Scenario 4: Tree with 1000 individuals** - Visualization renders within 5 seconds

### Stress Testing Approach
- Use Apache JMeter or k6 for load testing
- Gradually increase concurrent users until response time degrades
- Identify bottlenecks (database, API, frontend rendering)

### Performance Benchmarks
- **API Response Time**:
  - Simple GET requests: < 100ms (p95)
  - Complex tree queries: < 500ms (p95)
  - GEDCOM import (500 individuals): < 10s

- **Frontend Performance**:
  - Initial page load: < 2s
  - Tree visualization (100 nodes): < 3s
  - Search results: < 1s

- **Database Performance**:
  - Ancestor query (5 generations): < 200ms
  - Full tree fetch (500 individuals): < 500ms

## Bug Tracking
**How do we manage issues?**

### Issue Tracking Process
1. **Report bug**: Create GitHub/GitLab issue with template
2. **Triage**: Assign severity (critical, high, medium, low)
3. **Reproduce**: Add steps to reproduce and expected vs. actual behavior
4. **Fix**: Create branch, fix bug, add regression test
5. **Verify**: QA tests fix in staging environment
6. **Close**: Merge to main, close issue

### Bug Severity Levels
- **Critical**: System crash, data loss, security vulnerability
- **High**: Major feature broken, workaround exists
- **Medium**: Feature partially broken, minor impact
- **Low**: Cosmetic issue, minor UX inconvenience

### Regression Testing Strategy
- **After each bug fix**: Add regression test to prevent recurrence
- **Before each release**: Run full test suite
- **Automated CI/CD**: Run tests on every pull request
