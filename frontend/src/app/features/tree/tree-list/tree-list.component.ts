import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TreeService } from '../services/tree.service';
import { Tree } from '../models/tree.model';

@Component({
  selector: 'app-tree-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatCardModule,
    MatPaginatorModule,
    MatDialogModule,
    MatSnackBarModule,
    MatTooltipModule
  ],
  templateUrl: './tree-list.component.html',
  styleUrl: './tree-list.component.scss'
})
export class TreeListComponent implements OnInit {
  trees: Tree[] = [];
  displayedColumns: string[] = ['name', 'description', 'individualsCount', 'updatedAt', 'actions'];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  loading = false;

  constructor(
    private treeService: TreeService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.loadTrees();
  }

  loadTrees(): void {
    this.loading = true;
    this.treeService.getTrees(this.pageIndex, this.pageSize).subscribe({
      next: (response) => {
        this.trees = response.content;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading trees:', error);
        this.snackBar.open('Failed to load trees', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadTrees();
  }

  viewTree(tree: Tree): void {
    this.router.navigate(['/trees', tree.id]);
  }

  editTree(tree: Tree): void {
    this.router.navigate(['/trees', tree.id, 'edit']);
  }

  deleteTree(tree: Tree): void {
    if (confirm(`Are you sure you want to delete "${tree.name}"? This action cannot be undone.`)) {
      this.treeService.deleteTree(tree.id).subscribe({
        next: () => {
          this.snackBar.open('Tree deleted successfully', 'Close', { duration: 3000 });
          this.loadTrees();
        },
        error: (error) => {
          console.error('Error deleting tree:', error);
          this.snackBar.open('Failed to delete tree', 'Close', { duration: 3000 });
        }
      });
    }
  }

  createTree(): void {
    this.router.navigate(['/trees/new']);
  }
}
