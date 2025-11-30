import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  Relationship,
  CreateRelationshipRequest,
  UpdateRelationshipRequest,
  RelationshipPathResponse
} from '../models/relationship.model';

@Injectable({
  providedIn: 'root'
})
export class RelationshipService {
  private apiUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  /**
   * Create a new relationship in a tree
   */
  createRelationship(treeId: string, request: CreateRelationshipRequest): Observable<Relationship> {
    return this.http.post<Relationship>(`${this.apiUrl}/trees/${treeId}/relationships`, request);
  }

  /**
   * Get all relationships in a tree
   */
  getRelationshipsByTree(treeId: string): Observable<Relationship[]> {
    return this.http.get<Relationship[]>(`${this.apiUrl}/trees/${treeId}/relationships`);
  }

  /**
   * Get a specific relationship by ID
   */
  getRelationship(id: string): Observable<Relationship> {
    return this.http.get<Relationship>(`${this.apiUrl}/relationships/${id}`);
  }

  /**
   * Get all relationships for an individual
   */
  getRelationshipsForIndividual(individualId: string): Observable<Relationship[]> {
    return this.http.get<Relationship[]>(`${this.apiUrl}/individuals/${individualId}/relationships`);
  }

  /**
   * Get parents of an individual
   */
  getParents(individualId: string): Observable<Relationship[]> {
    return this.http.get<Relationship[]>(`${this.apiUrl}/individuals/${individualId}/parents`);
  }

  /**
   * Get children of an individual
   */
  getChildren(individualId: string): Observable<Relationship[]> {
    return this.http.get<Relationship[]>(`${this.apiUrl}/individuals/${individualId}/children`);
  }

  /**
   * Get spouses/partners of an individual
   */
  getSpouses(individualId: string): Observable<Relationship[]> {
    return this.http.get<Relationship[]>(`${this.apiUrl}/individuals/${individualId}/spouses`);
  }

  /**
   * Update a relationship
   */
  updateRelationship(id: string, request: UpdateRelationshipRequest): Observable<Relationship> {
    return this.http.put<Relationship>(`${this.apiUrl}/relationships/${id}`, request);
  }

  /**
   * Delete a relationship
   */
  deleteRelationship(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/relationships/${id}`);
  }

  /**
   * Calculate the relationship between two individuals
   * Returns detailed relationship information including path and bilingual terms
   */
  calculateRelationship(treeId: string, person1Id: string, person2Id: string): Observable<RelationshipPathResponse> {
    return this.http.get<RelationshipPathResponse>(
      `${this.apiUrl}/trees/${treeId}/relationship-path`,
      {
        params: {
          person1Id,
          person2Id
        }
      }
    );
  }
}
