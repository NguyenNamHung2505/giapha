export interface Tree {
  id: string;
  name: string;
  description: string;
  ownerId: string;
  ownerName: string;
  ownerEmail: string;
  individualsCount: number;
  relationshipsCount: number;
  createdAt: string;
  updatedAt: string;
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
