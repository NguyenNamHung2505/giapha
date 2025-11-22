import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { IndividualService } from '../services/individual.service';
import { RelationshipService } from '../../relationship/services/relationship.service';
import { Individual } from '../models/individual.model';
import { Relationship } from '../../relationship/models/relationship.model';

@Component({
  selector: 'app-individual-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatSnackBarModule,
    MatDialogModule
  ],
  templateUrl: './individual-detail.component.html',
  styleUrl: './individual-detail.component.scss'
})
export class IndividualDetailComponent implements OnInit {
  individual?: Individual;
  parents: Relationship[] = [];
  children: Relationship[] = [];
  spouses: Relationship[] = [];
  loading = true;
  treeId!: string;
  individualId!: string;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private individualService: IndividualService,
    private relationshipService: RelationshipService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('treeId') || '';
      this.individualId = params.get('id') || '';

      if (this.treeId && this.individualId) {
        this.loadIndividual();
        this.loadRelationships();
      }
    });
  }

  loadIndividual(): void {
    this.loading = true;
    this.individualService.getIndividual(this.treeId, this.individualId).subscribe({
      next: (individual) => {
        this.individual = individual;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading individual:', error);
        this.snackBar.open('Failed to load individual', 'Close', { duration: 3000 });
        this.router.navigate(['/trees', this.treeId, 'individuals']);
      }
    });
  }

  loadRelationships(): void {
    this.relationshipService.getParents(this.individualId).subscribe({
      next: (parents) => {
        this.parents = parents;
      },
      error: (error) => {
        console.error('Error loading parents:', error);
      }
    });

    this.relationshipService.getChildren(this.individualId).subscribe({
      next: (children) => {
        this.children = children;
      },
      error: (error) => {
        console.error('Error loading children:', error);
      }
    });

    this.relationshipService.getSpouses(this.individualId).subscribe({
      next: (spouses) => {
        this.spouses = spouses;
      },
      error: (error) => {
        console.error('Error loading spouses:', error);
      }
    });
  }

  getAge(): string | null {
    if (!this.individual?.birthDate) {
      return null;
    }

    const birthDate = new Date(this.individual.birthDate);
    const endDate = this.individual.deathDate ? new Date(this.individual.deathDate) : new Date();

    let age = endDate.getFullYear() - birthDate.getFullYear();
    const monthDiff = endDate.getMonth() - birthDate.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && endDate.getDate() < birthDate.getDate())) {
      age--;
    }

    return age.toString();
  }

  getLifeYears(): string {
    if (!this.individual) {
      return '';
    }

    const birthYear = this.individual.birthDate ? new Date(this.individual.birthDate).getFullYear() : '?';
    const deathYear = this.individual.deathDate ? new Date(this.individual.deathDate).getFullYear() : 'present';

    return `${birthYear} - ${deathYear}`;
  }

  goToIndividual(id: string): void {
    this.router.navigate(['/trees', this.treeId, 'individuals', id]);
  }

  editIndividual(): void {
    this.router.navigate(['/trees', this.treeId, 'individuals', this.individualId, 'edit']);
  }

  deleteIndividual(): void {
    if (confirm(`Are you sure you want to delete ${this.individual?.fullName}?`)) {
      this.individualService.deleteIndividual(this.treeId, this.individualId).subscribe({
        next: () => {
          this.snackBar.open('Individual deleted successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/trees', this.treeId, 'individuals']);
        },
        error: (error) => {
          console.error('Error deleting individual:', error);
          this.snackBar.open('Failed to delete individual', 'Close', { duration: 3000 });
        }
      });
    }
  }

  back(): void {
    this.router.navigate(['/trees', this.treeId, 'individuals']);
  }
}
