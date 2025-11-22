import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Tree, CreateTreeRequest, UpdateTreeRequest, TreePage } from '../models/tree.model';

@Injectable({
  providedIn: 'root'
})
export class TreeService {
  private readonly apiUrl = `${environment.apiUrl}/trees`;

  constructor(private http: HttpClient) { }

  /**
   * Create a new family tree
   */
  createTree(request: CreateTreeRequest): Observable<Tree> {
    return this.http.post<Tree>(this.apiUrl, request);
  }

  /**
   * Get all trees with pagination
   */
  getTrees(page: number = 0, size: number = 20): Observable<TreePage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<TreePage>(this.apiUrl, { params });
  }

  /**
   * Get a specific tree by ID
   */
  getTree(id: string): Observable<Tree> {
    return this.http.get<Tree>(`${this.apiUrl}/${id}`);
  }

  /**
   * Update a tree
   */
  updateTree(id: string, request: UpdateTreeRequest): Observable<Tree> {
    return this.http.put<Tree>(`${this.apiUrl}/${id}`, request);
  }

  /**
   * Delete a tree
   */
  deleteTree(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
