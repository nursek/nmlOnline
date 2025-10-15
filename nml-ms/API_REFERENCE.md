# Résumé concis des APIs

Voici un résumé court et pratique des principaux endpoints exposés par cette application.

## Auth (base path: `/api`)
- POST `/api/login` : body `{username,password,rememberMe}` → 200 `{token,id,username}` + cookie `refresh_token` (httpOnly) ; 401 si identifiants invalides ; 429 si trop de tentatives.
- POST `/api/auth/refresh` : lit le cookie `refresh_token` → 200 `{valid:true,token,id,name}` ou `{valid:false}`.
- POST `/api/register` : body `{username,password}` → 200 si créé ou 409 si déjà existant.
- POST `/api/auth/logout` : supprime le refresh token (cookie + serveur) → 200.

## Equipment (base path: `/api/equipment`)
- GET `/api/equipment` : liste tous les équipements → 200 (array of EquipmentDto).
- GET `/api/equipment/{id}` : récupère un équipement → 200 ou 404 si non trouvé.
- POST `/api/equipment` : crée un équipement à partir d'un EquipmentDto (id ignoré) → 200 avec l'objet créé (avec id).
- DELETE `/api/equipment/{id}` : supprime → 204 ou 404 si non trouvé.

## Players (base path: `/api/players`)
- GET `/api/players` : liste tous les joueurs → 200 (array of PlayerDto).
- GET `/api/players/{id}` : récupère un joueur → 200 ou 404.
- GET `/api/players/{id}/export` : export (même réponse que GET `/api/players/{id}`).
- POST `/api/players` : crée un joueur à partir d'un PlayerDto → 201 + header `Location: /api/players/{id}`.
- PUT `/api/players/{id}` : met à jour → 200 ou 404.
- DELETE `/api/players/{id}` : supprime → 204 ou 404.

## Notes rapides
- Pour les endpoints protégés, ajouter le header : `Authorization: Bearer <ACCESS_TOKEN>`.
- Le cookie de refresh s'appelle `refresh_token` : httpOnly, path `/api/auth`, SameSite=Lax, secure configurable via `app.cookie.secure`.
- Les DTO exposés sont des vues légères ; attention aux relations lazy et aux champs sensibles lorsqu'on mappe depuis les entités.

(Ce document est volontairement concis — pour plus de détails, voir les controllers et services dans le code.)

