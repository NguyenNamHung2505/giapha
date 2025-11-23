export enum RelationshipType {
  SPOUSE = 'SPOUSE',
  PARTNER = 'PARTNER',
  PARENT_CHILD = 'PARENT_CHILD',
  SIBLING = 'SIBLING',
  ADOPTED_PARENT_CHILD = 'ADOPTED_PARENT_CHILD',
  STEP_PARENT_CHILD = 'STEP_PARENT_CHILD',
  HALF_SIBLING = 'HALF_SIBLING',
  STEP_SIBLING = 'STEP_SIBLING'
}

export interface IndividualSummary {
  id: string;
  givenName: string;
  surname: string;
  fullName: string;
  birthDate?: string;
  deathDate?: string;
}

export interface Relationship {
  id: string;
  treeId: string;
  individual1: IndividualSummary;
  individual2: IndividualSummary;
  type: RelationshipType;
  startDate?: string;
  endDate?: string;
  createdAt: string;
}

export interface CreateRelationshipRequest {
  individual1Id: string;
  individual2Id: string;
  type: RelationshipType;
  startDate?: string;
  endDate?: string;
}

export interface UpdateRelationshipRequest {
  startDate?: string;
  endDate?: string;
}
