# Testing Checklist - Spouse Relationship Fix & Phase 4

**Date:** 2025-11-23
**Tester:** _____________

---

## Pre-Testing Setup

### 1. Start Infrastructure Services
```bash
# Start all Docker services
docker-compose up -d

# Verify services are running
docker-compose ps

# Expected: postgres, redis, minio all "Up"
```

### 2. Start Backend (Spring Boot)
```bash
cd backend
mvn spring-boot:run

# Wait for "Started FamilyTreeApplication"
# Backend should be on http://localhost:8080
```

### 3. Start Frontend (Angular)
```bash
cd frontend
ng serve

# Frontend should be on http://localhost:4200
```

---

## Test 1: Spouse Relationship Visualization Fix

### Setup Test Data
- [ ] Login to application (http://localhost:4200)
- [ ] Create or open a family tree
- [ ] Ensure you have at least 3 individuals:
  - Person A (e.g., John Smith)
  - Person B (e.g., Jane Doe)
  - Person C (e.g., Child)

### Test Spouse Relationship
- [ ] Add PARENT_CHILD relationship: Person A → Person C
- [ ] Navigate to tree visualization
- [ ] Verify Person A and Person C appear in tree
- [ ] Go back and add SPOUSE relationship: Person A ↔ Person B
- [ ] Navigate to tree visualization again
- [ ] Click **refresh button** in toolbar

### Expected Results
- [ ] ✅ Tree does NOT disappear
- [ ] ✅ Person A and Person B appear side-by-side
- [ ] ✅ Pink dashed line connects Person A and Person B
- [ ] ✅ Person C appears below as child
- [ ] ✅ Gray curved line connects parent to child
- [ ] ✅ All nodes are clickable
- [ ] ✅ Console shows: "Selected root: [name] with [X] descendants"
- [ ] ✅ Console shows: "Tree rendered successfully"

### Test Partner Relationship
- [ ] Create Person D
- [ ] Add PARTNER relationship: Person A ↔ Person D
- [ ] Refresh tree visualization
- [ ] Verify Partner D appears with pink dashed line

### Error Cases
- [ ] Try viewing tree with NO relationships
  - Expected: Message "No individuals in this tree yet..."
- [ ] Try viewing empty tree
  - Expected: Message "No individuals in this tree yet..."

---

## Test 2: Phase 4 Media Management (Quick Smoke Test)

### Backend API Test (Using Browser DevTools or Postman)

#### Authentication
```bash
# Register user
POST http://localhost:8080/api/auth/register
{
  "email": "test@example.com",
  "password": "password123",
  "name": "Test User"
}

# Login
POST http://localhost:8080/api/auth/login
{
  "email": "test@example.com",
  "password": "password123"
}

# Save JWT token
```

#### Quick Media Upload Test
- [ ] Create a tree via UI
- [ ] Create an individual via UI
- [ ] Go to individual detail page
- [ ] Switch to **Media** tab

### Frontend Media Upload Test
- [ ] Verify Media tab appears
- [ ] Verify upload zone is visible
- [ ] Try drag-and-drop an image file (JPG/PNG)
- [ ] Verify file appears in upload queue
- [ ] Click upload button
- [ ] Expected: Progress bar shows
- [ ] Expected: Success message appears
- [ ] Expected: Image appears in gallery below

### Frontend Media Gallery Test
- [ ] Verify uploaded image appears in gallery
- [ ] Click on image thumbnail
- [ ] Expected: Image opens in new tab or enlarges
- [ ] Edit caption:
  - Click edit icon
  - Change caption text
  - Click save
  - Expected: Caption updates immediately
- [ ] Download image:
  - Click download button
  - Expected: File downloads
- [ ] Delete image:
  - Click delete button
  - Expected: Confirmation dialog appears
  - Confirm deletion
  - Expected: Image removed from gallery

### File Validation Test
- [ ] Try uploading file > 5MB
- [ ] Expected: Error message "File too large"
- [ ] Try uploading .exe or unsupported file
- [ ] Expected: Error message about file type

---

## Test 3: Relationship Form (Quick Test)

- [ ] Go to individual detail page
- [ ] Click "Add Relationship" button
- [ ] Dialog opens with relationship form
- [ ] Select relationship type: SPOUSE
- [ ] Select another individual from dropdown
- [ ] Click Save
- [ ] Expected: Relationship created successfully
- [ ] Expected: Appears in Relationships tab

---

## Issues Found

### Issue 1:
**Description:**
**Steps to Reproduce:**
**Expected:**
**Actual:**
**Severity:** Critical / High / Medium / Low

### Issue 2:
**Description:**
**Steps to Reproduce:**
**Expected:**
**Actual:**
**Severity:**

---

## Overall Assessment

- [ ] All critical features working
- [ ] Tree visualization fix successful
- [ ] Media management functional
- [ ] Ready to commit

**Notes:**

**Completion Time:** ________

**Passed:** ☐ Yes  ☐ No  ☐ Partial

---

## Next Steps After Testing

1. If all tests pass: Proceed to commit
2. If issues found: Document and prioritize fixes
3. Run full Phase 4 test guide for comprehensive testing
