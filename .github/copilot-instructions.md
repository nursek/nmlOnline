# Copilot Instructions – NML Online

## 🎯 Objectif
Projet full-stack de jeu de stratégie :
- Backend : Spring Boot 3.5 (Java 21, JPA, JWT)
- Frontend : Angular 22 (standalone, signaux, Material)

Réponses et commentaires générés en **français**.

---

## 🔐 Règles de sécurité (CRITIQUE)

### 1️⃣ Ownership joueur
- Ne jamais accepter `playerId` depuis le body/params.
- Toujours récupérer le `userId` via `request.getAttribute("userId")`.
- Vérifier l'ownership dans le service.
- Si ressource ≠ joueur → `SecurityException` (403).

### 2️⃣ Admin
- Toute opération CRUD globale → `@PreAuthorize("hasRole('ADMIN')")`
- Endpoints admin regroupés sous `/api/admin/**`
- Ne jamais exposer un endpoint admin dans un controller joueur.

---

## 🧠 Règles métier importantes

- Source unique de propriété : `Sector.ownerId`
- `Board.sectors` est une map transient initialisée via `@PostLoad`
- Relations JPA : éviter boucles JSON (`@JsonIgnore` côté many)
- **Hygiène cascade JPA** : voir [`docs/jpa-pitfalls.md`](../docs/jpa-pitfalls.md)
  avant d'ajouter `@OneToMany(mappedBy=…, orphanRemoval=true)` avec sous-enfant à FK
  NOT NULL, ou avant `.remove()`/`.clear()` sur une telle collection. Audit des
  `orphanRemoval=true` existants (`Player.equipments`, `Player.resources`,
  `Player.buildings`) tracé dans ce doc — `Player.equipments` et `Player.resources`
  sont les suspects prioritaires d'un futur 500 si un path de transfert est ajouté.
- **Résolution du tour** : deux chemins de fin de tour admin —
  `TurnService.advanceTurn()` (atomique) et `TurnResolutionOrchestrator` (pas-à-pas
  par hop). `TurnLock` (bean `AtomicBoolean` partagé) les sérialise. La session de
  l'orchestrateur est en mémoire, JVM unique, perdue au redémarrage.
  `Board.currentTurn` est muté à **deux** endroits — les deux doivent invalider
  `TurnService.cachedTurn` (le cache de `getCurrentTurn`).

---

## 🏗️ Architecture backend

- `domain/model` → Entités
- `domain/service` → Logique métier
- `api/controller` → Endpoints REST
- `infrastructure/repository` → JPA
- `mapper` → Domain <-> DTO

Toujours respecter cette séparation.

---

## 🧩 Frontend

- Composants standalone uniquement
- Pas de NgRx — état global dans des services à base de signaux
  (`signal`/`computed`, `httpResource` pour les données serveur)
- Lazy loading via `loadComponent`

---

## ⚠️ Règles générales

- Ne pas dupliquer de logique.
- Ne pas sur-complexifier (YAGNI : pas d'abstraction spéculative, diff minimal, suppression > ajout).
- Pas de nouvelle dépendance quand quelques lignes de code suffisent.
- Raccourci délibéré → commentaire `ponytail:` indiquant la limite connue et le chemin d'amélioration.
- Respecter les conventions existantes.
- Générer du code cohérent avec l’existant.
- Tout changement de schéma (entité, colonne, index) s'accompagne d'un script Flyway
  `V<n>__description.sql` dans `nml-ms/src/main/resources/db/migration/` (actif en profil `prod`
  uniquement ; ne jamais modifier une migration déjà appliquée). Voir `AGENTS.md`.