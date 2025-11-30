import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatTabsModule } from '@angular/material/tabs';
import { Observable, startWith, map, forkJoin } from 'rxjs';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { AdminService, UserResponse, UserWithProfile, CreateUserRequest, UpdateUserRequest } from '../services/admin.service';
import { TreeService } from '../../tree/services/tree.service';
import { IndividualService } from '../../individual/services/individual.service';
import { Tree } from '../../tree/models/tree.model';
import { Individual } from '../../individual/models/individual.model';
import { environment } from '../../../../environments/environment';

interface IndividualWithUserStatus extends Individual {
  hasUser: boolean;
  linkedUsername?: string;
}

@Component({
  selector: 'app-user-management',
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
    MatPaginatorModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatSnackBarModule,
    MatProgressSpinnerModule,
    MatToolbarModule,
    MatChipsModule,
    MatTooltipModule,
    MatSlideToggleModule,
    MatSelectModule,
    MatAutocompleteModule,
    MatTabsModule,
    TranslateModule
  ],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.scss'
})
export class UserManagementComponent implements OnInit {
  // Tab index
  selectedTabIndex = 0;

  // User Management
  users: UserResponse[] = [];
  loading = true;
  totalElements = 0;
  pageSize = 20;
  pageIndex = 0;
  displayedColumns = ['username', 'name', 'email', 'admin', 'enabled', 'createdAt', 'actions'];

  // Form for create/edit
  showUserForm = false;
  editingUser: UserResponse | null = null;
  userForm!: FormGroup;

  // User Linking
  trees: Tree[] = [];
  selectedTreeId: string = '';
  usersWithProfiles: UserWithProfile[] = [];
  individuals: Individual[] = [];
  linkingLoading = false;
  linkDisplayedColumns = ['userName', 'userEmail', 'linkedIndividual', 'actions'];

  // For linking autocomplete
  selectedUser: UserWithProfile | null = null;
  individualSearchControl = new FormControl<string | Individual>('');
  filteredIndividuals$!: Observable<Individual[]>;

  // Create from tree
  createFromTreeId: string = '';
  createFromTreeLoading = false;
  creatingUsers = false;
  individualsForCreate: IndividualWithUserStatus[] = [];
  createFromTreeColumns = ['avatar', 'fullName', 'birthDate', 'createActions'];

  constructor(
    private adminService: AdminService,
    private treeService: TreeService,
    private individualService: IndividualService,
    private snackBar: MatSnackBar,
    private fb: FormBuilder,
    private translate: TranslateService
  ) {
    this.initForm();
  }

  ngOnInit(): void {
    this.loadUsers();
    this.loadTrees();

    // Setup autocomplete filter
    this.filteredIndividuals$ = this.individualSearchControl.valueChanges.pipe(
      startWith(''),
      map(value => {
        const searchValue = typeof value === 'string' ? value : (value as any)?.fullName || '';
        return this._filterIndividuals(searchValue);
      })
    );
  }

  initForm(): void {
    this.userForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50), Validators.pattern(/^[a-zA-Z0-9_]+$/)]],
      name: ['', Validators.required],
      email: ['', [Validators.email]],
      password: ['', this.editingUser ? [] : [Validators.required, Validators.minLength(6)]],
      admin: [false],
      enabled: [true]
    });
  }

  // ==================== User Management ====================

  loadUsers(): void {
    this.loading = true;
    this.adminService.getUsers(this.pageIndex, this.pageSize).subscribe({
      next: (response) => {
        this.users = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.translate.get(['admin.loadFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.loadFailed'], t['common.close'], { duration: 3000 });
        });
        this.loading = false;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadUsers();
  }

  openCreateForm(): void {
    this.editingUser = null;
    this.userForm.reset({
      username: '',
      name: '',
      email: '',
      password: '',
      admin: false,
      enabled: true
    });
    this.userForm.get('password')?.setValidators([Validators.required, Validators.minLength(6)]);
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserForm = true;
  }

  openEditForm(user: UserResponse): void {
    this.editingUser = user;
    this.userForm.patchValue({
      username: user.username,
      name: user.name,
      email: user.email || '',
      password: '',
      admin: user.admin,
      enabled: user.enabled
    });
    this.userForm.get('password')?.clearValidators();
    this.userForm.get('password')?.updateValueAndValidity();
    this.showUserForm = true;
  }

  cancelForm(): void {
    this.showUserForm = false;
    this.editingUser = null;
  }

  saveUser(): void {
    if (this.userForm.invalid) {
      return;
    }

    const formValue = this.userForm.value;

    if (this.editingUser) {
      // Update existing user
      const request: UpdateUserRequest = {
        username: formValue.username,
        name: formValue.name,
        email: formValue.email || undefined,
        admin: formValue.admin,
        enabled: formValue.enabled
      };

      if (formValue.password) {
        request.password = formValue.password;
      }

      this.adminService.updateUser(this.editingUser.id, request).subscribe({
        next: () => {
          this.translate.get(['admin.updateSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(t['admin.updateSuccess'], t['common.close'], { duration: 3000 });
          });
          this.showUserForm = false;
          this.editingUser = null;
          this.loadUsers();
        },
        error: (error) => {
          console.error('Error updating user:', error);
          this.translate.get(['admin.updateFailed', 'common.close']).subscribe(t => {
            this.snackBar.open(t['admin.updateFailed'], t['common.close'], { duration: 3000 });
          });
        }
      });
    } else {
      // Create new user
      const request: CreateUserRequest = {
        username: formValue.username,
        name: formValue.name,
        email: formValue.email || undefined,
        password: formValue.password,
        admin: formValue.admin
      };

      this.adminService.createUser(request).subscribe({
        next: () => {
          this.translate.get(['admin.createSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(t['admin.createSuccess'], t['common.close'], { duration: 3000 });
          });
          this.showUserForm = false;
          this.loadUsers();
        },
        error: (error) => {
          console.error('Error creating user:', error);
          this.translate.get(['admin.createFailed', 'common.close']).subscribe(t => {
            this.snackBar.open(error.error?.message || t['admin.createFailed'], t['common.close'], { duration: 3000 });
          });
        }
      });
    }
  }

  deleteUser(user: UserResponse): void {
    this.translate.get('admin.deleteConfirm', { name: user.name }).subscribe(msg => {
      if (!confirm(msg)) {
        return;
      }

      this.adminService.deleteUser(user.id).subscribe({
        next: () => {
          this.translate.get(['admin.deleteSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(t['admin.deleteSuccess'], t['common.close'], { duration: 3000 });
          });
          this.loadUsers();
        },
        error: (error) => {
          console.error('Error deleting user:', error);
          this.translate.get(['admin.deleteFailed', 'common.close']).subscribe(t => {
            this.snackBar.open(error.error?.message || t['admin.deleteFailed'], t['common.close'], { duration: 3000 });
          });
        }
      });
    });
  }

  toggleAdmin(user: UserResponse): void {
    this.adminService.updateUser(user.id, { admin: !user.admin }).subscribe({
      next: () => {
        user.admin = !user.admin;
        this.translate.get(['admin.updateSuccess', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.updateSuccess'], t['common.close'], { duration: 2000 });
        });
      },
      error: (error) => {
        console.error('Error toggling admin:', error);
        this.translate.get(['admin.updateFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.updateFailed'], t['common.close'], { duration: 3000 });
        });
      }
    });
  }

  toggleEnabled(user: UserResponse): void {
    this.adminService.updateUser(user.id, { enabled: !user.enabled }).subscribe({
      next: () => {
        user.enabled = !user.enabled;
        this.translate.get(['admin.updateSuccess', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.updateSuccess'], t['common.close'], { duration: 2000 });
        });
      },
      error: (error) => {
        console.error('Error toggling enabled:', error);
        this.translate.get(['admin.updateFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.updateFailed'], t['common.close'], { duration: 3000 });
        });
      }
    });
  }

  // ==================== User Linking ====================

  loadTrees(): void {
    this.treeService.getTrees(0, 100).subscribe({
      next: (response) => {
        this.trees = response.content;
      },
      error: (error) => {
        console.error('Error loading trees:', error);
      }
    });
  }

  onTreeSelected(): void {
    if (!this.selectedTreeId) {
      this.usersWithProfiles = [];
      this.individuals = [];
      return;
    }

    this.linkingLoading = true;
    forkJoin({
      users: this.adminService.getUsersWithProfiles(this.selectedTreeId),
      individuals: this.individualService.getIndividuals(this.selectedTreeId, 0, 1000)
    }).subscribe({
      next: (result) => {
        this.usersWithProfiles = result.users;
        this.individuals = result.individuals.content.sort((a, b) => a.fullName.localeCompare(b.fullName));
        this.linkingLoading = false;
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.translate.get(['admin.loadFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.loadFailed'], t['common.close'], { duration: 3000 });
        });
        this.linkingLoading = false;
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
    if (!this.selectedUser || !individual || !this.selectedTreeId) return;

    this.adminService.linkUserToIndividual(this.selectedTreeId, this.selectedUser.id, individual.id).subscribe({
      next: () => {
        this.translate.get(['admin.linkSuccess', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.linkSuccess'], t['common.close'], { duration: 3000 });
        });
        this.selectedUser = null;
        this.individualSearchControl.setValue('');
        this.onTreeSelected(); // Reload data
      },
      error: (error) => {
        console.error('Error linking user:', error);
        this.translate.get(['admin.linkFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.linkFailed'], t['common.close'], { duration: 3000 });
        });
      }
    });
  }

  unlinkUser(user: UserWithProfile): void {
    this.translate.get('admin.unlinkConfirm').subscribe(msg => {
      if (!confirm(msg)) {
        return;
      }

      this.adminService.unlinkUserFromIndividual(this.selectedTreeId, user.id).subscribe({
        next: () => {
          this.translate.get(['admin.unlinkSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(t['admin.unlinkSuccess'], t['common.close'], { duration: 3000 });
          });
          this.onTreeSelected(); // Reload data
        },
        error: (error) => {
          console.error('Error unlinking user:', error);
          this.translate.get(['admin.unlinkFailed', 'common.close']).subscribe(t => {
            this.snackBar.open(t['admin.unlinkFailed'], t['common.close'], { duration: 3000 });
          });
        }
      });
    });
  }

  getAvatarUrl(url: string | null | undefined): string | null {
    if (!url) return null;
    if (url.startsWith('http')) return url;
    return `${environment.baseUrl}${url}`;
  }

  // ==================== Create Users from Tree ====================

  onCreateFromTreeSelected(): void {
    if (!this.createFromTreeId) {
      this.individualsForCreate = [];
      return;
    }

    this.createFromTreeLoading = true;
    forkJoin({
      users: this.adminService.getUsersWithProfiles(this.createFromTreeId),
      individuals: this.individualService.getIndividuals(this.createFromTreeId, 0, 1000)
    }).subscribe({
      next: (result) => {
        // Build a map of individual IDs that have linked users
        const linkedIndividuals = new Map<string, string>();
        result.users.forEach(user => {
          if (user.linkedIndividual) {
            linkedIndividuals.set(user.linkedIndividual.id, user.username);
          }
        });

        // Filter out individuals who already have linked users
        this.individualsForCreate = result.individuals.content
          .filter(ind => !linkedIndividuals.has(ind.id))
          .map(ind => ({
            ...ind,
            hasUser: false,
            linkedUsername: undefined
          }))
          .sort((a, b) => a.fullName.localeCompare(b.fullName));

        this.createFromTreeLoading = false;
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.translate.get(['admin.loadFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['admin.loadFailed'], t['common.close'], { duration: 3000 });
        });
        this.createFromTreeLoading = false;
      }
    });
  }

  createUserFromIndividual(individual: IndividualWithUserStatus): void {
    if (individual.hasUser) return;

    this.creatingUsers = true;
    this.adminService.createUserFromIndividual(this.createFromTreeId, individual.id).subscribe({
      next: (user) => {
        this.translate.get('admin.userCreatedFromIndividual', { username: user.username }).subscribe(msg => {
          this.snackBar.open(msg, 'OK', { duration: 5000 });
        });
        this.creatingUsers = false;
        // Refresh the list
        this.onCreateFromTreeSelected();
        // Also refresh user list in tab 1
        this.loadUsers();
      },
      error: (error) => {
        console.error('Error creating user:', error);
        const errorMsg = error.error?.message || 'Failed to create user';
        this.snackBar.open(errorMsg, 'OK', { duration: 5000 });
        this.creatingUsers = false;
      }
    });
  }

  createAllUsersFromTree(): void {
    if (!this.createFromTreeId) return;

    const unlinkedCount = this.individualsForCreate.filter(i => !i.hasUser).length;
    if (unlinkedCount === 0) {
      this.translate.get('admin.allIndividualsHaveUsers').subscribe(msg => {
        this.snackBar.open(msg, 'OK', { duration: 3000 });
      });
      return;
    }

    this.translate.get('admin.confirmCreateAllUsers', { count: unlinkedCount }).subscribe(msg => {
      if (!confirm(msg)) return;

      this.creatingUsers = true;
      this.adminService.createUsersFromTree(this.createFromTreeId).subscribe({
        next: (users) => {
          this.translate.get('admin.bulkUserCreated', { count: users.length }).subscribe(msg => {
            this.snackBar.open(msg, 'OK', { duration: 5000 });
          });
          this.creatingUsers = false;
          // Refresh the list
          this.onCreateFromTreeSelected();
          // Also refresh user list in tab 1
          this.loadUsers();
        },
        error: (error) => {
          console.error('Error creating users:', error);
          const errorMsg = error.error?.message || 'Failed to create users';
          this.snackBar.open(errorMsg, 'OK', { duration: 5000 });
          this.creatingUsers = false;
        }
      });
    });
  }
}
