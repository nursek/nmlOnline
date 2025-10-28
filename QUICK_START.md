# Guide de démarrage rapide - NML Online

## Prérequis
- Node.js 18+ installé
- Java 17+ installé  
- Maven installé

## Démarrage

### 1. Backend (Spring Boot)
```bash
cd nml-ms
mvn spring-boot:run
```
Le backend démarre sur `http://localhost:8080`

### 2. Frontend (React)
```bash
cd nml-ui-copilot
npm install
npm run dev
```
Le frontend démarre sur `http://localhost:5174`

### 3. Accéder à l'application
Ouvrez votre navigateur sur `http://localhost:5174`

## Fonctionnalités disponibles

✅ **Login** - Connexion sécurisée avec JWT et refresh token
- Option "Remember Me" (30 jours)
- Protection contre le brute force

✅ **Carte du monde** - Visualisation des territoires
- Affichage de tous les joueurs et leurs territoires
- Statistiques publiques
- Carte interactive (en cours d'amélioration)

✅ **Profil du joueur** - Vos informations complètes
- Statistiques (argent, influence, territoires)
- Liste des équipements possédés
- Territoires contrôlés avec bonus

✅ **Boutique** - Achat d'équipements
- Catalogue complet des équipements
- Panier d'achats avec gestion des quantités
- Affichage de vos équipements actuels
- Vérification des fonds disponibles

✅ **Règles** - Guide du jeu
- But du jeu
- Déroulement des parties
- Conditions de victoire
- Conseils stratégiques

## Design et interface

- **Thème sombre** optimisé pour l'ambiance de jeu de stratégie
- **TailwindCSS v4** pour un design moderne et responsive
- **Icônes Lucide** pour une interface claire
- **Animations fluides** pour une meilleure expérience utilisateur
- **Design mobile-friendly**

## Architecture technique

### Frontend
- React 19 + TypeScript
- Redux Toolkit pour la gestion d'état globale
- React Router pour la navigation
- Axios pour les requêtes API
- Vite pour le build rapide

### Backend
- Spring Boot 3
- Spring Security avec JWT
- Base de données H2 (développement)
- Architecture en couches (API, Domain, Infrastructure)

## API Endpoints utilisés

### Auth
- `POST /api/login` - Connexion
- `POST /api/auth/logout` - Déconnexion
- `POST /api/auth/refresh` - Refresh du token

### Players
- `GET /api/players` - Liste des joueurs
- `GET /api/players/{name}` - Détails d'un joueur

### Equipment
- `GET /api/equipment` - Liste des équipements
- `GET /api/equipment/{id}` - Détails d'un équipement

## Prochaines étapes

🔧 **En cours d'implémentation** (backend)
- API d'achat d'équipements
- API de combat entre joueurs
- API de capture de territoires
- Informations publiques de la carte (sans authentification)

🎯 **Améliorations prévues** (frontend)
- WebSockets pour les mises à jour en temps réel
- Carte interactive avec zoom et pan
- Système de notifications
- Animations de combat
- Classement des joueurs

## Problèmes connus

- L'achat d'équipements affiche une alerte (API backend en cours)
- La carte ne se met pas à jour automatiquement
- Pas encore de système de combat implémenté

## Support

Pour toute question ou problème, consultez :
- `nml-ms/API_REFERENCE.md` - Documentation de l'API
- `nml-ms/DATABASE_DOCUMENTATION.md` - Structure de la base de données
- `nml-ms/README.md` - Documentation du backend

