package com.familytree.dto.gedcom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a GEDCOM import operation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GedcomImportResult {

    /**
     * Number of individuals successfully imported
     */
    private int individualsImported;

    /**
     * Number of relationships successfully imported
     */
    private int relationshipsImported;

    /**
     * Warning messages (non-fatal issues)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Error messages (fatal issues that prevented import)
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * Time taken to process the import in milliseconds
     */
    private long processingTimeMs;

    /**
     * Whether the import was successful
     */
    private boolean success;

    /**
     * ID of the tree that was imported into
     */
    private String treeId;

    /**
     * Add a warning message
     */
    public void addWarning(String warning) {
        if (warnings == null) {
            warnings = new ArrayList<>();
        }
        warnings.add(warning);
    }

    /**
     * Add an error message
     */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    /**
     * Check if there are any errors
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Check if there are any warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}
