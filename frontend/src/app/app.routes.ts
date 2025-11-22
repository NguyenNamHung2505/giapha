import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
  },
  {
    path: 'trees',
    canActivate: [authGuard],
    loadComponent: () => import('./features/tree/tree-list/tree-list.component').then(m => m.TreeListComponent)
  },
  {
    path: 'trees/new',
    canActivate: [authGuard],
    loadComponent: () => import('./features/tree/tree-form/tree-form.component').then(m => m.TreeFormComponent)
  },
  {
    path: 'trees/:id/edit',
    canActivate: [authGuard],
    loadComponent: () => import('./features/tree/tree-form/tree-form.component').then(m => m.TreeFormComponent)
  },
  {
    path: 'trees/:treeId/visualize',
    canActivate: [authGuard],
    loadComponent: () => import('./features/tree-visualization/tree-visualization.component').then(m => m.TreeVisualizationComponent)
  },
  {
    path: 'trees/:treeId/individuals',
    canActivate: [authGuard],
    loadComponent: () => import('./features/individual/individual-list/individual-list.component').then(m => m.IndividualListComponent)
  },
  {
    path: 'trees/:treeId/individuals/new',
    canActivate: [authGuard],
    loadComponent: () => import('./features/individual/individual-form/individual-form.component').then(m => m.IndividualFormComponent)
  },
  {
    path: 'trees/:treeId/individuals/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/individual/individual-detail/individual-detail.component').then(m => m.IndividualDetailComponent)
  },
  {
    path: 'trees/:treeId/individuals/:id/edit',
    canActivate: [authGuard],
    loadComponent: () => import('./features/individual/individual-form/individual-form.component').then(m => m.IndividualFormComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
