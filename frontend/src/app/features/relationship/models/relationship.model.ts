export enum RelationshipType {
  SPOUSE = 'SPOUSE',
  PARTNER = 'PARTNER',
  PARENT_CHILD = 'PARENT_CHILD',  // Deprecated - use MOTHER_CHILD or FATHER_CHILD
  MOTHER_CHILD = 'MOTHER_CHILD',
  FATHER_CHILD = 'FATHER_CHILD',
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

// Relationship Path Calculator Models

export enum RelationshipCategory {
  SELF = 'SELF',
  DIRECT_ANCESTOR = 'DIRECT_ANCESTOR',
  DIRECT_DESCENDANT = 'DIRECT_DESCENDANT',
  SIBLING = 'SIBLING',
  SPOUSE = 'SPOUSE',
  UNCLE_AUNT = 'UNCLE_AUNT',
  NEPHEW_NIECE = 'NEPHEW_NIECE',
  COUSIN = 'COUSIN',
  GRANDUNCLE_GRANDAUNT = 'GRANDUNCLE_GRANDAUNT',
  GRANDNEPHEW_GRANDNIECE = 'GRANDNEPHEW_GRANDNIECE',
  IN_LAW = 'IN_LAW',
  STEP_FAMILY = 'STEP_FAMILY',
  NOT_RELATED = 'NOT_RELATED'
}

export interface PersonSummary {
  id: string;
  fullName: string;
  givenName: string;
  surname: string;
  gender?: string;
  birthDate?: string;
  deathDate?: string;
}

export interface PathStep {
  person: PersonSummary;
  relationshipToNext: string;
  relationshipToNextVi: string;
}

export interface RelationshipPathResponse {
  person1: PersonSummary;
  person2: PersonSummary;
  relationshipFromPerson1: string;
  relationshipFromPerson2: string;
  relationshipFromPerson1Vi: string;
  relationshipFromPerson2Vi: string;
  category: RelationshipCategory;
  generationDifference: number;
  path: PathStep[];
  relationshipFound: boolean;
  commonAncestor?: PersonSummary;
}
