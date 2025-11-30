# Plan: Tạo cây gia phả mới từ cá nhân được chọn

**Status**: ✅ IMPLEMENTED (2025-11-30)
**Backend**: `TreeCloneService.java` - Full implementation
**Frontend**: `create-tree-from-individual-dialog.component.ts` - Dialog component exists

---

## Tổng quan

Tính năng cho phép tạo một cây gia phả mới từ một cá nhân được chọn, bao gồm:
- Tất cả tổ tiên (ancestors) của người đó
- Tất cả con cháu (descendants) của người đó
- Vợ/chồng của người được chọn và tổ tiên/con cháu
- Đầy đủ thông tin cá nhân và media files

## Validation

- Kiểm tra nếu cá nhân đã là root của một cây gia phả khác (đã được export trước đó)
- Kiểm tra quyền truy cập vào cây nguồn

---

## Backend Implementation

### 1. DTOs mới

**File:** `backend/src/main/java/com/familytree/dto/tree/CreateTreeFromIndividualRequest.java`
```java
- sourceTreeId: UUID (cây nguồn)
- rootIndividualId: UUID (cá nhân được chọn làm gốc)
- newTreeName: String (tên cây mới)
- newTreeDescription: String (mô tả - optional)
- includeMedia: boolean (có copy media không - default true)
```

**File:** `backend/src/main/java/com/familytree/dto/tree/CreateTreeFromIndividualResponse.java`
```java
- newTreeId: UUID
- newTreeName: String
- totalIndividuals: int
- totalRelationships: int
- totalMediaFiles: int
- rootIndividualId: UUID (ID mới của cá nhân gốc trong cây mới)
```

### 2. Service mới

**File:** `backend/src/main/java/com/familytree/service/TreeCloneService.java`

```java
@Service
@Transactional
public class TreeCloneService {

    // Main method
    public CreateTreeFromIndividualResponse createTreeFromIndividual(
        CreateTreeFromIndividualRequest request,
        String userEmail
    );

    // Validation: Kiểm tra cá nhân đã có cây riêng chưa
    private void validateNotAlreadyExported(UUID individualId, UUID sourceTreeId);

    // Thu thập tất cả cá nhân cần copy (ancestors + descendants + spouses)
    private Set<UUID> collectAllRelatedIndividuals(UUID rootIndividualId, UUID treeId);

    // Thu thập ancestors (đệ quy lên trên)
    private void collectAncestors(UUID individualId, Set<UUID> visited);

    // Thu thập descendants (đệ quy xuống dưới)
    private void collectDescendants(UUID individualId, Set<UUID> visited);

    // Thu thập spouses
    private void collectSpouses(UUID individualId, Set<UUID> visited);

    // Clone individuals sang cây mới
    private Map<UUID, Individual> cloneIndividuals(
        Set<UUID> individualIds,
        FamilyTree newTree
    );

    // Clone relationships
    private List<Relationship> cloneRelationships(
        Set<UUID> originalIndividualIds,
        Map<UUID, Individual> idMapping,
        FamilyTree newTree
    );

    // Clone media files (download từ MinIO và upload lại với path mới)
    private void cloneMedia(
        Map<UUID, Individual> idMapping,
        boolean includeMedia
    );
}
```

### 3. Repository method mới

**File:** `backend/src/main/java/com/familytree/repository/FamilyTreeRepository.java`

Thêm method kiểm tra cá nhân đã là root của cây khác:
```java
// Kiểm tra xem có cây nào được tạo từ cá nhân này không
@Query("SELECT COUNT(t) > 0 FROM FamilyTree t WHERE t.sourceIndividualId = :individualId AND t.id != :excludeTreeId")
boolean existsBySourceIndividualId(@Param("individualId") UUID individualId, @Param("excludeTreeId") UUID excludeTreeId);
```

### 4. Model update

**File:** `backend/src/main/java/com/familytree/model/FamilyTree.java`

Thêm fields để track nguồn gốc:
```java
@Column(name = "source_tree_id")
private UUID sourceTreeId;  // Cây nguồn (nếu được clone)

@Column(name = "source_individual_id")
private UUID sourceIndividualId;  // Cá nhân gốc được chọn để tạo cây

@Column(name = "cloned_at")
private LocalDateTime clonedAt;  // Thời điểm clone
```

### 5. Controller endpoint

**File:** `backend/src/main/java/com/familytree/controller/TreeController.java`

```java
/**
 * Tạo cây gia phả mới từ một cá nhân
 * POST /api/trees/from-individual
 */
@PostMapping("/from-individual")
public ResponseEntity<CreateTreeFromIndividualResponse> createTreeFromIndividual(
    @Valid @RequestBody CreateTreeFromIndividualRequest request,
    Authentication authentication
);
```

---

## Frontend Implementation

### 1. Service update

**File:** `frontend/src/app/features/tree/services/tree.service.ts`

```typescript
// Thêm method mới
createTreeFromIndividual(request: CreateTreeFromIndividualRequest): Observable<CreateTreeFromIndividualResponse>;

// Kiểm tra cá nhân đã được export chưa
checkIndividualExported(treeId: string, individualId: string): Observable<boolean>;
```

### 2. Models mới

**File:** `frontend/src/app/features/tree/models/tree.model.ts`

```typescript
export interface CreateTreeFromIndividualRequest {
  sourceTreeId: string;
  rootIndividualId: string;
  newTreeName: string;
  newTreeDescription?: string;
  includeMedia: boolean;
}

export interface CreateTreeFromIndividualResponse {
  newTreeId: string;
  newTreeName: string;
  totalIndividuals: number;
  totalRelationships: number;
  totalMediaFiles: number;
  rootIndividualId: string;
}
```

### 3. UI Component

**Vị trí:** Thêm button "Tạo gia phả mới" trong:
- `individual-detail.component.html` - Chi tiết cá nhân
- `tree-visualization.component.html` - Context menu khi click vào node

**Dialog Component mới:** `create-tree-from-individual-dialog.component.ts`
- Form nhập tên cây mới
- Checkbox "Bao gồm hình ảnh và tài liệu"
- Preview số lượng: ancestors, descendants, media
- Validation warning nếu đã export trước đó
- Progress bar khi đang xử lý

### 4. Translations

**Files:** `frontend/src/assets/i18n/vi.json` và `en.json`

```json
{
  "tree": {
    "createFromIndividual": "Tạo gia phả mới từ người này",
    "createFromIndividualTitle": "Tạo gia phả mới",
    "createFromIndividualDesc": "Tạo cây gia phả mới bao gồm tổ tiên và con cháu của người được chọn",
    "newTreeName": "Tên gia phả mới",
    "includeMedia": "Bao gồm hình ảnh và tài liệu",
    "alreadyExported": "Người này đã được tạo gia phả riêng trước đó",
    "createFromIndividualSuccess": "Đã tạo gia phả mới thành công",
    "createFromIndividualFailed": "Không thể tạo gia phả mới",
    "previewAncestors": "Số tổ tiên",
    "previewDescendants": "Số con cháu",
    "previewMedia": "Số file media",
    "processing": "Đang xử lý..."
  }
}
```

---

## Database Migration

**File:** `backend/src/main/resources/db/migration/V{X}__add_tree_clone_fields.sql`

```sql
ALTER TABLE family_trees ADD COLUMN source_tree_id UUID;
ALTER TABLE family_trees ADD COLUMN source_individual_id UUID;
ALTER TABLE family_trees ADD COLUMN cloned_at TIMESTAMP;

-- Index để tìm kiếm nhanh
CREATE INDEX idx_family_trees_source_individual ON family_trees(source_individual_id);
```

---

## Flow xử lý

```
1. User chọn cá nhân → Click "Tạo gia phả mới"
                ↓
2. Validate: Kiểm tra cá nhân đã export chưa
   - Nếu đã export → Hiện warning, cho phép tiếp tục hoặc hủy
                ↓
3. User nhập tên cây mới, chọn options
                ↓
4. Backend xử lý:
   a. Thu thập tất cả cá nhân liên quan (ancestors + descendants + spouses)
   b. Tạo FamilyTree mới
   c. Clone từng Individual (new IDs)
   d. Clone Relationships (map old IDs → new IDs)
   e. Clone Media files (download/upload MinIO)
                ↓
5. Trả về response với thông tin cây mới
                ↓
6. Redirect user đến cây mới hoặc hiển thị success dialog
```

---

## Tasks breakdown

### Backend (ưu tiên cao) ✅ COMPLETE
1. [x] Thêm fields vào FamilyTree model (source tracking)
2. [x] Tạo DTOs (Request/Response)
3. [x] Tạo TreeCloneService với các methods - `TreeCloneService.java`
4. [x] Thêm repository methods
5. [x] Tạo controller endpoint (trong TreeController)
6. [ ] Unit tests - Pending

### Frontend (ưu tiên cao) ✅ MOSTLY COMPLETE
7. [x] Thêm models và service methods - `tree.service.ts`
8. [x] Tạo dialog component - `create-tree-from-individual-dialog.component.ts`
9. [ ] Thêm button vào individual-detail - Needs verification
10. [ ] Thêm context menu option vào tree-visualization - Needs verification
11. [x] Thêm translations - vi.json, en.json
12. [ ] Integration testing - Pending

### Database
13. [x] Fields added to FamilyTree model (Hibernate auto-update)

---

## Estimated complexity

- **Backend:** Medium-High (recursive traversal, media copy)
- **Frontend:** Medium (dialog, form, progress)
- **Total files:** ~10-12 files

## Risks & Considerations

1. **Performance:** Cây lớn có thể mất nhiều thời gian để copy media
   - Giải pháp: Async processing hoặc background job

2. **Storage:** Copy media sẽ duplicate storage
   - Giải pháp: Có option để không copy media

3. **Circular relationships:** Cần handle đúng để tránh infinite loop
   - Giải pháp: Sử dụng Set<UUID> visited như existing code

4. **Transaction size:** Nhiều records có thể timeout
   - Giải pháp: Batch processing hoặc chunked commits

---

## Implementation Summary (2025-11-30)

### Files Created/Modified:
**Backend:**
- `backend/src/main/java/com/familytree/service/TreeCloneService.java` - Main service
- DTOs in `backend/src/main/java/com/familytree/dto/tree/`
- Endpoint in TreeController

**Frontend:**
- `frontend/src/app/features/tree/create-tree-from-individual-dialog/create-tree-from-individual-dialog.component.ts`
- Updates to `tree.service.ts`
- Translations in `assets/i18n/`

### Remaining Work:
- [ ] Add unit tests for TreeCloneService
- [ ] Verify UI integration in individual-detail and tree-visualization
- [ ] End-to-end testing
- [ ] Documentation for users
