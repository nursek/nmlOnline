# Services API Angular

Ce dossier contient tous les services pour communiquer avec l'API backend.

## Structure

```
services/
├── auth.service.ts       - Gestion de l'authentification
├── player.service.ts     - Gestion des joueurs
├── equipment.service.ts  - Gestion des équipements
└── index.ts              - Exports des services
```

## Configuration

L'URL de l'API est définie dans `src/environments/environment.ts` :

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

## Utilisation

### AuthService

Service pour gérer l'authentification avec JWT et refresh tokens.

```typescript
import { AuthService } from './services/auth.service';

// Injection du service
constructor(private authService: AuthService) {}

// Connexion
this.authService.login({ 
  username: 'user', 
  password: 'pass',
  rememberMe: true 
}).subscribe({
  next: (response) => console.log('Connecté:', response),
  error: (err) => console.error('Erreur:', err)
});

// Vérifier si l'utilisateur est authentifié
const isAuth = this.authService.isAuthenticated();

// Récupérer l'utilisateur actuel
const user = this.authService.currentUser();

// Déconnexion
this.authService.logout().subscribe();
```

### PlayerService

Service pour gérer les joueurs.

```typescript
import { PlayerService } from './services/player.service';

constructor(private playerService: PlayerService) {}

// Récupérer tous les joueurs
this.playerService.getAll().subscribe(players => {
  console.log('Joueurs:', players);
});

// Récupérer un joueur par ID
this.playerService.getById(1).subscribe(player => {
  console.log('Joueur:', player);
});

// Créer un joueur
this.playerService.create({ name: 'Nouveau joueur' }).subscribe();

// Mettre à jour un joueur
this.playerService.update(1, { name: 'Nom modifié' }).subscribe();

// Supprimer un joueur
this.playerService.delete(1).subscribe();
```

### EquipmentService

Service pour gérer les équipements.

```typescript
import { EquipmentService } from './services/equipment.service';

constructor(private equipmentService: EquipmentService) {}

// Récupérer tous les équipements
this.equipmentService.getAll().subscribe(equipment => {
  console.log('Équipements:', equipment);
});

// Créer un équipement
this.equipmentService.create({ 
  name: 'Épée', 
  description: 'Une épée puissante',
  price: 100 
}).subscribe();
```

## Authentification automatique

L'intercepteur HTTP (`auth.interceptor.ts`) ajoute automatiquement :
- Le header `Authorization: Bearer <token>` à toutes les requêtes
- Le flag `withCredentials: true` pour gérer les cookies httpOnly

Vous n'avez pas besoin de gérer manuellement les tokens dans vos composants.

## Signals Angular

Les services utilisent les signals d'Angular pour la réactivité :

```typescript
// Dans un composant
import { Component, effect } from '@angular/core';

export class MyComponent {
  constructor(private authService: AuthService) {
    // Réagir aux changements d'authentification
    effect(() => {
      if (this.authService.isAuthenticated()) {
        console.log('Utilisateur connecté:', this.authService.currentUser());
      }
    });
  }
}
```

## Gestion des erreurs

Toutes les requêtes HTTP peuvent échouer. Utilisez toujours un gestionnaire d'erreur :

```typescript
this.playerService.getAll().subscribe({
  next: (players) => {
    // Succès
    console.log(players);
  },
  error: (error) => {
    // Erreur
    console.error('Erreur:', error);
    // Afficher un message à l'utilisateur
  }
});
```

## Exemple complet

Voir `components/api-example/api-example.component.ts` pour un exemple complet d'utilisation.
