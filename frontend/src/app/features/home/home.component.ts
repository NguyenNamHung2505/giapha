import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { TranslateModule } from '@ngx-translate/core';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';
import { Observable } from 'rxjs';

interface QuickAction {
  titleKey: string;
  descriptionKey: string;
  icon: string;
  route: string;
  color: string;
  adminOnly?: boolean;
}

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatGridListModule,
    TranslateModule
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  currentUser$: Observable<User | null>;
  quickActions: QuickAction[] = [];

  private allActions: QuickAction[] = [
    {
      titleKey: 'home.myTrees',
      descriptionKey: 'home.myTreesDesc',
      icon: 'folder',
      route: '/trees',
      color: 'primary',
      adminOnly: false
    },
    {
      titleKey: 'home.userManagement',
      descriptionKey: 'home.userManagementDesc',
      icon: 'people',
      route: '/admin/users',
      color: 'accent',
      adminOnly: true
    }
  ];

  constructor(private authService: AuthService) {
    this.currentUser$ = this.authService.currentUser$;
  }

  ngOnInit(): void {
    this.updateQuickActions();

    // Update actions when user changes
    this.currentUser$.subscribe(() => {
      this.updateQuickActions();
    });
  }

  private updateQuickActions(): void {
    const isAdmin = this.authService.isAdmin();
    this.quickActions = this.allActions.filter(action => !action.adminOnly || isAdmin);
  }
}
