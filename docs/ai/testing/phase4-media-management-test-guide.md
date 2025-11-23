# Phase 4: Media Management - Complete Testing Guide

## 🎯 Overview
This guide covers complete testing for the Media Management feature (Phase 4) including backend API testing, frontend UI testing, and integration testing.

---

## ✅ Prerequisites Checklist

### 1. Infrastructure Services Running

```bash
# Verify all Docker services are running
docker-compose ps

# Expected output: postgres, redis, and minio should all be "Up"
```

**Services Required:**
- ✅ PostgreSQL (port 5432)
- ✅ Redis (port 6379)
- ✅ MinIO (ports 9000 API, 9001 Console)

**Start all services if not running:**
```bash
docker-compose up -d
```

**Check MinIO is ready:**
- Open browser: `http://localhost:9001`
- Login: `minioadmin` / `minioadmin123`
- Verify bucket `family-tree-media` exists

### 2. Backend (Spring Boot) Running

```bash
cd backend
mvn spring-boot:run
```

**Verify backend is running:**
```bash
curl http://localhost:8080/actuator/health
```

Expected response: `{"status":"UP"}`

### 3. Frontend (Angular) Running

```bash
cd frontend
npm install  # If first time
ng serve
```

**Verify frontend is running:**
- Open browser: `http://localhost:4200`
- You should see the login page

---

## 🧪 Testing Plan

### Phase 1: Backend API Testing (Using cURL or Postman)

#### Step 1: Authentication

First, register and login to get JWT token:

```bash
# Register a new user
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "name": "Test User"
  }'
```

```bash
# Login to get JWT token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

**Save the JWT token** from the response - you'll need it for authenticated requests.

Set it as an environment variable:
```bash
export JWT_TOKEN="your-jwt-token-here"
```

#### Step 2: Create Test Data

Create a tree and individual for testing:

```bash
# Create a family tree
curl -X POST http://localhost:8080/api/trees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "name": "Test Family Tree",
    "description": "For testing media uploads"
  }'
```

Save the `treeId` from the response.

```bash
# Create an individual
curl -X POST http://localhost:8080/api/trees/{treeId}/individuals \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "givenName": "John",
    "surname": "Doe",
    "gender": "MALE",
    "birthDate": "1950-01-15"
  }'
```

Save the `individualId` from the response.

#### Step 3: Test Media Upload

**Upload an image:**
```bash
curl -X POST "http://localhost:8080/api/individuals/{individualId}/media" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/path/to/your/image.jpg" \
  -F "caption=Family photo from 1975"
```

**Expected Response:**
```json
{
  "id": "uuid",
  "individualId": "uuid",
  "type": "PHOTO",
  "filename": "image.jpg",
  "caption": "Family photo from 1975",
  "fileSize": 245678,
  "mimeType": "image/jpeg",
  "downloadUrl": "/api/media/{id}/download",
  "thumbnailUrl": "/api/media/{id}/thumbnail",
  "uploadedAt": "2025-11-22T15:30:00"
}
```

**Upload a PDF document:**
```bash
curl -X POST "http://localhost:8080/api/individuals/{individualId}/media" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/path/to/document.pdf" \
  -F "caption=Birth certificate"
```

#### Step 4: Test Media Retrieval

**List all media for an individual:**
```bash
curl http://localhost:8080/api/individuals/{individualId}/media \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Get specific media metadata:**
```bash
curl http://localhost:8080/api/media/{mediaId} \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Download media file:**
```bash
curl -O http://localhost:8080/api/media/{mediaId}/download \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Get thumbnail (images only):**
```bash
curl -O http://localhost:8080/api/media/{mediaId}/thumbnail \
  -H "Authorization: Bearer $JWT_TOKEN"
```

#### Step 5: Test Media Update

**Update caption:**
```bash
curl -X PUT http://localhost:8080/api/media/{mediaId} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{
    "caption": "Updated caption text"
  }'
```

#### Step 6: Test Media Delete

**Delete media:**
```bash
curl -X DELETE http://localhost:8080/api/media/{mediaId} \
  -H "Authorization: Bearer $JWT_TOKEN"
```

**Expected Response:** 204 No Content

#### Step 7: Test File Validation

**Test file size limit (>5MB should fail):**
```bash
curl -X POST "http://localhost:8080/api/individuals/{individualId}/media" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/path/to/large-file.jpg"
```

**Expected Response:** 400 Bad Request with error message

**Test invalid file type:**
```bash
curl -X POST "http://localhost:8080/api/individuals/{individualId}/media" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "file=@/path/to/file.exe"
```

**Expected Response:** 400 Bad Request with error message

---

### Phase 2: Frontend UI Testing

#### Step 1: Setup

1. **Login to the application:**
   - Navigate to `http://localhost:4200`
   - Click "Register" or "Login"
   - Use credentials: `test@example.com` / `password123`

2. **Navigate to a tree:**
   - Click on "My Trees" or create a new tree
   - Open a tree

3. **Navigate to an individual:**
   - Click on "Individuals" or create a new individual
   - Open an individual's detail page

#### Step 2: Test Media Upload Component

**Test Drag-and-Drop:**
1. Switch to the "Media" tab
2. Drag an image file from your file explorer
3. Drop it onto the upload zone
4. ✅ **Verify:** Zone highlights on hover
5. ✅ **Verify:** File appears in upload queue

**Test File Browser:**
1. Click "Browse Files" button
2. Select multiple images/documents
3. ✅ **Verify:** All files appear in queue

**Test Upload Queue:**
1. Add several files to queue
2. Add captions to some files
3. ✅ **Verify:** Caption input appears for each file
4. ✅ **Verify:** File icons match file types

**Test Individual File Upload:**
1. Click upload button (↑) on a single file
2. ✅ **Verify:** Progress bar appears
3. ✅ **Verify:** Progress percentage updates
4. ✅ **Verify:** Success message appears
5. ✅ **Verify:** File marked as complete with ✓

**Test Batch Upload:**
1. Add multiple files to queue
2. Click "Upload All" button
3. ✅ **Verify:** All files upload with progress
4. ✅ **Verify:** Gallery refreshes automatically

**Test File Validation:**
1. Try uploading a file >5MB
2. ✅ **Verify:** Error message appears
3. ✅ **Verify:** File not added to queue

4. Try uploading unsupported file type (.exe, .zip)
5. ✅ **Verify:** Error message appears

**Test Queue Management:**
1. Add files to queue
2. Click "Remove" (X) button on a file
3. ✅ **Verify:** File removed from queue
4. Upload some files
5. Click "Clear Completed" button
6. ✅ **Verify:** Completed files removed from queue

#### Step 3: Test Media Gallery Component

**Test Gallery Display:**
1. Scroll to media gallery below uploader
2. ✅ **Verify:** Uploaded media appears in grid
3. ✅ **Verify:** Thumbnails load correctly for images
4. ✅ **Verify:** Document icons appear for PDFs

**Test Image Preview:**
1. Click on an image thumbnail
2. ✅ **Verify:** Image opens in new tab or modal
3. ✅ **Verify:** Full-size image displays

**Test Document Download:**
1. Click on a PDF document
2. ✅ **Verify:** File downloads automatically

**Test Caption Editing:**
1. Click the edit button (✏️) next to caption
2. Modify the caption text
3. Press Enter or click Save (✓)
4. ✅ **Verify:** Caption updates immediately
5. ✅ **Verify:** Success notification appears

**Test Caption Cancel:**
1. Click edit button
2. Modify text
3. Click Cancel (X) or press Escape
4. ✅ **Verify:** Caption reverts to original

**Test Download:**
1. Click "Download" button on a media item
2. ✅ **Verify:** File downloads
3. ✅ **Verify:** Filename is correct

**Test Delete:**
1. Click "Delete" button on a media item
2. ✅ **Verify:** Confirmation dialog appears
3. Confirm deletion
4. ✅ **Verify:** Media removed from gallery
5. ✅ **Verify:** Success notification appears

**Test Responsive Design:**
1. Resize browser window to mobile size
2. ✅ **Verify:** Gallery switches to single column
3. ✅ **Verify:** All features still work

**Test Empty State:**
1. Delete all media for an individual
2. ✅ **Verify:** "No media files yet" message appears
3. ✅ **Verify:** Upload instruction shows

#### Step 4: Test Integration

**Test Auto-Refresh:**
1. Have gallery and uploader visible
2. Upload new media
3. ✅ **Verify:** Gallery automatically updates
4. ✅ **Verify:** New media appears instantly

**Test Multiple File Types:**
1. Upload: JPEG, PNG, GIF, PDF, DOCX
2. ✅ **Verify:** All upload successfully
3. ✅ **Verify:** Images show thumbnails
4. ✅ **Verify:** Documents show appropriate icons

**Test Large Batch:**
1. Upload 10+ files at once
2. ✅ **Verify:** All files process correctly
3. ✅ **Verify:** Gallery displays all media
4. ✅ **Verify:** Performance remains good

---

### Phase 3: MinIO Verification

**Access MinIO Console:**
1. Open `http://localhost:9001`
2. Login: `minioadmin` / `minioadmin123`
3. Navigate to `family-tree-media` bucket

**Verify Storage Structure:**
1. ✅ **Verify:** Files organized by `{treeId}/{individualId}/`
2. ✅ **Verify:** Original files present
3. ✅ **Verify:** Thumbnail files present (with `_thumb` suffix)
4. ✅ **Verify:** Files have unique UUIDs in names

**Verify Thumbnails:**
1. Download a thumbnail file
2. ✅ **Verify:** Image is 200x200 pixels
3. ✅ **Verify:** Aspect ratio preserved
4. ✅ **Verify:** Quality is good

---

## 🐛 Common Issues & Solutions

### Issue 1: MinIO Not Starting
**Symptom:** Can't upload files, connection refused on port 9000

**Solution:**
```bash
# Check if MinIO is running
docker-compose ps

# Restart MinIO
docker-compose restart minio

# Check logs
docker-compose logs minio
```

### Issue 2: Bucket Not Created
**Symptom:** "Bucket does not exist" error

**Solution:**
```bash
# Run minio-init container
docker-compose up minio-init

# Or create manually in MinIO console
```

### Issue 3: File Upload Fails
**Symptom:** 500 error on upload

**Check:**
1. MinIO is running and accessible
2. JWT token is valid and not expired
3. Individual exists
4. User has permission to tree

**Debug:**
```bash
# Check backend logs
# Look for MinIO connection errors
```

### Issue 4: Thumbnails Not Generating
**Symptom:** Thumbnail URL returns 404

**Check:**
1. Thumbnailator library is in classpath
2. Check backend logs for thumbnail generation errors
3. Verify image format is supported

### Issue 5: CORS Errors
**Symptom:** Browser console shows CORS errors

**Solution:**
- Verify `@CrossOrigin` annotation on MediaController
- Check `application.properties` for CORS configuration

### Issue 6: File Too Large
**Symptom:** Upload fails for files >5MB

**Expected Behavior** - This is correct!
- Validation message should appear
- File should not upload

### Issue 7: Angular Gallery Not Refreshing
**Symptom:** New uploads don't appear

**Solution:**
1. Check browser console for errors
2. Verify `onMediaUploaded` method is called
3. Try manual refresh button

---

## ✨ Expected Results Summary

### Backend Validation
- ✅ Images upload successfully (JPEG, PNG, GIF, WebP)
- ✅ Documents upload successfully (PDF, DOC, DOCX, TXT)
- ✅ Thumbnails generated for images
- ✅ Files stored in MinIO with correct structure
- ✅ File size limit enforced (5MB)
- ✅ File type validation works
- ✅ Permission checks work
- ✅ CRUD operations work correctly

### Frontend Features
- ✅ Drag-and-drop works smoothly
- ✅ File browser works
- ✅ Upload progress shows correctly
- ✅ Queue management works
- ✅ Gallery displays media correctly
- ✅ Caption editing works
- ✅ Download works
- ✅ Delete works with confirmation
- ✅ Auto-refresh after upload
- ✅ Responsive design works
- ✅ Empty states show correctly

### Integration
- ✅ Backend and frontend communicate correctly
- ✅ MinIO stores files correctly
- ✅ Permissions enforced across all operations
- ✅ Multiple file types supported
- ✅ Performance is acceptable

---

## 📊 Test Results Template

Use this checklist to track your testing:

```
## Phase 4 Media Management - Test Results

**Tested By:** _______________
**Date:** _______________
**Environment:** Local Development

### Backend API Tests
- [ ] Authentication works
- [ ] Media upload (image) works
- [ ] Media upload (document) works  
- [ ] Media list works
- [ ] Media get works
- [ ] Media update works
- [ ] Media delete works
- [ ] File size validation works
- [ ] File type validation works
- [ ] Thumbnail generation works

### Frontend UI Tests  
- [ ] Drag-and-drop upload works
- [ ] File browser upload works
- [ ] Upload progress tracking works
- [ ] Queue management works
- [ ] Gallery display works
- [ ] Image preview works
- [ ] Caption editing works
- [ ] Download works
- [ ] Delete works
- [ ] Auto-refresh works
- [ ] Responsive design works

### Integration Tests
- [ ] MinIO storage verified
- [ ] Thumbnail files created
- [ ] File structure correct
- [ ] Permissions enforced
- [ ] Multiple file types work

### Issues Found
(List any bugs or issues discovered)

1. 
2. 
3. 

### Overall Assessment
- [ ] All critical features working
- [ ] Ready for next phase
- [ ] Additional work needed (explain below)

**Notes:**
```

---

## 🎯 Next Steps After Testing

Once Phase 4 testing is complete:

1. **Document any bugs** found during testing
2. **Fix critical issues** before proceeding
3. **Update planning document** with test results
4. **Move to Phase 5** (GEDCOM Import/Export) or **Phase 6** (Collaboration)

---

## 📞 Need Help?

If you encounter issues:
1. Check the backend logs for errors
2. Check browser console for frontend errors
3. Verify all services are running
4. Check MinIO console for storage issues
5. Review the code in the following files:
   - Backend: `MediaController.java`, `MediaService.java`, `MinioService.java`
   - Frontend: `media-uploader.component.ts`, `media-gallery.component.ts`

Good luck with testing! 🚀

