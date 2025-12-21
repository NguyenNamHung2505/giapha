import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
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
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import { TranslateModule } from '@ngx-translate/core';
import { TreeService } from '../services/tree.service';
import { Tree } from '../models/tree.model';
import { AuthService } from '../../../core/services/auth.service';
import { GedcomImportComponent, GedcomImportData } from '../../gedcom/gedcom-import/gedcom-import.component';
import { GedcomExportButtonComponent } from '../../gedcom/gedcom-export/gedcom-export-button.component';
import { MergeDialogComponent, MergeDialogData } from '../../merge/components/merge-dialog/merge-dialog.component';

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
    MatTooltipModule,
    MatMenuModule,
    MatDividerModule,
    TranslateModule,
    GedcomExportButtonComponent
  ],
  templateUrl: './tree-list.component.html',
  styleUrl: './tree-list.component.scss'
})
export class TreeListComponent implements OnInit, OnDestroy {
  trees: Tree[] = [];
  displayedColumns: string[] = ['name', 'description', 'owner', 'admin', 'individualsCount', 'updatedAt', 'actions'];
  totalElements = 0;
  pageSize = 10;
  pageIndex = 0;
  loading = false;

  constructor(
    private treeService: TreeService,
    private authService: AuthService,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {
    this.updateDisplayedColumns();
  }

  /**
   * Check if current user is system admin
   */
  get isSystemAdmin(): boolean {
    return this.authService.isAdmin();
  }

  @HostListener('window:resize', ['$event'])
  onResize() {
    this.updateDisplayedColumns();
  }

  private updateDisplayedColumns(): void {
    const width = window.innerWidth;
    if (width < 600) {
      // Mobile: only name and actions
      this.displayedColumns = ['name', 'actions'];
    } else if (width < 900) {
      // Tablet: name, count, actions
      this.displayedColumns = ['name', 'individualsCount', 'actions'];
    } else if (width < 1200) {
      // Medium desktop: exclude admin
      this.displayedColumns = ['name', 'description', 'owner', 'individualsCount', 'updatedAt', 'actions'];
    } else {
      // Large desktop: all columns including admin
      this.displayedColumns = ['name', 'description', 'owner', 'admin', 'individualsCount', 'updatedAt', 'actions'];
    }
  }

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
    // Navigate to the tree visualization
    this.router.navigate(['/trees', tree.id, 'visualize']);
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

  /**
   * Open GEDCOM import dialog for a specific tree
   */
  importGedcom(tree: Tree, event: Event): void {
    event.stopPropagation();

    const dialogRef = this.dialog.open(GedcomImportComponent, {
      width: '600px',
      data: {
        treeId: tree.id,
        treeName: tree.name
      } as GedcomImportData
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.success) {
        this.snackBar.open('GEDCOM imported successfully!', 'Close', { duration: 3000 });
        this.loadTrees(); // Refresh the list
        // Optionally navigate to the tree
        if (result.treeId) {
          this.router.navigate(['/trees', result.treeId, 'visualize']);
        }
      }
    });
  }

  /**
   * Open merge dialog for a specific tree
   */
  openMergeDialog(tree: Tree, event: Event): void {
    event.stopPropagation();

    const dialogRef = this.dialog.open(MergeDialogComponent, {
      width: '700px',
      maxHeight: '90vh',
      data: {
        targetTree: tree,
        availableTrees: this.trees
      } as MergeDialogData
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.success) {
        this.snackBar.open('Gộp cây thành công!', 'Đóng', { duration: 3000 });
        this.loadTrees(); // Refresh the list
      }
    });
  }

  ngOnDestroy(): void {
    // Cleanup if needed
  }
}
