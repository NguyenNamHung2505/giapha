import { Injectable } from '@angular/core';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { TranslateService } from '@ngx-translate/core';
import { Subject } from 'rxjs';

@Injectable()
export class CustomPaginatorIntl implements MatPaginatorIntl {
  changes = new Subject<void>();

  // Default values
  itemsPerPageLabel = 'Số mục mỗi trang:';
  nextPageLabel = 'Trang sau';
  previousPageLabel = 'Trang trước';
  firstPageLabel = 'Trang đầu';
  lastPageLabel = 'Trang cuối';

  constructor(private translate: TranslateService) {
    this.translate.onLangChange.subscribe(() => {
      this.updateLabels();
      this.changes.next();
    });
    this.updateLabels();
  }

  private updateLabels(): void {
    const currentLang = this.translate.currentLang || this.translate.defaultLang;

    if (currentLang === 'vi') {
      this.itemsPerPageLabel = 'Số mục mỗi trang:';
      this.nextPageLabel = 'Trang sau';
      this.previousPageLabel = 'Trang trước';
      this.firstPageLabel = 'Trang đầu';
      this.lastPageLabel = 'Trang cuối';
    } else {
      this.itemsPerPageLabel = 'Items per page:';
      this.nextPageLabel = 'Next page';
      this.previousPageLabel = 'Previous page';
      this.firstPageLabel = 'First page';
      this.lastPageLabel = 'Last page';
    }
  }

  getRangeLabel = (page: number, pageSize: number, length: number): string => {
    const currentLang = this.translate.currentLang || this.translate.defaultLang;

    if (length === 0 || pageSize === 0) {
      return currentLang === 'vi' ? `0 của ${length}` : `0 of ${length}`;
    }

    length = Math.max(length, 0);
    const startIndex = page * pageSize;
    const endIndex = startIndex < length
      ? Math.min(startIndex + pageSize, length)
      : startIndex + pageSize;

    if (currentLang === 'vi') {
      return `${startIndex + 1} – ${endIndex} của ${length}`;
    }
    return `${startIndex + 1} – ${endIndex} of ${length}`;
  };
}
