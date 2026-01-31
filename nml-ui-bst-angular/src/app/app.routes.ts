import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'carte',
    loadComponent: () => import('./pages/carte/carte.component').then(m => m.CarteComponent),
    canActivate: [authGuard]
  },
  {
    path: 'joueur',
    loadComponent: () => import('./pages/joueur/joueur.component').then(m => m.JoueurComponent),
    canActivate: [authGuard]
  },
  {
    path: 'boutique',
    loadComponent: () => import('./pages/boutique/boutique.component').then(m => m.BoutiqueComponent),
    canActivate: [authGuard]
  },
  {
    path: 'regles',
    loadComponent: () => import('./pages/regles/regles.component').then(m => m.ReglesComponent),
    canActivate: [authGuard]
  },
  {
    path: '',
    redirectTo: 'carte',
    pathMatch: 'full'
  },
  {
    path: '**',
    redirectTo: 'carte'
  }
];
