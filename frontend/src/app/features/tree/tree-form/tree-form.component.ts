import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { TreeService } from '../services/tree.service';
import { Tree } from '../models/tree.model';
import { AdminService, UserWithProfile } from '../../admin/services/admin.service';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-tree-form',
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
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatSelectModule,
    MatDividerModule,
    MatTooltipModule,
    TranslateModule
  ],
  templateUrl: './tree-form.component.html',
  styleUrl: './tree-form.component.scss'
})
export class TreeFormComponent implements OnInit {
  treeForm!: FormGroup;
  loading = false;
  isEditMode = false;
  treeId?: string;
  tree?: Tree;

  // Admin management
  users: UserWithProfile[] = [];
  selectedAdminId: string | null = null;
  isOwner = false;
  currentUserId?: string;
  loadingUsers = false;
  savingAdmin = false;

  constructor(
    private formBuilder: FormBuilder,
    private treeService: TreeService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
    private adminService: AdminService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.treeForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(255)]],
      description: ['', [Validators.maxLength(5000)]]
    });

    // Get current user ID
    const currentUser = this.authService.currentUserValue;
    this.currentUserId = currentUser?.id;

    // Check if we're in edit mode
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('id') || undefined;
      if (this.treeId) {
        this.isEditMode = true;
        this.loadTree(this.treeId);
      }
    });
  }

  loadTree(id: string): void {
    this.loading = true;
    this.treeService.getTree(id).subscribe({
      next: (tree) => {
        this.tree = tree;
        this.treeForm.patchValue({
          name: tree.name,
          description: tree.description
        });

        // Check if current user is the owner
        this.isOwner = this.currentUserId === tree.ownerId;

        // Load users for admin selection if owner
        if (this.isOwner) {
          this.loadUsers(id);
        }

        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading tree:', error);
        this.translate.get(['tree.loadFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['tree.loadFailed'], t['common.close'], { duration: 3000 });
        });
        this.router.navigate(['/trees']);
      }
    });
  }

  loadUsers(treeId: string): void {
    this.loadingUsers = true;
    this.adminService.getUsersWithProfiles(treeId).subscribe({
      next: (users) => {
        // Filter out the owner from the list (owner cannot be admin)
        this.users = users.filter(u => u.id !== this.tree?.ownerId);
        this.loadingUsers = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.loadingUsers = false;
      }
    });
  }

  get name() {
    return this.treeForm.get('name');
  }

  get description() {
    return this.treeForm.get('description');
  }

  onSubmit(): void {
    if (this.treeForm.invalid) {
      return;
    }

    this.loading = true;
    const formValue = this.treeForm.value;

    if (this.isEditMode && this.treeId) {
      // Update existing tree
      this.treeService.updateTree(this.treeId, formValue).subscribe({
        next: () => {
          this.translate.get(['tree.updateSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(t['tree.updateSuccess'], t['common.close'], { duration: 3000 });
          });
          this.router.navigate(['/trees']);
        },
        error: (error) => {
          console.error('Error updating tree:', error);
          this.translate.get(['tree.updateFailed', 'common.close']).subscribe(t => {
            this.snackBar.open(t['tree.updateFailed'], t['common.close'], { duration: 3000 });
          });
          this.loading = false;
        }
      });
    } else {
      // Create new tree
      this.treeService.createTree(formValue).subscribe({
        next: (tree) => {
          this.translate.get(['tree.createSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(t['tree.createSuccess'], t['common.close'], { duration: 3000 });
          });
          this.router.navigate(['/trees', tree.id]);
        },
        error: (error) => {
          console.error('Error creating tree:', error);
          this.translate.get(['tree.createFailed', 'common.close']).subscribe(t => {
            this.snackBar.open(t['tree.createFailed'], t['common.close'], { duration: 3000 });
          });
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/trees']);
  }

  addAdmin(): void {
    if (!this.treeId || !this.selectedAdminId) {
      return;
    }

    this.savingAdmin = true;
    this.treeService.addTreeAdmin(this.treeId, this.selectedAdminId).subscribe({
      next: (tree) => {
        this.tree = tree;
        this.selectedAdminId = null; // Reset selection after adding
        this.translate.get(['tree.setAdminSuccess', 'common.close']).subscribe(t => {
          this.snackBar.open(t['tree.setAdminSuccess'], t['common.close'], { duration: 3000 });
        });
        this.savingAdmin = false;
      },
      error: (error) => {
        console.error('Error adding admin:', error);
        this.translate.get(['tree.setAdminFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['tree.setAdminFailed'], t['common.close'], { duration: 3000 });
        });
        this.savingAdmin = false;
      }
    });
  }

  removeAdmin(adminId: string): void {
    if (!this.treeId || !adminId) {
      return;
    }

    this.savingAdmin = true;
    this.treeService.removeTreeAdmin(this.treeId, adminId).subscribe({
      next: (tree) => {
        this.tree = tree;
        this.translate.get(['tree.removeAdminSuccess', 'common.close']).subscribe(t => {
          this.snackBar.open(t['tree.removeAdminSuccess'], t['common.close'], { duration: 3000 });
        });
        this.savingAdmin = false;
      },
      error: (error) => {
        console.error('Error removing admin:', error);
        this.translate.get(['tree.removeAdminFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['tree.removeAdminFailed'], t['common.close'], { duration: 3000 });
        });
        this.savingAdmin = false;
      }
    });
  }

  isUserAdmin(userId: string): boolean {
    return this.tree?.admins?.some(admin => admin.id === userId) || false;
  }
}
