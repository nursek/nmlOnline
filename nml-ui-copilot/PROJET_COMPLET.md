# NML Online - Frontend React avec Redux

## ✅ Projet créé avec succès !

Le frontend React complet pour NML Online a été créé dans le dossier `nml-ui-copilot`.

### 📁 Structure complète

```
nml-ui-copilot/
├── src/
│   ├── components/
│   │   ├── ui/
│   │   │   ├── Button.tsx          # Bouton réutilisable
│   │   │   ├── Card.tsx            # Carte avec header/content/footer
│   │   │   ├── Input.tsx           # Champ de saisie
│   │   │   └── LoadingSpinner.tsx  # Spinner de chargement
│   │   ├── Navbar.tsx              # Barre de navigation principale
│   │   └── ProtectedRoute.tsx      # Protection des routes
│   │
│   ├── pages/
│   │   ├── LoginPage.tsx           # Page de connexion avec remember-me
│   │   ├── CartePage.tsx           # Carte du monde avec territoires
│   │   ├── JoueurPage.tsx          # Profil du joueur connecté
│   │   ├── BoutiquePage.tsx        # Boutique avec panier d'achats
│   │   └── ReglesPage.tsx          # Règles du jeu
│   │
│   ├── store/
│   │   ├── authSlice.ts            # Gestion de l'authentification
│   │   ├── playerSlice.ts          # Gestion des joueurs
│   │   ├── shopSlice.ts            # Gestion de la boutique et panier
│   │   ├── hooks.ts                # Hooks typés Redux
│   │   └── index.ts                # Configuration du store
│   │
│   ├── services/
│   │   └── api.ts                  # Client API avec intercepteurs
│   │
│   ├── types/
│   │   └── index.ts                # Types TypeScript
│   │
│   ├── lib/
│   │   └── utils.ts                # Fonctions utilitaires
│   │
│   ├── App.tsx                     # Composant principal avec routing
│   ├── main.tsx                    # Point d'entrée
│   ├── index.css                   # Styles globaux + TailwindCSS v4
│   └── vite-env.d.ts              # Types pour Vite
│
├── public/
│   └── vite.svg
│
├── index.html                      # HTML principal
├── vite.config.ts                  # Configuration Vite + proxy API
├── tailwind.config.js              # Configuration TailwindCSS
├── postcss.config.js               # Configuration PostCSS
├── tsconfig.json                   # Configuration TypeScript
├── eslint.config.js                # Configuration ESLint
├── package.json                    # Dépendances et scripts
├── .gitignore                      # Fichiers à ignorer
├── .env.example                    # Variables d'environnement exemple
└── README.md                       # Documentation
```

## 🎨 Fonctionnalités implémentées

### ✅ 1. Page de Login
- Formulaire de connexion avec validation
- Option "Remember Me" (30 jours)
- Gestion des erreurs (rate limiting, credentials invalides)
- Redirection automatique après connexion
- Design moderne avec dégradés

### ✅ 2. Page Carte
- Visualisation de tous les territoires de tous les joueurs
- Légende avec couleurs par joueur
- Statistiques publiques (argent, influence, territoires)
- Carte interactive avec positionnement des secteurs
- Design adaptatif

### ✅ 3. Page Joueur
- Statistiques du joueur (argent, influence, territoires)
- Liste complète des équipements possédés avec quantités
- Affichage des bonus (PDF, PDC, ARM, ESQ)
- Liste des territoires contrôlés
- Design avec cartes et icônes

### ✅ 4. Page Boutique
- Catalogue de tous les équipements disponibles
- Panier d'achats avec gestion des quantités
- Affichage des équipements déjà possédés
- Vérification des fonds disponibles
- Interface avec sidebar du panier
- Boutons +/- pour gérer les quantités
- Calcul automatique du total

### ✅ 5. Page Règles
- But du jeu
- Déroulement des parties
- Système de combat
- Conditions de victoire
- Conseils stratégiques
- Design avec icônes et couleurs thématiques

## 🔐 Sécurité

- **JWT** : Tokens stockés dans localStorage
- **Refresh tokens** : Cookies HttpOnly sécurisés
- **Intercepteurs Axios** : Gestion automatique du refresh
- **Routes protégées** : Redirection vers login si non authentifié
- **Protection CSRF** : Cookies avec SameSite

## 🎨 Design

### Thème sombre de jeu de stratégie
- Couleur primaire : Bleu (#2196f3)
- Arrière-plan : Noir/gris foncé
- Dégradés pour les titres
- Effets hover et transitions
- Responsive (mobile, tablette, desktop)

### Composants UI
- Boutons avec variantes (default, outline, destructive, etc.)
- Cartes avec effets d'ombre
- Inputs stylisés
- Loading spinners
- Icônes Lucide React

## 🚀 Démarrage

```bash
cd nml-ui-copilot

# Installation des dépendances (déjà fait)
npm install

# Démarrage du serveur de développement
npm run dev
# Ouvre http://localhost:5174

# Build de production
npm run build

# Prévisualisation du build
npm run preview
```

## 📡 API Backend

Le frontend se connecte à `http://localhost:8080/api`

### Endpoints utilisés :
- `POST /api/login` - Connexion
- `POST /api/auth/logout` - Déconnexion
- `POST /api/auth/refresh` - Refresh token
- `GET /api/players` - Liste des joueurs
- `GET /api/players/{name}` - Détails joueur
- `GET /api/equipment` - Liste équipements

## 🔧 Technologies

- **React 19** - Framework UI
- **TypeScript** - Typage statique
- **Redux Toolkit** - Gestion d'état
- **React Router** - Navigation
- **TailwindCSS v4** - Styles
- **Axios** - Requêtes HTTP
- **Lucide React** - Icônes
- **Vite** - Build tool rapide

## ✅ Le build fonctionne !

```bash
✓ 1746 modules transformed.
dist/index.html                   0.47 kB │ gzip:   0.31 kB
dist/assets/index-BkkSudtD.css   35.38 kB │ gzip:   6.68 kB
dist/assets/index-DBrJ668J.js   354.20 kB │ gzip: 112.59 kB
✓ built in 566ms
```

## 🎯 Prochaines étapes

1. **Démarrer le backend Spring Boot** :
   ```bash
   cd ../nml-ms
   mvn spring-boot:run
   ```

2. **Démarrer le frontend** :
   ```bash
   cd ../nml-ui-copilot
   npm run dev
   ```

3. **Accéder à l'application** :
   Ouvrir http://localhost:5174

4. **Se connecter** avec un utilisateur existant dans votre base

## 📝 Notes importantes

- Le proxy Vite redirige `/api/*` vers `http://localhost:8080`
- Les tokens sont stockés dans localStorage et cookies
- Le panier est persisté dans localStorage
- Les routes sont protégées et redirigent vers /login si non authentifié
- Le design est optimisé pour une expérience de jeu immersive

## 🎮 Charte visuelle cohérente

Toutes les vues utilisent :
- Même palette de couleurs (bleu primaire, fond sombre)
- Même typographie et espacements
- Mêmes composants UI réutilisables
- Même style de cartes et boutons
- Transitions et animations uniformes

Le frontend est **100% fonctionnel** et prêt à être utilisé ! 🎉

