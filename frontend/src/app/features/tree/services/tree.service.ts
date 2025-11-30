import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Tree, CreateTreeRequest, UpdateTreeRequest, TreePage, CreateTreeFromIndividualRequest, CreateTreeFromIndividualResponse, IndividualCloneInfo, TreeCloneInfo } from '../models/tree.model';

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

  /**
   * Create a new tree from an individual (clone ancestors and descendants)
   */
  createTreeFromIndividual(request: CreateTreeFromIndividualRequest): Observable<CreateTreeFromIndividualResponse> {
    return this.http.post<CreateTreeFromIndividualResponse>(`${this.apiUrl}/from-individual`, request);
  }

  /**
   * Check if an individual has already been exported to a separate tree
   */
  checkIndividualExported(treeId: string, individualId: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.apiUrl}/${treeId}/individuals/${individualId}/is-exported`);
  }

  /**
   * Get clone information for an individual
   * Shows both trees this individual was cloned TO and source info if this is a clone
   */
  getIndividualCloneInfo(treeId: string, individualId: string): Observable<IndividualCloneInfo> {
    return this.http.get<IndividualCloneInfo>(`${this.apiUrl}/${treeId}/individuals/${individualId}/clone-info`);
  }

  /**
   * Get clone information for a tree
   * Shows if this tree is a clone or has been cloned, and provides navigation to related trees
   */
  getTreeCloneInfo(treeId: string): Observable<TreeCloneInfo> {
    return this.http.get<TreeCloneInfo>(`${this.apiUrl}/${treeId}/clone-info`);
  }

  /**
   * Add a tree admin
   * Only the owner can add tree admins
   */
  addTreeAdmin(treeId: string, adminUserId: string): Observable<Tree> {
    return this.http.post<Tree>(`${this.apiUrl}/${treeId}/admins/${adminUserId}`, {});
  }

  /**
   * Remove a tree admin
   * Only the owner can remove tree admins
   */
  removeTreeAdmin(treeId: string, adminUserId: string): Observable<Tree> {
    return this.http.delete<Tree>(`${this.apiUrl}/${treeId}/admins/${adminUserId}`);
  }
}
