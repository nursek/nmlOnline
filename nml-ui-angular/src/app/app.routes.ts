import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./views/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./views/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'carte',
    loadComponent: () => import('./views/carte/carte.component').then(m => m.CarteComponent)
    // Note: authGuard retiré - le composant gère le cas "non connecté" avec un sélecteur de joueur
  },
  {
    path: 'joueur',
    loadComponent: () => import('./views/joueur/joueur.component').then(m => m.JoueurComponent)
    // Note: authGuard retiré - le composant gère le cas "non connecté" avec un sélecteur de joueur
  },
  {
    path: 'boutique',
    loadComponent: () => import('./views/boutique/boutique.component').then(m => m.BoutiqueComponent)
  },
  {
    path: 'regles',
    loadComponent: () => import('./views/regles/regles.component').then(m => m.ReglesComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
