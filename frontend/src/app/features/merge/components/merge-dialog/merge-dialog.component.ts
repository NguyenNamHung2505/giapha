import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

import { MergeService } from '../../services/merge.service';
import { TreeService } from '../../../tree/services/tree.service';
import {
    TreeMergeRequest,
    MergePreviewResponse,
    MergeResultResponse,
    MergeStrategy,
    ConflictResolution,
    IndividualMatch,
    MergeConflict,
    IndividualPreview,
    FieldConflict,
    RelationshipPreview
} from '../../models/merge.model';
import { Tree } from '../../../tree/models/tree.model';

export interface MergeDialogData {
    targetTree: Tree;
    availableTrees: Tree[];
}

@Component({
    selector: 'app-merge-dialog',
    standalone: true,
    imports: [
        CommonModule,
        FormsModule,
        ReactiveFormsModule,
        MatDialogModule,
        MatButtonModule,
        MatSelectModule,
        MatFormFieldModule,
        MatInputModule,
        MatProgressSpinnerModule,
        MatIconModule,
        MatChipsModule,
        MatExpansionModule,
        MatDividerModule,
        MatProgressBarModule,
        MatTooltipModule,
        MatSnackBarModule,
        MatCheckboxModule,
        TranslateModule
    ],
    templateUrl: './merge-dialog.component.html',
    styleUrls: ['./merge-dialog.component.scss']
})
export class MergeDialogComponent implements OnInit {
    mergeForm: FormGroup;
    loading = false;
    previewLoading = false;
    preview: MergePreviewResponse | null = null;
    mergeResult: MergeResultResponse | null = null;
    step: 'select' | 'preview' | 'result' = 'select';

    // Selection tracking
    selectedIndividuals: Set<string> = new Set();

    strategies: { value: MergeStrategy; label: string; description: string }[] = [
        { value: 'IMPORT', label: 'Gộp cây', description: 'Gộp tất cả người từ cây nguồn vào cây đích.' }
    ];

    resolutions: { value: ConflictResolution; label: string }[] = [
        { value: 'OURS', label: 'Giữ dữ liệu cây đích' },
        { value: 'THEIRS', label: 'Dùng dữ liệu cây nguồn' },
        { value: 'AUTO_MERGE', label: 'Tự động gộp' }
    ];

    constructor(
        public dialogRef: MatDialogRef<MergeDialogComponent>,
        @Inject(MAT_DIALOG_DATA) public data: MergeDialogData,
        private fb: FormBuilder,
        private mergeService: MergeService,
        private snackBar: MatSnackBar,
        private translate: TranslateService
    ) {
        this.mergeForm = this.fb.group({
            sourceTreeId: ['', Validators.required],
            strategy: ['IMPORT', Validators.required],
            conflictResolution: ['OURS', Validators.required],
            includeMedia: [true]
        });
    }

    ngOnInit(): void { }

    get selectedStrategyDescription(): string {
        const strategy = this.mergeForm.get('strategy')?.value;
        const found = this.strategies.find(s => s.value === strategy);
        return found?.description || '';
    }

    get sourceTrees(): Tree[] {
        return this.data.availableTrees.filter(t => t.id !== this.data.targetTree.id);
    }

    // Selection methods
    isSelected(individualId: string): boolean {
        return this.selectedIndividuals.has(individualId);
    }

    toggleIndividual(individualId: string): void {
        if (this.selectedIndividuals.has(individualId)) {
            this.selectedIndividuals.delete(individualId);
        } else {
            this.selectedIndividuals.add(individualId);
        }
    }

    selectAll(): void {
        if (this.preview?.individualPreviews) {
            this.preview.individualPreviews.forEach(ind => {
                this.selectedIndividuals.add(ind.sourceIndividualId);
            });
        }
    }

    deselectAll(): void {
        this.selectedIndividuals.clear();
    }

    get allSelected(): boolean {
        if (!this.preview?.individualPreviews) return false;
        return this.preview.individualPreviews.every(ind =>
            this.selectedIndividuals.has(ind.sourceIndividualId));
    }

    get selectedCount(): number {
        return this.selectedIndividuals.size;
    }

    onPreview(): void {
        if (this.mergeForm.invalid) return;

        this.previewLoading = true;
        const request: TreeMergeRequest = {
            ...this.mergeForm.value,
            previewOnly: true
        };

        this.mergeService.previewMerge(this.data.targetTree.id, request).subscribe({
            next: (preview) => {
                this.preview = preview;
                this.step = 'preview';
                this.previewLoading = false;
                // Select all individuals by default
                this.selectAll();
            },
            error: (error) => {
                this.previewLoading = false;
                this.snackBar.open(error.error?.message || 'Lỗi khi xem trước gộp cây', 'Đóng', {
                    duration: 5000
                });
            }
        });
    }

    onExecuteMerge(): void {
        if (!this.preview?.canMerge) return;
        if (this.selectedIndividuals.size === 0) {
            this.snackBar.open('Vui lòng chọn ít nhất một người để gộp', 'Đóng', {
                duration: 3000
            });
            return;
        }

        this.loading = true;
        const request: TreeMergeRequest = {
            ...this.mergeForm.value,
            previewOnly: false,
            selectedIndividualIds: Array.from(this.selectedIndividuals)
        };

        this.mergeService.executeMerge(this.data.targetTree.id, request).subscribe({
            next: (result) => {
                this.mergeResult = result;
                this.step = 'result';
                this.loading = false;
            },
            error: (error) => {
                this.loading = false;
                this.snackBar.open(error.error?.message || 'Lỗi khi gộp cây', 'Đóng', {
                    duration: 5000
                });
            }
        });
    }

    onBackToSelect(): void {
        this.step = 'select';
        this.preview = null;
        this.selectedIndividuals.clear();
    }

    onClose(): void {
        this.dialogRef.close(this.mergeResult);
    }

    // Helper methods for preview display
    getMatchTypeLabelFromPreview(ind: IndividualPreview): string {
        switch (ind.matchType) {
            case 'CLONE_MAPPING': return 'Clone';
            case 'HIGH_CONFIDENCE': return 'Cao';
            case 'MEDIUM_CONFIDENCE': return 'Trung bình';
            case 'LOW_CONFIDENCE': return 'Thấp';
            case 'NEW': return 'Mới';
            default: return ind.matchType;
        }
    }

    /**
     * Convert raw match score to percentage (max possible score = 210)
     * 100 (name) + 80 (birthdate) + 30 (birthplace) = 210
     */
    getMatchPercentage(score: number): number {
        const MAX_SCORE = 210;
        return Math.min(100, Math.round((score / MAX_SCORE) * 100));
    }

    getMatchTypeColorFromPreview(ind: IndividualPreview): string {
        switch (ind.matchType) {
            case 'CLONE_MAPPING': return 'primary';
            case 'HIGH_CONFIDENCE': return 'accent';
            case 'MEDIUM_CONFIDENCE': return 'warn';
            case 'NEW': return '';
            default: return '';
        }
    }

    getRelationshipTypeLabel(type: string): string {
        switch (type) {
            case 'PARENT_CHILD': return 'Cha/Mẹ-Con';
            case 'FATHER_CHILD': return 'Cha-Con';
            case 'MOTHER_CHILD': return 'Mẹ-Con';
            case 'SPOUSE': return 'Vợ/Chồng';
            case 'SIBLING': return 'Anh/Chị/Em';
            default: return type;
        }
    }

    getMatchTypeLabel(match: IndividualMatch): string {
        switch (match.matchType) {
            case 'CLONE_MAPPING': return 'Clone';
            case 'HIGH_CONFIDENCE': return 'Cao';
            case 'MEDIUM_CONFIDENCE': return 'Trung bình';
            case 'LOW_CONFIDENCE': return 'Thấp';
            default: return match.matchType;
        }
    }

    getMatchTypeColor(match: IndividualMatch): string {
        switch (match.matchType) {
            case 'CLONE_MAPPING': return 'primary';
            case 'HIGH_CONFIDENCE': return 'accent';
            case 'MEDIUM_CONFIDENCE': return 'warn';
            default: return '';
        }
    }

    getConflictIcon(conflict: MergeConflict): string {
        switch (conflict.severity) {
            case 'ERROR': return 'error';
            case 'WARNING': return 'warning';
            default: return 'info';
        }
    }

    getConflictColor(conflict: MergeConflict): string {
        switch (conflict.severity) {
            case 'ERROR': return '#f44336';
            case 'WARNING': return '#ff9800';
            default: return '#2196f3';
        }
    }
}

