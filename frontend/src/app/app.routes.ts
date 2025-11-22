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
    path: '**',
    redirectTo: ''
  }
];
