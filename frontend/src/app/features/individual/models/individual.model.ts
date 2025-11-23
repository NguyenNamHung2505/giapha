export enum Gender {
  MALE = 'MALE',
  FEMALE = 'FEMALE',
  OTHER = 'OTHER',
  UNKNOWN = 'UNKNOWN'
}

export interface Individual {
  id: string;
  treeId: string;
  treeName: string;
  givenName: string;
  surname: string;
  suffix?: string;
  fullName: string;
  gender?: Gender;
  birthDate?: string;
  birthPlace?: string;
  deathDate?: string;
  deathPlace?: string;
  biography?: string;
  profilePictureUrl?: string;
  mediaCount: number;
  eventCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateIndividualRequest {
  givenName?: string;
  surname?: string;
  suffix?: string;
  gender?: Gender;
  birthDate?: string;
  birthPlace?: string;
  deathDate?: string;
  deathPlace?: string;
  biography?: string;
}

export interface UpdateIndividualRequest {
  givenName?: string;
  surname?: string;
  suffix?: string;
  gender?: Gender;
  birthDate?: string;
  birthPlace?: string;
  deathDate?: string;
  deathPlace?: string;
  biography?: string;
}

export interface IndividualPage {
  content: Individual[];
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
