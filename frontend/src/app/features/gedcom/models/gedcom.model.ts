/**
 * Result of GEDCOM import operation
 */
export interface GedcomImportResult {
  individualsImported: number;
  relationshipsImported: number;
  warnings: string[];
  errors: string[];
  processingTimeMs: number;
  success: boolean;
  treeId: string;
}

/**
 * Information about GEDCOM support
 */
export interface GedcomInfo {
  version: string;
  application: string;
  supportedFeatures: string;
}
