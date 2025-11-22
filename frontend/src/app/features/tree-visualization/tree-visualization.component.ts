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
  @ViewChild('treeContainer', { static: true }) treeContainer!: ElementRef;

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
            this.initializeVisualization();
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
    if (this.individuals.length === 0) {
      this.snackBar.open('No individuals in this tree', 'Close', { duration: 3000 });
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
      this.snackBar.open('Could not determine tree root', 'Close', { duration: 3000 });
      return;
    }

    const treeData = this.buildTreeStructure(rootIndividual);
    this.renderTree(treeData);
  }

  findRootIndividual(): Individual | null {
    // Find an individual with no parents (root of tree)
    for (const individual of this.individuals) {
      const hasParents = this.relationships.some(rel =>
        rel.individual2.id === individual.id &&
        (rel.type === 'PARENT_CHILD' || rel.type === 'ADOPTED_PARENT_CHILD' || rel.type === 'STEP_PARENT_CHILD')
      );

      if (!hasParents) {
        return individual;
      }
    }

    // Fallback: return first individual
    return this.individuals[0] || null;
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
        children: []
      };

      // Find children
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

    return buildNode(root);
  }

  renderTree(treeData: any): void {
    if (!treeData) return;

    // Create tree layout
    const treeLayout = d3.tree<any>()
      .size([this.width - 200, this.height - 200]);

    // Create hierarchy
    const root = d3.hierarchy(treeData);
    const treeNodes = treeLayout(root);

    // Center the tree
    const translateX = 100;
    const translateY = 50;

    // Draw links
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

    // Draw nodes
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
