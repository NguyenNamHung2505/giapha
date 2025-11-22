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
import { TreeService } from '../services/tree.service';
import { Tree } from '../models/tree.model';

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
    MatSnackBarModule
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

  constructor(
    private formBuilder: FormBuilder,
    private treeService: TreeService,
    private router: Router,
    private route: ActivatedRoute,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.treeForm = this.formBuilder.group({
      name: ['', [Validators.required, Validators.minLength(1), Validators.maxLength(255)]],
      description: ['', [Validators.maxLength(5000)]]
    });

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
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading tree:', error);
        this.snackBar.open('Failed to load tree', 'Close', { duration: 3000 });
        this.router.navigate(['/trees']);
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
          this.snackBar.open('Tree updated successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/trees']);
        },
        error: (error) => {
          console.error('Error updating tree:', error);
          this.snackBar.open('Failed to update tree', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
    } else {
      // Create new tree
      this.treeService.createTree(formValue).subscribe({
        next: (tree) => {
          this.snackBar.open('Tree created successfully', 'Close', { duration: 3000 });
          this.router.navigate(['/trees', tree.id]);
        },
        error: (error) => {
          console.error('Error creating tree:', error);
          this.snackBar.open('Failed to create tree', 'Close', { duration: 3000 });
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate(['/trees']);
  }
}
