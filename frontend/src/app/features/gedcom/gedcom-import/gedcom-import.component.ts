import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatListModule } from '@angular/material/list';
import { MatChipsModule } from '@angular/material/chips';
import { GedcomService } from '../services/gedcom.service';
import { GedcomImportResult } from '../models/gedcom.model';

export interface GedcomImportData {
  treeId: string;
  treeName: string;
}

@Component({
  selector: 'app-gedcom-import',
  standalone: true,
  imports: [
    CommonModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatListModule,
    MatChipsModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>upload_file</mat-icon>
      Import GEDCOM File
    </h2>

    <mat-dialog-content>
      <!-- File Selection -->
      <div *ngIf="!selectedFile && !result" class="upload-section">
        <div
          class="drop-zone"
          (click)="fileInput.click()"
          (dragover)="onDragOver($event)"
          (dragleave)="onDragLeave($event)"
          (drop)="onDrop($event)"
          [class.drag-over]="isDragOver">

          <mat-icon class="upload-icon">cloud_upload</mat-icon>
          <h3>Drop GEDCOM file here or click to browse</h3>
          <p>Supported format: .ged (GEDCOM 5.5.1)</p>
          <p class="size-limit">Maximum file size: 10MB</p>

          <input
            #fileInput
            type="file"
            accept=".ged"
            (change)="onFileSelected($event)"
            style="display: none;">
        </div>

        <div *ngIf="errorMessage" class="error-message">
          <mat-icon>error</mat-icon>
          {{ errorMessage }}
        </div>
      </div>

      <!-- File Selected -->
      <div *ngIf="selectedFile && !uploading && !result" class="file-info">
        <mat-icon class="file-icon">description</mat-icon>
        <div class="file-details">
          <h4>{{ selectedFile.name }}</h4>
          <p>{{ formatFileSize(selectedFile.size) }}</p>
        </div>
        <button mat-icon-button (click)="clearFile()" matTooltip="Remove">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Uploading -->
      <div *ngIf="uploading" class="uploading">
        <mat-spinner diameter="50"></mat-spinner>
        <h3>Importing GEDCOM file...</h3>
        <p>Please wait while we process your family tree data</p>
      </div>

      <!-- Import Result -->
      <div *ngIf="result" class="result">
        <div class="result-header" [class.success]="result.success" [class.error]="!result.success">
          <mat-icon>{{ result.success ? 'check_circle' : 'error' }}</mat-icon>
          <h3>{{ result.success ? 'Import Successful!' : 'Import Failed' }}</h3>
        </div>

        <div class="result-stats" *ngIf="result.success">
          <div class="stat">
            <mat-icon>person</mat-icon>
            <span>{{ result.individualsImported }} individuals imported</span>
          </div>
          <div class="stat">
            <mat-icon>family_restroom</mat-icon>
            <span>{{ result.relationshipsImported }} relationships imported</span>
          </div>
          <div class="stat">
            <mat-icon>timer</mat-icon>
            <span>Completed in {{ result.processingTimeMs }}ms</span>
          </div>
        </div>

        <!-- Warnings -->
        <div *ngIf="result.warnings && result.warnings.length > 0" class="messages warnings">
          <h4>
            <mat-icon>warning</mat-icon>
            Warnings ({{ result.warnings.length }})
          </h4>
          <mat-list dense>
            <mat-list-item *ngFor="let warning of result.warnings">
              {{ warning }}
            </mat-list-item>
          </mat-list>
        </div>

        <!-- Errors -->
        <div *ngIf="result.errors && result.errors.length > 0" class="messages errors">
          <h4>
            <mat-icon>error</mat-icon>
            Errors ({{ result.errors.length }})
          </h4>
          <mat-list dense>
            <mat-list-item *ngIf="result.errors && result.errors.length > 0" class="messages errors">
              {{ result.errors }}
            </mat-list-item>
          </mat-list>
        </div>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">
        {{ result ? 'Close' : 'Cancel' }}
      </button>
      <button
        mat-raised-button
        color="primary"
        *ngIf="selectedFile && !uploading && !result"
        (click)="onImport()">
        <mat-icon>upload</mat-icon>
        Import
      </button>
      <button
        mat-raised-button
        color="primary"
        *ngIf="result && result.success"
        (click)="onViewTree()">
        <mat-icon>visibility</mat-icon>
        View Tree
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      min-width: 500px;
      min-height: 300px;
      padding: 20px;
    }

    h2[mat-dialog-title] {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .upload-section {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .drop-zone {
      border: 2px dashed #ccc;
      border-radius: 8px;
      padding: 40px;
      text-align: center;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .drop-zone:hover,
    .drop-zone.drag-over {
      border-color: #3f51b5;
      background: #f5f5f5;
    }

    .upload-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #999;
      margin-bottom: 16px;
    }

    .drop-zone h3 {
      margin: 0 0 8px 0;
      color: #333;
    }

    .drop-zone p {
      margin: 4px 0;
      color: #666;
    }

    .size-limit {
      font-size: 12px;
      color: #999;
    }

    .file-info {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px;
      background: #f5f5f5;
      border-radius: 8px;
    }

    .file-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #3f51b5;
    }

    .file-details {
      flex: 1;
    }

    .file-details h4 {
      margin: 0 0 4px 0;
    }

    .file-details p {
      margin: 0;
      color: #666;
      font-size: 14px;
    }

    .uploading {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;
      padding: 40px 20px;
    }

    .uploading h3 {
      margin: 0;
    }

    .uploading p {
      margin: 0;
      color: #666;
    }

    .result {
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .result-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      border-radius: 8px;
    }

    .result-header.success {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .result-header.error {
      background: #ffebee;
      color: #c62828;
    }

    .result-header mat-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
    }

    .result-header h3 {
      margin: 0;
    }

    .result-stats {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .stat {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .stat mat-icon {
      color: #3f51b5;
    }

    .messages {
      border-radius: 8px;
      padding: 12px;
    }

    .messages h4 {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0 0 8px 0;
    }

    .warnings {
      background: #fff3e0;
      color: #e65100;
    }

    .errors {
      background: #ffebee;
      color: #c62828;
    }

    .error-message {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      background: #ffebee;
      color: #c62828;
      border-radius: 4px;
    }
  `]
})
export class GedcomImportComponent {
  selectedFile: File | null = null;
  uploading = false;
  result: GedcomImportResult | null = null;
  errorMessage = '';
  isDragOver = false;

  constructor(
    private dialogRef: MatDialogRef<GedcomImportComponent>,
    @Inject(MAT_DIALOG_DATA) public data: GedcomImportData,
    private gedcomService: GedcomService
  ) {}

  onFileSelected(event: any): void {
    const file = event.target.files[0];
    this.validateAndSelectFile(file);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragOver = false;

    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      this.validateAndSelectFile(files[0]);
    }
  }

  private validateAndSelectFile(file: File): void {
    this.errorMessage = '';

    // Validate file extension
    if (!file.name.toLowerCase().endsWith('.ged')) {
      this.errorMessage = 'Please select a .ged file';
      return;
    }

    // Validate file size (10MB)
    if (file.size > 10 * 1024 * 1024) {
      this.errorMessage = 'File size exceeds 10MB limit';
      return;
    }

    this.selectedFile = file;
  }

  clearFile(): void {
    this.selectedFile = null;
    this.errorMessage = '';
  }

  onImport(): void {
    if (!this.selectedFile) return;

    this.uploading = true;
    this.errorMessage = '';

    this.gedcomService.importGedcom(this.data.treeId, this.selectedFile).subscribe({
      next: (result) => {
        this.uploading = false;
        this.result = result;
      },
      error: (error) => {
        this.uploading = false;
        this.errorMessage = error.error?.message || 'Failed to import GEDCOM file';
      }
    });
  }

  onViewTree(): void {
    this.dialogRef.close({ success: true, treeId: this.data.treeId });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  formatFileSize(bytes: number): string {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
  }
}
