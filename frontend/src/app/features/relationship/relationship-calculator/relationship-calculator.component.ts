import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatInputModule } from '@angular/material/input';
import { MatAutocompleteModule } from '@angular/material/autocomplete';

import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { Individual } from '../../individual/models/individual.model';
import { IndividualService } from '../../individual/services/individual.service';
import { RelationshipService } from '../services/relationship.service';
import { RelationshipPathResponse, RelationshipCategory } from '../models/relationship.model';
import { LanguageService } from '../../../core/services/language.service';

@Component({
  selector: 'app-relationship-calculator',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatDividerModule,
    MatChipsModule,
    MatTooltipModule,
    MatInputModule,
    MatAutocompleteModule,
    TranslateModule
  ],
  templateUrl: './relationship-calculator.component.html',
  styleUrl: './relationship-calculator.component.scss'
})
export class RelationshipCalculatorComponent implements OnInit {
  treeId: string = '';
  individuals: Individual[] = [];
  filteredIndividuals1: Individual[] = [];
  filteredIndividuals2: Individual[] = [];

  selectedPerson1: Individual | null = null;
  selectedPerson2: Individual | null = null;
  searchText1: string = '';
  searchText2: string = '';

  result: RelationshipPathResponse | null = null;

  loading = false;
  loadingIndividuals = false;
  calculating = false;

  constructor(
    private route: ActivatedRoute,
    private individualService: IndividualService,
    private relationshipService: RelationshipService,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
    private languageService: LanguageService
  ) {}

  isVietnamese(): boolean {
    return this.languageService.getCurrentLanguage() === 'vi';
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('treeId') || '';
      if (this.treeId) {
        this.loadIndividuals();
      }
    });
  }

  loadIndividuals(): void {
    this.loadingIndividuals = true;
    this.individualService.getIndividuals(this.treeId, 0, 1000).subscribe({
      next: (page) => {
        this.individuals = page.content;
        this.filteredIndividuals1 = [...this.individuals];
        this.filteredIndividuals2 = [...this.individuals];
        this.loadingIndividuals = false;
      },
      error: (error) => {
        console.error('Error loading individuals:', error);
        this.translate.get(['individual.loadFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['individual.loadFailed'], t['common.close'], { duration: 3000 });
        });
        this.loadingIndividuals = false;
      }
    });
  }

  filterIndividuals1(): void {
    const search = this.searchText1.toLowerCase();
    this.filteredIndividuals1 = this.individuals.filter(ind =>
      ind.fullName.toLowerCase().includes(search) ||
      (ind.givenName && ind.givenName.toLowerCase().includes(search)) ||
      (ind.surname && ind.surname.toLowerCase().includes(search))
    );
  }

  filterIndividuals2(): void {
    const search = this.searchText2.toLowerCase();
    this.filteredIndividuals2 = this.individuals.filter(ind =>
      ind.fullName.toLowerCase().includes(search) ||
      (ind.givenName && ind.givenName.toLowerCase().includes(search)) ||
      (ind.surname && ind.surname.toLowerCase().includes(search))
    );
  }

  selectPerson1(individual: Individual): void {
    this.selectedPerson1 = individual;
    this.searchText1 = individual.fullName;
    this.result = null;
  }

  selectPerson2(individual: Individual): void {
    this.selectedPerson2 = individual;
    this.searchText2 = individual.fullName;
    this.result = null;
  }

  clearPerson1(): void {
    this.selectedPerson1 = null;
    this.searchText1 = '';
    this.filteredIndividuals1 = [...this.individuals];
    this.result = null;
  }

  clearPerson2(): void {
    this.selectedPerson2 = null;
    this.searchText2 = '';
    this.filteredIndividuals2 = [...this.individuals];
    this.result = null;
  }

  swapPersons(): void {
    const temp = this.selectedPerson1;
    this.selectedPerson1 = this.selectedPerson2;
    this.selectedPerson2 = temp;

    const tempSearch = this.searchText1;
    this.searchText1 = this.searchText2;
    this.searchText2 = tempSearch;

    this.result = null;
  }

  canCalculate(): boolean {
    return !!this.selectedPerson1 && !!this.selectedPerson2 && !this.calculating;
  }

  calculateRelationship(): void {
    if (!this.canCalculate()) return;

    this.calculating = true;
    this.result = null;

    this.relationshipService.calculateRelationship(
      this.treeId,
      this.selectedPerson1!.id,
      this.selectedPerson2!.id
    ).subscribe({
      next: (response) => {
        this.result = response;
        this.calculating = false;
      },
      error: (error) => {
        console.error('Error calculating relationship:', error);
        this.translate.get(['relationship.calculateFailed', 'common.close']).subscribe(t => {
          this.snackBar.open(t['relationship.calculateFailed'], t['common.close'], { duration: 3000 });
        });
        this.calculating = false;
      }
    });
  }

  getCategoryLabel(category: RelationshipCategory): string {
    const labels: Record<RelationshipCategory, string> = {
      [RelationshipCategory.SELF]: 'Same Person',
      [RelationshipCategory.DIRECT_ANCESTOR]: 'Direct Ancestor',
      [RelationshipCategory.DIRECT_DESCENDANT]: 'Direct Descendant',
      [RelationshipCategory.SIBLING]: 'Sibling',
      [RelationshipCategory.SPOUSE]: 'Spouse',
      [RelationshipCategory.UNCLE_AUNT]: 'Uncle/Aunt',
      [RelationshipCategory.NEPHEW_NIECE]: 'Nephew/Niece',
      [RelationshipCategory.COUSIN]: 'Cousin',
      [RelationshipCategory.GRANDUNCLE_GRANDAUNT]: 'Grand-Uncle/Aunt',
      [RelationshipCategory.GRANDNEPHEW_GRANDNIECE]: 'Grand-Nephew/Niece',
      [RelationshipCategory.IN_LAW]: 'In-Law',
      [RelationshipCategory.STEP_FAMILY]: 'Step Family',
      [RelationshipCategory.NOT_RELATED]: 'Not Related'
    };
    return labels[category] || category;
  }

  getCategoryLabelVi(category: RelationshipCategory): string {
    const labels: Record<RelationshipCategory, string> = {
      [RelationshipCategory.SELF]: 'Cùng một người',
      [RelationshipCategory.DIRECT_ANCESTOR]: 'Trực hệ bề trên',
      [RelationshipCategory.DIRECT_DESCENDANT]: 'Trực hệ bề dưới',
      [RelationshipCategory.SIBLING]: 'Anh chị em ruột',
      [RelationshipCategory.SPOUSE]: 'Vợ chồng',
      [RelationshipCategory.UNCLE_AUNT]: 'Chú/Bác/Cô/Dì',
      [RelationshipCategory.NEPHEW_NIECE]: 'Cháu',
      [RelationshipCategory.COUSIN]: 'Anh chị em họ',
      [RelationshipCategory.GRANDUNCLE_GRANDAUNT]: 'Ông/Bà bác',
      [RelationshipCategory.GRANDNEPHEW_GRANDNIECE]: 'Cháu họ',
      [RelationshipCategory.IN_LAW]: 'Họ hàng bên chồng/vợ',
      [RelationshipCategory.STEP_FAMILY]: 'Gia đình kế',
      [RelationshipCategory.NOT_RELATED]: 'Không có quan hệ'
    };
    return labels[category] || category;
  }

  getCategoryIcon(category: RelationshipCategory): string {
    const icons: Record<RelationshipCategory, string> = {
      [RelationshipCategory.SELF]: 'person',
      [RelationshipCategory.DIRECT_ANCESTOR]: 'arrow_upward',
      [RelationshipCategory.DIRECT_DESCENDANT]: 'arrow_downward',
      [RelationshipCategory.SIBLING]: 'people',
      [RelationshipCategory.SPOUSE]: 'favorite',
      [RelationshipCategory.UNCLE_AUNT]: 'person_outline',
      [RelationshipCategory.NEPHEW_NIECE]: 'child_care',
      [RelationshipCategory.COUSIN]: 'group',
      [RelationshipCategory.GRANDUNCLE_GRANDAUNT]: 'elderly',
      [RelationshipCategory.GRANDNEPHEW_GRANDNIECE]: 'face',
      [RelationshipCategory.IN_LAW]: 'family_restroom',
      [RelationshipCategory.STEP_FAMILY]: 'diversity_3',
      [RelationshipCategory.NOT_RELATED]: 'block'
    };
    return icons[category] || 'help';
  }

  getCategoryColor(category: RelationshipCategory): string {
    const colors: Record<RelationshipCategory, string> = {
      [RelationshipCategory.SELF]: '#9e9e9e',
      [RelationshipCategory.DIRECT_ANCESTOR]: '#2196f3',
      [RelationshipCategory.DIRECT_DESCENDANT]: '#4caf50',
      [RelationshipCategory.SIBLING]: '#ff9800',
      [RelationshipCategory.SPOUSE]: '#e91e63',
      [RelationshipCategory.UNCLE_AUNT]: '#9c27b0',
      [RelationshipCategory.NEPHEW_NIECE]: '#00bcd4',
      [RelationshipCategory.COUSIN]: '#ff5722',
      [RelationshipCategory.GRANDUNCLE_GRANDAUNT]: '#673ab7',
      [RelationshipCategory.GRANDNEPHEW_GRANDNIECE]: '#009688',
      [RelationshipCategory.IN_LAW]: '#795548',
      [RelationshipCategory.STEP_FAMILY]: '#607d8b',
      [RelationshipCategory.NOT_RELATED]: '#f44336'
    };
    return colors[category] || '#9e9e9e';
  }

  getGenerationText(): string {
    if (!this.result) return '';
    const diff = this.result.generationDifference;
    if (diff === 0) return 'Same generation';
    if (diff > 0) return `${diff} generation(s) younger`;
    return `${Math.abs(diff)} generation(s) older`;
  }

  getGenerationTextVi(): string {
    if (!this.result) return '';
    const diff = this.result.generationDifference;
    if (diff === 0) return 'Cùng thế hệ';
    if (diff > 0) return `Nhỏ hơn ${diff} đời`;
    return `Lớn hơn ${Math.abs(diff)} đời`;
  }

  getGenderIcon(gender: string | undefined): string {
    if (gender === 'MALE') return 'male';
    if (gender === 'FEMALE') return 'female';
    return 'person';
  }

  getGenderColor(gender: string | undefined): string {
    if (gender === 'MALE') return '#2196f3';
    if (gender === 'FEMALE') return '#e91e63';
    return '#9e9e9e';
  }
}
