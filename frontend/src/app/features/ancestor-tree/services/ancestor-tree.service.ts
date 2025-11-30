import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';

export interface AncestorNode {
  id: string;
  givenName: string;
  surname: string;
  suffix: string;
  fullName: string;
  gender: string;
  birthDate: string;
  birthPlace: string;
  deathDate: string;
  deathPlace: string;
  profilePictureUrl: string;
  generation: number;
  parents: AncestorNode[];
}

export interface AncestorTreeResponse {
  root: AncestorNode;
  totalAncestors: number;
  maxGeneration: number;
}

@Injectable({
  providedIn: 'root'
})
export class AncestorTreeService {
  private readonly apiUrl = `${environment.apiUrl}/trees`;

  constructor(private http: HttpClient) { }

  /**
   * Get ancestor tree for an individual
   * @param treeId The family tree ID
   * @param individualId The individual to get ancestors for
   * @param generations Number of generations to fetch
   * @returns Observable of AncestorTreeResponse
   */
  getAncestorTree(treeId: string, individualId: string, generations: number = 3): Observable<AncestorTreeResponse> {
    const params = new HttpParams()
      .set('generations', generations.toString());

    return this.http.get<AncestorTreeResponse>(
      `${this.apiUrl}/${treeId}/individuals/${individualId}/ancestors`,
      { params }
    );
  }
}
