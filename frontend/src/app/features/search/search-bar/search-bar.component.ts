import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { SearchService } from '../services/search.service';
import { SearchResult } from '../models/search.model';

@Component({
  selector: 'app-search-bar',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    TranslateModule
  ],
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.scss']
})
export class SearchBarComponent implements OnInit, OnDestroy {
  searchControl = new FormControl('');
  searchResults: SearchResult[] = [];
  isLoading = false;
  showResults = false;
  private destroy$ = new Subject<void>();
  private searchQuery$ = new Subject<string>();

  constructor(
    private searchService: SearchService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Set up debounced search
    this.searchService.searchWithDebounce(this.searchQuery$, undefined, 300)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (results) => {
          this.searchResults = results;
          this.isLoading = false;
          this.showResults = results.length > 0;
        },
        error: (error) => {
          console.error('Search error:', error);
          this.isLoading = false;
          this.showResults = false;
        }
      });

    // Listen to input changes
    this.searchControl.valueChanges
      .pipe(takeUntil(this.destroy$))
      .subscribe((query) => {
        if (query && query.length >= 2) {
          this.isLoading = true;
          this.searchQuery$.next(query);
        } else {
          this.searchResults = [];
          this.showResults = false;
          this.isLoading = false;
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.searchQuery$.complete();
  }

  onResultClick(result: SearchResult): void {
    this.showResults = false;
    this.searchControl.setValue('');
    // Navigate to individual detail page
    this.router.navigate(['/trees', result.treeId, 'individuals', result.id]);
  }

  onBlur(): void {
    // Delay hiding results to allow click events to fire
    setTimeout(() => {
      this.showResults = false;
    }, 200);
  }

  onFocus(): void {
    if (this.searchResults.length > 0 && this.searchControl.value && this.searchControl.value.length >= 2) {
      this.showResults = true;
    }
  }

  clearSearch(): void {
    this.searchControl.setValue('');
    this.searchResults = [];
    this.showResults = false;
  }
}
