// Merge Strategy - only IMPORT for combining trees
// SYNC is a separate feature on tree detail page
export type MergeStrategy = 'IMPORT';

// Conflict Resolution enum
export type ConflictResolution = 'THEIRS' | 'OURS' | 'MANUAL' | 'AUTO_MERGE';

// Match Type enum
export type MatchType = 'CLONE_MAPPING' | 'HIGH_CONFIDENCE' | 'MEDIUM_CONFIDENCE' | 'LOW_CONFIDENCE' | 'MANUAL';

// Conflict severity
export type ConflictSeverity = 'INFO' | 'WARNING' | 'ERROR';

// Conflict type
export type ConflictType = 'DATA_MISMATCH' | 'DELETED_IN_SOURCE' | 'DELETED_IN_TARGET' | 'RELATIONSHIP_MISMATCH' | 'POSSIBLE_DUPLICATE';

// Merge request
export interface TreeMergeRequest {
  sourceTreeId: string;
  strategy: MergeStrategy;
  conflictResolution: ConflictResolution;
  selectedIndividualIds?: string[];
  includeMedia: boolean;
  previewOnly: boolean;
  manualResolutions?: ManualConflictResolution[];
}

export interface ManualConflictResolution {
  conflictId: string;
  field: string;
  chosenValue: string;
  useSource: boolean;
}

// Individual match
export interface IndividualMatch {
  targetIndividualId: string;
  sourceIndividualId: string;
  targetName: string;
  sourceName: string;
  matchScore: number;
  matchType: MatchType;
  matchReason: string;
  clonedMatch: boolean;
}

// Merge conflict
export interface MergeConflict {
  conflictId: string;
  targetIndividualId: string;
  sourceIndividualId: string;
  individualName: string;
  conflictType: ConflictType;
  field: string;
  targetValue: string;
  sourceValue: string;
  severity: ConflictSeverity;
  suggestion: string;
}

// Individual info
export interface MergeIndividualInfo {
  id: string;
  name: string;
  birthDate?: string;
  deathDate?: string;
  gender?: string;
  sourceIndividualId?: string;
}

// Relationship info
export interface MergeRelationshipInfo {
  id: string;
  individual1Name: string;
  individual2Name: string;
  type: string;
  sourceRelationshipId?: string;
}

// Validation error
export interface MergeValidationError {
  code: string;
  message: string;
  field?: string;
  entityId?: string;
  blocking: boolean;
}

// Merge summary
export interface MergeSummary {
  totalMatchedIndividuals: number;
  totalNewIndividuals: number;
  totalUpdatedIndividuals: number;
  totalNewRelationships: number;
  totalConflicts: number;
  totalErrors: number;
  totalWarnings: number;
}

// Field conflict for an individual
export interface FieldConflict {
  field: string;
  sourceValue: string;
  targetValue: string;
  resolvedValue?: string;
}

// Relationship preview grouped under individual
export interface RelationshipPreview {
  sourceRelationshipId: string;
  type: string;
  relatedPersonName: string;
  relatedPersonSourceId: string;
  isParentRelation: boolean;
  existsInTarget: boolean;
}

// Detailed individual preview for interactive selection
export interface IndividualPreview {
  // Source individual info
  sourceIndividualId: string;
  sourceName: string;
  sourceBirthDate?: string;
  sourceDeathDate?: string;
  sourceGender?: string;

  // Target individual info (null if new)
  targetIndividualId?: string;
  targetName?: string;
  targetBirthDate?: string;
  targetDeathDate?: string;

  // Match info
  matchType: string; // CLONE_MAPPING, HIGH_CONFIDENCE, MEDIUM_CONFIDENCE, LOW_CONFIDENCE, NEW
  matchScore: number;
  isNew: boolean;
  hasConflicts: boolean;

  // Conflicts for this individual
  conflicts: FieldConflict[];

  // Relationships from source (grouped under this individual)
  relationships: RelationshipPreview[];
}

// Merge preview response
export interface MergePreviewResponse {
  targetTreeId: string;
  sourceTreeId: string;
  strategy: MergeStrategy;
  isPreview: boolean;
  summary: MergeSummary;
  matchedIndividuals: IndividualMatch[];
  newIndividuals: MergeIndividualInfo[];
  updatedIndividuals: MergeIndividualInfo[];
  newRelationships: MergeRelationshipInfo[];
  conflicts: MergeConflict[];
  individualPreviews: IndividualPreview[];
  validationErrors: MergeValidationError[];
  warnings: string[];
  canMerge: boolean;
  generatedAt: string;
}

// Merge result
export interface MergeResultResponse {
  success: boolean;
  targetTreeId: string;
  sourceTreeId: string;
  mergeId: string;
  summary: {
    individualsAdded: number;
    individualsUpdated: number;
    relationshipsAdded: number;
    mediaFilesAdded: number;
    conflictsResolved: number;
  };
  message: string;
  mergedAt: string;
  canUndo: boolean;
}

// Strategy info
export interface StrategyInfo {
  strategy: MergeStrategy;
  name: string;
  description: string;
  requiresCloneRelation: boolean;
}

export interface MergeStrategiesResponse {
  strategies: StrategyInfo[];
}

