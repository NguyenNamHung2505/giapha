import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
    TreeMergeRequest,
    MergePreviewResponse,
    MergeResultResponse,
    MergeStrategiesResponse
} from '../models/merge.model';

@Injectable({
    providedIn: 'root'
})
export class MergeService {
    private readonly apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    /**
     * Preview a merge operation
     */
    previewMerge(targetTreeId: string, request: TreeMergeRequest): Observable<MergePreviewResponse> {
        return this.http.post<MergePreviewResponse>(
            `${this.apiUrl}/trees/${targetTreeId}/merge/preview`,
            request
        );
    }

    /**
     * Execute a merge operation
     */
    executeMerge(targetTreeId: string, request: TreeMergeRequest): Observable<MergeResultResponse> {
        return this.http.post<MergeResultResponse>(
            `${this.apiUrl}/trees/${targetTreeId}/merge/execute`,
            request
        );
    }

    /**
     * Get available merge strategies
     */
    getStrategies(targetTreeId: string): Observable<MergeStrategiesResponse> {
        return this.http.get<MergeStrategiesResponse>(
            `${this.apiUrl}/trees/${targetTreeId}/merge/strategies`
        );
    }
}
