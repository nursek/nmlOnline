# Copilot Instructions – NML Online

## 🎯 Objectif
Projet full-stack de jeu de stratégie :
- Backend : Spring Boot 3.5 (Java 21, JPA, JWT)
- Frontend : Angular 21 (standalone, NgRx, Material)

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
- NgRx obligatoire pour état global
- État immutable (spread operator)
- Lazy loading via `loadComponent`

---

## ⚠️ Règles générales

- Ne pas dupliquer de logique.
- Ne pas sur-complexifier.
- Respecter les conventions existantes.
- Générer du code cohérent avec l’existant.