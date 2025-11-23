package com.familytree.controller;

import com.familytree.dto.gedcom.GedcomImportResult;
import com.familytree.security.UserPrincipal;
import com.familytree.service.GedcomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * REST Controller for GEDCOM import/export operations
 */
@RestController
@RequestMapping("/api/trees/{treeId}/gedcom")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
@RequiredArgsConstructor
public class GedcomController {

    private final GedcomService gedcomService;

    /**
     * Import a GEDCOM file into a tree
     *
     * POST /api/trees/{treeId}/gedcom/import
     *
     * @param treeId The ID of the tree to import into
     * @param file The GEDCOM file to import
     * @param userPrincipal The authenticated user
     * @return Import result with statistics and messages
     */
    @PostMapping("/import")
    public ResponseEntity<GedcomImportResult> importGedcom(
            @PathVariable String treeId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID userId = userPrincipal.getId();

            log.info("GEDCOM import requested by user {} for tree {}", userId, treeId);
            log.info("File: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

            GedcomImportResult result = gedcomService.importGedcom(file, treeUuid, userId);

            if (result.isSuccess()) {
                log.info("GEDCOM import successful: {} individuals, {} relationships",
                        result.getIndividualsImported(), result.getRelationshipsImported());
                return ResponseEntity.ok(result);
            } else {
                log.warn("GEDCOM import failed with {} errors", result.getErrors().size());
                return ResponseEntity.badRequest().body(result);
            }

        } catch (IllegalArgumentException e) {
            log.error("Invalid tree ID: {}", treeId);
            GedcomImportResult result = GedcomImportResult.builder()
                    .success(false)
                    .build();
            result.addError("Invalid tree ID");
            return ResponseEntity.badRequest().body(result);
        } catch (Exception e) {
            log.error("Unexpected error during GEDCOM import", e);
            GedcomImportResult result = GedcomImportResult.builder()
                    .success(false)
                    .build();
            result.addError("Unexpected error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    /**
     * Export a tree to GEDCOM format
     *
     * GET /api/trees/{treeId}/gedcom/export
     *
     * @param treeId The ID of the tree to export
     * @param userPrincipal The authenticated user
     * @return GEDCOM file as byte array
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportGedcom(
            @PathVariable String treeId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        try {
            UUID treeUuid = UUID.fromString(treeId);
            UUID userId = userPrincipal.getId();

            log.info("GEDCOM export requested by user {} for tree {}", userId, treeId);

            byte[] gedcomData = gedcomService.exportGedcom(treeUuid, userId);

            // Prepare response headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/x-gedcom"));
            headers.setContentDispositionFormData("attachment", "family-tree-" + treeId + ".ged");
            headers.setContentLength(gedcomData.length);

            log.info("GEDCOM export successful: {} bytes", gedcomData.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(gedcomData);

        } catch (IllegalArgumentException e) {
            log.error("Invalid tree ID: {}", treeId);
            return ResponseEntity.badRequest().build();
        } catch (IOException e) {
            log.error("IO error during GEDCOM export", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (Exception e) {
            log.error("Unexpected error during GEDCOM export", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get information about GEDCOM support
     *
     * GET /api/trees/{treeId}/gedcom/info
     *
     * @return Information about supported GEDCOM features
     */
    @GetMapping("/info")
    public ResponseEntity<GedcomInfo> getGedcomInfo() {
        GedcomInfo info = new GedcomInfo(
                "5.5.1",
                "Family Tree Manager",
                "Supports individuals, families, events, dates, and notes"
        );
        return ResponseEntity.ok(info);
    }

    /**
     * DTO for GEDCOM information (Java 11 compatible)
     */
    public static class GedcomInfo {
        private final String version;
        private final String application;
        private final String supportedFeatures;

        public GedcomInfo(String version, String application, String supportedFeatures) {
            this.version = version;
            this.application = application;
            this.supportedFeatures = supportedFeatures;
        }

        public String getVersion() { return version; }
        public String getApplication() { return application; }
        public String getSupportedFeatures() { return supportedFeatures; }
    }
}
