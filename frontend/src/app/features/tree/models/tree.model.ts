export interface Tree {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  ownerName: string;
  ownerEmail: string;
  // Tree Admins - can edit content but different from owner
  admins: AdminInfo[];
  individualsCount: number;
  relationshipsCount: number;
  rootIndividualId?: string;
  rootIndividualName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface AdminInfo {
  id: string;
  name: string;
  email: string;
}

export interface CreateTreeRequest {
  name: string;
  description?: string;
}

export interface UpdateTreeRequest {
  name: string;
  description?: string;
}

export interface TreePage {
  content: Tree[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  first: boolean;
  numberOfElements: number;
  size: number;
  number: number;
  empty: boolean;
}

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
  rootIndividualId: string;
  totalIndividuals: number;
  totalRelationships: number;
  totalMediaFiles: number;
  sourceTreeId: string;
  sourceIndividualId: string;
  clonedAt: string;
  message: string;
}

export interface IndividualCloneInfo {
  individualId: string;
  individualName: string;
  hasClones: boolean;
  clone: boolean; // JSON serializes "isClone" as "clone"
  rootClonedPerson: boolean; // True only for the person selected as root when cloning
  clonedToTrees: ClonedTreeInfo[];
  sourceInfo: SourceInfo | null;
  allTreeLocations: TreeLocation[];
}

export interface TreeLocation {
  treeId: string;
  treeName: string;
  individualId: string;
  isCurrentTree: boolean;
  isSourceTree: boolean;
}

export interface ClonedTreeInfo {
  clonedTreeId: string;
  clonedTreeName: string;
  clonedIndividualId: string;
  clonedIndividualName: string;
  isRootOfClone: boolean;
  clonedAt: string;
}

export interface SourceInfo {
  sourceTreeId: string;
  sourceTreeName: string;
  sourceIndividualId: string;
  sourceIndividualName: string;
  clonedAt: string;
}

// Tree Clone Info Response
export interface TreeCloneInfo {
  treeId: string;
  treeName: string;
  isClone: boolean;
  hasClones: boolean;
  sourceTreeInfo: SourceTreeInfo | null;
  clonedTrees: ClonedTree[];
  allRelatedTrees: RelatedTree[];
}

export interface SourceTreeInfo {
  sourceTreeId: string;
  sourceTreeName: string;
  sourceIndividualId: string;
  sourceIndividualName: string;
  clonedAt: string;
}

export interface ClonedTree {
  clonedTreeId: string;
  clonedTreeName: string;
  rootIndividualId: string;
  rootIndividualName: string;
  clonedAt: string;
}

export interface RelatedTree {
  treeId: string;
  treeName: string;
  isCurrentTree: boolean;
  isSourceTree: boolean;
  clonedAt: string;
}
