import { Component, OnInit, ViewChild } from '@angular/core';
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
import { MatTabsModule } from '@angular/material/tabs';
import { IndividualService } from '../services/individual.service';
import { RelationshipService } from '../../relationship/services/relationship.service';
import { Individual } from '../models/individual.model';
import { Relationship } from '../../relationship/models/relationship.model';
import { MediaUploaderComponent } from '../../media/media-uploader/media-uploader.component';
import { MediaGalleryComponent } from '../../media/media-gallery/media-gallery.component';
import { Media } from '../../media/models/media.model';
import { RelationshipFormComponent } from '../../relationship/relationship-form/relationship-form.component';

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
    MatDialogModule,
    MatTabsModule,
    MediaUploaderComponent,
    MediaGalleryComponent
  ],
  templateUrl: './individual-detail.component.html',
  styleUrl: './individual-detail.component.scss'
})
export class IndividualDetailComponent implements OnInit {
  @ViewChild(MediaGalleryComponent) mediaGallery?: MediaGalleryComponent;

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

  /**
   * Get avatar URL for display
   */
  getAvatarUrl(): string {
    if (!this.individual?.profilePictureUrl) {
      return '';
    }
    // If it's already a full URL, return as is
    if (this.individual.profilePictureUrl.startsWith('http')) {
      return this.individual.profilePictureUrl;
    }
    // Otherwise, prepend API base URL
    return `http://localhost:8080${this.individual.profilePictureUrl}`;
  }

  /**
   * Handle avatar file selection
   */
  onAvatarSelected(event: any): void {
    const file: File = event.target.files[0];
    if (!file) {
      return;
    }

    // Validate file type
    if (!file.type.startsWith('image/')) {
      this.snackBar.open('Please select an image file', 'Close', { duration: 3000 });
      return;
    }

    // Validate file size (max 5MB)
    const maxSize = 5 * 1024 * 1024; // 5MB
    if (file.size > maxSize) {
      this.snackBar.open('Image size must be less than 5MB', 'Close', { duration: 3000 });
      return;
    }

    // Upload avatar
    this.individualService.uploadAvatar(this.treeId, this.individualId, file).subscribe({
      next: (response) => {
        this.snackBar.open('Avatar uploaded successfully', 'Close', { duration: 3000 });
        // Reload individual to get updated avatar URL
        this.loadIndividual();
      },
      error: (error) => {
        console.error('Error uploading avatar:', error);
        this.snackBar.open('Failed to upload avatar', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Delete avatar
   */
  deleteAvatar(): void {
    if (!confirm('Are you sure you want to remove the avatar?')) {
      return;
    }

    this.individualService.deleteAvatar(this.treeId, this.individualId).subscribe({
      next: () => {
        this.snackBar.open('Avatar removed successfully', 'Close', { duration: 3000 });
        // Reload individual to update avatar URL
        this.loadIndividual();
      },
      error: (error) => {
        console.error('Error deleting avatar:', error);
        this.snackBar.open('Failed to remove avatar', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Handle media upload completion
   */
  onMediaUploaded(media: Media): void {
    // Refresh the gallery when a new media is uploaded
    if (this.mediaGallery) {
      this.mediaGallery.refresh();
    }
  }

  /**
   * Open dialog to add a new relationship
   */
  addRelationship(): void {
    const dialogRef = this.dialog.open(RelationshipFormComponent, {
      width: '500px',
      data: {
        treeId: this.treeId,
        individualId: this.individualId,
        individualName: this.individual?.fullName || 'this person'
      }
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        // Relationship was created, reload relationships
        this.loadRelationships();
        this.snackBar.open('Relationship added successfully', 'Close', { duration: 3000 });
      }
    });
  }

  /**
   * Delete a relationship
   */
  deleteRelationship(relationshipId: string, type: string): void {
    if (!confirm(`Are you sure you want to delete this ${type.toLowerCase()} relationship?`)) {
      return;
    }

    this.relationshipService.deleteRelationship(relationshipId).subscribe({
      next: () => {
        this.loadRelationships();
        this.snackBar.open('Relationship deleted', 'Close', { duration: 2000 });
      },
      error: (error) => {
        console.error('Error deleting relationship:', error);
        this.snackBar.open('Failed to delete relationship', 'Close', { duration: 3000 });
      }
    });
  }
}
