import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { RelationshipService } from '../services/relationship.service';
import { IndividualService } from '../../individual/services/individual.service';
import { Individual } from '../../individual/models/individual.model';
import { RelationshipType } from '../models/relationship.model';

export interface RelationshipDialogData {
  treeId: string;
  individualId: string;
  individualName: string;
}

@Component({
  selector: 'app-relationship-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    TranslateModule
  ],
  template: `
    <h2 mat-dialog-title>{{ 'relationship.addRelationship' | translate }} - {{ data.individualName }}</h2>

    <mat-dialog-content>
      <form [formGroup]="relationshipForm">
        <!-- Relationship Type -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ 'relationship.type' | translate }}</mat-label>
          <mat-select formControlName="type" required>
            <mat-option value="MOTHER_CHILD">{{ 'relationship.motherChild' | translate }}</mat-option>
            <mat-option value="FATHER_CHILD">{{ 'relationship.fatherChild' | translate }}</mat-option>
            <mat-option value="PARENT_CHILD">{{ 'relationship.parentChild' | translate }}</mat-option>
            <mat-option value="SPOUSE">{{ 'relationship.spouse' | translate }}</mat-option>
            <mat-option value="PARTNER">{{ 'relationship.partner' | translate }}</mat-option>
            <mat-option value="SIBLING">{{ 'relationship.sibling' | translate }}</mat-option>
            <mat-option value="ADOPTED_PARENT_CHILD">{{ 'relationship.adoptedParentChild' | translate }}</mat-option>
            <mat-option value="STEP_PARENT_CHILD">{{ 'relationship.stepParentChild' | translate }}</mat-option>
            <mat-option value="HALF_SIBLING">{{ 'relationship.halfSibling' | translate }}</mat-option>
            <mat-option value="STEP_SIBLING">{{ 'relationship.stepSibling' | translate }}</mat-option>
          </mat-select>
          <mat-error *ngIf="relationshipForm.get('type')?.hasError('required')">
            {{ 'validation.required' | translate }}
          </mat-error>
        </mat-form-field>

        <!-- Individual Selection -->
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>{{ 'common.select' | translate }}</mat-label>
          <mat-select formControlName="relatedIndividualId" required>
            <mat-option *ngFor="let ind of individuals" [value]="ind.id">
              {{ ind.fullName }}
              <span *ngIf="ind.birthDate" class="individual-dates">
                ({{ ind.birthDate | date:'yyyy' }}{{ ind.deathDate ? ' - ' + (ind.deathDate | date:'yyyy') : '' }})
              </span>
            </mat-option>
          </mat-select>
          <mat-error *ngIf="relationshipForm.get('relatedIndividualId')?.hasError('required')">
            {{ 'validation.required' | translate }}
          </mat-error>
        </mat-form-field>

        <!-- Optional Dates for Spouse/Partner -->
        <div *ngIf="isSpouseOrPartner()">
          <mat-form-field appearance="outline" class="half-width">
            <mat-label>{{ 'relationship.startDate' | translate }}</mat-label>
            <input matInput [matDatepicker]="startPicker" formControlName="startDate">
            <mat-datepicker-toggle matSuffix [for]="startPicker"></mat-datepicker-toggle>
            <mat-datepicker #startPicker></mat-datepicker>
          </mat-form-field>

          <mat-form-field appearance="outline" class="half-width">
            <mat-label>{{ 'relationship.endDate' | translate }}</mat-label>
            <input matInput [matDatepicker]="endPicker" formControlName="endDate">
            <mat-datepicker-toggle matSuffix [for]="endPicker"></mat-datepicker-toggle>
            <mat-datepicker #endPicker></mat-datepicker>
          </mat-form-field>
        </div>
      </form>

      <div *ngIf="loading" class="loading">
        <mat-spinner diameter="40"></mat-spinner>
        <p>{{ 'common.loading' | translate }}</p>
      </div>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">{{ 'common.cancel' | translate }}</button>
      <button mat-raised-button color="primary"
              [disabled]="!relationshipForm.valid || saving"
              (click)="onSave()">
        {{ saving ? ('common.loading' | translate) : ('common.save' | translate) }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .half-width {
      width: calc(50% - 8px);
      margin-right: 16px;
      margin-bottom: 16px;
    }

    .half-width:last-child {
      margin-right: 0;
    }

    .help-text {
      background: #f5f5f5;
      padding: 12px;
      border-radius: 4px;
      margin-bottom: 16px;
      font-size: 13px;
    }

    .help-text ul {
      margin: 8px 0 0 0;
      padding-left: 20px;
    }

    .help-text li {
      margin-bottom: 4px;
    }

    .individual-dates {
      color: #666;
      font-size: 12px;
      margin-left: 8px;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 20px;
    }

    .loading p {
      margin-top: 12px;
      color: #666;
    }

    mat-dialog-content {
      min-width: 400px;
      max-height: 70vh;
      overflow-y: auto;
    }
  `]
})
export class RelationshipFormComponent {
  relationshipForm: FormGroup;
  individuals: Individual[] = [];
  loading = false;
  saving = false;

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<RelationshipFormComponent>,
    @Inject(MAT_DIALOG_DATA) public data: RelationshipDialogData,
    private relationshipService: RelationshipService,
    private individualService: IndividualService
  ) {
    this.relationshipForm = this.fb.group({
      type: ['', Validators.required],
      relatedIndividualId: ['', Validators.required],
      startDate: [''],
      endDate: ['']
    });

    this.loadIndividuals();
  }

  loadIndividuals(): void {
    this.loading = true;
    // Load all individuals in the tree except the current one
    this.individualService.getIndividuals(this.data.treeId, 0, 1000).subscribe({
      next: (response) => {
        this.individuals = response.content.filter(
          ind => ind.id !== this.data.individualId
        );
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading individuals:', error);
        this.loading = false;
      }
    });
  }

  isSpouseOrPartner(): boolean {
    const type = this.relationshipForm.get('type')?.value;
    return type === 'SPOUSE' || type === 'PARTNER';
  }

  onSave(): void {
    if (this.relationshipForm.invalid) {
      return;
    }

    this.saving = true;
    const formValue = this.relationshipForm.value;

    const request = {
      individual1Id: this.data.individualId,
      individual2Id: formValue.relatedIndividualId,
      type: formValue.type as RelationshipType,
      startDate: formValue.startDate || undefined,
      endDate: formValue.endDate || undefined
    };

    this.relationshipService.createRelationship(this.data.treeId, request).subscribe({
      next: (relationship) => {
        this.dialogRef.close(relationship);
      },
      error: (error) => {
        console.error('Error creating relationship:', error);
        this.saving = false;
        // TODO: Show error message
      }
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }
}

