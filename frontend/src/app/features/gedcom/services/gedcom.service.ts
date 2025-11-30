import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { GedcomImportResult, GedcomInfo } from '../models/gedcom.model';
import { environment } from '../../../../environments/environment';

/**
 * Service for GEDCOM import/export operations
 */
@Injectable({
  providedIn: 'root'
})
export class GedcomService {
  private apiUrl = `${environment.apiUrl}/trees`;

  constructor(private http: HttpClient) {}

  /**
   * Import a GEDCOM file into a tree
   */
  importGedcom(treeId: string, file: File): Observable<GedcomImportResult> {
    const formData = new FormData();
    formData.append('file', file);

    return this.http.post<GedcomImportResult>(
      `${this.apiUrl}/${treeId}/gedcom/import`,
      formData
    );
  }

  /**
   * Export a tree to GEDCOM format
   */
  exportGedcom(treeId: string, treeName?: string): Observable<Blob> {
    return this.http.get(
      `${this.apiUrl}/${treeId}/gedcom/export`,
      { responseType: 'blob' }
    ).pipe(
      map(blob => {
        // Trigger download
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${treeName || 'family-tree'}-${treeId}.ged`;
        link.click();
        window.URL.revokeObjectURL(url);
        return blob;
      })
    );
  }

  /**
   * Get information about GEDCOM support
   */
  getGedcomInfo(treeId: string): Observable<GedcomInfo> {
    return this.http.get<GedcomInfo>(`${this.apiUrl}/${treeId}/gedcom/info`);
  }
}
