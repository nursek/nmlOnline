import { Routes } from '@angular/router';
import { authGuard, noAuthGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';
import { instanceGuard } from './guards/instance.guard';
import { playerGuard } from './guards/player.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent),
    canActivate: [noAuthGuard],
  },
  {
    path: 'parties',
    loadComponent: () =>
      import('./pages/parties/parties.component').then((m) => m.PartiesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'admin',
    loadComponent: () => import('./pages/admin/admin.component').then((m) => m.AdminComponent),
    canActivate: [authGuard, instanceGuard, adminGuard],
  },
  {
    path: 'admin/resolution',
    redirectTo: 'admin/tour',
    pathMatch: 'full',
  },
  {
    path: 'admin/tour',
    loadComponent: () =>
      import('./pages/turn-management/turn-management.component').then(
        (m) => m.TurnManagementComponent,
      ),
    canActivate: [authGuard, instanceGuard, adminGuard],
  },
  {
    path: 'admin/joueurs',
    loadComponent: () =>
      import('./pages/player-management/player-management.component').then(
        (m) => m.PlayerManagementComponent,
      ),
    canActivate: [authGuard, instanceGuard, adminGuard],
  },
  {
    path: 'admin/scenarios',
    loadComponent: () =>
      import('./pages/dev-scenarios/dev-scenarios.component').then((m) => m.DevScenariosComponent),
    canActivate: [authGuard, instanceGuard, adminGuard],
  },
  {
    path: 'accueil',
    loadComponent: () =>
      import('./pages/accueil/accueil.component').then((m) => m.AccueilComponent),
    canActivate: [authGuard, instanceGuard],
  },
  {
    path: 'carte',
    loadComponent: () => import('./pages/carte/carte.component').then((m) => m.CarteComponent),
    canActivate: [authGuard, instanceGuard],
  },
  {
    path: 'joueur',
    loadComponent: () => import('./pages/joueur/joueur.component').then((m) => m.JoueurComponent),
    canActivate: [authGuard, instanceGuard, playerGuard],
  },
  {
    path: 'boutique',
    loadComponent: () =>
      import('./pages/boutique/boutique.component').then((m) => m.BoutiqueComponent),
    canActivate: [authGuard, instanceGuard, playerGuard],
  },
  {
    path: 'regles/tir-a-distance',
    loadComponent: () =>
      import('./pages/regles/tir-a-distance/tir-a-distance.component').then(
        (m) => m.TirADistanceComponent,
      ),
    canActivate: [authGuard],
  },
  {
    path: 'regles',
    loadComponent: () => import('./pages/regles/regles.component').then((m) => m.ReglesComponent),
    canActivate: [authGuard],
  },
  {
    path: 'ordres',
    loadComponent: () => import('./pages/ordres/ordres.component').then((m) => m.OrdresComponent),
    canActivate: [authGuard, instanceGuard, playerGuard],
  },
  {
    path: 'rapports',
    loadComponent: () =>
      import('./pages/rapports/rapports.component').then((m) => m.RapportsComponent),
    canActivate: [authGuard, instanceGuard, playerGuard],
  },
  {
    path: '',
    redirectTo: 'accueil',
    pathMatch: 'full',
  },
  {
    path: '**',
    loadComponent: () =>
      import('./pages/not-found/not-found.component').then((m) => m.NotFoundComponent),
  },
];
