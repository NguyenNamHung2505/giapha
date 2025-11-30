import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { Observable, startWith, map, forkJoin } from 'rxjs';
import { AdminService, UserWithProfile } from '../services/admin.service';
import { IndividualService } from '../../individual/services/individual.service';
import { Individual } from '../../individual/models/individual.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-user-mapping',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule,
    MatSelectModule,
    MatFormFieldModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatChipsModule,
    MatTooltipModule,
    MatAutocompleteModule,
    MatInputModule
  ],
  templateUrl: './user-mapping.component.html',
  styleUrl: './user-mapping.component.scss'
})
export class UserMappingComponent implements OnInit {
  treeId!: string;
  users: UserWithProfile[] = [];
  individuals: Individual[] = [];
  loading = true;

  displayedColumns = ['userName', 'userEmail', 'linkedIndividual', 'actions'];

  // For linking
  selectedUser: UserWithProfile | null = null;
  individualSearchControl = new FormControl<string | Individual>('');
  filteredIndividuals$!: Observable<Individual[]>;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private adminService: AdminService,
    private individualService: IndividualService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('treeId') || '';
      if (this.treeId) {
        this.loadData();
      }
    });

    // Setup autocomplete filter
    this.filteredIndividuals$ = this.individualSearchControl.valueChanges.pipe(
      startWith(''),
      map(value => {
        const searchValue = typeof value === 'string' ? value : (value as any)?.fullName || '';
        return this._filterIndividuals(searchValue);
      })
    );
  }

  loadData(): void {
    this.loading = true;

    forkJoin({
      users: this.adminService.getUsersWithProfiles(this.treeId),
      individuals: this.individualService.getIndividuals(this.treeId, 0, 1000)
    }).subscribe({
      next: (result) => {
        this.users = result.users;
        this.individuals = result.individuals.content.sort((a, b) => a.fullName.localeCompare(b.fullName));
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.snackBar.open('Khong the tai du lieu', 'Dong', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  private _filterIndividuals(value: string): Individual[] {
    if (!value) return this.individuals;
    const filterValue = value.toLowerCase();
    return this.individuals.filter(ind =>
      ind.fullName.toLowerCase().includes(filterValue)
    );
  }

  displayIndividual(individual: Individual): string {
    if (!individual) return '';
    const birthYear = individual.birthDate ? new Date(individual.birthDate).getFullYear() : '';
    return birthYear ? `${individual.fullName}, ${birthYear}` : individual.fullName;
  }

  startLinking(user: UserWithProfile): void {
    this.selectedUser = user;
    this.individualSearchControl.setValue('');
  }

  cancelLinking(): void {
    this.selectedUser = null;
    this.individualSearchControl.setValue('');
  }

  linkUser(individual: Individual): void {
    if (!this.selectedUser || !individual) return;

    this.adminService.linkUserToIndividual(this.treeId, this.selectedUser.id, individual.id).subscribe({
      next: () => {
        this.snackBar.open(`Da lien ket ${this.selectedUser!.name} voi ${individual.fullName}`, 'Dong', { duration: 3000 });
        this.selectedUser = null;
        this.individualSearchControl.setValue('');
        this.loadData();
      },
      error: (error) => {
        console.error('Error linking user:', error);
        this.snackBar.open('Khong the lien ket', 'Dong', { duration: 3000 });
      }
    });
  }

  unlinkUser(user: UserWithProfile): void {
    if (!confirm(`Ban co chac muon huy lien ket cua ${user.name}?`)) {
      return;
    }

    this.adminService.unlinkUserFromIndividual(this.treeId, user.id).subscribe({
      next: () => {
        this.snackBar.open('Da huy lien ket', 'Dong', { duration: 3000 });
        this.loadData();
      },
      error: (error) => {
        console.error('Error unlinking user:', error);
        this.snackBar.open('Khong the huy lien ket', 'Dong', { duration: 3000 });
      }
    });
  }

  getAvatarUrl(url: string | null | undefined): string | null {
    if (!url) return null;
    if (url.startsWith('http')) return url;
    return `${environment.baseUrl}${url}`;
  }

  back(): void {
    this.router.navigate(['/trees', this.treeId, 'individuals']);
  }
}
