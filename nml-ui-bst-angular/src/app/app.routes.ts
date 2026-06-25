import { Routes } from '@angular/router';
import { authGuard, noAuthGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
    canActivate: [noAuthGuard],
  },
  {
    path: 'admin',
    loadComponent: () => import('./pages/admin/admin.component').then((m) => m.AdminComponent),
    canActivate: [authGuard, adminGuard],
  },
  {
    path: 'carte',
    loadComponent: () => import('./pages/carte/carte.component').then((m) => m.CarteComponent),
    canActivate: [authGuard],
  },
  {
    path: 'joueur',
    loadComponent: () => import('./pages/joueur/joueur.component').then((m) => m.JoueurComponent),
    canActivate: [authGuard],
  },
  {
    path: 'boutique',
    loadComponent: () =>
      import('./pages/boutique/boutique.component').then((m) => m.BoutiqueComponent),
    canActivate: [authGuard],
  },
  {
    path: 'regles',
    loadComponent: () => import('./pages/regles/regles.component').then((m) => m.ReglesComponent),
    canActivate: [authGuard],
  },
  {
    path: '',
    redirectTo: 'carte',
    pathMatch: 'full',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
];
