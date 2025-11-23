/**
 * Media type enum
 */
export enum MediaType {
  PHOTO = 'PHOTO',
  DOCUMENT = 'DOCUMENT',
  VIDEO = 'VIDEO',
  AUDIO = 'AUDIO',
  OTHER = 'OTHER'
}

/**
 * Media interface
 */
export interface Media {
  id: string;
  individualId: string;
  type: MediaType;
  filename: string;
  caption?: string;
  fileSize: number;
  mimeType: string;
  downloadUrl: string;
  thumbnailUrl?: string;
  uploadedAt: Date;
}

/**
 * Media upload request
 */
export interface MediaUploadRequest {
  file: File;
  caption?: string;
}

/**
 * Media update request
 */
export interface MediaUpdateRequest {
  caption?: string;
}

