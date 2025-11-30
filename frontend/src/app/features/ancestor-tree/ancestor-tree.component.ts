import { Component, OnInit, ElementRef, ViewChild, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormControl, FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatRadioModule } from '@angular/material/radio';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import * as d3 from 'd3';
import { Observable, startWith, map, debounceTime, distinctUntilChanged } from 'rxjs';
import { IndividualService } from '../individual/services/individual.service';
import { environment } from '../../../environments/environment';
import { Individual } from '../individual/models/individual.model';
import { AncestorTreeService, AncestorTreeResponse, AncestorNode } from './services/ancestor-tree.service';
import { UserTreeProfileService, UserTreeProfile } from '../user-profile/services/user-tree-profile.service';

type LayoutDirection = 'left' | 'right' | 'up' | 'down';

@Component({
  selector: 'app-ancestor-tree',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
    MatToolbarModule,
    MatSelectModule,
    MatFormFieldModule,
    MatTooltipModule,
    MatRadioModule,
    MatAutocompleteModule,
    MatInputModule,
    MatChipsModule
  ],
  templateUrl: './ancestor-tree.component.html',
  styleUrl: './ancestor-tree.component.scss'
})
export class AncestorTreeComponent implements OnInit, OnDestroy {
  @ViewChild('treeContainer', { static: false }) treeContainer!: ElementRef;

  treeId!: string;
  individuals: Individual[] = [];
  loading = true;
  loadingTree = false;

  // Form controls
  selectedIndividual: Individual | null = null;
  selectedGenerations = 3;
  layoutDirection: LayoutDirection = 'up';
  individualSearchControl = new FormControl<string | Individual>('');
  filteredIndividuals$!: Observable<Individual[]>;

  // Generation options
  generationOptions = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

  // Tree data
  ancestorTree: AncestorTreeResponse | null = null;

  // User profile
  userProfile: UserTreeProfile | null = null;

  private svg: any;
  private g: any;
  private zoom: any;
  private width = 1200;
  private height = 800;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private individualService: IndividualService,
    private ancestorTreeService: AncestorTreeService,
    private userTreeProfileService: UserTreeProfileService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.treeId = params.get('treeId') || '';
      if (this.treeId) {
        this.loadIndividuals();
      }
    });

    // Setup autocomplete filter
    this.filteredIndividuals$ = this.individualSearchControl.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged(),
      map(value => {
        const searchValue = typeof value === 'string' ? value : (value as any)?.fullName || '';
        return this._filterIndividuals(searchValue);
      })
    );
  }

  ngOnDestroy(): void {
    if (this.svg) {
      this.svg.remove();
    }
  }

  loadIndividuals(): void {
    this.loading = true;
    this.individualService.getIndividuals(this.treeId, 0, 1000).subscribe({
      next: (response) => {
        this.individuals = response.content.sort((a, b) => a.fullName.localeCompare(b.fullName));
        // Load user's profile mapping and set as default
        this.loadUserProfile();
      },
      error: (error) => {
        console.error('Error loading individuals:', error);
        this.snackBar.open('Failed to load individuals', 'Close', { duration: 3000 });
        this.loading = false;
      }
    });
  }

  loadUserProfile(): void {
    this.userTreeProfileService.getMyProfile(this.treeId).subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.loading = false;

        if (profile && profile.individual) {
          // Find the matching individual in our list
          const matchingIndividual = this.individuals.find(ind => ind.id === profile.individual.id);
          if (matchingIndividual) {
            this.selectedIndividual = matchingIndividual;
            this.individualSearchControl.setValue(matchingIndividual);
            // Auto-load the ancestor tree
            this.viewAncestorTree();
          }
        }
      },
      error: (error) => {
        console.error('Error loading user profile:', error);
        this.loading = false;
      }
    });
  }

  private _filterIndividuals(value: string): Individual[] {
    if (!value) return this.individuals;
    const filterValue = value.toLowerCase();
    return this.individuals.filter(ind =>
      ind.fullName.toLowerCase().includes(filterValue) ||
      (ind.givenName && ind.givenName.toLowerCase().includes(filterValue)) ||
      (ind.surname && ind.surname.toLowerCase().includes(filterValue))
    );
  }

  displayIndividual(individual: Individual): string {
    if (!individual) return '';
    const birthYear = individual.birthDate ? new Date(individual.birthDate).getFullYear() : '';
    return birthYear ? `${individual.fullName}, ${birthYear}` : individual.fullName;
  }

  onIndividualSelected(individual: Individual): void {
    this.selectedIndividual = individual;
  }

  viewAncestorTree(): void {
    if (!this.selectedIndividual) {
      this.snackBar.open('Please select a person first', 'Close', { duration: 3000 });
      return;
    }

    this.loadingTree = true;
    this.ancestorTreeService.getAncestorTree(
      this.treeId,
      this.selectedIndividual.id,
      this.selectedGenerations
    ).subscribe({
      next: (response) => {
        this.ancestorTree = response;
        this.loadingTree = false;
        setTimeout(() => this.renderTree(), 0);
      },
      error: (error) => {
        console.error('Error loading ancestor tree:', error);
        this.snackBar.open('Failed to load ancestor tree', 'Close', { duration: 3000 });
        this.loadingTree = false;
      }
    });
  }

  renderTree(): void {
    if (!this.ancestorTree || !this.ancestorTree.root) {
      return;
    }

    if (!this.treeContainer || !this.treeContainer.nativeElement) {
      setTimeout(() => this.renderTree(), 100);
      return;
    }

    // Clear existing SVG
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

    // Convert ancestor data to d3 hierarchy format
    const hierarchyData = this.convertToHierarchy(this.ancestorTree.root);
    const root = d3.hierarchy(hierarchyData);

    // Create tree layout based on direction
    const treeLayout = this.createTreeLayout();
    const treeNodes = treeLayout(root);

    // Calculate translation based on layout direction
    const { translateX, translateY } = this.calculateTranslation(treeNodes);

    // Draw links
    this.drawLinks(treeNodes, translateX, translateY);

    // Draw nodes
    this.drawNodes(treeNodes, translateX, translateY);

    // Initial zoom to fit
    this.fitToScreen();
  }

  private convertToHierarchy(node: AncestorNode): any {
    return {
      data: node,
      children: node.parents?.map(parent => this.convertToHierarchy(parent)) || []
    };
  }

  private createTreeLayout(): any {
    const nodeWidth = 180;
    const nodeHeight = 150;

    switch (this.layoutDirection) {
      case 'left':
        return d3.tree().size([this.height - 100, this.width - 200])
          .separation((a, b) => (a.parent === b.parent ? 1 : 1.5));
      case 'right':
        return d3.tree().size([this.height - 100, this.width - 200])
          .separation((a, b) => (a.parent === b.parent ? 1 : 1.5));
      case 'up':
        return d3.tree().size([this.width - 200, this.height - 100])
          .separation((a, b) => (a.parent === b.parent ? 1 : 1.5));
      case 'down':
      default:
        return d3.tree().size([this.width - 200, this.height - 100])
          .separation((a, b) => (a.parent === b.parent ? 1 : 1.5));
    }
  }

  private calculateTranslation(treeNodes: any): { translateX: number, translateY: number } {
    switch (this.layoutDirection) {
      case 'left':
        return { translateX: this.width - 150, translateY: 50 };
      case 'right':
        return { translateX: 150, translateY: 50 };
      case 'up':
        return { translateX: 100, translateY: this.height - 100 };
      case 'down':
      default:
        return { translateX: 100, translateY: 80 };
    }
  }

  private drawLinks(treeNodes: any, translateX: number, translateY: number): void {
    const linkGenerator = this.createLinkGenerator();

    this.g.selectAll('.link')
      .data(treeNodes.links())
      .enter()
      .append('path')
      .attr('class', 'link')
      .attr('d', (d: any) => {
        const source = this.getNodePosition(d.source, translateX, translateY);
        const target = this.getNodePosition(d.target, translateX, translateY);
        return linkGenerator({ source, target });
      })
      .attr('fill', 'none')
      .attr('stroke', '#ccc')
      .attr('stroke-width', 2);
  }

  private createLinkGenerator(): any {
    switch (this.layoutDirection) {
      case 'left':
      case 'right':
        return (d: any) => {
          return `M${d.source.x},${d.source.y}
                  C${(d.source.x + d.target.x) / 2},${d.source.y}
                   ${(d.source.x + d.target.x) / 2},${d.target.y}
                   ${d.target.x},${d.target.y}`;
        };
      case 'up':
      case 'down':
      default:
        return (d: any) => {
          return `M${d.source.x},${d.source.y}
                  C${d.source.x},${(d.source.y + d.target.y) / 2}
                   ${d.target.x},${(d.source.y + d.target.y) / 2}
                   ${d.target.x},${d.target.y}`;
        };
    }
  }

  private createLinkPath(source: {x: number, y: number}, target: {x: number, y: number}): string {
    switch (this.layoutDirection) {
      case 'left':
      case 'right':
        return `M${source.x},${source.y}
                C${(source.x + target.x) / 2},${source.y}
                 ${(source.x + target.x) / 2},${target.y}
                 ${target.x},${target.y}`;
      case 'up':
      case 'down':
      default:
        return `M${source.x},${source.y}
                C${source.x},${(source.y + target.y) / 2}
                 ${target.x},${(source.y + target.y) / 2}
                 ${target.x},${target.y}`;
    }
  }

  private getNodePosition(node: any, translateX: number, translateY: number): { x: number, y: number } {
    switch (this.layoutDirection) {
      case 'left':
        return { x: translateX - node.y, y: node.x + translateY };
      case 'right':
        return { x: node.y + translateX, y: node.x + translateY };
      case 'up':
        return { x: node.x + translateX, y: translateY - node.y };
      case 'down':
      default:
        return { x: node.x + translateX, y: node.y + translateY };
    }
  }

  private drawNodes(treeNodes: any, translateX: number, translateY: number): void {
    // Store node positions for dragging
    const nodePositions = new Map<string, {x: number, y: number}>();
    treeNodes.descendants().forEach((d: any) => {
      const pos = this.getNodePosition(d, translateX, translateY);
      nodePositions.set(d.data.data.id, { x: pos.x, y: pos.y });
    });

    // Create drag behavior
    const dragBehavior = d3.drag()
      .subject((event: any, d: any) => {
        const nodeId = d.data.data.id;
        const pos = nodePositions.get(nodeId);
        if (pos) {
          return { x: pos.x, y: pos.y };
        }
        return { x: event.x, y: event.y };
      })
      .on('start', (event: any, d: any) => {
        const nodeGroup = d3.select(event.sourceEvent.target).node().closest('.node');
        if (nodeGroup) {
          d3.select(nodeGroup).raise();
        }
      })
      .on('drag', (event: any, d: any) => {
        const nodeId = d.data.data.id;
        const nodeGroup = d3.select(event.sourceEvent.target).node().closest('.node');
        if (!nodeGroup) return;

        // Update node position
        d3.select(nodeGroup)
          .attr('transform', `translate(${event.x},${event.y})`);

        // Update stored position
        nodePositions.set(nodeId, { x: event.x, y: event.y });

        // Update all links connected to this node
        this.g.selectAll('.link')
          .attr('d', (linkData: any) => {
            const sourceId = linkData.source.data.data.id;
            const targetId = linkData.target.data.data.id;

            const sourcePos = nodePositions.get(sourceId) || this.getNodePosition(linkData.source, translateX, translateY);
            const targetPos = nodePositions.get(targetId) || this.getNodePosition(linkData.target, translateX, translateY);

            return this.createLinkPath(sourcePos, targetPos);
          });
      });

    const nodes = this.g.selectAll('.node')
      .data(treeNodes.descendants())
      .enter()
      .append('g')
      .attr('class', 'node')
      .attr('transform', (d: any) => {
        const pos = this.getNodePosition(d, translateX, translateY);
        return `translate(${pos.x},${pos.y})`;
      })
      .on('click', (event: any, d: any) => {
        if (event.defaultPrevented) return;
        this.onNodeClick(d.data.data);
      })
      .style('cursor', 'move')
      .call(dragBehavior);

    // Add node content
    nodes.each((d: any, i: number, nodeElements: any) => {
      const node = d3.select(nodeElements[i]);
      const individual = d.data.data as AncestorNode;

      // Frame dimensions
      const frameWidth = 160;
      const frameHeight = 120;

      // Add background frame (gray if deceased)
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
          if (individual.gender === 'MALE') return '#4A90E2';
          if (individual.gender === 'FEMALE') return '#E294A9';
          return '#9B9B9B';
        })
        .attr('stroke-width', 2)
        .attr('filter', 'drop-shadow(0px 2px 4px rgba(0,0,0,0.1))');

      // Generation badge
      const genBadge = node.append('g')
        .attr('transform', `translate(${frameWidth / 2 - 20}, ${-frameHeight / 2 + 10})`);

      genBadge.append('circle')
        .attr('r', 12)
        .attr('fill', this.getGenerationColor(individual.generation));

      genBadge.append('text')
        .attr('text-anchor', 'middle')
        .attr('dy', 4)
        .attr('font-size', '10px')
        .attr('font-weight', 'bold')
        .attr('fill', 'white')
        .text(individual.generation.toString());

      // Avatar or initials
      const avatarUrl = this.getAvatarUrl(individual);
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
          .attr('stroke', () => {
            if (individual.gender === 'MALE') return '#4A90E2';
            if (individual.gender === 'FEMALE') return '#E294A9';
            return '#9B9B9B';
          })
          .attr('stroke-width', 2);
      } else {
        node.append('circle')
          .attr('cy', -20)
          .attr('r', 30)
          .attr('fill', () => {
            if (individual.gender === 'MALE') return '#4A90E2';
            if (individual.gender === 'FEMALE') return '#E294A9';
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

      // Name
      node.append('text')
        .attr('y', 30)
        .attr('text-anchor', 'middle')
        .attr('font-size', '11px')
        .attr('font-weight', 'bold')
        .attr('fill', '#333')
        .text(this.truncateText(individual.fullName, 18));

      // Birth info
      const birthDate = individual.birthDate
        ? new Date(individual.birthDate).toLocaleDateString('vi-VN', { year: 'numeric', month: 'short', day: 'numeric' })
        : '?';

      node.append('text')
        .attr('y', 45)
        .attr('text-anchor', 'middle')
        .attr('font-size', '9px')
        .attr('fill', '#666')
        .text(`Sinh: ${birthDate}`);

      // Death info (if applicable)
      if (individual.deathDate) {
        const deathDate = new Date(individual.deathDate).toLocaleDateString('vi-VN', { year: 'numeric', month: 'short', day: 'numeric' });
        node.append('text')
          .attr('y', 57)
          .attr('text-anchor', 'middle')
          .attr('font-size', '9px')
          .attr('fill', '#666')
          .text(`Mat: ${deathDate}`);
      }
    });
  }

  getGenerationColor(generation: number): string {
    const colors = [
      '#4CAF50', // 0 - Self (green)
      '#2196F3', // 1 - Parents (blue)
      '#9C27B0', // 2 - Grandparents (purple)
      '#FF9800', // 3 - Great-grandparents (orange)
      '#F44336', // 4 - Great-great-grandparents (red)
      '#00BCD4', // 5 - (cyan)
      '#795548', // 6 - (brown)
      '#607D8B', // 7 - (blue-gray)
      '#E91E63', // 8 - (pink)
      '#3F51B5', // 9 - (indigo)
      '#009688'  // 10 - (teal)
    ];
    return colors[generation] || colors[colors.length - 1];
  }

  private getAvatarUrl(individual: AncestorNode): string | null {
    if (!individual.profilePictureUrl) return null;
    if (individual.profilePictureUrl.startsWith('http')) {
      return individual.profilePictureUrl;
    }
    return `${environment.baseUrl}${individual.profilePictureUrl}`;
  }

  private getInitials(individual: AncestorNode): string {
    const given = individual.givenName || '';
    const surname = individual.surname || '';
    return (given.charAt(0) + surname.charAt(0)).toUpperCase() || '?';
  }

  private truncateText(text: string, maxLength: number): string {
    if (!text) return '';
    if (text.length <= maxLength) return text;
    return text.substring(0, maxLength) + '...';
  }

  private fitToScreen(): void {
    if (!this.svg || !this.g) return;

    const bounds = this.g.node()?.getBBox();
    if (!bounds) return;

    const fullWidth = this.width;
    const fullHeight = this.height;
    const width = bounds.width;
    const height = bounds.height;
    const midX = bounds.x + width / 2;
    const midY = bounds.y + height / 2;

    if (width === 0 || height === 0) return;

    const scale = 0.85 / Math.max(width / fullWidth, height / fullHeight);
    const translate = [
      fullWidth / 2 - scale * midX,
      fullHeight / 2 - scale * midY
    ];

    this.svg.transition()
      .duration(500)
      .call(this.zoom.transform, d3.zoomIdentity.translate(translate[0], translate[1]).scale(scale));
  }

  onNodeClick(node: AncestorNode): void {
    this.router.navigate(['/trees', this.treeId, 'individuals', node.id]);
  }

  zoomIn(): void {
    if (this.svg) {
      this.svg.transition().call(this.zoom.scaleBy, 1.3);
    }
  }

  zoomOut(): void {
    if (this.svg) {
      this.svg.transition().call(this.zoom.scaleBy, 0.7);
    }
  }

  resetZoom(): void {
    this.fitToScreen();
  }

  back(): void {
    this.router.navigate(['/trees', this.treeId, 'visualization']);
  }

  getGenerationLabel(gen: number): string {
    switch (gen) {
      case 1: return '1 (Cha Me)';
      case 2: return '2 (Ong Ba)';
      case 3: return '3 (Cu)';
      case 4: return '4 (Ky)';
      case 5: return '5 (Cao)';
      default: return `${gen} the he`;
    }
  }

  linkMeToIndividual(): void {
    if (!this.selectedIndividual) {
      this.snackBar.open('Vui long chon mot nguoi truoc', 'Dong', { duration: 3000 });
      return;
    }

    this.userTreeProfileService.linkToIndividual(this.treeId, this.selectedIndividual.id).subscribe({
      next: (profile) => {
        this.userProfile = profile;
        this.snackBar.open(`Da lien ket voi ${profile.individual.fullName}`, 'Dong', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error linking to individual:', error);
        this.snackBar.open('Khong the lien ket. Vui long thu lai.', 'Dong', { duration: 3000 });
      }
    });
  }

  unlinkMe(): void {
    this.userTreeProfileService.unlinkFromIndividual(this.treeId).subscribe({
      next: () => {
        this.userProfile = null;
        this.snackBar.open('Da huy lien ket', 'Dong', { duration: 3000 });
      },
      error: (error) => {
        console.error('Error unlinking:', error);
        this.snackBar.open('Khong the huy lien ket. Vui long thu lai.', 'Dong', { duration: 3000 });
      }
    });
  }
}
