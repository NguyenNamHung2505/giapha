import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';

export interface IndividualInfo {
  id: string;
  givenName: string;
  surname: string;
  fullName: string;
  gender: string;
  birthDate: string;
  profilePictureUrl: string;
}

export interface UserTreeProfile {
  id: string;
  treeId: string;
  treeName: string;
  individual: IndividualInfo;
  createdAt: string;
}

export interface LinkProfileRequest {
  individualId: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserTreeProfileService {
  private readonly apiUrl = `${environment.apiUrl}/trees`;

  constructor(private http: HttpClient) { }

  /**
   * Get current user's profile mapping for a tree
   * Returns null if no mapping exists
   */
  getMyProfile(treeId: string): Observable<UserTreeProfile | null> {
    return this.http.get<UserTreeProfile>(`${this.apiUrl}/${treeId}/my-profile`).pipe(
      catchError(() => of(null))
    );
  }

  /**
   * Link current user to an individual in the tree
   */
  linkToIndividual(treeId: string, individualId: string): Observable<UserTreeProfile> {
    const request: LinkProfileRequest = { individualId };
    return this.http.post<UserTreeProfile>(`${this.apiUrl}/${treeId}/my-profile`, request);
  }

  /**
   * Remove current user's profile mapping
   */
  unlinkFromIndividual(treeId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${treeId}/my-profile`);
  }
}
