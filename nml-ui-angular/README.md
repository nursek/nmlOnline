# NML Online - Angular Frontend# NmlUiAngular



Interface utilisateur moderne et réactive pour le jeu NML Online, recréée en Angular avec des améliorations significatives par rapport à la version React.This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 20.3.6.



## 🚀 Fonctionnalités## Development server



### ✅ Authentification ComplèteTo start a local development server, run:

- Connexion/Déconnexion avec JWT

- Système d'inscription```bash

- Refresh tokens automatiquesng serve

- Auth guard pour les routes protégées```

- Gestion des sessions avec "Se souvenir de moi"

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

### 🎮 Pages de Jeu

## Code scaffolding

#### 🏠 Page d'Accueil

- Design hero moderne avec animationsAngular CLI includes powerful code scaffolding tools. To generate a new component, run:

- Cards flottantes interactives

- Section des fonctionnalités```bash

- Call-to-action adaptatifs selon l'état de connexionng generate component component-name

```

#### 👤 Profil Joueur

- Visualisation des unités avec cartes détailléesFor a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

- Barres de santé animées

- Statistiques en temps réel```bash

- Tags pour troupes et équipementsng generate --help

- Actions de gestion (modifier, supprimer)```



#### 🛒 Boutique## Building

- Grille d'équipements avec filtres

- Recherche en temps réelTo build the project run:

- Filtrage par catégorie (Armes, Armures, Magie)

- Système de rareté (Common, Rare, Epic, Legendary)```bash

- Affichage de l'or disponibleng build

- États de chargement et notifications de succès```



#### 🗺️ Carte InteractiveThis will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

- Carte SVG cliquable et interactive

- Système de zoom et contrôles## Running unit tests

- Panneau d'informations détaillées

- Affichage des ressources par territoireTo execute unit tests with the [Karma](https://karma-runner.github.io) test runner, use the following command:

- Actions contextuelles selon le propriétaire

- Légende avec types de territoires```bash

ng test

#### 📖 Règles du Jeu```

- Documentation complète et structurée

- Design moderne avec sections colorées## Running end-to-end tests

- Conseils stratégiques

- Conditions de victoireFor end-to-end (e2e) testing, run:

- Guide du système de combat

```bash

## 🎨 Améliorations par rapport à Reactng e2e

```

### Design & UX

- ✨ **Gradient modernes** : Utilisation de dégradés visuellement attractifsAngular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

- 🎭 **Animations fluides** : Transitions et animations CSS avancées

- 📱 **Responsive complet** : Optimisé pour mobile, tablette et desktop## Additional Resources

- 🎨 **Design system cohérent** : Palette de couleurs et composants uniformes

- 💫 **Micro-interactions** : Hover effects, loading states, et feedbacks visuelsFor more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.


### Architecture
- 🔧 **Standalone Components** : Architecture modulaire Angular moderne
- 📦 **Lazy Loading** : Chargement à la demande des routes
- 🎯 **Signals** : Gestion d'état réactive avec Angular Signals
- 🛡️ **Type Safety** : TypeScript strict pour tous les composants
- 🔐 **Auth Guard** : Protection des routes avec redirection automatique

### Fonctionnalités Avancées
- 🔄 **Intercepteur HTTP** : Ajout automatique des tokens JWT
- 📊 **États de chargement** : Spinners et skeletons
- ⚠️ **Gestion d'erreurs** : Messages d'erreur contextuels
- 🎯 **Empty states** : Messages informatifs quand pas de données
- 🎊 **Toasts** : Notifications non-intrusives
- 🔍 **Recherche et filtres** : Fonctionnalités de recherche avancées

## 📁 Structure du Projet

```
src/app/
├── components/
│   └── navbar/                 # Barre de navigation globale
├── guards/
│   └── auth.guard.ts           # Protection des routes
├── interceptors/
│   └── auth.interceptor.ts     # Injection automatique des tokens
├── models/
│   ├── auth.model.ts           # Interfaces d'authentification
│   ├── equipment.model.ts      # Interface équipement
│   └── player.model.ts         # Interface joueur
├── services/
│   ├── auth.service.ts         # Service d'authentification
│   ├── equipment.service.ts    # Service équipements
│   └── player.service.ts       # Service joueurs
├── views/
│   ├── home/                   # Page d'accueil
│   ├── login/                  # Page de connexion
│   ├── joueur/                 # Page profil joueur
│   ├── boutique/               # Page boutique
│   ├── carte/                  # Page carte interactive
│   └── regles/                 # Page règles du jeu
├── app.config.ts               # Configuration Angular
└── app.routes.ts               # Routes de l'application
```

## 🛠️ Installation et Démarrage

### Prérequis
- Node.js 18+ et npm
- Angular CLI 19+

### Installation

```bash
cd nml-ui-angular
npm install
```

### Développement

```bash
npm start
# ou
ng serve
```

L'application sera accessible sur `http://localhost:4200`

### Build Production

```bash
npm run build
# ou
ng build --configuration production
```

## 🔌 Configuration de l'API

L'URL de l'API est configurable dans `src/environments/`:

**Development** (`environment.ts`):
```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

**Production** (`environment.prod.ts`):
```typescript
export const environment = {
  production: true,
  apiUrl: '/api'
};
```

## 🎯 Routes Disponibles

| Route | Protection | Description |
|-------|-----------|-------------|
| `/` | Public | Page d'accueil |
| `/login` | Public | Connexion/Inscription |
| `/carte` | 🔒 Protégée | Carte interactive |
| `/joueur` | 🔒 Protégée | Profil joueur |
| `/boutique` | 🔒 Protégée | Boutique d'équipements |
| `/regles` | Public | Règles du jeu |

## 🎨 Palette de Couleurs

- **Primary**: `#667eea` → `#764ba2` (Gradient)
- **Success**: `#4caf50`
- **Danger**: `#f44336`
- **Warning**: `#ff9800`
- **Info**: `#2196F3`
- **Gold**: `#ffd700`

## 🚀 Fonctionnalités à Venir

- [ ] Connexion aux APIs backend réelles
- [ ] Mode sombre
- [ ] Notifications en temps réel (WebSocket)
- [ ] Chat entre joueurs
- [ ] Système d'achievements
- [ ] Classement/Leaderboard
- [ ] Animations de combat
- [ ] Multi-langue (i18n)

## 📝 Notes de Développement

### Données de Démonstration
Actuellement, l'application utilise des données mockées pour la démonstration. Une fois le backend prêt, décommentez les appels API dans les services et supprimez les `setTimeout` simulant les requêtes.

### Services API
Tous les services sont prêts pour se connecter au backend :
- `AuthService` → `/api/login`, `/api/auth/refresh`, etc.
- `PlayerService` → `/api/players`
- `EquipmentService` → `/api/equipment`

## 🤝 Contribution

1. Les composants sont standalone et indépendants
2. Utilisez les Signals pour la réactivité
3. Suivez le style guide Angular
4. Testez la responsivité sur tous les appareils
5. Documentez les nouvelles fonctionnalités

## 📄 Licence

Projet éducatif - NML Online

---

**Développé avec** ❤️ **en Angular 19**
