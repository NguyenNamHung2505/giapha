import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { SearchRequest, SearchResult, SearchResponse } from '../models/search.model';
import { environment } from '../../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SearchService {
  private apiUrl = `${environment.apiUrl}/search`;

  constructor(private http: HttpClient) {}

  /**
   * Search individuals in a specific tree
   */
  searchInTree(treeId: string, request: SearchRequest): Observable<SearchResponse> {
    let params = new HttpParams();

    if (request.query) params = params.set('query', request.query);
    if (request.gender) params = params.set('gender', request.gender);
    if (request.birthYearFrom) params = params.set('birthYearFrom', request.birthYearFrom.toString());
    if (request.birthYearTo) params = params.set('birthYearTo', request.birthYearTo.toString());
    if (request.deathYearFrom) params = params.set('deathYearFrom', request.deathYearFrom.toString());
    if (request.deathYearTo) params = params.set('deathYearTo', request.deathYearTo.toString());
    if (request.birthPlace) params = params.set('birthPlace', request.birthPlace);
    if (request.deathPlace) params = params.set('deathPlace', request.deathPlace);
    if (request.page !== undefined) params = params.set('page', request.page.toString());
    if (request.size !== undefined) params = params.set('size', request.size.toString());

    return this.http.get<SearchResponse>(`${this.apiUrl}/trees/${treeId}`, { params });
  }

  /**
   * Global search across all accessible trees
   */
  globalSearch(query: string, maxResults: number = 10): Observable<SearchResult[]> {
    const params = new HttpParams()
      .set('query', query)
      .set('maxResults', maxResults.toString());

    return this.http.get<SearchResult[]>(`${this.apiUrl}/global`, { params });
  }

  /**
   * Search with debounce for autocomplete
   */
  searchWithDebounce(query$: Observable<string>, treeId?: string, debounce: number = 300): Observable<SearchResult[]> {
    return query$.pipe(
      debounceTime(debounce),
      distinctUntilChanged(),
      switchMap(query => {
        if (!query || query.length < 2) {
          return new Observable<SearchResult[]>(observer => {
            observer.next([]);
            observer.complete();
          });
        }

        if (treeId) {
          return this.searchInTree(treeId, { query, size: 10 }).pipe(
            switchMap(response => new Observable<SearchResult[]>(observer => {
              observer.next(response.content);
              observer.complete();
            }))
          );
        } else {
          return this.globalSearch(query, 10);
        }
      })
    );
  }
}
