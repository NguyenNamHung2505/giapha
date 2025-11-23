import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { GedcomService } from '../services/gedcom.service';

@Component({
  selector: 'app-gedcom-export-button',
  standalone: true,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  template: `
    <button
      mat-raised-button
      [color]="color"
      (click)="onExport()"
      [disabled]="exporting"
      matTooltip="Export tree to GEDCOM format">
      <mat-spinner *ngIf="exporting" diameter="20" class="spinner"></mat-spinner>
      <mat-icon *ngIf="!exporting">download</mat-icon>
      {{ exporting ? 'Exporting...' : 'Export GEDCOM' }}
    </button>
  `,
  styles: [`
    button {
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }

    .spinner {
      display: inline-block;
    }
  `]
})
export class GedcomExportButtonComponent {
  @Input() treeId!: string;
  @Input() treeName?: string;
  @Input() color: 'primary' | 'accent' | 'warn' = 'primary';

  exporting = false;

  constructor(
    private gedcomService: GedcomService,
    private snackBar: MatSnackBar
  ) {}

  onExport(): void {
    if (!this.treeId) {
      this.snackBar.open('No tree selected', 'Close', { duration: 3000 });
      return;
    }

    this.exporting = true;

    this.gedcomService.exportGedcom(this.treeId, this.treeName).subscribe({
      next: () => {
        this.exporting = false;
        this.snackBar.open('GEDCOM file exported successfully!', 'Close', { duration: 3000 });
      },
      error: (error) => {
        this.exporting = false;
        const message = error.error?.message || 'Failed to export GEDCOM file';
        this.snackBar.open(message, 'Close', { duration: 5000 });
      }
    });
  }
}
