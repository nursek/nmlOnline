# NML Online - Angular UI

Application Angular 21 migrée depuis React (nml-ui-copilot).

## Technologies utilisées

- **Angular 21** - Framework frontend
- **Angular Material** - Composants UI
- **NgRx** - Gestion d'état (équivalent Redux)
- **SCSS** - Styles
- **TypeScript** - Langage

## Fonctionnalités

### Authentification

- Connexion avec JWT token
- Refresh token automatique via interceptor HTTP
- Stockage local du token et des infos utilisateur
- Route guard pour les pages protégées

### Pages

- **Login** : Formulaire de connexion avec validation
- **Carte** : Vue d'ensemble des territoires et joueurs
- **Joueur** : Statistiques détaillées du joueur connecté
- **Boutique** : Achat d'équipements avec panier persistant
- **Règles** : Documentation du jeu

### Gestion d'état (NgRx)

- **Auth Store** : Gestion de l'authentification
- **Player Store** : Données joueurs (current + all)
- **Shop Store** : Équipements et panier

## Running unit tests

This project uses [Jest](https://jestjs.io/) via `jest-preset-angular`.

```bash
npm test
npm run test:coverage
```
