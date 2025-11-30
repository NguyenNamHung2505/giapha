import { Component, Inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TreeService } from '../services/tree.service';
import { CreateTreeFromIndividualRequest, CreateTreeFromIndividualResponse } from '../models/tree.model';

export interface CreateTreeFromIndividualDialogData {
  treeId: string;
  treeName: string;
  individualId: string;
  individualName: string;
}

@Component({
  selector: 'app-create-tree-from-individual-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatProgressBarModule,
    MatIconModule,
    TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>account_tree</mat-icon>
      {{ 'tree.createFromIndividual.title' | translate }}
    </h2>

    <mat-dialog-content>
      <!-- Warning if already exported -->
      <div class="warning-box" *ngIf="alreadyExported">
        <mat-icon>warning</mat-icon>
        <span>{{ 'tree.createFromIndividual.alreadyExported' | translate }}</span>
      </div>

      <!-- Info box -->
      <div class="info-box">
        <mat-icon>info</mat-icon>
        <div>
          <p>{{ 'tree.createFromIndividual.info' | translate }}</p>
          <p class="person-info">
            <strong>{{ 'tree.createFromIndividual.selectedPerson' | translate }}:</strong> {{ data.individualName }}
          </p>
          <p class="tree-info">
            <strong>{{ 'tree.createFromIndividual.sourceTree' | translate }}:</strong> {{ data.treeName }}
          </p>
        </div>
      </div>

      <form [formGroup]="form" *ngIf="!creating && !result">
        <!-- New Tree Name -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ 'tree.createFromIndividual.newTreeName' | translate }}</mat-label>
          <input matInput formControlName="newTreeName" required>
          <mat-error *ngIf="form.get('newTreeName')?.hasError('required')">
            {{ 'validation.required' | translate }}
          </mat-error>
          <mat-error *ngIf="form.get('newTreeName')?.hasError('maxlength')">
            {{ 'validation.maxLength' | translate: { max: 255 } }}
          </mat-error>
        </mat-form-field>

        <!-- New Tree Description -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ 'tree.createFromIndividual.newTreeDescription' | translate }}</mat-label>
          <textarea matInput formControlName="newTreeDescription" rows="3"></textarea>
          <mat-error *ngIf="form.get('newTreeDescription')?.hasError('maxlength')">
            {{ 'validation.maxLength' | translate: { max: 1000 } }}
          </mat-error>
        </mat-form-field>

        <!-- Include Media Checkbox -->
        <div class="checkbox-wrapper">
          <mat-checkbox formControlName="includeMedia" color="primary">
            {{ 'tree.createFromIndividual.includeMedia' | translate }}
          </mat-checkbox>
          <p class="checkbox-hint">{{ 'tree.createFromIndividual.includeMediaHint' | translate }}</p>
        </div>
      </form>

      <!-- Creating state -->
      <div class="creating-state" *ngIf="creating">
        <mat-spinner diameter="50"></mat-spinner>
        <p>{{ 'tree.createFromIndividual.creating' | translate }}</p>
        <p class="creating-hint">{{ 'tree.createFromIndividual.creatingHint' | translate }}</p>
      </div>

      <!-- Result state -->
      <div class="result-state" *ngIf="result">
        <mat-icon class="success-icon">check_circle</mat-icon>
        <h3>{{ 'tree.createFromIndividual.success' | translate }}</h3>

        <div class="result-details">
          <div class="result-item">
            <mat-icon>account_tree</mat-icon>
            <span>{{ 'tree.createFromIndividual.newTreeCreated' | translate }}: <strong>{{ result.newTreeName }}</strong></span>
          </div>
          <div class="result-item">
            <mat-icon>people</mat-icon>
            <span>{{ 'tree.createFromIndividual.individualsCloned' | translate }}: <strong>{{ result.totalIndividuals }}</strong></span>
          </div>
          <div class="result-item">
            <mat-icon>link</mat-icon>
            <span>{{ 'tree.createFromIndividual.relationshipsCloned' | translate }}: <strong>{{ result.totalRelationships }}</strong></span>
          </div>
          <div class="result-item" *ngIf="result.totalMediaFiles > 0">
            <mat-icon>photo_library</mat-icon>
            <span>{{ 'tree.createFromIndividual.mediaCloned' | translate }}: <strong>{{ result.totalMediaFiles }}</strong></span>
          </div>
        </div>
      </div>

      <!-- Error state -->
      <div class="error-state" *ngIf="error">
        <mat-icon class="error-icon">error</mat-icon>
        <p>{{ error }}</p>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()" *ngIf="!creating">
        {{ result ? ('common.close' | translate) : ('common.cancel' | translate) }}
      </button>
      <button mat-raised-button color="primary"
              [disabled]="!form.valid || creating"
              (click)="onCreate()"
              *ngIf="!result">
        <mat-icon>content_copy</mat-icon>
        {{ creating ? ('tree.createFromIndividual.creating' | translate) : ('tree.createFromIndividual.create' | translate) }}
      </button>
      <button mat-raised-button color="primary"
              (click)="onViewNewTree()"
              *ngIf="result">
        <mat-icon>visibility</mat-icon>
        {{ 'tree.createFromIndividual.viewNewTree' | translate }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    h2[mat-dialog-title] {
      display: flex;
      align-items: center;
      gap: 8px;
      margin: 0;
      padding: 16px 24px;
    }

    h2[mat-dialog-title] mat-icon {
      color: #1976d2;
    }

    mat-dialog-content {
      min-width: 450px;
      max-height: 70vh;
      overflow-y: auto;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .warning-box {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 16px;
      background: #fff3e0;
      border-left: 4px solid #ff9800;
      border-radius: 4px;
      margin-bottom: 16px;
    }

    .warning-box mat-icon {
      color: #ff9800;
      flex-shrink: 0;
    }

    .info-box {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 16px;
      background: #e3f2fd;
      border-left: 4px solid #1976d2;
      border-radius: 4px;
      margin-bottom: 20px;
    }

    .info-box mat-icon {
      color: #1976d2;
      flex-shrink: 0;
    }

    .info-box p {
      margin: 0 0 8px 0;
      font-size: 14px;
    }

    .info-box p:last-child {
      margin-bottom: 0;
    }

    .info-box .person-info,
    .info-box .tree-info {
      color: #666;
    }

    .checkbox-wrapper {
      margin-bottom: 16px;
    }

    .checkbox-hint {
      margin: 4px 0 0 32px;
      font-size: 12px;
      color: #666;
    }

    .creating-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px 20px;
      text-align: center;
    }

    .creating-state p {
      margin: 16px 0 0;
      font-size: 16px;
    }

    .creating-hint {
      color: #666;
      font-size: 14px !important;
    }

    .result-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px;
      text-align: center;
    }

    .success-icon {
      font-size: 64px;
      width: 64px;
      height: 64px;
      color: #4caf50;
    }

    .result-state h3 {
      margin: 16px 0;
      color: #4caf50;
    }

    .result-details {
      width: 100%;
      background: #f5f5f5;
      border-radius: 8px;
      padding: 16px;
      margin-top: 16px;
    }

    .result-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 8px 0;
    }

    .result-item mat-icon {
      color: #666;
    }

    .result-item strong {
      color: #1976d2;
    }

    .error-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px;
      text-align: center;
    }

    .error-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #f44336;
    }

    .error-state p {
      margin: 16px 0 0;
      color: #f44336;
    }

    mat-dialog-actions button mat-icon {
      margin-right: 4px;
    }
  `]
})
export class CreateTreeFromIndividualDialogComponent implements OnInit {
  form: FormGroup;
  creating = false;
  alreadyExported = false;
  checkingExport = true;
  result: CreateTreeFromIndividualResponse | null = null;
  error: string | null = null;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<CreateTreeFromIndividualDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: CreateTreeFromIndividualDialogData,
    private treeService: TreeService,
    private translate: TranslateService
  ) {
    // Initialize form with default tree name based on individual
    const defaultTreeName = `${data.individualName} - ${this.translate.instant('tree.familyTree')}`;

    this.form = this.fb.group({
      newTreeName: [defaultTreeName, [Validators.required, Validators.maxLength(255)]],
      newTreeDescription: ['', Validators.maxLength(1000)],
      includeMedia: [true]
    });
  }

  ngOnInit(): void {
    // Check if this individual has already been exported
    this.checkIfAlreadyExported();
  }

  checkIfAlreadyExported(): void {
    this.checkingExport = true;
    this.treeService.checkIndividualExported(this.data.treeId, this.data.individualId).subscribe({
      next: (isExported) => {
        this.alreadyExported = isExported;
        this.checkingExport = false;
      },
      error: (error) => {
        console.error('Error checking export status:', error);
        this.checkingExport = false;
      }
    });
  }

  onCreate(): void {
    if (this.form.invalid) {
      return;
    }

    this.creating = true;
    this.error = null;

    const request: CreateTreeFromIndividualRequest = {
      sourceTreeId: this.data.treeId,
      rootIndividualId: this.data.individualId,
      newTreeName: this.form.value.newTreeName,
      newTreeDescription: this.form.value.newTreeDescription || undefined,
      includeMedia: this.form.value.includeMedia
    };

    this.treeService.createTreeFromIndividual(request).subscribe({
      next: (response) => {
        this.creating = false;
        this.result = response;
      },
      error: (error) => {
        console.error('Error creating tree from individual:', error);
        this.creating = false;
        this.error = error.error?.message || this.translate.instant('tree.createFromIndividual.error');
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close(this.result);
  }

  onViewNewTree(): void {
    if (this.result) {
      this.dialogRef.close({
        ...this.result,
        navigateToTree: true
      });
    }
  }
}
