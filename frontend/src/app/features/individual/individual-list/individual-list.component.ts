import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatChipsModule } from '@angular/material/chips';
import { IndividualService } from '../services/individual.service';
import { Individual } from '../models/individual.model';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

@Component({
  selector: 'app-individual-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatPaginatorModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatChipsModule
  ],
  templateUrl: './individual-list.component.html',
  styleUrl: './individual-list.component.scss'
})
export class IndividualListComponent implements OnInit {
  individuals: Individual[] = [];
  displayedColumns: string[] = ['fullName', 'birthDate', 'birthPlace', 'gender', 'actions'];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  loading = false;
  treeId!: string;
  treeName?: string;
  searchControl = new FormControl('');

  constructor(
    private individualService: IndividualService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('treeId') || '';
      if (this.treeId) {
        this.loadIndividuals();
      }
    });

    // Setup search with debounce
    this.searchControl.valueChanges.pipe(
      debounceTime(300),
      distinctUntilChanged()
    ).subscribe(() => {
      this.pageIndex = 0;
      this.loadIndividuals();
    });
  }

  loadIndividuals(): void {
    this.loading = true;
    const searchTerm = this.searchControl.value || undefined;

    this.individualService.getIndividuals(this.treeId, this.pageIndex, this.pageSize, searchTerm).subscribe({
      next: (response) => {
        this.individuals = response.content;
        this.totalElements = response.totalElements;
        if (this.individuals.length > 0 && !this.treeName) {
          this.treeName = this.individuals[0].treeName;
        }
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading individuals:', error);
        this.snackBar.open('Failed to load individuals', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadIndividuals();
  }

  viewIndividual(individual: Individual): void {
    this.router.navigate(['/trees', this.treeId, 'individuals', individual.id]);
  }

  editIndividual(individual: Individual): void {
    this.router.navigate(['/trees', this.treeId, 'individuals', individual.id, 'edit']);
  }

  deleteIndividual(individual: Individual): void {
    if (confirm(`Are you sure you want to delete "${individual.fullName}"? This action cannot be undone.`)) {
      this.individualService.deleteIndividual(this.treeId, individual.id).subscribe({
        next: () => {
          this.snackBar.open('Individual deleted successfully', 'Close', { duration: 3000 });
          this.loadIndividuals();
        },
        error: (error) => {
          console.error('Error deleting individual:', error);
          this.snackBar.open('Failed to delete individual', 'Close', { duration: 3000 });
        }
      });
    }
  }

  createIndividual(): void {
    this.router.navigate(['/trees', this.treeId, 'individuals', 'new']);
  }

  backToTree(): void {
    this.router.navigate(['/trees']);
  }

  clearSearch(): void {
    this.searchControl.setValue('');
  }

  viewTreeVisualization(): void {
    this.router.navigate(['/trees', this.treeId, 'visualize']);
  }
}
