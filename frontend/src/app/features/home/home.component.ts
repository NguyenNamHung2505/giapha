import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatGridListModule } from '@angular/material/grid-list';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';
import { Observable } from 'rxjs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatGridListModule
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent implements OnInit {
  currentUser$: Observable<User | null>;

  quickActions = [
    {
      title: 'Create Family Tree',
      description: 'Start building your family tree',
      icon: 'account_tree',
      route: '/trees/new',
      color: 'primary'
    },
    {
      title: 'My Trees',
      description: 'View and manage your family trees',
      icon: 'folder',
      route: '/trees',
      color: 'accent'
    },
    {
      title: 'Add Individual',
      description: 'Add a new person to your tree',
      icon: 'person_add',
      route: '/individuals/new',
      color: 'primary'
    },
    {
      title: 'Upload Media',
      description: 'Add photos and documents',
      icon: 'photo_library',
      route: '/media',
      color: 'accent'
    }
  ];

  constructor(private authService: AuthService) {
    this.currentUser$ = this.authService.currentUser$;
  }

  ngOnInit(): void {}
}
