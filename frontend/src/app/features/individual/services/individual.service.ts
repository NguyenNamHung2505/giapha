import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Individual, CreateIndividualRequest, UpdateIndividualRequest, IndividualPage } from '../models/individual.model';

@Injectable({
  providedIn: 'root'
})
export class IndividualService {
  private readonly apiUrl = `${environment.apiUrl}/trees`;

  constructor(private http: HttpClient) { }

  /**
   * Create a new individual in a tree
   */
  createIndividual(treeId: string, request: CreateIndividualRequest): Observable<Individual> {
    return this.http.post<Individual>(`${this.apiUrl}/${treeId}/individuals`, request);
  }

  /**
   * Get all individuals in a tree with pagination
   */
  getIndividuals(treeId: string, page: number = 0, size: number = 20, search?: string): Observable<IndividualPage> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (search && search.trim()) {
      params = params.set('search', search.trim());
    }

    return this.http.get<IndividualPage>(`${this.apiUrl}/${treeId}/individuals`, { params });
  }

  /**
   * Get a specific individual by ID
   */
  getIndividual(treeId: string, id: string): Observable<Individual> {
    return this.http.get<Individual>(`${this.apiUrl}/${treeId}/individuals/${id}`);
  }

  /**
   * Update an individual
   */
  updateIndividual(treeId: string, id: string, request: UpdateIndividualRequest): Observable<Individual> {
    return this.http.put<Individual>(`${this.apiUrl}/${treeId}/individuals/${id}`, request);
  }

  /**
   * Delete an individual
   */
  deleteIndividual(treeId: string, id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${treeId}/individuals/${id}`);
  }
}
