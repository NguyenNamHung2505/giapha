import { Component, OnInit, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import * as d3 from 'd3';
import { IndividualService } from '../individual/services/individual.service';
import { RelationshipService } from '../relationship/services/relationship.service';
import { UserTreeProfileService } from '../user-profile/services/user-tree-profile.service';
import { TreeService } from '../tree/services/tree.service';
import { GedcomService } from '../gedcom/services/gedcom.service';
import { GedcomImportComponent, GedcomImportData } from '../gedcom/gedcom-import/gedcom-import.component';
import { Individual } from '../individual/models/individual.model';
import { Relationship } from '../relationship/models/relationship.model';
import { Tree, TreeCloneInfo, RelatedTree, IndividualCloneInfo, TreeLocation } from '../tree/models/tree.model';
import { LanguageService } from '../../core/services/language.service';
import { environment } from '../../../environments/environment';

interface TreeNode {
  individual: Individual;
  name: string;
  children?: TreeNode[];
  spouses?: Individual[];
  parents?: TreeNode[];
  siblings?: TreeNode[];
}

type ViewMode = 'descendants' | 'ancestors' | 'both';

@Component({
  selector: 'app-tree-visualization',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatMenuModule,
    MatTooltipModule,
    MatDividerModule,
    MatDialogModule,
    TranslateModule
  ],
  templateUrl: './tree-visualization.component.html',
  styleUrl: './tree-visualization.component.scss'
})
export class TreeVisualizationComponent implements OnInit, OnDestroy {
  @ViewChild('treeContainer', { static: false }) treeContainer!: ElementRef;

  treeId!: string;
  individuals: Individual[] = [];
  relationships: Relationship[] = [];
  loading = true;

  // Perspective switching
  availablePerspectives: Individual[] = [];
  currentPerspective: Individual | null = null;
  viewHistory: { perspective: Individual; viewMode: ViewMode }[] = [];

  // View mode: descendants, ancestors, or both
  viewMode: ViewMode = 'both';

  // Tree clone info
  treeCloneInfo: TreeCloneInfo | null = null;

  // Individual clone info for current perspective
  perspectiveCloneInfo: IndividualCloneInfo | null = null;

  // Current tree data
  currentTree: Tree | null = null;

  private svg: any;
  private g: any;
  private zoom: any;
  private width = 1200;
  private height = 800;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private individualService: IndividualService,
    private relationshipService: RelationshipService,
    private userTreeProfileService: UserTreeProfileService,
    private treeService: TreeService,
    private gedcomService: GedcomService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar,
    private translate: TranslateService,
    private languageService: LanguageService
  ) { }

  /**
   * Format date based on current language
   */
  private formatDate(dateStr: string | undefined): string {
    if (!dateStr) return '?';
    const date = new Date(dateStr);
    const locale = this.languageService.getCurrentLanguage() === 'vi' ? 'vi-VN' : 'en-US';
    return date.toLocaleDateString(locale, { year: 'numeric', month: 'short', day: 'numeric' });
  }

  /**
   * Get translated label for Born/Died
   */
  private getBornLabel(): string {
    return this.translate.instant('visualization.born') || 'Sinh';
  }

  private getDiedLabel(): string {
    return this.translate.instant('visualization.died') || 'Mất';
  }

  // Perspective ID from query param (used when navigating between cloned trees)
  private perspectiveFromQuery: string | null = null;

  ngOnInit(): void {
    // Subscribe to query params to get perspective ID
    this.route.queryParamMap.subscribe(queryParams => {
      this.perspectiveFromQuery = queryParams.get('perspective');
    });

    this.route.paramMap.subscribe(params => {
      const newTreeId = params.get('treeId') || '';
      if (newTreeId && newTreeId !== this.treeId) {
        // Reset state when switching trees
        this.resetVisualization();
        this.treeId = newTreeId;
        this.loadTreeData();
        // Note: loadTreeCloneInfo is called inside loadUserProfileAndInitialize
      } else if (newTreeId && !this.treeId) {
        // Initial load
        this.treeId = newTreeId;
        this.loadTreeData();
        // Note: loadTreeCloneInfo is called inside loadUserProfileAndInitialize
      }
    });
  }

  /**
   * Reset visualization state when switching trees
   */
  private resetVisualization(): void {
    // Clear existing SVG
    if (this.svg) {
      this.svg.remove();
      this.svg = null;
    }

    // Reset data
    this.individuals = [];
    this.relationships = [];
    this.availablePerspectives = [];
    this.currentPerspective = null;
    this.viewHistory = [];
    this.viewMode = 'both';
    this.treeCloneInfo = null;
    this.perspectiveCloneInfo = null;
    this.currentTree = null;
    this.loading = true;
  }

  /**
   * Navigate to a related tree
   */
  navigateToTree(tree: RelatedTree): void {
    if (tree.isCurrentTree) {
      return;
    }
    this.router.navigate(['/trees', tree.treeId, 'visualize']);
  }

  /**
   * Check if current perspective person is the ROOT cloned person
   * Only shows "View in other trees" for the main person who was cloned,
   * not for other family members who were copied along.
   */
  perspectiveHasClones(): boolean {
    return this.perspectiveCloneInfo !== null &&
      this.perspectiveCloneInfo.rootClonedPerson === true &&
      this.perspectiveCloneInfo.allTreeLocations !== null &&
      this.perspectiveCloneInfo.allTreeLocations.length > 1;
  }

  /**
   * Navigate to view this person in another tree
   * Passes the individual ID so the target tree opens with that person's perspective
   */
  navigateToPersonInTree(location: TreeLocation): void {
    if (location.isCurrentTree) {
      return;
    }
    // Navigate with query param to set perspective to this person in the target tree
    this.router.navigate(['/trees', location.treeId, 'visualize'], {
      queryParams: { perspective: location.individualId }
    });
  }

  /**
   * Check if this tree was cloned from another tree
   */
  isClonedTree(): boolean {
    return this.treeCloneInfo?.sourceTreeInfo != null;
  }

  /**
   * Open sync dialog for cloned trees
   * For now, shows info about the source tree and redirects to merge feature
   */
  openSyncDialog(): void {
    const sourceInfo = this.treeCloneInfo?.sourceTreeInfo;
    if (!sourceInfo) {
      this.snackBar.open('Không tìm thấy cây nguồn', 'Đóng', { duration: 3000 });
      return;
    }

    // Show source tree info and offer to open merge dialog
    const message = `Cây này được clone từ "${sourceInfo.sourceTreeName}". Tính năng đồng bộ đang được phát triển.`;
    this.snackBar.open(message, 'Xem cây gốc', { duration: 5000 })
      .onAction().subscribe(() => {
        // Navigate to source tree
        this.router.navigate(['/trees', sourceInfo.sourceTreeId, 'visualize']);
      });
  }

  /**
   * Load clone info for the current perspective person
   */
  private loadPerspectiveCloneInfo(): void {
    if (!this.currentPerspective) {
      this.perspectiveCloneInfo = null;
      return;
    }

    this.treeService.getIndividualCloneInfo(this.treeId, this.currentPerspective.id).subscribe({
      next: (cloneInfo) => {
        this.perspectiveCloneInfo = cloneInfo;
      },
      error: () => {
        // Clone info is optional
        this.perspectiveCloneInfo = null;
      }
    });
  }

  ngOnDestroy(): void {
    if (this.svg) {
      this.svg.remove();
    }
  }

  loadTreeData(): void {
    this.loading = true;

    // Load individuals
    this.individualService.getIndividuals(this.treeId, 0, 1000).subscribe({
      next: (response) => {
        this.individuals = response.content;

        // Load relationships
        this.relationshipService.getRelationshipsByTree(this.treeId).subscribe({
          next: (relationships) => {
            this.relationships = relationships;

            // Populate available perspectives with all individuals who have any relationships
            // (not just spouses - include anyone connected to the tree)
            this.availablePerspectives = this.individuals.filter(ind => {
              return this.relationships.some(rel =>
                rel.individual1.id === ind.id || rel.individual2.id === ind.id
              );
            });

            // If no one has relationships yet, include all individuals
            if (this.availablePerspectives.length === 0) {
              this.availablePerspectives = [...this.individuals];
            }

            // Sort by name for better UX
            this.availablePerspectives.sort((a, b) => a.fullName.localeCompare(b.fullName));

            // Load user profile to set default perspective
            this.loadUserProfileAndInitialize();
          },
          error: (error) => {
            console.error('Error loading relationships:', error);
            this.snackBar.open('Failed to load relationships', 'Close', { duration: 3000 });
            this.loading = false;
          }
        });
      },
      error: (error) => {
        console.error('Error loading individuals:', error);
        this.snackBar.open('Failed to load individuals', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  /**
   * Load tree data and user's profile mapping to set default perspective
   * Priority: 1. User's profile, 2. Tree's rootIndividualId
   */
  private loadUserProfileAndInitialize(): void {
    // First, load tree data to get rootIndividualId
    this.treeService.getTree(this.treeId).subscribe({
      next: (tree) => {
        this.currentTree = tree;

        // Also load clone info for the "View Related Trees" dropdown
        this.treeService.getTreeCloneInfo(this.treeId).subscribe({
          next: (cloneInfo) => {
            this.treeCloneInfo = cloneInfo;
          },
          error: () => {
            // Clone info is optional
          }
        });

        // Now try to load user profile (priority 1)
        this.loadUserProfileForPerspective(tree);
      },
      error: () => {
        console.log('Could not load tree data');
        this.loading = false;
        setTimeout(() => this.initializeVisualization(), 0);
      }
    });
  }

  /**
   * Load user profile to set perspective
   * Priority: 1. Query param (from clone navigation), 2. User's profile, 3. Tree's rootIndividualId
   */
  private loadUserProfileForPerspective(tree: Tree): void {
    // Priority 0: Query param perspective (highest priority - from clone navigation)
    if (this.perspectiveFromQuery) {
      const perspectiveIndividual = this.individuals.find(ind => ind.id === this.perspectiveFromQuery);
      if (perspectiveIndividual) {
        this.currentPerspective = perspectiveIndividual;
        this.loadPerspectiveCloneInfo();
        // Clear query param after use
        this.perspectiveFromQuery = null;
        this.loading = false;
        setTimeout(() => this.initializeVisualization(), 0);
        return;
      }
    }

    this.userTreeProfileService.getMyProfile(this.treeId).subscribe({
      next: (profile) => {
        if (profile && profile.individual) {
          // Priority 1: User's profile
          const matchingIndividual = this.individuals.find(ind => ind.id === profile.individual.id);
          if (matchingIndividual) {
            this.currentPerspective = matchingIndividual;
            this.loadPerspectiveCloneInfo();
            this.loading = false;
            setTimeout(() => this.initializeVisualization(), 0);
            return;
          }
        }

        // Priority 2: Tree's rootIndividualId (for cloned trees)
        this.setRootIndividualPerspective(tree);
      },
      error: () => {
        // Profile not found - try rootIndividualId
        this.setRootIndividualPerspective(tree);
      }
    });
  }

  /**
   * Set perspective to tree's root individual (used for cloned trees)
   */
  private setRootIndividualPerspective(tree: Tree): void {
    if (tree.rootIndividualId) {
      const rootIndividual = this.individuals.find(ind => ind.id === tree.rootIndividualId);
      if (rootIndividual) {
        this.currentPerspective = rootIndividual;
        this.loadPerspectiveCloneInfo();
      }
    }

    this.loading = false;
    setTimeout(() => this.initializeVisualization(), 0);
  }

  initializeVisualization(): void {
    try {
      if (this.individuals.length === 0) {
        console.warn('No individuals in tree');
        this.snackBar.open('No individuals in this tree yet. Add some individuals to see the visualization.', 'Close', { duration: 5000 });
        return;
      }

      // Check if container is available
      if (!this.treeContainer || !this.treeContainer.nativeElement) {
        console.error('Tree container not available');
        setTimeout(() => this.initializeVisualization(), 100); // Retry after a delay
        return;
      }

      // Clear any existing SVG
      d3.select(this.treeContainer.nativeElement).selectAll('*').remove();

      // Create SVG
      this.svg = d3.select(this.treeContainer.nativeElement)
        .append('svg')
        .attr('width', '100%')
        .attr('height', this.height)
        .attr('viewBox', `0 0 ${this.width} ${this.height}`);

      // Create main group for zooming/panning
      this.g = this.svg.append('g');

      // Add zoom behavior
      this.zoom = d3.zoom()
        .scaleExtent([0.1, 3])
        .on('zoom', (event) => {
          this.g.attr('transform', event.transform);
        });

      this.svg.call(this.zoom);

      // Build tree structure
      const rootIndividual = this.findRootIndividual();
      if (!rootIndividual) {
        console.error('Could not find root individual');
        this.snackBar.open('Could not determine tree root. Please check your family tree data.', 'Close', { duration: 5000 });
        return;
      }

      const treeData = this.buildTreeStructure(rootIndividual);
      if (!treeData) {
        console.error('Failed to build tree structure');
        this.snackBar.open('Failed to build tree structure. Please check the relationships.', 'Close', { duration: 5000 });
        return;
      }

      this.renderTree(treeData);
    } catch (error) {
      console.error('Error initializing visualization:', error);
      this.snackBar.open('Error displaying family tree. Please try refreshing the page.', 'Close', { duration: 5000 });
    }
  }

  refresh(): void {
    this.loadTreeData();
  }

  // View mode methods
  setViewMode(mode: ViewMode): void {
    if (this.viewMode !== mode) {
      this.viewMode = mode;
      this.initializeVisualization();
    }
  }

  getViewModeLabel(): string {
    switch (this.viewMode) {
      case 'ancestors': return this.translate.instant('visualization.ancestors');
      case 'descendants': return this.translate.instant('visualization.descendants');
      case 'both': return this.translate.instant('visualization.both');
      default: return this.translate.instant('visualization.descendants');
    }
  }

  getTreeTitle(): string {
    if (!this.currentPerspective) {
      return this.translate.instant('visualization.title');
    }
    switch (this.viewMode) {
      case 'ancestors': return `${this.translate.instant('visualization.viewingAncestors')} ${this.currentPerspective.fullName}`;
      case 'descendants': return `${this.translate.instant('visualization.viewingDescendants')} ${this.currentPerspective.fullName}`;
      case 'both': return `${this.translate.instant('visualization.viewingFamily')} ${this.currentPerspective.fullName}`;
      default: return `${this.currentPerspective.fullName}`;
    }
  }

  changePerspective(person: Individual): void {
    // Save current perspective and view mode to history before changing
    if (this.currentPerspective && this.currentPerspective.id !== person.id) {
      this.viewHistory.push({ perspective: this.currentPerspective, viewMode: this.viewMode });
    }
    this.currentPerspective = person;
    this.loadPerspectiveCloneInfo(); // Load clone info for new perspective
    this.snackBar.open(`Viewing tree from ${person.fullName}'s perspective`, 'Close', { duration: 2000 });
    this.initializeVisualization();
  }

  setNodeAsRoot(person: Individual): void {
    // Save current perspective and view mode to history before changing
    if (this.currentPerspective && this.currentPerspective.id !== person.id) {
      this.viewHistory.push({ perspective: this.currentPerspective, viewMode: this.viewMode });
    } else if (!this.currentPerspective) {
      // Save default root to history
      const defaultRoot = this.findRootIndividual();
      if (defaultRoot) {
        this.viewHistory.push({ perspective: defaultRoot, viewMode: this.viewMode });
      }
    }
    this.currentPerspective = person;
    this.loadPerspectiveCloneInfo(); // Load clone info for new perspective

    // When clicking "View" on a node in ancestors view, switch to descendants view
    // This allows users to explore the family tree by clicking on ancestors
    // and then seeing their descendants
    if (this.viewMode === 'ancestors') {
      this.viewMode = 'descendants';
      this.snackBar.open(`Viewing descendants of ${person.fullName}`, 'Close', { duration: 2000 });
    } else {
      this.snackBar.open(`Viewing tree from ${person.fullName}'s perspective`, 'Close', { duration: 2000 });
    }
    this.initializeVisualization();
  }

  returnToPreviousView(): void {
    if (this.viewHistory.length > 0) {
      const previousView = this.viewHistory.pop()!;
      this.currentPerspective = previousView.perspective;
      this.viewMode = previousView.viewMode;
      this.loadPerspectiveCloneInfo(); // Load clone info for previous perspective
      this.snackBar.open(`Returned to ${previousView.perspective.fullName}'s perspective`, 'Close', { duration: 2000 });
      this.initializeVisualization();
    } else {
      // Return to default view
      this.currentPerspective = null;
      this.perspectiveCloneInfo = null;
      this.viewMode = 'both';
      this.snackBar.open('Returned to default view', 'Close', { duration: 2000 });
      this.initializeVisualization();
    }
  }

  canReturnToPreviousView(): boolean {
    return this.currentPerspective !== null || this.viewHistory.length > 0;
  }

  findRootIndividual(): Individual | null {
    // If a perspective is selected, use it as the root
    if (this.currentPerspective) {
      console.log(`Using perspective root: ${this.currentPerspective.fullName}`);
      return this.currentPerspective;
    }

    // Find all individuals with no parents (potential roots)
    const potentialRoots: Individual[] = [];

    for (const individual of this.individuals) {
      const hasParents = this.relationships.some(rel =>
        rel.individual2.id === individual.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
          rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      if (!hasParents) {
        potentialRoots.push(individual);
      }
    }

    if (potentialRoots.length === 0) {
      console.warn('No root individual found, using first individual');
      return this.individuals[0] || null;
    }

    // If only one root, use it
    if (potentialRoots.length === 1) {
      return potentialRoots[0];
    }

    // Multiple potential roots - find the best one:
    // 1. Prefer the one with the most descendants
    // 2. Prefer the oldest (earliest birth date)
    let bestRoot = potentialRoots[0];
    let maxDescendants = this.countDescendants(bestRoot);

    for (let i = 1; i < potentialRoots.length; i++) {
      const descendantCount = this.countDescendants(potentialRoots[i]);

      if (descendantCount > maxDescendants) {
        bestRoot = potentialRoots[i];
        maxDescendants = descendantCount;
      } else if (descendantCount === maxDescendants) {
        // If same number of descendants, prefer older individual
        const currentBirthDate = potentialRoots[i].birthDate ? new Date(potentialRoots[i].birthDate!) : null;
        const bestBirthDate = bestRoot.birthDate ? new Date(bestRoot.birthDate!) : null;

        if (currentBirthDate && bestBirthDate && currentBirthDate < bestBirthDate) {
          bestRoot = potentialRoots[i];
        } else if (currentBirthDate && !bestBirthDate) {
          bestRoot = potentialRoots[i];
        }
      }
    }

    console.log(`Selected root: ${bestRoot.fullName} with ${maxDescendants} descendants`);
    return bestRoot;
  }

  countDescendants(individual: Individual): number {
    const visited = new Set<string>();

    const countRecursive = (ind: Individual): number => {
      if (visited.has(ind.id)) {
        return 0;
      }
      visited.add(ind.id);

      let count = 0;
      const childRels = this.relationships.filter(rel =>
        rel.individual1.id === ind.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
          rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      for (const rel of childRels) {
        const child = this.individuals.find(i => i.id === rel.individual2.id);
        if (child) {
          count += 1 + countRecursive(child);
        }
      }

      return count;
    };

    return countRecursive(individual);
  }

  /**
   * Find siblings of an individual (share at least one parent)
   */
  findSiblings(individual: Individual, visited: Set<string>): any[] {
    const siblings: any[] = [];
    const siblingIds = new Set<string>();

    // Find all parents of this individual
    const parentRelationships = this.relationships.filter(rel =>
      rel.individual2.id === individual.id &&
      (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
        rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
    );

    // For each parent, find their other children (siblings)
    for (const parentRel of parentRelationships) {
      const parentId = parentRel.individual1.id;

      // Find all children of this parent
      const childRelationships = this.relationships.filter(rel =>
        rel.individual1.id === parentId &&
        rel.individual2.id !== individual.id && // Exclude the individual themselves
        (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
          rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      for (const childRel of childRelationships) {
        const siblingId = childRel.individual2.id;

        // Skip if already added or already in visited
        if (siblingIds.has(siblingId) || visited.has(siblingId)) {
          continue;
        }

        const sibling = this.individuals.find(ind => ind.id === siblingId);
        if (sibling) {
          siblingIds.add(siblingId);
          visited.add(siblingId);

          // Build sibling node with their descendants
          const siblingNode: any = {
            individual: sibling,
            name: sibling.fullName,
            children: [],
            spouses: []
          };

          // Find spouses of sibling
          const spouseRelationships = this.relationships.filter(rel =>
            (rel.individual1.id === sibling.id || rel.individual2.id === sibling.id) &&
            (rel.type === 'SPOUSE' || rel.type === 'PARTNER')
          );

          for (const spouseRel of spouseRelationships) {
            const spouseId = spouseRel.individual1.id === sibling.id ? spouseRel.individual2.id : spouseRel.individual1.id;
            const spouse = this.individuals.find(ind => ind.id === spouseId);
            if (spouse && !visited.has(spouse.id)) {
              siblingNode.spouses.push(spouse);
            }
          }

          // Find children of sibling (recursively)
          const siblingChildRels = this.relationships.filter(rel =>
            rel.individual1.id === sibling.id &&
            (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
              rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
          );

          for (const childRel of siblingChildRels) {
            const child = this.individuals.find(ind => ind.id === childRel.individual2.id);
            if (child && !visited.has(child.id)) {
              const childNode = this.buildSiblingDescendants(child, visited);
              if (childNode) {
                siblingNode.children.push(childNode);
              }
            }
          }

          // Sort children by birth date
          siblingNode.children.sort((a: any, b: any) => {
            const birthA = a.individual.birthDate ? new Date(a.individual.birthDate).getTime() : Infinity;
            const birthB = b.individual.birthDate ? new Date(b.individual.birthDate).getTime() : Infinity;
            return birthA - birthB;
          });

          siblings.push(siblingNode);
        }
      }
    }

    // Sort siblings by birth date (oldest first)
    siblings.sort((a, b) => {
      const birthA = a.individual.birthDate ? new Date(a.individual.birthDate).getTime() : Infinity;
      const birthB = b.individual.birthDate ? new Date(b.individual.birthDate).getTime() : Infinity;
      return birthA - birthB;
    });

    return siblings;
  }

  /**
   * Build descendants tree for a sibling
   */
  buildSiblingDescendants(individual: Individual, visited: Set<string>): any {
    if (visited.has(individual.id)) {
      return null;
    }
    visited.add(individual.id);

    const node: any = {
      individual,
      name: individual.fullName,
      children: [],
      spouses: []
    };

    // Find spouses
    const spouseRelationships = this.relationships.filter(rel =>
      (rel.individual1.id === individual.id || rel.individual2.id === individual.id) &&
      (rel.type === 'SPOUSE' || rel.type === 'PARTNER')
    );

    for (const rel of spouseRelationships) {
      const spouseId = rel.individual1.id === individual.id ? rel.individual2.id : rel.individual1.id;
      const spouse = this.individuals.find(ind => ind.id === spouseId);
      if (spouse && !visited.has(spouse.id)) {
        node.spouses.push(spouse);
      }
    }

    // Find children
    const childRelationships = this.relationships.filter(rel =>
      rel.individual1.id === individual.id &&
      (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
        rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
    );

    for (const rel of childRelationships) {
      const child = this.individuals.find(ind => ind.id === rel.individual2.id);
      if (child && !visited.has(child.id)) {
        const childNode = this.buildSiblingDescendants(child, visited);
        if (childNode) {
          node.children.push(childNode);
        }
      }
    }

    // Sort children by birth date
    node.children.sort((a: any, b: any) => {
      const birthA = a.individual.birthDate ? new Date(a.individual.birthDate).getTime() : Infinity;
      const birthB = b.individual.birthDate ? new Date(b.individual.birthDate).getTime() : Infinity;
      return birthA - birthB;
    });

    return node;
  }

  buildTreeStructure(root: Individual): any {
    const visited = new Set<string>();

    // Build descendants tree (children, grandchildren, etc.)
    const buildDescendantsNode = (individual: Individual): any => {
      if (visited.has(individual.id)) {
        return null;
      }
      visited.add(individual.id);

      const node: any = {
        individual,
        name: individual.fullName,
        children: [],
        spouses: []
      };

      // Find spouses/partners
      const spouseRelationships = this.relationships.filter(rel =>
        (rel.individual1.id === individual.id || rel.individual2.id === individual.id) &&
        (rel.type === 'SPOUSE' || rel.type === 'PARTNER')
      );

      for (const rel of spouseRelationships) {
        const spouseId = rel.individual1.id === individual.id ? rel.individual2.id : rel.individual1.id;
        const spouse = this.individuals.find(ind => ind.id === spouseId);
        if (spouse && !visited.has(spouse.id)) {
          node.spouses.push(spouse);
        }
      }

      // Find children
      const childRelationships = this.relationships.filter(rel =>
        rel.individual1.id === individual.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
          rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      for (const rel of childRelationships) {
        const child = this.individuals.find(ind => ind.id === rel.individual2.id);
        if (child) {
          const childNode = buildDescendantsNode(child);
          if (childNode) {
            node.children.push(childNode);
          }
        }
      }

      // Sort children by birth date (oldest first)
      node.children.sort((a: any, b: any) => {
        const birthA = a.individual.birthDate ? new Date(a.individual.birthDate).getTime() : Infinity;
        const birthB = b.individual.birthDate ? new Date(b.individual.birthDate).getTime() : Infinity;
        return birthA - birthB;
      });

      return node;
    };

    // Build ancestors tree (parents, grandparents, etc.)
    const buildAncestorsNode = (individual: Individual, visitedAncestors: Set<string>): any => {
      if (visitedAncestors.has(individual.id)) {
        return null;
      }
      visitedAncestors.add(individual.id);

      const node: any = {
        individual,
        name: individual.fullName,
        children: [], // In ancestor view, "children" are actually parents (going up the tree)
        spouses: []
      };

      // Find parents (they become "children" in the tree structure for upward display)
      const parentRelationships = this.relationships.filter(rel =>
        rel.individual2.id === individual.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
          rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      for (const rel of parentRelationships) {
        const parent = this.individuals.find(ind => ind.id === rel.individual1.id);
        if (parent) {
          const parentNode = buildAncestorsNode(parent, visitedAncestors);
          if (parentNode) {
            node.children.push(parentNode);
          }
        }
      }

      // Sort parents (father first if known)
      node.children.sort((a: any, b: any) => {
        if (a.individual.gender === 'MALE' && b.individual.gender !== 'MALE') return -1;
        if (a.individual.gender !== 'MALE' && b.individual.gender === 'MALE') return 1;
        return 0;
      });

      return node;
    };

    let tree: any;

    if (this.viewMode === 'ancestors') {
      // Build ancestors only
      tree = buildAncestorsNode(root, new Set<string>());
    } else if (this.viewMode === 'descendants') {
      // Build descendants only
      tree = buildDescendantsNode(root);
    } else {
      // Build both - first ancestors as a separate branch, then descendants
      // For "both" mode, we show descendants normally but also show ancestors above the root
      visited.clear();
      tree = buildDescendantsNode(root);

      // Add ancestors as a special property
      const ancestorsVisited = new Set<string>();
      ancestorsVisited.add(root.id); // Don't include root again

      const parentRelationships = this.relationships.filter(rel =>
        rel.individual2.id === root.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'MOTHER_CHILD' || rel.type === 'FATHER_CHILD' ||
          rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      tree.ancestors = [];
      for (const rel of parentRelationships) {
        const parent = this.individuals.find(ind => ind.id === rel.individual1.id);
        if (parent) {
          const parentNode = buildAncestorsNode(parent, ancestorsVisited);
          if (parentNode) {
            tree.ancestors.push(parentNode);
          }
        }
      }

      // Find siblings (people who share at least one parent with root)
      tree.siblings = this.findSiblings(root, visited);
    }

    // Log tree structure for debugging
    console.log('Tree structure built:', tree);
    console.log('View mode:', this.viewMode);
    console.log('Total individuals:', this.individuals.length);
    console.log('Total relationships:', this.relationships.length);

    return tree;
  }

  renderTree(treeData: any): void {
    if (!treeData) {
      console.error('No tree data to render');
      this.snackBar.open('Unable to render tree - no data available', 'Close', { duration: 3000 });
      return;
    }

    // For "both" mode, render ancestors above and descendants below
    if (this.viewMode === 'both' && treeData.ancestors && treeData.ancestors.length > 0) {
      this.renderBothView(treeData);
      return;
    }

    // For "ancestors" mode, render with selected person at bottom and ancestors above
    if (this.viewMode === 'ancestors') {
      this.renderAncestorsView(treeData);
      return;
    }

    // Create tree layout
    const treeLayout = d3.tree<any>()
      .size([this.width - 200, this.height - 200]);

    // Create hierarchy
    const root = d3.hierarchy(treeData);
    const treeNodes = treeLayout(root);

    // Store node positions for dragging
    const nodePositions = new Map<string, { x: number, y: number }>();
    treeNodes.descendants().forEach((d: any) => {
      nodePositions.set(d.data.individual.id, { x: d.x, y: d.y });
    });

    // Center the tree
    const translateX = 100;
    const translateY = 50;
    const spouseOffset = 180; // Horizontal distance between spouses

    // Adjust positions to make room for spouses
    // Process each level and shift siblings to make room for spouses
    this.adjustPositionsForSpouses(treeNodes, nodePositions, spouseOffset);

    // Detect and resolve collisions
    this.resolveCollisions(treeNodes, nodePositions);

    // Store links data for updating during drag
    const linksData = treeNodes.links();

    // Create drag behavior
    const dragBehavior = d3.drag()
      .subject((event: any, d: any) => {
        // Return the current position of the node as the subject
        // This ensures drag starts from the node's actual position
        const individualId = d.data ? d.data.individual.id : d.id;
        const pos = nodePositions.get(individualId);
        if (pos) {
          return { x: pos.x + translateX, y: pos.y + translateY };
        }
        // Fallback to d's position if available
        if (d.x !== undefined && d.y !== undefined) {
          return { x: d.x + translateX, y: d.y + translateY };
        }
        return { x: event.x, y: event.y };
      })
      .on('start', (event: any, d: any) => {
        // Find the node group element (the <g> with class 'node' or 'spouse-node')
        const nodeGroup = d3.select(event.sourceEvent.target).node().closest('.node, .spouse-node');
        if (nodeGroup) {
          d3.select(nodeGroup).raise();
        }
      })
      .on('drag', (event: any, d: any) => {
        const individualId = d.data ? d.data.individual.id : d.id;

        // Find the node group element (the <g> with class 'node' or 'spouse-node')
        const nodeGroup = d3.select(event.sourceEvent.target).node().closest('.node, .spouse-node');
        if (!nodeGroup) return;

        // Update node position using the subject's new position (event.x, event.y)
        // event.x and event.y are the subject coordinates updated by the drag delta
        d3.select(nodeGroup)
          .attr('transform', `translate(${event.x},${event.y})`);

        // Update stored position (relative to translate offset)
        const newX = event.x - translateX;
        const newY = event.y - translateY;
        nodePositions.set(individualId, { x: newX, y: newY });

        // Update all parent-child links connected to this node
        this.g.selectAll('.link')
          .attr('d', (linkData: any) => {
            const sourceId = linkData.source.data.individual.id;
            const targetId = linkData.target.data.individual.id;

            // Get current positions (either updated or original)
            const sourcePos = nodePositions.get(sourceId) || { x: linkData.source.x, y: linkData.source.y };
            const targetPos = nodePositions.get(targetId) || { x: linkData.target.x, y: linkData.target.y };

            const sourceX = sourcePos.x + translateX;
            const sourceY = sourcePos.y + translateY;
            const targetX = targetPos.x + translateX;
            const targetY = targetPos.y + translateY;

            return `M${sourceX},${sourceY}
                    C${sourceX},${(sourceY + targetY) / 2}
                     ${targetX},${(sourceY + targetY) / 2}
                     ${targetX},${targetY}`;
          });

        // Update spouse links connected to this node
        this.g.selectAll('.spouse-link')
          .attr('x1', function (this: any) {
            const spouse1Id = d3.select(this).attr('data-spouse1');
            const pos = nodePositions.get(spouse1Id);
            return pos ? pos.x + translateX : d3.select(this).attr('x1');
          })
          .attr('y1', function (this: any) {
            const spouse1Id = d3.select(this).attr('data-spouse1');
            const pos = nodePositions.get(spouse1Id);
            return pos ? pos.y + translateY : d3.select(this).attr('y1');
          })
          .attr('x2', function (this: any) {
            const spouse2Id = d3.select(this).attr('data-spouse2');
            const pos = nodePositions.get(spouse2Id);
            return pos ? pos.x + translateX : d3.select(this).attr('x2');
          })
          .attr('y2', function (this: any) {
            const spouse2Id = d3.select(this).attr('data-spouse2');
            const pos = nodePositions.get(spouse2Id);
            return pos ? pos.y + translateY : d3.select(this).attr('y2');
          });
      });

    // Draw parent-child links
    this.g.selectAll('.link')
      .data(treeNodes.links())
      .enter()
      .append('path')
      .attr('class', 'link')
      .attr('d', (d: any) => {
        return `M${d.source.x + translateX},${d.source.y + translateY}
                C${d.source.x + translateX},${(d.source.y + d.target.y) / 2 + translateY}
                 ${d.target.x + translateX},${(d.source.y + d.target.y) / 2 + translateY}
                 ${d.target.x + translateX},${d.target.y + translateY}`;
      })
      .attr('fill', 'none')
      .attr('stroke', '#ccc')
      .attr('stroke-width', 2);

    // Draw spouse connections
    treeNodes.descendants().forEach((d: any) => {
      if (d.data.spouses && d.data.spouses.length > 0) {
        d.data.spouses.forEach((spouse: Individual, index: number) => {
          // Place spouse to the right of the person (space has been reserved by adjustPositionsForSpouses)
          const spouseX = d.x + spouseOffset * (index + 1);
          const spouseY = d.y;

          // Store spouse position
          nodePositions.set(spouse.id, { x: spouseX, y: spouseY });

          // Store reference to partner for updating spouse links
          const partnerId = d.data.individual.id;

          // Draw horizontal line connecting spouses
          const spouseLinkElement = this.g.append('line')
            .attr('class', 'spouse-link')
            .attr('data-spouse1', partnerId)
            .attr('data-spouse2', spouse.id)
            .attr('x1', d.x + translateX)
            .attr('y1', spouseY + translateY)
            .attr('x2', spouseX + translateX)
            .attr('y2', spouseY + translateY)
            .attr('stroke', '#ff69b4')
            .attr('stroke-width', 2)
            .attr('stroke-dasharray', '5,5');

          // Draw spouse node with data attached
          const spouseNode = this.g.append('g')
            .datum({ id: spouse.id, partnerId: partnerId })
            .attr('class', 'spouse-node')
            .attr('transform', `translate(${spouseX + translateX},${spouseY + translateY})`)
            .on('click', (event: any) => {
              // Prevent click event when dragging or when button is clicked
              if (event.defaultPrevented) return;
              this.onNodeClick(spouse);
            })
            .style('cursor', 'pointer')
            .call(dragBehavior);

          // Add avatar image or colored circle with information frame
          const spouseAvatarUrl = this.getAvatarUrl(spouse);

          // Check if this spouse is the current perspective
          const isSpouseCurrentPerspective = this.currentPerspective?.id === spouse.id;

          // Calculate frame dimensions
          const frameWidth = 160;
          const frameHeight = 120;

          // Add background frame/card (gray if deceased)
          const isSpouseDeceased = !!spouse.deathDate;
          spouseNode.append('rect')
            .attr('x', -frameWidth / 2)
            .attr('y', -frameHeight / 2)
            .attr('width', frameWidth)
            .attr('height', frameHeight)
            .attr('rx', 8)
            .attr('ry', 8)
            .attr('fill', isSpouseDeceased ? '#e0e0e0' : '#ffffff')
            .attr('stroke', () => {
              // Gold border for current perspective
              if (isSpouseCurrentPerspective) return '#FFD700';
              if (spouse.gender === 'MALE') return '#4A90E2';
              if (spouse.gender === 'FEMALE') return '#E294A9';
              return '#9B9B9B';
            })
            .attr('stroke-width', isSpouseCurrentPerspective ? 4 : 2)
            .attr('filter', isSpouseCurrentPerspective
              ? 'drop-shadow(0px 4px 8px rgba(255,215,0,0.5))'
              : 'drop-shadow(0px 2px 4px rgba(0,0,0,0.1))');

          if (spouseAvatarUrl) {
            // Show full rectangular image without circular clip
            spouseNode.append('image')
              .attr('xlink:href', spouseAvatarUrl)
              .attr('x', -35)
              .attr('y', -52)
              .attr('width', 70)
              .attr('height', 70)
              .attr('preserveAspectRatio', 'xMidYMid slice');

            // Add border around image
            spouseNode.append('rect')
              .attr('x', -35)
              .attr('y', -52)
              .attr('width', 70)
              .attr('height', 70)
              .attr('rx', 4)
              .attr('ry', 4)
              .attr('fill', 'none')
              .attr('stroke', () => {
                if (spouse.gender === 'MALE') return '#4A90E2';
                if (spouse.gender === 'FEMALE') return '#E294A9';
                return '#9B9B9B';
              })
              .attr('stroke-width', 2);
          } else {
            // Fallback to colored circle with initials
            spouseNode.append('circle')
              .attr('cy', -20)
              .attr('r', 30)
              .attr('fill', () => {
                if (spouse.gender === 'MALE') return '#4A90E2';
                if (spouse.gender === 'FEMALE') return '#E294A9';
                return '#9B9B9B';
              })
              .attr('stroke', '#fff')
              .attr('stroke-width', 2);

            spouseNode.append('text')
              .attr('y', -15)
              .attr('text-anchor', 'middle')
              .attr('fill', 'white')
              .attr('font-size', '16px')
              .attr('font-weight', 'bold')
              .text(this.getInitials(spouse));
          }

          // Add name text inside frame (moved down with more spacing)
          spouseNode.append('text')
            .attr('y', 30)
            .attr('text-anchor', 'middle')
            .attr('font-size', '11px')
            .attr('font-weight', 'bold')
            .attr('fill', '#333')
            .text(this.truncateText(spouse.fullName, 18));

          // Add birth/death dates inside frame
          const spouseBirthDate = this.formatDate(spouse.birthDate);
          const spouseDeathDate = spouse.deathDate ? this.formatDate(spouse.deathDate) : '';

          spouseNode.append('text')
            .attr('y', 45)
            .attr('text-anchor', 'middle')
            .attr('font-size', '9px')
            .attr('fill', '#666')
            .text(`${this.getBornLabel()}: ${spouseBirthDate}`);

          if (spouseDeathDate) {
            spouseNode.append('text')
              .attr('y', 57)
              .attr('text-anchor', 'middle')
              .attr('font-size', '9px')
              .attr('fill', '#666')
              .text(`${this.getDiedLabel()}: ${spouseDeathDate}`);
          }

          // Add "Set as Root" button for spouse
          const spouseButtonGroup = spouseNode.append('g')
            .attr('class', 'set-root-button')
            .attr('transform', 'translate(0, -60)')
            .style('cursor', 'pointer')
            .style('pointer-events', 'all')
            .on('mousedown', function (event: any) {
              event.preventDefault();
              event.stopPropagation();
              event.stopImmediatePropagation();
            })
            .on('click', function (event: any) {
              event.preventDefault();
              event.stopPropagation();
              event.stopImmediatePropagation();
            })
            .on('mouseup', (event: any) => {
              event.preventDefault();
              event.stopPropagation();
              event.stopImmediatePropagation();
              this.setNodeAsRoot(spouse);
            });

          spouseButtonGroup.append('rect')
            .attr('x', -20)
            .attr('y', -8)
            .attr('width', 40)
            .attr('height', 16)
            .attr('rx', 8)
            .attr('fill', '#3f51b5')
            .attr('opacity', 0.9)
            .style('pointer-events', 'all');

          spouseButtonGroup.append('text')
            .attr('text-anchor', 'middle')
            .attr('font-size', '9px')
            .attr('font-weight', 'bold')
            .attr('fill', 'white')
            .style('pointer-events', 'none')
            .text('View');
        });
      }
    });

    // Draw main nodes
    const nodes = this.g.selectAll('.node')
      .data(treeNodes.descendants())
      .enter()
      .append('g')
      .attr('class', 'node')
      .attr('transform', (d: any) => `translate(${d.x + translateX},${d.y + translateY})`)
      .attr('data-id', (d: any) => d.data.individual.id)
      .on('click', (event: any, d: any) => {
        // Prevent click event when dragging
        if (event.defaultPrevented) return;
        this.onNodeClick(d.data.individual);
      })
      .style('cursor', 'move')
      .call(dragBehavior);

    // Add avatar images or circles for nodes with information frame
    nodes.each((d: any, i: number, nodeElements: any) => {
      const node = d3.select(nodeElements[i]);
      const individual = d.data.individual;
      const avatarUrl = this.getAvatarUrl(individual);

      // Check if this is the current perspective (selected person)
      const isCurrentPerspective = this.currentPerspective?.id === individual.id;

      // Calculate frame dimensions
      const frameWidth = 160;
      const frameHeight = 120;

      // Add background frame/card (gray if deceased)
      const isDeceased = !!individual.deathDate;
      node.append('rect')
        .attr('x', -frameWidth / 2)
        .attr('y', -frameHeight / 2)
        .attr('width', frameWidth)
        .attr('height', frameHeight)
        .attr('rx', 8)
        .attr('ry', 8)
        .attr('fill', isDeceased ? '#e0e0e0' : '#ffffff')
        .attr('stroke', () => {
          // Gold border for current perspective
          if (isCurrentPerspective) return '#FFD700';
          const gender = individual.gender;
          if (gender === 'MALE') return '#4A90E2';
          if (gender === 'FEMALE') return '#E294A9';
          return '#9B9B9B';
        })
        .attr('stroke-width', isCurrentPerspective ? 4 : 2)
        .attr('filter', isCurrentPerspective
          ? 'drop-shadow(0px 4px 8px rgba(255,215,0,0.5))'
          : 'drop-shadow(0px 2px 4px rgba(0,0,0,0.1))');

      if (avatarUrl) {
        // Show full rectangular image without circular clip
        node.append('image')
          .attr('xlink:href', avatarUrl)
          .attr('x', -35)
          .attr('y', -52)
          .attr('width', 70)
          .attr('height', 70)
          .attr('preserveAspectRatio', 'xMidYMid slice');

        // Add border around image
        node.append('rect')
          .attr('x', -35)
          .attr('y', -52)
          .attr('width', 70)
          .attr('height', 70)
          .attr('rx', 4)
          .attr('ry', 4)
          .attr('fill', 'none')
          .attr('stroke', () => {
            const gender = individual.gender;
            if (gender === 'MALE') return '#4A90E2';
            if (gender === 'FEMALE') return '#E294A9';
            return '#9B9B9B';
          })
          .attr('stroke-width', 2);
      } else {
        // Fallback to colored circle with initials
        node.append('circle')
          .attr('cy', -20)
          .attr('r', 30)
          .attr('fill', () => {
            const gender = individual.gender;
            if (gender === 'MALE') return '#4A90E2';
            if (gender === 'FEMALE') return '#E294A9';
            return '#9B9B9B';
          })
          .attr('stroke', '#fff')
          .attr('stroke-width', 2);

        node.append('text')
          .attr('y', -15)
          .attr('text-anchor', 'middle')
          .attr('fill', 'white')
          .attr('font-size', '16px')
          .attr('font-weight', 'bold')
          .text(this.getInitials(individual));
      }

      // Add name text inside frame (moved down with more spacing)
      node.append('text')
        .attr('y', 30)
        .attr('text-anchor', 'middle')
        .attr('font-size', '11px')
        .attr('font-weight', 'bold')
        .attr('fill', '#333')
        .text(this.truncateText(individual.fullName, 18));

      // Add birth/death dates inside frame
      const birthDate = this.formatDate(individual.birthDate);
      const deathDate = individual.deathDate ? this.formatDate(individual.deathDate) : '';

      node.append('text')
        .attr('y', 45)
        .attr('text-anchor', 'middle')
        .attr('font-size', '9px')
        .attr('fill', '#666')
        .text(`${this.getBornLabel()}: ${birthDate}`);

      if (deathDate) {
        node.append('text')
          .attr('y', 57)
          .attr('text-anchor', 'middle')
          .attr('font-size', '9px')
          .attr('fill', '#666')
          .text(`${this.getDiedLabel()}: ${deathDate}`);
      }

      // Add "Set as Root" button for main node
      const buttonGroup = node.append('g')
        .attr('class', 'set-root-button')
        .attr('transform', 'translate(0, -60)')
        .style('cursor', 'pointer')
        .style('pointer-events', 'all')
        .on('mousedown', function (event: any) {
          event.preventDefault();
          event.stopPropagation();
          event.stopImmediatePropagation();
        })
        .on('click', function (event: any) {
          event.preventDefault();
          event.stopPropagation();
          event.stopImmediatePropagation();
        })
        .on('mouseup', (event: any) => {
          event.preventDefault();
          event.stopPropagation();
          event.stopImmediatePropagation();
          this.setNodeAsRoot(individual);
        });

      buttonGroup.append('rect')
        .attr('x', -20)
        .attr('y', -8)
        .attr('width', 40)
        .attr('height', 16)
        .attr('rx', 8)
        .attr('fill', '#3f51b5')
        .attr('opacity', 0.9)
        .style('pointer-events', 'all');

      buttonGroup.append('text')
        .attr('text-anchor', 'middle')
        .attr('font-size', '9px')
        .attr('font-weight', 'bold')
        .attr('fill', 'white')
        .style('pointer-events', 'none')
        .text('View');
    });

    console.log('Tree rendered successfully');
  }

  getInitials(individual: Individual): string {
    const given = individual.givenName || '';
    const surname = individual.surname || '';
    return (given.charAt(0) + surname.charAt(0)).toUpperCase() || '?';
  }

  getLifeYears(individual: Individual): string {
    const birthYear = individual.birthDate ? new Date(individual.birthDate).getFullYear() : '?';
    const deathYear = individual.deathDate ? new Date(individual.deathDate).getFullYear() : '';
    return deathYear ? `${birthYear}-${deathYear}` : `${birthYear}-`;
  }

  /**
   * Truncate text to max length and add ellipsis
   */
  truncateText(text: string, maxLength: number): string {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }

  /**
   * Get full avatar URL from profilePictureUrl
   */
  getAvatarUrl(individual: Individual): string | null {
    if (!individual.profilePictureUrl) {
      return null;
    }
    // If it's already a full URL, return as is
    if (individual.profilePictureUrl.startsWith('http')) {
      return individual.profilePictureUrl;
    }
    // Otherwise, prepend API base URL
    return `${environment.baseUrl}${individual.profilePictureUrl}`;
  }

  onNodeClick(individual: Individual): void {
    this.router.navigate(['/trees', this.treeId, 'individuals', individual.id]);
  }

  zoomIn(): void {
    this.svg.transition().call(this.zoom.scaleBy, 1.3);
  }

  zoomOut(): void {
    this.svg.transition().call(this.zoom.scaleBy, 0.7);
  }

  resetZoom(): void {
    this.svg.transition().call(this.zoom.transform, d3.zoomIdentity);
  }

  back(): void {
    this.router.navigate(['/trees']);
  }

  /**
   * Adjust node positions to make room for spouses.
   * Spouses should be placed immediately to the right of their partner,
   * and all siblings to the right should be shifted accordingly.
   */
  private adjustPositionsForSpouses(treeNodes: any, nodePositions: Map<string, { x: number, y: number }>, spouseOffset: number): void {
    // Group nodes by depth (level in the tree)
    const nodesByDepth = new Map<number, any[]>();
    treeNodes.descendants().forEach((node: any) => {
      const depth = node.depth;
      if (!nodesByDepth.has(depth)) {
        nodesByDepth.set(depth, []);
      }
      nodesByDepth.get(depth)!.push(node);
    });

    // Process each level
    nodesByDepth.forEach((nodesAtLevel, depth) => {
      // Sort nodes at this level by x position (left to right)
      nodesAtLevel.sort((a: any, b: any) => {
        const posA = nodePositions.get(a.data.individual.id);
        const posB = nodePositions.get(b.data.individual.id);
        return (posA?.x || a.x) - (posB?.x || b.x);
      });

      // Calculate cumulative shift for each node based on spouses of nodes to the left
      let cumulativeShift = 0;

      for (let i = 0; i < nodesAtLevel.length; i++) {
        const node = nodesAtLevel[i];
        const nodeId = node.data.individual.id;
        const currentPos = nodePositions.get(nodeId) || { x: node.x, y: node.y };

        // Apply cumulative shift from previous spouses
        currentPos.x += cumulativeShift;
        nodePositions.set(nodeId, currentPos);

        // Also update the d3 node's x for link rendering
        node.x = currentPos.x;

        // Count spouses for this node
        const numSpouses = node.data.spouses?.length || 0;

        // Add to cumulative shift for nodes to the right
        if (numSpouses > 0) {
          cumulativeShift += spouseOffset * numSpouses;
        }
      }
    });

    // Also shift all descendants when an ancestor has spouses
    this.shiftDescendantsForSpouses(treeNodes, nodePositions, spouseOffset);
  }

  /**
   * Shift descendant subtrees when their ancestor has spouses
   */
  private shiftDescendantsForSpouses(treeNodes: any, nodePositions: Map<string, { x: number, y: number }>, spouseOffset: number): void {
    const shiftSubtree = (node: any, shift: number) => {
      if (shift === 0) return;

      const nodeId = node.data.individual.id;
      const pos = nodePositions.get(nodeId) || { x: node.x, y: node.y };
      pos.x += shift;
      nodePositions.set(nodeId, pos);
      node.x = pos.x;

      // Recursively shift children
      if (node.children) {
        node.children.forEach((child: any) => shiftSubtree(child, shift));
      }
    };

    // Process from root, calculating shift for each subtree
    const processNode = (node: any, inheritedShift: number) => {
      // Apply inherited shift
      if (inheritedShift > 0) {
        shiftSubtree(node, inheritedShift);
      }

      // Calculate additional shift from this node's spouses
      const numSpouses = node.data.spouses?.length || 0;
      const additionalShift = numSpouses > 0 ? spouseOffset * numSpouses : 0;

      // Process children with accumulated shift
      if (node.children) {
        // Children to the right of the first child should be shifted more
        node.children.forEach((child: any, index: number) => {
          // First child stays, subsequent children get extra shift
          const childShift = index > 0 ? additionalShift : 0;
          processNode(child, childShift);
        });
      }
    };

    processNode(treeNodes, 0);
  }

  /**
   * Resolve collisions between nodes by adjusting their positions
   */
  private resolveCollisions(treeNodes: any, nodePositions: Map<string, { x: number, y: number }>): void {
    const frameWidth = 160;
    const frameHeight = 120;
    const minDistance = 20; // Minimum space between frames

    const nodes = treeNodes.descendants();

    // Iterate multiple times to resolve all collisions
    for (let iteration = 0; iteration < 5; iteration++) {
      let hasCollision = false;

      // Check each pair of nodes
      for (let i = 0; i < nodes.length; i++) {
        for (let j = i + 1; j < nodes.length; j++) {
          const node1 = nodes[i];
          const node2 = nodes[j];

          const id1 = node1.data.individual.id;
          const id2 = node2.data.individual.id;

          const pos1 = nodePositions.get(id1) || { x: node1.x, y: node1.y };
          const pos2 = nodePositions.get(id2) || { x: node2.x, y: node2.y };

          // Calculate distance between centers
          const dx = pos2.x - pos1.x;
          const dy = pos2.y - pos1.y;

          // Required minimum distance (sum of half-widths + spacing)
          const minDistX = frameWidth + minDistance;
          const minDistY = frameHeight + minDistance;

          // Check if they overlap
          if (Math.abs(dx) < minDistX && Math.abs(dy) < minDistY) {
            hasCollision = true;

            // Calculate adjustment needed
            const overlapX = minDistX - Math.abs(dx);
            const overlapY = minDistY - Math.abs(dy);

            // Move nodes apart based on smallest overlap
            if (overlapX < overlapY) {
              // Separate horizontally
              const adjustX = (overlapX / 2) * Math.sign(dx);
              pos1.x -= adjustX;
              pos2.x += adjustX;
            } else {
              // Separate vertically
              const adjustY = (overlapY / 2) * Math.sign(dy);
              pos1.y -= adjustY;
              pos2.y += adjustY;
            }

            // Update positions
            nodePositions.set(id1, pos1);
            nodePositions.set(id2, pos2);

            // Update the node data as well
            node1.x = pos1.x;
            node1.y = pos1.y;
            node2.x = pos2.x;
            node2.y = pos2.y;
          }
        }
      }

      // If no collisions found, we're done
      if (!hasCollision) break;
    }
  }

  /**
   * Render both ancestors (above) and descendants (below) with selected person in the middle
   */
  private renderBothView(treeData: any): void {
    const nodePositions = new Map<string, { x: number, y: number }>();
    const spouseOffset = 180;
    const levelHeight = 150; // Vertical distance between generations
    const nodeWidth = 180; // Width of each node including some padding
    const centerX = this.width / 2;
    const centerY = this.height / 2;

    // Calculate the width needed for a node including its spouses
    const getNodeWidthWithSpouses = (node: any): number => {
      const numSpouses = node.spouses?.length || 0;
      return nodeWidth + (numSpouses * spouseOffset);
    };

    // Calculate total width needed for a subtree
    const calculateSubtreeWidth = (node: any): number => {
      if (!node.children || node.children.length === 0) {
        return getNodeWidthWithSpouses(node);
      }

      let childrenWidth = 0;
      node.children.forEach((child: any) => {
        childrenWidth += calculateSubtreeWidth(child);
      });

      return Math.max(getNodeWidthWithSpouses(node), childrenWidth);
    };

    // Position the root (selected person) in the center
    const rootIndividual = treeData.individual;
    nodePositions.set(rootIndividual.id, { x: centerX, y: centerY });

    // Build ancestors tree going UP from center
    const positionAncestors = (ancestors: any[], parentX: number, parentY: number, level: number) => {
      if (!ancestors || ancestors.length === 0) return;

      // Calculate total width needed for all ancestors at this level
      let totalWidth = 0;
      ancestors.forEach((ancestor: any) => {
        totalWidth += getNodeWidthWithSpouses(ancestor);
      });

      let currentX = parentX - totalWidth / 2 + nodeWidth / 2;

      ancestors.forEach((ancestor: any) => {
        const nodeWidthNeeded = getNodeWidthWithSpouses(ancestor);
        const x = currentX + (nodeWidthNeeded - nodeWidth) / 2; // Center the node within its allocated space
        const y = parentY - levelHeight; // Go UP

        nodePositions.set(ancestor.individual.id, { x, y });
        currentX += nodeWidthNeeded;

        // Recursively position their ancestors (parents)
        if (ancestor.children && ancestor.children.length > 0) {
          positionAncestors(ancestor.children, x, y, level + 1);
        }
      });
    };

    // Build descendants tree going DOWN from center - now accounts for spouse widths
    const positionDescendants = (node: any, parentX: number, parentY: number, level: number) => {
      if (!node.children || node.children.length === 0) return;

      const children = node.children;

      // Calculate total width needed for all children and their subtrees
      let totalWidth = 0;
      children.forEach((child: any) => {
        totalWidth += calculateSubtreeWidth(child);
      });

      // Start positioning from the left
      let currentX = parentX - totalWidth / 2;

      children.forEach((child: any) => {
        const subtreeWidth = calculateSubtreeWidth(child);
        const childNodeWidth = getNodeWidthWithSpouses(child);

        // Position child in the center of its subtree allocation
        const x = currentX + subtreeWidth / 2 - (childNodeWidth - nodeWidth) / 2;
        const y = parentY + levelHeight; // Go DOWN

        nodePositions.set(child.individual.id, { x, y });

        // Move to next position
        currentX += subtreeWidth;

        // Recursively position their descendants
        positionDescendants(child, x, y, level + 1);
      });
    };

    // Position ancestors above
    if (treeData.ancestors && treeData.ancestors.length > 0) {
      positionAncestors(treeData.ancestors, centerX, centerY, 1);
    }

    // Position descendants below
    positionDescendants(treeData, centerX, centerY, 1);

    // Position siblings at the same level as root (to the right of root and their spouses)
    const positionSiblings = (siblings: any[], rootX: number, rootY: number) => {
      if (!siblings || siblings.length === 0) return;

      // Calculate total width needed for root including their spouses
      const rootWidth = getNodeWidthWithSpouses(treeData);

      // Position siblings to the right of root AND root's spouses
      // Add extra spacing to avoid overlap with root's spouses
      let currentX = rootX + rootWidth + 80; // Start after root + all spouses with spacing

      siblings.forEach((sibling: any) => {
        const subtreeWidth = calculateSubtreeWidth(sibling);

        // Position sibling
        const x = currentX;
        const y = rootY; // Same level as root

        nodePositions.set(sibling.individual.id, { x, y });

        // Move to next position (include sibling's width + their spouses)
        currentX += subtreeWidth + 50;

        // Position sibling's descendants
        positionDescendants(sibling, x, y, 1);
      });
    };

    // Position siblings
    if (treeData.siblings && treeData.siblings.length > 0) {
      positionSiblings(treeData.siblings, centerX, centerY);
    }

    // Position spouses for all nodes
    const allNodes: any[] = [];
    const collectNodes = (node: any) => {
      allNodes.push(node);
      if (node.children) {
        node.children.forEach((child: any) => collectNodes(child));
      }
    };
    collectNodes(treeData);

    // Also collect ancestor nodes
    const collectAncestorNodes = (ancestors: any[]) => {
      if (!ancestors) return;
      ancestors.forEach((ancestor: any) => {
        allNodes.push(ancestor);
        if (ancestor.children) {
          collectAncestorNodes(ancestor.children);
        }
      });
    };
    collectAncestorNodes(treeData.ancestors);

    // Also collect sibling nodes and their descendants
    const collectSiblingNodes = (siblings: any[]) => {
      if (!siblings) return;
      siblings.forEach((sibling: any) => {
        collectNodes(sibling);
      });
    };
    collectSiblingNodes(treeData.siblings);

    // Add spouse positions
    allNodes.forEach((node: any) => {
      if (node.spouses && node.spouses.length > 0) {
        const nodePos = nodePositions.get(node.individual.id);
        if (nodePos) {
          node.spouses.forEach((spouse: Individual, index: number) => {
            nodePositions.set(spouse.id, {
              x: nodePos.x + spouseOffset * (index + 1),
              y: nodePos.y
            });
          });
        }
      }
    });

    // Now render all nodes and links
    this.renderBothViewNodes(treeData, nodePositions, spouseOffset);
  }

  /**
   * Render nodes and links for "both" view
   */
  private renderBothViewNodes(treeData: any, nodePositions: Map<string, { x: number, y: number }>, spouseOffset: number): void {
    const self = this;

    // Create drag behavior
    const dragBehavior = d3.drag()
      .subject((event: any, d: any) => {
        const individualId = d.individual ? d.individual.id : d.id;
        const pos = nodePositions.get(individualId);
        if (pos) {
          return { x: pos.x, y: pos.y };
        }
        return { x: event.x, y: event.y };
      })
      .on('drag', (event: any, d: any) => {
        const individualId = d.individual ? d.individual.id : d.id;
        const nodeGroup = d3.select(event.sourceEvent.target).node().closest('.node, .spouse-node');
        if (!nodeGroup) return;

        d3.select(nodeGroup).attr('transform', `translate(${event.x},${event.y})`);
        nodePositions.set(individualId, { x: event.x, y: event.y });

        // Update links
        this.g.selectAll('.link').attr('d', function (this: any) {
          const sourceId = d3.select(this).attr('data-source');
          const targetId = d3.select(this).attr('data-target');
          const sourcePos = nodePositions.get(sourceId);
          const targetPos = nodePositions.get(targetId);
          if (sourcePos && targetPos) {
            return `M${sourcePos.x},${sourcePos.y} C${sourcePos.x},${(sourcePos.y + targetPos.y) / 2} ${targetPos.x},${(sourcePos.y + targetPos.y) / 2} ${targetPos.x},${targetPos.y}`;
          }
          return '';
        });

        // Update spouse links
        this.g.selectAll('.spouse-link')
          .attr('x1', function (this: any) {
            const spouse1Id = d3.select(this).attr('data-spouse1');
            const pos = nodePositions.get(spouse1Id);
            return pos ? pos.x : 0;
          })
          .attr('y1', function (this: any) {
            const spouse1Id = d3.select(this).attr('data-spouse1');
            const pos = nodePositions.get(spouse1Id);
            return pos ? pos.y : 0;
          })
          .attr('x2', function (this: any) {
            const spouse2Id = d3.select(this).attr('data-spouse2');
            const pos = nodePositions.get(spouse2Id);
            return pos ? pos.x : 0;
          })
          .attr('y2', function (this: any) {
            const spouse2Id = d3.select(this).attr('data-spouse2');
            const pos = nodePositions.get(spouse2Id);
            return pos ? pos.y : 0;
          });
      });

    // Helper to draw a node
    const drawNode = (individual: Individual, isRoot: boolean = false) => {
      const pos = nodePositions.get(individual.id);
      if (!pos) return;

      const node = this.g.append('g')
        .datum({ individual })
        .attr('class', 'node')
        .attr('transform', `translate(${pos.x},${pos.y})`)
        .on('click', (event: any) => {
          if (event.defaultPrevented) return;
          this.onNodeClick(individual);
        })
        .style('cursor', 'move')
        .call(dragBehavior);

      const avatarUrl = this.getAvatarUrl(individual);
      const frameWidth = 160;
      const frameHeight = 120;
      const isDeceased = !!individual.deathDate;

      // Background frame - highlight root node
      node.append('rect')
        .attr('x', -frameWidth / 2)
        .attr('y', -frameHeight / 2)
        .attr('width', frameWidth)
        .attr('height', frameHeight)
        .attr('rx', 8)
        .attr('ry', 8)
        .attr('fill', isDeceased ? '#e0e0e0' : '#ffffff')
        .attr('stroke', () => {
          if (isRoot) return '#FFD700'; // Gold border for root
          if (individual.gender === 'MALE') return '#4A90E2';
          if (individual.gender === 'FEMALE') return '#E294A9';
          return '#9B9B9B';
        })
        .attr('stroke-width', isRoot ? 4 : 2)
        .attr('filter', 'drop-shadow(0px 2px 4px rgba(0,0,0,0.1))');

      if (avatarUrl) {
        node.append('image')
          .attr('xlink:href', avatarUrl)
          .attr('x', -35)
          .attr('y', -52)
          .attr('width', 70)
          .attr('height', 70)
          .attr('preserveAspectRatio', 'xMidYMid slice');

        node.append('rect')
          .attr('x', -35)
          .attr('y', -52)
          .attr('width', 70)
          .attr('height', 70)
          .attr('rx', 4)
          .attr('ry', 4)
          .attr('fill', 'none')
          .attr('stroke', individual.gender === 'MALE' ? '#4A90E2' : individual.gender === 'FEMALE' ? '#E294A9' : '#9B9B9B')
          .attr('stroke-width', 2);
      } else {
        node.append('circle')
          .attr('cy', -20)
          .attr('r', 30)
          .attr('fill', individual.gender === 'MALE' ? '#4A90E2' : individual.gender === 'FEMALE' ? '#E294A9' : '#9B9B9B')
          .attr('stroke', '#fff')
          .attr('stroke-width', 2);

        node.append('text')
          .attr('y', -15)
          .attr('text-anchor', 'middle')
          .attr('fill', 'white')
          .attr('font-size', '16px')
          .attr('font-weight', 'bold')
          .text(this.getInitials(individual));
      }

      node.append('text')
        .attr('y', 30)
        .attr('text-anchor', 'middle')
        .attr('font-size', '11px')
        .attr('font-weight', 'bold')
        .attr('fill', '#333')
        .text(this.truncateText(individual.fullName, 18));

      node.append('text')
        .attr('y', 45)
        .attr('text-anchor', 'middle')
        .attr('font-size', '9px')
        .attr('fill', '#666')
        .text(`${this.getBornLabel()}: ${this.formatDate(individual.birthDate)}`);

      if (individual.deathDate) {
        node.append('text')
          .attr('y', 57)
          .attr('text-anchor', 'middle')
          .attr('font-size', '9px')
          .attr('fill', '#666')
          .text(`${this.getDiedLabel()}: ${this.formatDate(individual.deathDate)}`);
      }

      // View button
      const buttonGroup = node.append('g')
        .attr('class', 'set-root-button')
        .attr('transform', 'translate(0, -60)')
        .style('cursor', 'pointer')
        .style('pointer-events', 'all')
        .on('mousedown', (event: any) => { event.preventDefault(); event.stopPropagation(); })
        .on('click', (event: any) => { event.preventDefault(); event.stopPropagation(); })
        .on('mouseup', (event: any) => {
          event.preventDefault();
          event.stopPropagation();
          this.setNodeAsRoot(individual);
        });

      buttonGroup.append('rect')
        .attr('x', -20)
        .attr('y', -8)
        .attr('width', 40)
        .attr('height', 16)
        .attr('rx', 8)
        .attr('fill', '#3f51b5')
        .attr('opacity', 0.9);

      buttonGroup.append('text')
        .attr('text-anchor', 'middle')
        .attr('font-size', '9px')
        .attr('font-weight', 'bold')
        .attr('fill', 'white')
        .style('pointer-events', 'none')
        .text('View');
    };

    // Helper to draw link between parent and child
    const drawLink = (parentId: string, childId: string) => {
      const parentPos = nodePositions.get(parentId);
      const childPos = nodePositions.get(childId);
      if (!parentPos || !childPos) return;

      this.g.append('path')
        .attr('class', 'link')
        .attr('data-source', parentId)
        .attr('data-target', childId)
        .attr('d', `M${parentPos.x},${parentPos.y} C${parentPos.x},${(parentPos.y + childPos.y) / 2} ${childPos.x},${(parentPos.y + childPos.y) / 2} ${childPos.x},${childPos.y}`)
        .attr('fill', 'none')
        .attr('stroke', '#ccc')
        .attr('stroke-width', 2);
    };

    // Helper to draw spouse link
    const drawSpouseLink = (person1Id: string, person2Id: string) => {
      const pos1 = nodePositions.get(person1Id);
      const pos2 = nodePositions.get(person2Id);
      if (!pos1 || !pos2) return;

      this.g.append('line')
        .attr('class', 'spouse-link')
        .attr('data-spouse1', person1Id)
        .attr('data-spouse2', person2Id)
        .attr('x1', pos1.x)
        .attr('y1', pos1.y)
        .attr('x2', pos2.x)
        .attr('y2', pos2.y)
        .attr('stroke', '#ff69b4')
        .attr('stroke-width', 2)
        .attr('stroke-dasharray', '5,5');
    };

    // Draw all links first (so they appear behind nodes)
    // Draw ancestor links (going up)
    const drawAncestorLinks = (ancestors: any[], childId: string) => {
      if (!ancestors) return;
      ancestors.forEach((ancestor: any) => {
        drawLink(ancestor.individual.id, childId); // Link from ancestor to child (reversed for upward)
        if (ancestor.children && ancestor.children.length > 0) {
          drawAncestorLinks(ancestor.children, ancestor.individual.id);
        }
      });
    };

    // Draw descendant links (going down)
    const drawDescendantLinks = (node: any) => {
      if (!node.children) return;
      node.children.forEach((child: any) => {
        drawLink(node.individual.id, child.individual.id);
        drawDescendantLinks(child);
      });
    };

    // Draw links
    if (treeData.ancestors) {
      drawAncestorLinks(treeData.ancestors, treeData.individual.id);
    }
    drawDescendantLinks(treeData);

    // Draw sibling links (siblings share parent, so draw from parent to sibling)
    const drawSiblingLinks = (siblings: any[]) => {
      if (!siblings || siblings.length === 0) return;

      // Find first parent from ancestors (if available)
      if (treeData.ancestors && treeData.ancestors.length > 0) {
        const parentId = treeData.ancestors[0].individual.id;
        siblings.forEach((sibling: any) => {
          drawLink(parentId, sibling.individual.id);
          // Also draw links for sibling's descendants
          drawDescendantLinks(sibling);
        });
      }
    };
    drawSiblingLinks(treeData.siblings);

    // Draw spouse links and spouse nodes
    const drawSpousesForNode = (node: any) => {
      if (node.spouses && node.spouses.length > 0) {
        node.spouses.forEach((spouse: Individual) => {
          drawSpouseLink(node.individual.id, spouse.id);
          drawNode(spouse);
        });
      }
    };

    // Draw all nodes
    // Draw root node (selected person) with highlight
    drawNode(treeData.individual, true);
    drawSpousesForNode(treeData);

    // Draw ancestor nodes
    const drawAncestorNodes = (ancestors: any[]) => {
      if (!ancestors) return;
      ancestors.forEach((ancestor: any) => {
        drawNode(ancestor.individual);
        drawSpousesForNode(ancestor);
        if (ancestor.children && ancestor.children.length > 0) {
          drawAncestorNodes(ancestor.children);
        }
      });
    };

    // Draw descendant nodes
    const drawDescendantNodes = (node: any) => {
      if (!node.children) return;
      node.children.forEach((child: any) => {
        drawNode(child.individual);
        drawSpousesForNode(child);
        drawDescendantNodes(child);
      });
    };

    if (treeData.ancestors) {
      drawAncestorNodes(treeData.ancestors);
    }
    drawDescendantNodes(treeData);

    // Draw sibling nodes and their descendants
    const drawSiblingNodes = (siblings: any[]) => {
      if (!siblings) return;
      siblings.forEach((sibling: any) => {
        drawNode(sibling.individual);
        drawSpousesForNode(sibling);
        drawDescendantNodes(sibling);
      });
    };
    if (treeData.siblings) {
      drawSiblingNodes(treeData.siblings);
    }

    console.log('Both view rendered successfully');
  }

  /**
   * Render ancestors view with selected person at bottom center and ancestors above
   */
  private renderAncestorsView(treeData: any): void {
    const nodePositions = new Map<string, { x: number, y: number }>();
    const spouseOffset = 180;
    const levelHeight = 150; // Vertical distance between generations
    const nodeWidth = 180;
    const centerX = this.width / 2;
    const bottomY = this.height * 2 / 3; // Position selected person at 2/3 of view height

    // Calculate the width needed for a node including its spouses
    const getNodeWidthWithSpouses = (node: any): number => {
      const numSpouses = node.spouses?.length || 0;
      return nodeWidth + (numSpouses * spouseOffset);
    };

    // Position the root (selected person) at bottom center
    const rootIndividual = treeData.individual;
    nodePositions.set(rootIndividual.id, { x: centerX, y: bottomY });

    // Build ancestors tree going UP from bottom
    const positionAncestors = (ancestors: any[], parentX: number, parentY: number, level: number) => {
      if (!ancestors || ancestors.length === 0) return;

      // In ancestor view, "children" are actually parents (going up)
      // Calculate total width needed for all ancestors at this level
      let totalWidth = 0;
      ancestors.forEach((ancestor: any) => {
        totalWidth += getNodeWidthWithSpouses(ancestor);
      });

      let currentX = parentX - totalWidth / 2 + nodeWidth / 2;

      ancestors.forEach((ancestor: any) => {
        const nodeWidthNeeded = getNodeWidthWithSpouses(ancestor);
        const x = currentX + (nodeWidthNeeded - nodeWidth) / 2;
        const y = parentY - levelHeight; // Go UP

        nodePositions.set(ancestor.individual.id, { x, y });
        currentX += nodeWidthNeeded;

        // Recursively position their ancestors (parents become children in tree structure)
        if (ancestor.children && ancestor.children.length > 0) {
          positionAncestors(ancestor.children, x, y, level + 1);
        }
      });
    };

    // Position ancestors above - in ancestor mode, treeData.children are the parents
    if (treeData.children && treeData.children.length > 0) {
      positionAncestors(treeData.children, centerX, bottomY, 1);
    }

    // Collect all nodes for spouse positioning
    const allNodes: any[] = [treeData];
    const collectAncestorNodes = (nodes: any[]) => {
      if (!nodes) return;
      nodes.forEach((node: any) => {
        allNodes.push(node);
        if (node.children) {
          collectAncestorNodes(node.children);
        }
      });
    };
    collectAncestorNodes(treeData.children);

    // Add spouse positions
    allNodes.forEach((node: any) => {
      if (node.spouses && node.spouses.length > 0) {
        const nodePos = nodePositions.get(node.individual.id);
        if (nodePos) {
          node.spouses.forEach((spouse: Individual, index: number) => {
            nodePositions.set(spouse.id, {
              x: nodePos.x + spouseOffset * (index + 1),
              y: nodePos.y
            });
          });
        }
      }
    });

    // Render nodes and links
    this.renderAncestorsViewNodes(treeData, nodePositions, spouseOffset);
  }

  /**
   * Render nodes and links for ancestors view
   */
  private renderAncestorsViewNodes(treeData: any, nodePositions: Map<string, { x: number, y: number }>, spouseOffset: number): void {
    // Create drag behavior
    const dragBehavior = d3.drag()
      .subject((event: any, d: any) => {
        const individualId = d.individual ? d.individual.id : d.id;
        const pos = nodePositions.get(individualId);
        if (pos) {
          return { x: pos.x, y: pos.y };
        }
        return { x: event.x, y: event.y };
      })
      .on('drag', (event: any, d: any) => {
        const individualId = d.individual ? d.individual.id : d.id;
        const nodeGroup = d3.select(event.sourceEvent.target).node().closest('.node, .spouse-node');
        if (!nodeGroup) return;

        d3.select(nodeGroup).attr('transform', `translate(${event.x},${event.y})`);
        nodePositions.set(individualId, { x: event.x, y: event.y });

        // Update links
        this.g.selectAll('.link').attr('d', function (this: any) {
          const sourceId = d3.select(this).attr('data-source');
          const targetId = d3.select(this).attr('data-target');
          const sourcePos = nodePositions.get(sourceId);
          const targetPos = nodePositions.get(targetId);
          if (sourcePos && targetPos) {
            return `M${sourcePos.x},${sourcePos.y} C${sourcePos.x},${(sourcePos.y + targetPos.y) / 2} ${targetPos.x},${(sourcePos.y + targetPos.y) / 2} ${targetPos.x},${targetPos.y}`;
          }
          return '';
        });

        // Update spouse links
        this.g.selectAll('.spouse-link')
          .attr('x1', function (this: any) {
            const spouse1Id = d3.select(this).attr('data-spouse1');
            const pos = nodePositions.get(spouse1Id);
            return pos ? pos.x : 0;
          })
          .attr('y1', function (this: any) {
            const spouse1Id = d3.select(this).attr('data-spouse1');
            const pos = nodePositions.get(spouse1Id);
            return pos ? pos.y : 0;
          })
          .attr('x2', function (this: any) {
            const spouse2Id = d3.select(this).attr('data-spouse2');
            const pos = nodePositions.get(spouse2Id);
            return pos ? pos.x : 0;
          })
          .attr('y2', function (this: any) {
            const spouse2Id = d3.select(this).attr('data-spouse2');
            const pos = nodePositions.get(spouse2Id);
            return pos ? pos.y : 0;
          });
      });

    // Helper to draw a node
    const drawNode = (individual: Individual, isRoot: boolean = false) => {
      const pos = nodePositions.get(individual.id);
      if (!pos) return;

      const node = this.g.append('g')
        .datum({ individual })
        .attr('class', 'node')
        .attr('transform', `translate(${pos.x},${pos.y})`)
        .on('click', (event: any) => {
          if (event.defaultPrevented) return;
          this.onNodeClick(individual);
        })
        .style('cursor', 'move')
        .call(dragBehavior);

      const avatarUrl = this.getAvatarUrl(individual);
      const frameWidth = 160;
      const frameHeight = 120;
      const isDeceased = !!individual.deathDate;

      // Background frame - highlight root node with gold border
      node.append('rect')
        .attr('x', -frameWidth / 2)
        .attr('y', -frameHeight / 2)
        .attr('width', frameWidth)
        .attr('height', frameHeight)
        .attr('rx', 8)
        .attr('ry', 8)
        .attr('fill', isDeceased ? '#e0e0e0' : '#ffffff')
        .attr('stroke', () => {
          if (isRoot) return '#FFD700'; // Gold border for selected person
          if (individual.gender === 'MALE') return '#4A90E2';
          if (individual.gender === 'FEMALE') return '#E294A9';
          return '#9B9B9B';
        })
        .attr('stroke-width', isRoot ? 4 : 2)
        .attr('filter', 'drop-shadow(0px 2px 4px rgba(0,0,0,0.1))');

      if (avatarUrl) {
        node.append('image')
          .attr('xlink:href', avatarUrl)
          .attr('x', -35)
          .attr('y', -52)
          .attr('width', 70)
          .attr('height', 70)
          .attr('preserveAspectRatio', 'xMidYMid slice');

        node.append('rect')
          .attr('x', -35)
          .attr('y', -52)
          .attr('width', 70)
          .attr('height', 70)
          .attr('rx', 4)
          .attr('ry', 4)
          .attr('fill', 'none')
          .attr('stroke', individual.gender === 'MALE' ? '#4A90E2' : individual.gender === 'FEMALE' ? '#E294A9' : '#9B9B9B')
          .attr('stroke-width', 2);
      } else {
        node.append('circle')
          .attr('cy', -20)
          .attr('r', 30)
          .attr('fill', individual.gender === 'MALE' ? '#4A90E2' : individual.gender === 'FEMALE' ? '#E294A9' : '#9B9B9B')
          .attr('stroke', '#fff')
          .attr('stroke-width', 2);

        node.append('text')
          .attr('y', -15)
          .attr('text-anchor', 'middle')
          .attr('fill', 'white')
          .attr('font-size', '16px')
          .attr('font-weight', 'bold')
          .text(this.getInitials(individual));
      }

      node.append('text')
        .attr('y', 30)
        .attr('text-anchor', 'middle')
        .attr('font-size', '11px')
        .attr('font-weight', 'bold')
        .attr('fill', '#333')
        .text(this.truncateText(individual.fullName, 18));

      node.append('text')
        .attr('y', 45)
        .attr('text-anchor', 'middle')
        .attr('font-size', '9px')
        .attr('fill', '#666')
        .text(`${this.getBornLabel()}: ${this.formatDate(individual.birthDate)}`);

      if (individual.deathDate) {
        node.append('text')
          .attr('y', 57)
          .attr('text-anchor', 'middle')
          .attr('font-size', '9px')
          .attr('fill', '#666')
          .text(`${this.getDiedLabel()}: ${this.formatDate(individual.deathDate)}`);
      }

      // View button
      const buttonGroup = node.append('g')
        .attr('class', 'set-root-button')
        .attr('transform', 'translate(0, -60)')
        .style('cursor', 'pointer')
        .style('pointer-events', 'all')
        .on('mousedown', (event: any) => { event.preventDefault(); event.stopPropagation(); })
        .on('click', (event: any) => { event.preventDefault(); event.stopPropagation(); })
        .on('mouseup', (event: any) => {
          event.preventDefault();
          event.stopPropagation();
          this.setNodeAsRoot(individual);
        });

      buttonGroup.append('rect')
        .attr('x', -20)
        .attr('y', -8)
        .attr('width', 40)
        .attr('height', 16)
        .attr('rx', 8)
        .attr('fill', '#3f51b5')
        .attr('opacity', 0.9);

      buttonGroup.append('text')
        .attr('text-anchor', 'middle')
        .attr('font-size', '9px')
        .attr('font-weight', 'bold')
        .attr('fill', 'white')
        .style('pointer-events', 'none')
        .text('View');
    };

    // Helper to draw link from child to parent (child at bottom, parent at top)
    const drawLink = (childId: string, parentId: string) => {
      const childPos = nodePositions.get(childId);
      const parentPos = nodePositions.get(parentId);
      if (!childPos || !parentPos) return;

      this.g.append('path')
        .attr('class', 'link')
        .attr('data-source', childId)
        .attr('data-target', parentId)
        .attr('d', `M${childPos.x},${childPos.y} C${childPos.x},${(childPos.y + parentPos.y) / 2} ${parentPos.x},${(childPos.y + parentPos.y) / 2} ${parentPos.x},${parentPos.y}`)
        .attr('fill', 'none')
        .attr('stroke', '#ccc')
        .attr('stroke-width', 2);
    };

    // Helper to draw spouse link
    const drawSpouseLink = (person1Id: string, person2Id: string) => {
      const pos1 = nodePositions.get(person1Id);
      const pos2 = nodePositions.get(person2Id);
      if (!pos1 || !pos2) return;

      this.g.append('line')
        .attr('class', 'spouse-link')
        .attr('data-spouse1', person1Id)
        .attr('data-spouse2', person2Id)
        .attr('x1', pos1.x)
        .attr('y1', pos1.y)
        .attr('x2', pos2.x)
        .attr('y2', pos2.y)
        .attr('stroke', '#ff69b4')
        .attr('stroke-width', 2)
        .attr('stroke-dasharray', '5,5');
    };

    // Draw links first (so they appear behind nodes)
    // Draw links from selected person to parents, and recursively up
    const drawAncestorLinks = (childId: string, ancestors: any[]) => {
      if (!ancestors) return;
      ancestors.forEach((ancestor: any) => {
        drawLink(childId, ancestor.individual.id);
        if (ancestor.children && ancestor.children.length > 0) {
          drawAncestorLinks(ancestor.individual.id, ancestor.children);
        }
      });
    };

    // Draw links from root to its parents (which are in treeData.children for ancestor view)
    if (treeData.children && treeData.children.length > 0) {
      drawAncestorLinks(treeData.individual.id, treeData.children);
    }

    // Draw spouse links and nodes
    const drawSpousesForNode = (node: any) => {
      if (node.spouses && node.spouses.length > 0) {
        node.spouses.forEach((spouse: Individual) => {
          drawSpouseLink(node.individual.id, spouse.id);
          drawNode(spouse);
        });
      }
    };

    // Draw root node (selected person) with highlight at bottom
    drawNode(treeData.individual, true);
    drawSpousesForNode(treeData);

    // Draw ancestor nodes going up
    const drawAncestorNodes = (ancestors: any[]) => {
      if (!ancestors) return;
      ancestors.forEach((ancestor: any) => {
        drawNode(ancestor.individual);
        drawSpousesForNode(ancestor);
        if (ancestor.children && ancestor.children.length > 0) {
          drawAncestorNodes(ancestor.children);
        }
      });
    };

    if (treeData.children && treeData.children.length > 0) {
      drawAncestorNodes(treeData.children);
    }

    console.log('Ancestors view rendered successfully');
  }

  /**
   * Open GEDCOM import dialog
   */
  openGedcomImport(): void {
    const treeName = this.currentTree?.name || 'Tree';

    const dialogRef = this.dialog.open(GedcomImportComponent, {
      width: '600px',
      data: {
        treeId: this.treeId,
        treeName: treeName
      } as GedcomImportData
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result?.success) {
        this.snackBar.open(
          this.translate.instant('gedcom.importSuccess'),
          this.translate.instant('common.close'),
          { duration: 3000 }
        );
        // Refresh the tree visualization
        this.loadTreeData();
      }
    });
  }

  /**
   * Export tree to GEDCOM format
   */
  exportGedcom(): void {
    const treeName = this.currentTree?.name || 'family-tree';

    this.gedcomService.exportGedcom(this.treeId, treeName).subscribe({
      next: () => {
        this.snackBar.open(
          this.translate.instant('gedcom.exportSuccess'),
          this.translate.instant('common.close'),
          { duration: 3000 }
        );
      },
      error: (error) => {
        console.error('GEDCOM export failed:', error);
        this.snackBar.open(
          this.translate.instant('gedcom.exportError'),
          this.translate.instant('common.close'),
          { duration: 5000 }
        );
      }
    });
  }
}
