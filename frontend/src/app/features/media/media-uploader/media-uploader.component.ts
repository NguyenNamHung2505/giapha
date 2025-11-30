import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { MediaService } from '../services/media.service';
import { Media } from '../models/media.model';

interface UploadItem {
  file: File;
  progress: number;
  caption: string;
  uploading: boolean;
  error?: string;
  media?: Media;
}

@Component({
  selector: 'app-media-uploader',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
    MatSnackBarModule,
    MatTooltipModule,
    TranslateModule
  ],
  templateUrl: './media-uploader.component.html',
  styleUrl: './media-uploader.component.scss'
})
export class MediaUploaderComponent {
  @Input() individualId!: string;
  @Output() mediaUploaded = new EventEmitter<Media>();

  uploadQueue: UploadItem[] = [];
  dragOver = false;

  // File validation
  maxFileSize = 5 * 1024 * 1024; // 5MB
  allowedTypes = [
    'image/jpeg',
    'image/jpg',
    'image/png',
    'image/gif',
    'image/webp',
    'image/bmp',
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
    'text/plain'
  ];

  constructor(
    private mediaService: MediaService,
    private snackBar: MatSnackBar,
    private translate: TranslateService
  ) {}

  /**
   * Handle file input change
   */
  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.addFilesToQueue(Array.from(input.files));
      input.value = ''; // Reset input
    }
  }

  /**
   * Handle drag over event
   */
  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = true;
  }

  /**
   * Handle drag leave event
   */
  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  /**
   * Handle drop event
   */
  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;

    if (event.dataTransfer && event.dataTransfer.files.length > 0) {
      this.addFilesToQueue(Array.from(event.dataTransfer.files));
    }
  }

  /**
   * Add files to upload queue
   */
  private addFilesToQueue(files: File[]): void {
    for (const file of files) {
      // Validate file
      const validation = this.validateFile(file);
      if (!validation.valid) {
        this.translate.get('common.close').subscribe(close => {
          this.snackBar.open(validation.error!, close, { duration: 5000 });
        });
        continue;
      }

      // Add to queue
      const uploadItem: UploadItem = {
        file,
        progress: 0,
        caption: '',
        uploading: false
      };
      this.uploadQueue.push(uploadItem);
    }
  }

  /**
   * Validate file
   */
  private validateFile(file: File): { valid: boolean; error?: string } {
    if (file.size > this.maxFileSize) {
      let errorMsg = `File "${file.name}" exceeds maximum size of 5MB`;
      this.translate.get('media.fileTooLarge').subscribe(msg => {
        errorMsg = `${file.name}: ${msg}`;
      });
      return {
        valid: false,
        error: errorMsg
      };
    }

    if (!this.allowedTypes.includes(file.type)) {
      let errorMsg = `File "${file.name}" has unsupported type`;
      this.translate.get('media.unsupportedType').subscribe(msg => {
        errorMsg = `${file.name}: ${msg}`;
      });
      return {
        valid: false,
        error: errorMsg
      };
    }

    return { valid: true };
  }

  /**
   * Upload a specific file from queue
   */
  uploadFile(item: UploadItem): void {
    if (!this.individualId) {
      this.translate.get(['validation.required', 'common.close']).subscribe(t => {
        this.snackBar.open(t['validation.required'], t['common.close'], { duration: 3000 });
      });
      return;
    }

    item.uploading = true;
    item.error = undefined;

    this.mediaService.uploadMedia(this.individualId, item.file, item.caption).subscribe({
      next: (result) => {
        item.progress = result.progress;
        if (result.media) {
          item.media = result.media;
          this.translate.get(['media.uploadSuccess', 'common.close']).subscribe(t => {
            this.snackBar.open(`${item.file.name}: ${t['media.uploadSuccess']}`, t['common.close'], { duration: 3000 });
          });
          this.mediaUploaded.emit(result.media);
        }
      },
      error: (error) => {
        item.uploading = false;
        item.error = error.error?.message || 'Upload failed';
        this.translate.get(['media.uploadFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(`${item.file.name}: ${t['media.uploadFailed']}`, t['common.close'], { duration: 5000 });
        });
      },
      complete: () => {
        item.uploading = false;
      }
    });
  }

  /**
   * Upload all files in queue
   */
  uploadAll(): void {
    for (const item of this.uploadQueue) {
      if (!item.uploading && !item.media) {
        this.uploadFile(item);
      }
    }
  }

  /**
   * Remove item from queue
   */
  removeFromQueue(item: UploadItem): void {
    const index = this.uploadQueue.indexOf(item);
    if (index > -1) {
      this.uploadQueue.splice(index, 1);
    }
  }

  /**
   * Clear completed uploads
   */
  clearCompleted(): void {
    this.uploadQueue = this.uploadQueue.filter(item => !item.media);
  }

  /**
   * Get file icon based on type
   */
  getFileIcon(file: File): string {
    if (file.type.startsWith('image/')) {
      return 'image';
    } else if (file.type === 'application/pdf') {
      return 'picture_as_pdf';
    } else if (file.type.includes('word')) {
      return 'description';
    }
    return 'insert_drive_file';
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
   * Check if all files are uploaded or uploading
   */
  isUploadAllDisabled(): boolean {
    return this.uploadQueue.every(item => item.uploading || item.media);
  }

  /**
   * Check if there are any completed uploads
   */
  hasCompletedUploads(): boolean {
    return this.uploadQueue.some(item => item.media !== undefined);
  }
}

