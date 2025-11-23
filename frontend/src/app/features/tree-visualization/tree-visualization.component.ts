import { Component, OnInit, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import * as d3 from 'd3';
import { IndividualService } from '../individual/services/individual.service';
import { RelationshipService } from '../relationship/services/relationship.service';
import { Individual } from '../individual/models/individual.model';
import { Relationship } from '../relationship/models/relationship.model';

interface TreeNode {
  individual: Individual;
  name: string;
  children?: TreeNode[];
  spouses?: Individual[];
}

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
    MatToolbarModule
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
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('treeId') || '';
      if (this.treeId) {
        this.loadTreeData();
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
            this.loading = false;
            // Wait for Angular to render the container
            setTimeout(() => this.initializeVisualization(), 0);
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

  findRootIndividual(): Individual | null {
    // Find all individuals with no parents (potential roots)
    const potentialRoots: Individual[] = [];

    for (const individual of this.individuals) {
      const hasParents = this.relationships.some(rel =>
        rel.individual2.id === individual.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
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
        (rel.type === 'PARENT_CHILD' || rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
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

  buildTreeStructure(root: Individual): any {
    const visited = new Set<string>();

    const buildNode = (individual: Individual): any => {
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

      // Find children (from this individual or their spouses)
      const childRelationships = this.relationships.filter(rel =>
        rel.individual1.id === individual.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      for (const rel of childRelationships) {
        const child = this.individuals.find(ind => ind.id === rel.individual2.id);
        if (child) {
          const childNode = buildNode(child);
          if (childNode) {
            node.children.push(childNode);
          }
        }
      }

      return node;
    };

    const tree = buildNode(root);

    // Log tree structure for debugging
    console.log('Tree structure built:', tree);
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

    // Create tree layout
    const treeLayout = d3.tree<any>()
      .size([this.width - 200, this.height - 200]);

    // Create hierarchy
    const root = d3.hierarchy(treeData);
    const treeNodes = treeLayout(root);

    // Center the tree
    const translateX = 100;
    const translateY = 50;
    const spouseOffset = 80; // Horizontal distance between spouses

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
          const spouseX = d.x + translateX + spouseOffset * (index + 1);
          const spouseY = d.y + translateY;

          // Draw horizontal line connecting spouses
          this.g.append('line')
            .attr('class', 'spouse-link')
            .attr('x1', d.x + translateX)
            .attr('y1', spouseY)
            .attr('x2', spouseX)
            .attr('y2', spouseY)
            .attr('stroke', '#ff69b4')
            .attr('stroke-width', 2)
            .attr('stroke-dasharray', '5,5');

          // Draw spouse node
          const spouseNode = this.g.append('g')
            .attr('class', 'spouse-node')
            .attr('transform', `translate(${spouseX},${spouseY})`)
            .on('click', () => {
              this.onNodeClick(spouse);
            })
            .style('cursor', 'pointer');

          spouseNode.append('circle')
            .attr('r', 30)
            .attr('fill', () => {
              if (spouse.gender === 'MALE') return '#4A90E2';
              if (spouse.gender === 'FEMALE') return '#E294A9';
              return '#9B9B9B';
            })
            .attr('stroke', '#fff')
            .attr('stroke-width', 3);

          spouseNode.append('text')
            .attr('dy', 5)
            .attr('text-anchor', 'middle')
            .attr('fill', 'white')
            .attr('font-size', '14px')
            .attr('font-weight', 'bold')
            .text(this.getInitials(spouse));

          spouseNode.append('text')
            .attr('dy', 50)
            .attr('text-anchor', 'middle')
            .attr('font-size', '12px')
            .attr('fill', '#333')
            .text(spouse.fullName);

          spouseNode.append('text')
            .attr('dy', 65)
            .attr('text-anchor', 'middle')
            .attr('font-size', '10px')
            .attr('fill', '#666')
            .text(this.getLifeYears(spouse));
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
      .on('click', (event: any, d: any) => {
        this.onNodeClick(d.data.individual);
      })
      .style('cursor', 'pointer');

    // Add circles for nodes
    nodes.append('circle')
      .attr('r', 30)
      .attr('fill', (d: any) => {
        const gender = d.data.individual.gender;
        if (gender === 'MALE') return '#4A90E2';
        if (gender === 'FEMALE') return '#E294A9';
        return '#9B9B9B';
      })
      .attr('stroke', '#fff')
      .attr('stroke-width', 3);

    // Add initials text
    nodes.append('text')
      .attr('dy', 5)
      .attr('text-anchor', 'middle')
      .attr('fill', 'white')
      .attr('font-size', '14px')
      .attr('font-weight', 'bold')
      .text((d: any) => this.getInitials(d.data.individual));

    // Add names below nodes
    nodes.append('text')
      .attr('dy', 50)
      .attr('text-anchor', 'middle')
      .attr('font-size', '12px')
      .attr('fill', '#333')
      .text((d: any) => d.data.individual.fullName);

    // Add life years below names
    nodes.append('text')
      .attr('dy', 65)
      .attr('text-anchor', 'middle')
      .attr('font-size', '10px')
      .attr('fill', '#666')
      .text((d: any) => this.getLifeYears(d.data.individual));

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
    this.router.navigate(['/trees', this.treeId, 'individuals']);
  }
}
