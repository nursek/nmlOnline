# NML Online - Angular UI

Application Angular 19 migrée depuis React (nml-ui-copilot).

## Technologies utilisées

- **Angular 19** - Framework frontend
- **Angular Material** - Composants UI
- **NgRx** - Gestion d'état (équivalent Redux)
- **SCSS** - Styles
- **TypeScript** - Langage

## Structure du projet

```
src/
├── app/
│   ├── components/           # Composants réutilisables
│   │   └── navbar/           # Barre de navigation
│   ├── guards/               # Guards de route (authentification)
│   ├── models/               # Interfaces TypeScript
│   ├── pages/                # Pages/Vues de l'application
│   │   ├── login/            # Page de connexion
│   │   ├── carte/            # Carte du monde
│   │   ├── joueur/           # Profil joueur
│   │   ├── boutique/         # Boutique d'équipements
│   │   └── regles/           # Règles du jeu
│   ├── services/             # Services (API, interceptors)
│   └── store/                # NgRx Store
│       ├── auth/             # État authentification
│       ├── player/           # État joueurs
│       └── shop/             # État boutique
├── environments/             # Configuration d'environnement
└── styles.scss               # Styles globaux
```

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

## Development server

To start a local development server, run:

```bash
ng serve
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.


## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
