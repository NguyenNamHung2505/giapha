# Phase 5: GEDCOM Import/Export - Implementation Guide

**Date:** 2025-11-23
**Status:** In Progress

---

## Overview

GEDCOM (Genealogical Data Communication) is the standard file format for exchanging genealogical data between different software applications. This phase implements full GEDCOM 5.5.1 support for importing and exporting family tree data.

---

## GEDCOM Basics

### What is GEDCOM?

GEDCOM is a line-based text format with hierarchical structure:
```
0 HEAD
1 SOUR Family Tree Manager
1 GEDC
2 VERS 5.5.1
0 @I1@ INDI
1 NAME John /Doe/
2 GIVN John
2 SURN Doe
1 SEX M
1 BIRT
2 DATE 15 JAN 1950
2 PLAC New York, USA
0 @F1@ FAM
1 HUSB @I1@
1 WIFE @I2@
1 MARR
2 DATE 10 JUN 1975
```

### Key GEDCOM Concepts

- **Tags**: Keywords like INDI, FAM, NAME, BIRT
- **IDs**: References like @I1@, @F1@
- **Levels**: Indentation showing hierarchy (0, 1, 2, etc.)
- **Records**: INDI (Individual), FAM (Family)

### Mapping Strategy

| GEDCOM | Our Model |
|--------|-----------|
| INDI record | Individual entity |
| FAM record | Relationship entity (PARENT_CHILD) |
| HUSB/WIFE in FAM | Relationship (SPOUSE) |
| NAME | givenName + surname |
| BIRT/DEAT | birthDate/deathDate + birthPlace/deathPlace |
| SEX | gender |

---

## Architecture

### Backend Components

```
GedcomService
├── importGedcom(file, treeId) -> ImportResult
│   ├── Parse GEDCOM file (gedcom4j)
│   ├── Validate structure
│   ├── Map INDI -> Individual
│   ├── Map FAM -> Relationships
│   └── Save to database (@Transactional)
└── exportGedcom(treeId) -> byte[]
    ├── Load tree data
    ├── Map Individual -> INDI
    ├── Map Relationships -> FAM
    └── Generate GEDCOM file (gedcom4j)

GedcomController
├── POST /api/trees/{id}/gedcom/import
└── GET /api/trees/{id}/gedcom/export
```

### Frontend Components

```
gedcom-import.component
├── File upload
├── Validation
├── Progress indicator
└── Import summary display

gedcom-export.component
├── Export button
├── Download trigger
└── Success notification
```

---

## Data Mapping Details

### Individual Mapping (INDI → Individual)

```java
GEDCOM Field          → Java Field
--------------------- → ---------------------
INDI/@ID@             → (temp map, not stored)
NAME/GIVN             → givenName
NAME/SURN             → surname
SEX (M/F/U)           → gender (MALE/FEMALE/UNKNOWN)
BIRT/DATE             → birthDate
BIRT/PLAC             → birthPlace
DEAT/DATE             → deathDate
DEAT/PLAC             → deathPlace
NOTE                  → notes
OCCU                  → occupation (if added to model)
```

### Family Mapping (FAM → Relationships)

```java
GEDCOM Structure      → Relationships
--------------------- → ---------------------
FAM/@ID@              → (temp map, not stored)
HUSB + WIFE           → SPOUSE relationship
CHIL + HUSB/WIFE      → PARENT_CHILD relationship
MARR/DATE             → startDate (for SPOUSE)
DIV/DATE              → endDate (for SPOUSE)
```

### Date Handling

GEDCOM supports various date formats:
- `15 JAN 1950` - Exact date
- `ABT 1950` - About (approximate)
- `BEF 1950` - Before
- `AFT 1950` - After
- `BET 1950 AND 1955` - Between
- `CAL 1950` - Calculated
- `EST 1950` - Estimated

**Strategy**: Parse to LocalDate when possible, store as-is in string format for complex dates

---

## Implementation Plan

### Task 5.1: GEDCOM Service - Import ✅

**File**: `backend/src/main/java/com/familytree/service/GedcomService.java`

**Key Methods**:
```java
@Transactional
public GedcomImportResult importGedcom(MultipartFile file, String treeId, String userId)

private Map<String, Individual> processIndividuals(Gedcom gedcom, FamilyTree tree)

private List<Relationship> processFamilies(Gedcom gedcom, Map<String, Individual> individualMap)

private LocalDate parseGedcomDate(String gedcomDate)
```

**Error Handling**:
- File parsing errors
- Invalid GEDCOM structure
- Duplicate individuals
- Circular relationships
- Missing required fields

### Task 5.2: GEDCOM Service - Export ✅

**Key Methods**:
```java
public byte[] exportGedcom(String treeId, String userId)

private void addIndividualRecords(Gedcom gedcom, List<Individual> individuals)

private void addFamilyRecords(Gedcom gedcom, List<Relationship> relationships)

private String formatGedcomDate(LocalDate date)
```

### Task 5.3: REST API Controller ✅

**File**: `backend/src/main/java/com/familytree/controller/GedcomController.java`

**Endpoints**:
```java
@PostMapping("/api/trees/{treeId}/gedcom/import")
ResponseEntity<GedcomImportResult> importGedcom(@PathVariable String treeId, @RequestParam MultipartFile file)

@GetMapping("/api/trees/{treeId}/gedcom/export")
ResponseEntity<byte[]> exportGedcom(@PathVariable String treeId)
```

### Task 5.4: Import Result DTO

**File**: `backend/src/main/java/com/familytree/dto/gedcom/GedcomImportResult.java`

```java
public class GedcomImportResult {
    private int individualsImported;
    private int relationshipsImported;
    private List<String> warnings;
    private List<String> errors;
    private long processingTimeMs;
}
```

### Task 5.5: Frontend Import Component

**File**: `frontend/src/app/features/gedcom/gedcom-import/gedcom-import.component.ts`

**Features**:
- File upload (drag-drop or browse)
- .ged file validation
- Upload progress bar
- Import summary display
- Error handling
- Navigation to imported tree

### Task 5.6: Frontend Export Component

**File**: `frontend/src/app/features/gedcom/gedcom-export/gedcom-export.component.ts`

**Features**:
- Export button on tree view
- Download trigger
- Success notification
- Error handling

---

## Testing Strategy

### Backend Tests

**Unit Tests** (GedcomServiceTest.java):
- Test individual parsing (various name formats)
- Test date parsing (all GEDCOM date types)
- Test family relationship creation
- Test error handling (invalid GEDCOM)
- Test duplicate detection

**Integration Tests** (GedcomControllerTest.java):
- Test full import workflow
- Test full export workflow
- Test authentication/authorization
- Test with real GEDCOM samples

### Frontend Tests

**Component Tests**:
- Test file upload
- Test import result display
- Test export download

**E2E Tests**:
- Import GEDCOM → Verify tree created
- Export tree → Verify file downloads
- Round-trip test (export → import → verify data)

### Sample GEDCOM Files

Create test files:
- `simple-family.ged` - 3 individuals, 1 family
- `complex-family.ged` - Multiple generations
- `edge-cases.ged` - Various date formats, missing data
- `invalid.ged` - Malformed GEDCOM for error testing

---

## Security Considerations

1. **File Size Limits**: Max 10MB for GEDCOM files
2. **Validation**: Check file extension (.ged)
3. **Virus Scanning**: Consider for production
4. **Authorization**: Verify tree ownership
5. **Injection Prevention**: Sanitize all text fields
6. **Rate Limiting**: Prevent abuse of import/export

---

## Performance Optimization

1. **Batch Processing**: Save individuals/relationships in batches
2. **Transaction Management**: One transaction per import
3. **Lazy Loading**: Don't load unnecessary relationships
4. **Streaming**: For large exports, stream to response
5. **Caching**: Cache gedcom4j parser instance

---

## Edge Cases to Handle

1. **Multiple families for one person** (remarriage)
2. **Adopted children** - Map to ADOPTED_PARENT_CHILD
3. **Step-children** - Map to STEP_PARENT_CHILD
4. **Unknown dates** - Store as null or placeholder
5. **Non-standard date formats** - Store as-is in notes
6. **Missing spouse** - Create placeholder or skip
7. **Circular relationships** - Detect and reject
8. **Duplicate names** - Add to notes or index

---

## User Documentation

### Import Instructions

1. Go to tree management page
2. Click "Import GEDCOM"
3. Select .ged file
4. Review import summary
5. Confirm import
6. Navigate to imported tree

### Export Instructions

1. Open tree
2. Click "Export GEDCOM"
3. File downloads automatically
4. Use file with other genealogy software

---

## Known Limitations (for MVP)

- Only GEDCOM 5.5.1 supported (not 7.0)
- Media/photos not included in GEDCOM (future enhancement)
- Only basic date formats fully supported
- Source citations not imported (future)
- Notes may be truncated (length limits)

---

## Future Enhancements

- GEDCOM 7.0 support
- Media file embedding (_LINK tags)
- Source citation import/export
- Merge duplicate detection on import
- Preview before import
- Selective import (choose which individuals)
- Export filters (descendants only, etc.)

---

## References

- GEDCOM 5.5.1 Specification: https://gedcom.io/specifications/FamilySearchGEDCOMv5.html
- gedcom4j Documentation: https://gedcom4j.org/
- FamilySearch GEDCOM: https://www.familysearch.org/developers/docs/guides/gedcom
