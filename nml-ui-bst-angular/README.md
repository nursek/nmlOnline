# NML Online - Angular UI

Application Angular 22 migrée depuis React (nml-ui-copilot).

## Technologies utilisées

- **Angular 22** - Framework frontend (standalone, signaux)
- **Angular Material** - Composants UI
- **Signaux + httpResource** - Gestion d'état dans les services (pas de NgRx)
- **SCSS** - Styles
- **TypeScript** - Langage

## Fonctionnalités

### Authentification

- Connexion avec JWT token
- Refresh token automatique via interceptor HTTP
- Stockage du token en `sessionStorage`
- Route guard pour les pages protégées

### Pages

- **Login** : Formulaire de connexion avec validation
- **Carte** : Vue d'ensemble des territoires et joueurs
- **Joueur** : Statistiques détaillées du joueur connecté
- **Boutique** : Achat d'équipements avec panier persistant
- **Admin** : Panneau d'administration (rôle ADMIN)
- **Règles** : Documentation du jeu

### Gestion d'état (services à signaux)

- **AuthService** : Gestion de l'authentification
- **PlayerService** : Données du joueur connecté
- **ShopService** : Équipements et panier (`httpResource` pour les catalogues)
- **AdminService** : Panneau admin (`httpResource`)

## Running unit tests

This project uses [Jest](https://jestjs.io/) via `jest-preset-angular`.

```bash
npm test
npm run test:coverage
```
