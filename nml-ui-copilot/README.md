# NML Online - Frontend

Interface utilisateur React pour le jeu de stratégie NML Online.

## 🎮 Fonctionnalités

- **Authentification** : Connexion sécurisée avec option "Remember Me"
- **Carte du monde** : Visualisation des territoires contrôlés par tous les joueurs
- **Profil du joueur** : Affichage des statistiques, équipements et territoires
- **Boutique** : Achat d'équipements avec panier d'achats
- **Règles du jeu** : Guide complet des règles

## 🛠️ Technologies utilisées

- **React 19** avec TypeScript
- **Redux Toolkit** pour la gestion d'état
- **React Router** pour la navigation
- **TailwindCSS** pour le style
- **Axios** pour les appels API
- **Lucide React** pour les icônes
- **Vite** pour le build

## 🚀 Installation et démarrage

```bash
# Installation des dépendances
npm install

# Démarrage du serveur de développement
npm run dev

# Build de production
npm run build
```

Le serveur de développement démarre sur `http://localhost:5174`

## 📁 Structure du projet

```
src/
├── components/          # Composants réutilisables
│   ├── ui/             # Composants UI (Button, Card, Input)
│   ├── Navbar.tsx      # Barre de navigation
│   └── ProtectedRoute.tsx
├── pages/              # Pages de l'application
│   ├── LoginPage.tsx
│   ├── CartePage.tsx
│   ├── JoueurPage.tsx
│   ├── BoutiquePage.tsx
│   └── ReglesPage.tsx
├── store/              # Redux store et slices
│   ├── authSlice.ts
│   ├── playerSlice.ts
│   ├── shopSlice.ts
│   └── index.ts
├── services/           # Services API
│   └── api.ts
├── types/              # Types TypeScript
│   └── index.ts
└── lib/                # Utilitaires
    └── utils.ts
```

## 🔌 Configuration API

Le frontend se connecte au backend Spring Boot sur `http://localhost:8080/api`

Configuration du proxy dans `vite.config.ts` :
```typescript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
}
```

## 🎨 Thème et design

L'interface utilise un thème sombre optimisé pour une ambiance de jeu de stratégie :
- Couleurs principales : bleu (#2196f3) et dégradés
- Composants avec effet glass-morphism
- Animations et transitions fluides
- Design responsive (mobile, tablette, desktop)

## 🔐 Sécurité

- Tokens JWT stockés dans localStorage
- Refresh tokens dans cookies HttpOnly
- Routes protégées avec ProtectedRoute
- Interception automatique des erreurs 401

## 📝 TODO

- [ ] Implémenter l'achat d'équipements (API backend)
- [ ] Ajouter la visualisation en temps réel de la carte
- [ ] Implémenter le système de combat
- [ ] Ajouter les notifications en temps réel
- [ ] Optimiser la carte pour de grandes quantités de territoires

