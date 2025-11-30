import { Component, Input, OnInit, OnChanges, SimpleChanges, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MediaService } from '../services/media.service';
import { Media, MediaType } from '../models/media.model';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-media-gallery',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    TranslateModule
  ],
  templateUrl: './media-gallery.component.html',
  styleUrl: './media-gallery.component.scss'
})
export class MediaGalleryComponent implements OnInit, OnChanges {
  @Input() individualId!: string;
  @Input() autoLoad = true;

  mediaList: Media[] = [];
  loading = false;
  editingCaption: { [key: string]: boolean } = {};
  editedCaptions: { [key: string]: string } = {};
  thumbnailUrls: { [key: string]: string } = {}; // Cache for blob URLs

  MediaType = MediaType; // Make enum available in template

  constructor(
    private mediaService: MediaService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    if (this.autoLoad && this.individualId) {
      this.loadMedia();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['individualId'] && !changes['individualId'].firstChange) {
      if (this.individualId) {
        this.loadMedia();
      }
    }
  }

  /**
   * Load media for the individual
   */
  loadMedia(): void {
    if (!this.individualId) {
      return;
    }

    this.loading = true;
    this.mediaService.listMediaForIndividual(this.individualId).subscribe({
      next: (media) => {
        this.mediaList = media;
        this.loading = false;
        // Load thumbnails for images
        this.loadThumbnails();
      },
      error: (error) => {
        console.error('Error loading media:', error);
        this.translate.get(['media.loadFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['media.loadFailed'], t['common.close'], { duration: 3000 });
        });
        this.loading = false;
      }
    });
  }

  /**
   * Load thumbnails as blob URLs
   */
  loadThumbnails(): void {
    this.mediaList.forEach(media => {
      if (this.isImage(media) && media.thumbnailUrl) {
        this.mediaService.getThumbnailBlobUrl(media.id).subscribe({
          next: (blobUrl) => {
            this.thumbnailUrls[media.id] = blobUrl;
          },
          error: (error) => {
            console.error('Error loading thumbnail:', error);
          }
        });
      }
    });
  }

  /**
   * Refresh media list (called after upload)
   */
  refresh(): void {
    this.loadMedia();
  }

  /**
   * Get thumbnail URL for media
   */
  getThumbnailUrl(media: Media): string {
    // Use cached blob URL if available
    if (this.thumbnailUrls[media.id]) {
      return this.thumbnailUrls[media.id];
    }
    // Fallback to icon
    return this.getIconForMediaType(media.type);
  }

  /**
   * Get stream URL for media
   */
  getStreamUrl(media: Media): string {
    return this.mediaService.getStreamUrl(media.id);
  }

  /**
   * Get icon for media type
   */
  getIconForMediaType(type: MediaType): string {
    switch (type) {
      case MediaType.PHOTO:
        return 'image';
      case MediaType.DOCUMENT:
        return 'description';
      case MediaType.VIDEO:
        return 'video_library';
      case MediaType.AUDIO:
        return 'audio_file';
      default:
        return 'insert_drive_file';
    }
  }

  /**
   * Check if media is an image
   */
  isImage(media: Media): boolean {
    return media.type === MediaType.PHOTO;
  }

  /**
   * Open media in lightbox/modal
   */
  openMedia(media: Media): void {
    if (this.isImage(media)) {
      // Get blob URL and open in new tab
      this.mediaService.getStreamBlobUrl(media.id).subscribe({
        next: (blobUrl) => {
          window.open(blobUrl, '_blank');
        },
        error: (error) => {
          console.error('Error opening media:', error);
          this.translate.get(['errors.generic', 'common.close']).subscribe(t => {
            this.snackBar.open(t['errors.generic'], t['common.close'], { duration: 3000 });
          });
        }
      });
    } else {
      // Download non-image files
      this.downloadMedia(media);
    }
  }

  /**
   * Download media
   */
  downloadMedia(media: Media): void {
    this.mediaService.downloadMedia(media.id);
  }

  /**
   * Start editing caption
   */
  startEditCaption(media: Media): void {
    this.editingCaption[media.id] = true;
    this.editedCaptions[media.id] = media.caption || '';
  }

  /**
   * Cancel editing caption
   */
  cancelEditCaption(media: Media): void {
    this.editingCaption[media.id] = false;
    delete this.editedCaptions[media.id];
  }

  /**
   * Save caption
   */
  saveCaption(media: Media): void {
    const newCaption = this.editedCaptions[media.id];

    this.mediaService.updateMedia(media.id, { caption: newCaption }).subscribe({
      next: (updatedMedia) => {
        // Update local copy
        const index = this.mediaList.findIndex(m => m.id === media.id);
        if (index > -1) {
          this.mediaList[index] = updatedMedia;
        }

        this.editingCaption[media.id] = false;
        delete this.editedCaptions[media.id];
        this.translate.get(['common.success', 'common.close']).subscribe(t => {
          this.snackBar.open(t['common.success'], t['common.close'], { duration: 2000 });
        });
      },
      error: (error) => {
        console.error('Error updating caption:', error);
        this.translate.get(['errors.generic', 'common.close']).subscribe(t => {
          this.snackBar.open(t['errors.generic'], t['common.close'], { duration: 3000 });
        });
      }
    });
  }

  /**
   * Delete media
   */
  deleteMedia(media: Media): void {
    this.translate.get('media.deleteConfirm', { name: media.filename }).subscribe(msg => {
      if (!confirm(msg)) {
        return;
      }

      this.mediaService.deleteMedia(media.id).subscribe({
        next: () => {
          this.mediaList = this.mediaList.filter(m => m.id !== media.id);
          this.translate.get(['media.deleteSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(t['media.deleteSuccess'], t['common.close'], { duration: 2000 });
          });
        },
        error: (error) => {
          console.error('Error deleting media:', error);
          this.translate.get(['media.deleteFailed', 'common.close']).subscribe(t => {
            this.snackBar.open(t['media.deleteFailed'], t['common.close'], { duration: 3000 });
          });
        }
      });
    });
  }

  /**
   * Format file size
   */
  formatFileSize(bytes: number): string {
    if (bytes < 1024) {
      return bytes + ' B';
    } else if (bytes < 1024 * 1024) {
      return (bytes / 1024).toFixed(2) + ' KB';
    } else {
      return (bytes / (1024 * 1024)).toFixed(2) + ' MB';
    }
  }

  /**
   * Format date
   */
  formatDate(date: Date): string {
    return new Date(date).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

