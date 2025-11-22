import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { IndividualService } from '../services/individual.service';
import { Individual, Gender } from '../models/individual.model';

@Component({
  selector: 'app-individual-form',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './individual-form.component.html',
  styleUrl: './individual-form.component.scss'
})
export class IndividualFormComponent implements OnInit {
  individualForm!: FormGroup;
  loading = false;
  isEditMode = false;
  treeId!: string;
  individualId?: string;
  individual?: Individual;
  genders = Object.values(Gender);

  constructor(
    private formBuilder: FormBuilder,
    private individualService: IndividualService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.individualForm = this.formBuilder.group({
      givenName: ['', [Validators.maxLength(255)]],
      surname: ['', [Validators.maxLength(255)]],
      suffix: ['', [Validators.maxLength(50)]],
      gender: [''],
      birthDate: [''],
      birthPlace: ['', [Validators.maxLength(500)]],
      deathDate: [''],
      deathPlace: ['', [Validators.maxLength(500)]],
      biography: ['', [Validators.maxLength(10000)]]
    });

    // Get tree ID and individual ID from route
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('treeId') || '';
      this.individualId = params.get('id') || undefined;

      if (this.individualId) {
        this.isEditMode = true;
        this.loadIndividual(this.treeId, this.individualId);
      }
    });
  }

  loadIndividual(treeId: string, id: string): void {
    this.loading = true;
    this.individualService.getIndividual(treeId, id).subscribe({
      next: (individual) => {
        this.individual = individual;
        this.individualForm.patchValue({
          givenName: individual.givenName,
          surname: individual.surname,
          suffix: individual.suffix,
          gender: individual.gender,
          birthDate: individual.birthDate ? new Date(individual.birthDate) : null,
          birthPlace: individual.birthPlace,
          deathDate: individual.deathDate ? new Date(individual.deathDate) : null,
          deathPlace: individual.deathPlace,
          biography: individual.biography
        });
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading individual:', error);
        this.snackBar.open('Failed to load individual', 'Close', { duration: 3000 });
        this.router.navigate(['/trees', treeId, 'individuals']);
      }
    });
  }

  get givenName() {
    return this.individualForm.get('givenName');
  }

  get surname() {
    return this.individualForm.get('surname');
  }

  get suffix() {
    return this.individualForm.get('suffix');
  }

  get birthPlace() {
    return this.individualForm.get('birthPlace');
  }

  get deathPlace() {
    return this.individualForm.get('deathPlace');
  }

  get biography() {
    return this.individualForm.get('biography');
  }

  onSubmit(): void {
    if (this.individualForm.invalid) {
      return;
    }

    this.loading = true;
    const formValue = this.individualForm.value;

    // Convert dates to ISO string format
    const request = {
      ...formValue,
      birthDate: formValue.birthDate ? new Date(formValue.birthDate).toISOString().split('T')[0] : null,
      deathDate: formValue.deathDate ? new Date(formValue.deathDate).toISOString().split('T')[0] : null
    };

    if (this.isEditMode && this.individualId) {
      // Update existing individual
      this.individualService.updateIndividual(this.treeId, this.individualId, request).subscribe({
        next: () => {
          this.snackBar.open('Individual updated successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/trees', this.treeId, 'individuals']);
        },
        error: (error) => {
          console.error('Error updating individual:', error);
          this.snackBar.open('Failed to update individual', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
    } else {
      // Create new individual
      this.individualService.createIndividual(this.treeId, request).subscribe({
        next: (individual) => {
          this.snackBar.open('Individual created successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/trees', this.treeId, 'individuals', individual.id]);
        },
        error: (error) => {
          console.error('Error creating individual:', error);
          this.snackBar.open('Failed to create individual', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/trees', this.treeId, 'individuals']);
  }
}
