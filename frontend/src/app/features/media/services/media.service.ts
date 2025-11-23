import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { environment } from '../../../../environments/environment';
import { Media, MediaUpdateRequest } from '../models/media.model';

@Injectable({
  providedIn: 'root'
})
export class MediaService {
  private apiUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Upload media for an individual
   */
  uploadMedia(individualId: string, file: File, caption?: string): Observable<{ progress: number; media?: Media }> {
    const formData = new FormData();
    formData.append('file', file);
    if (caption) {
      formData.append('caption', caption);
    }

    const req = new HttpRequest(
      'POST',
      `${this.apiUrl}/individuals/${individualId}/media`,
      formData,
      {
        reportProgress: true
      }
    );

    return this.http.request<Media>(req).pipe(
      map(event => {
        if (event.type === HttpEventType.UploadProgress) {
          const progress = event.total ? Math.round((100 * event.loaded) / event.total) : 0;
          return { progress };
        } else if (event.type === HttpEventType.Response) {
          return { progress: 100, media: event.body as Media };
        }
        return { progress: 0 };
      })
    );
  }

  /**
   * List media for an individual
   */
  listMediaForIndividual(individualId: string): Observable<Media[]> {
    return this.http.get<Media[]>(`${this.apiUrl}/individuals/${individualId}/media`);
  }

  /**
   * Get media by ID
   */
  getMedia(mediaId: string): Observable<Media> {
    return this.http.get<Media>(`${this.apiUrl}/media/${mediaId}`);
  }

  /**
   * Update media metadata
   */
  updateMedia(mediaId: string, request: MediaUpdateRequest): Observable<Media> {
    return this.http.put<Media>(`${this.apiUrl}/media/${mediaId}`, request);
  }

  /**
   * Delete media
   */
  deleteMedia(mediaId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/media/${mediaId}`);
  }

  /**
   * Get download URL for media
   */
  getDownloadUrl(mediaId: string): string {
    return `${this.apiUrl}/media/${mediaId}/download`;
  }

  /**
   * Get stream URL for media (inline viewing)
   */
  getStreamUrl(mediaId: string): string {
    return `${this.apiUrl}/media/${mediaId}/stream`;
  }

  /**
   * Get thumbnail URL for media
   */
  getThumbnailUrl(mediaId: string): string {
    return `${this.apiUrl}/media/${mediaId}/thumbnail`;
  }

  /**
   * Get thumbnail as blob URL (for authenticated requests)
   */
  getThumbnailBlobUrl(mediaId: string): Observable<string> {
    return this.http.get(this.getThumbnailUrl(mediaId), { responseType: 'blob' }).pipe(
      map(blob => window.URL.createObjectURL(blob))
    );
  }

  /**
   * Get stream as blob URL (for authenticated requests)
   */
  getStreamBlobUrl(mediaId: string): Observable<string> {
    return this.http.get(this.getStreamUrl(mediaId), { responseType: 'blob' }).pipe(
      map(blob => window.URL.createObjectURL(blob))
    );
  }

  /**
   * Download media file
   */
  downloadMedia(mediaId: string): void {
    // Use HttpClient to send JWT token
    this.http.get(this.getDownloadUrl(mediaId), { 
      responseType: 'blob',
      observe: 'response'
    }).subscribe({
      next: (response) => {
        // Extract filename from Content-Disposition header
        const contentDisposition = response.headers.get('Content-Disposition');
        let filename = 'download';
        if (contentDisposition) {
          const matches = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/.exec(contentDisposition);
          if (matches != null && matches[1]) {
            filename = matches[1].replace(/['"]/g, '');
          }
        }

        // Create blob URL and trigger download
        const blob = response.body;
        if (blob) {
          const url = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = url;
          link.download = filename;
          link.click();
          window.URL.revokeObjectURL(url);
        }
      },
      error: (error) => {
        console.error('Download failed:', error);
      }
    });
  }
}

