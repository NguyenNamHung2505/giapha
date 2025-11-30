import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface UserResponse {
  id: string;
  username: string;
  email?: string;
  name: string;
  admin: boolean;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface UserPage {
  content: UserResponse[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface IndividualInfo {
  id: string;
  fullName: string;
  gender: string;
  birthDate: string;
  profilePictureUrl: string;
}

export interface UserWithProfile {
  id: string;
  username: string;
  email?: string;
  name: string;
  admin: boolean;
  enabled: boolean;
  createdAt: string;
  linkedIndividual: IndividualInfo | null;
}

export interface CreateUserRequest {
  username: string;
  email?: string;
  password: string;
  name: string;
  admin: boolean;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  name?: string;
  password?: string;
  admin?: boolean;
  enabled?: boolean;
}

export interface LinkUserRequest {
  userId: string;
  individualId: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly apiUrl = `${environment.apiUrl}/admin`;

  constructor(private http: HttpClient) { }

  // ==================== User Management ====================

  /**
   * Get all users with pagination
   */
  getUsers(page: number = 0, size: number = 20): Observable<UserPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<UserPage>(`${this.apiUrl}/users`, { params });
  }

  /**
   * Get all users with their profile mappings for a tree
   */
  getUsersWithProfiles(treeId: string): Observable<UserWithProfile[]> {
    return this.http.get<UserWithProfile[]>(`${this.apiUrl}/trees/${treeId}/users`);
  }

  /**
   * Create a new user
   */
  createUser(request: CreateUserRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.apiUrl}/users`, request);
  }

  /**
   * Update a user
   */
  updateUser(userId: string, request: UpdateUserRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.apiUrl}/users/${userId}`, request);
  }

  /**
   * Delete a user
   */
  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/users/${userId}`);
  }

  // ==================== User-Individual Mapping ====================

  /**
   * Link a user to an individual in a tree
   */
  linkUserToIndividual(treeId: string, userId: string, individualId: string): Observable<any> {
    const request: LinkUserRequest = { userId, individualId };
    return this.http.post(`${this.apiUrl}/trees/${treeId}/user-profiles`, request);
  }

  /**
   * Unlink a user from an individual in a tree
   */
  unlinkUserFromIndividual(treeId: string, userId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/trees/${treeId}/user-profiles/${userId}`);
  }

  // ==================== Create Users from Tree ====================

  /**
   * Create a user from a specific individual in a tree
   */
  createUserFromIndividual(treeId: string, individualId: string): Observable<UserResponse> {
    return this.http.post<UserResponse>(
      `${this.apiUrl}/trees/${treeId}/create-user-from-individual/${individualId}`,
      {}
    );
  }

  /**
   * Bulk create users from all unlinked individuals in a tree
   */
  createUsersFromTree(treeId: string): Observable<UserResponse[]> {
    return this.http.post<UserResponse[]>(
      `${this.apiUrl}/trees/${treeId}/create-users-from-tree`,
      {}
    );
  }
}
