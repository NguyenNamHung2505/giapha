import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Collaborator, Invitation, InviteCollaboratorRequest, PermissionRole } from '../models/collaboration.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class CollaborationService {
  private apiUrl = `${environment.apiUrl}`;

  constructor(private http: HttpClient) {}

  // Collaborator Management
  getCollaborators(treeId: string): Observable<Collaborator[]> {
    return this.http.get<Collaborator[]>(`${this.apiUrl}/trees/${treeId}/collaboration/collaborators`);
  }

  inviteCollaborator(treeId: string, request: InviteCollaboratorRequest): Observable<Invitation> {
    return this.http.post<Invitation>(`${this.apiUrl}/trees/${treeId}/collaboration/invite`, request);
  }

  updateCollaboratorRole(treeId: string, collaboratorId: string, role: PermissionRole): Observable<void> {
    const params = new HttpParams().set('role', role);
    return this.http.put<void>(
      `${this.apiUrl}/trees/${treeId}/collaboration/collaborators/${collaboratorId}/role`,
      null,
      { params }
    );
  }

  removeCollaborator(treeId: string, collaboratorId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/trees/${treeId}/collaboration/collaborators/${collaboratorId}`);
  }

  leaveTree(treeId: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/trees/${treeId}/collaboration/leave`, null);
  }

  // Invitation Management
  getTreeInvitations(treeId: string): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.apiUrl}/trees/${treeId}/collaboration/invitations`);
  }

  cancelInvitation(treeId: string, invitationId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/trees/${treeId}/collaboration/invitations/${invitationId}`);
  }

  getMyInvitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${this.apiUrl}/invitations/my-invitations`);
  }

  getInvitationByToken(token: string): Observable<Invitation> {
    return this.http.get<Invitation>(`${this.apiUrl}/invitations/${token}`);
  }

  acceptInvitation(token: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/invitations/${token}/accept`, null);
  }

  declineInvitation(token: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/invitations/${token}/decline`, null);
  }
}
