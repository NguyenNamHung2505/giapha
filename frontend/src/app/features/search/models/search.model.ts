export interface SearchRequest {
  query?: string;
  gender?: string;
  birthYearFrom?: number;
  birthYearTo?: number;
  deathYearFrom?: number;
  deathYearTo?: number;
  birthPlace?: string;
  deathPlace?: string;
  page?: number;
  size?: number;
}

export interface SearchResult {
  id: string;
  givenName: string;
  surname: string;
  fullName: string;
  gender: string;
  birthDate?: string;
  birthPlace?: string;
  deathDate?: string;
  deathPlace?: string;
  treeId: string;
  treeName: string;
  relevanceScore: number;
}

export interface SearchResponse {
  content: SearchResult[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
}
