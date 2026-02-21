# Copilot Instructions - NML Online

## 📋 Vue d'ensemble du projet

**NML Online** est un jeu de stratégie en ligne permettant aux joueurs de conquérir des territoires, gérer des armées et combattre. Le projet est une application full-stack composée de :

- **Backend** : Spring Boot 3.5 (Java 21) avec JPA/H2
- **Frontend** : Angular 21 avec NgRx pour la gestion d'état et Angular Material pour l'UI

---

## 🏗️ Architecture du projet

```
nmlOnline/
├── nml-ms/                    # Backend Spring Boot
│   └── src/main/java/com/mg/nmlonline/
│       ├── api/               # Controllers, DTOs, exceptions
│       ├── config/            # Configuration Spring (Security, CORS)
│       ├── domain/model/      # Entités JPA (fusionnées avec le domaine)
│       ├── domain/service/    # Services métier
│       ├── infrastructure/    # Repositories JPA, loaders CSV
│       └── mapper/            # Mappers Domain <-> DTO
│
├── nml-ui-bst-angular/        # Frontend Angular
│   └── src/app/
│       ├── components/        # Composants réutilisables
│       ├── guards/            # Route guards (auth)
│       ├── models/            # Interfaces TypeScript
│       ├── pages/             # Pages (carte, joueur, boutique, regles)
│       ├── services/          # ApiService, auth.interceptor
│       └── store/             # NgRx (auth, player, shop)
│
├── Dockerfile                 # Build multi-stage
└── pom.xml                    # POM parent Maven
```

---

## 🔧 Technologies

| Backend | Frontend |
|---------|----------|
| Java 21 | Angular 21 (standalone) |
| Spring Boot 3.5.6 | NgRx 21 |
| Spring Data JPA / H2 | Angular Material 21 |
| Spring Security (JWT) | RxJS 7.8, TypeScript 5.9 |
| Lombok, Jackson | |

---

## 📐 Conventions Backend

### Nommage
- **Controllers** : `<Feature>Controller.java` dans `api/controller/`
- **Services** : `<Feature>Service.java` dans `domain/service/`
- **Repositories** : `<Feature>Repository.java` dans `infrastructure/repository/`
- **DTOs** : `<Feature>Dto.java` dans `api/dto/`

### Pattern Entité JPA
```java
@Entity
@Table(name = "TABLE_NAME")
@Data
@NoArgsConstructor
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore  // Éviter les boucles JSON
    private ParentEntity parent;
    
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChildEntity> children = new ArrayList<>();
}
```

### Pattern Mapper
```java
@Component
public class FeatureMapper {
    public FeatureDto toDto(Feature domain) { /* ... */ }
    public Feature toDomain(FeatureDto dto) { /* ... */ }
}
```

---

## 📐 Conventions Frontend

### Composants (standalone uniquement)
```typescript
@Component({
  selector: 'app-example',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  templateUrl: './example.component.html',
  styleUrls: ['./example.component.scss']
})
export class ExampleComponent {
  private readonly store = inject(Store);
  private readonly apiService = inject(ApiService);
}
```

### NgRx - Structure d'un slice
```
store/<feature>/
├── <feature>.actions.ts    # createActionGroup
├── <feature>.reducer.ts    # createReducer
├── <feature>.selectors.ts  # createSelector
└── <feature>.effects.ts    # createEffect
```

### NgRx - Pattern Actions
```typescript
export const FeatureActions = createActionGroup({
  source: 'Feature',
  events: {
    'Fetch Data': emptyProps(),
    'Fetch Data Success': props<{ data: DataType[] }>(),
    'Fetch Data Failure': props<{ error: string }>(),
  },
});
```

### NgRx - Pattern Reducer
```typescript
export interface FeatureState {
  data: DataType[];
  loading: boolean;
  error: string | null;
}

export const featureReducer = createReducer(
  initialState,
  on(FeatureActions.fetchData, (state) => ({ ...state, loading: true })),
  on(FeatureActions.fetchDataSuccess, (state, { data }) => ({ ...state, data, loading: false })),
  on(FeatureActions.fetchDataFailure, (state, { error }) => ({ ...state, error, loading: false })),
);
```

### NgRx - Pattern Effects
```typescript
@Injectable()
export class FeatureEffects {
  private actions$ = inject(Actions);
  private apiService = inject(ApiService);

  fetchData$ = createEffect(() =>
    this.actions$.pipe(
      ofType(FeatureActions.fetchData),
      exhaustMap(() =>
        this.apiService.getData().pipe(
          map((data) => FeatureActions.fetchDataSuccess({ data })),
          catchError((error) => of(FeatureActions.fetchDataFailure({ error: error.message })))
        )
      )
    )
  );
}
```

### NgRx - Pattern Selectors
```typescript
export const selectFeatureState = (state: AppState) => state.feature;
export const selectData = createSelector(selectFeatureState, (state) => state.data);
export const selectLoading = createSelector(selectFeatureState, (state) => state.loading);
```

### Signals Angular
```typescript
loading = signal(true);
board = signal<Board | null>(null);
allSectors = computed(() => this.board() ? Object.values(this.board()!.sectors) : []);
```

### SCSS - Styles partagés
Le fichier `src/styles/_shared.scss` contient des variables et mixins réutilisables.

**Import dans un composant :**
```scss
@use '../../../styles/shared' as shared;

.container {
  @include shared.page-container;
}

.title {
  @include shared.gradient-title;
}
```

**Variables disponibles :**
- `$primary-gradient` : Gradient principal violet
- `$primary-color` : Couleur primaire `#6366f1`
- `$text-muted` : Texte grisé `#64748b`

**Mixins disponibles :**
- `page-container` : Container de page (max-width, padding)
- `loading-container` : Centrage du spinner de chargement
- `page-header` : Header de page avec gap
- `avatar($size, $icon-size)` : Avatar circulaire avec gradient
- `gradient-title` : Titre avec gradient
- `card` : Style de carte Material
- `section-header` : En-tête de section avec icône
- `hover-lift` : Animation de survol
- `error-alert` : Alerte d'erreur stylisée

---

## 🎮 Entités métier

| Entité | Description | Clé |
|--------|-------------|-----|
| **Board** | Carte du jeu | `id` |
| **Sector** | Territoire (clé composite) | `board_id + number` |
| **Player** | Joueur avec stats/équipements | `id` |
| **Unit** | Unité militaire dans un secteur | `id` |
| **Equipment** | Équipement assignable | `id` |

### Relations clés
- `Sector.ownerId` → `Player.id` (source unique de vérité pour la propriété)
- `Board.sectors` : Map transient initialisée via `@PostLoad`

---

## 🔐 Authentification JWT

1. **Login** : `POST /api/login` → accessToken + refreshToken (cookie HttpOnly)
2. **Token** : Stocké dans `localStorage`
3. **Refresh** : `POST /api/auth/refresh` → Nouveau accessToken
4. **Interceptor** : Ajoute le token et gère le refresh sur 401

---

## 🛡️ Sécurité & Autorisation

### Architecture de sécurité
- `SecurityConfig` active `@EnableMethodSecurity` pour le support de `@PreAuthorize`
- `JwtAuthenticationFilter` peuple le `SecurityContext` et stocke le `userId` dans `request.setAttribute("userId", claims.userId())`
- Le rôle `ROLE_ADMIN` est attribué automatiquement si le JWT contient `role: "ADMIN"`

### 3 niveaux d'accès

| Niveau | Mécanisme | Quand l'utiliser |
|--------|-----------|------------------|
| **Public** | `permitAll()` dans `SecurityConfig` | Login, register, refresh, assets |
| **Authentifié (ownership)** | `authenticated()` + vérification du `userId` dans le controller | Actions du joueur sur SES propres données (vendre ressources, gérer troupes/équipements) |
| **Admin** | `@PreAuthorize("hasRole('ADMIN')")` sur le endpoint | Opérations destructives ou de gestion (CRUD players, boards, equipment catalogue) |

### Pattern Endpoint Joueur (ownership check)
Pour tout endpoint où un joueur agit sur ses propres données, **toujours** vérifier l'ownership via le `userId` du JWT. Ne **jamais** utiliser `@PreAuthorize("hasRole('ADMIN')")` pour ces endpoints.

```java
@PostMapping("/resources/sell/{resourceId}")
public ResponseEntity<?> sellResource(@PathVariable Long resourceId,
                                       @RequestParam("quantity") int quantity,
                                       HttpServletRequest request) {
    // 1. Extraire l'identité du joueur authentifié
    Long userId = (Long) request.getAttribute("userId");
    if (userId == null) {
        return ResponseEntity.status(401).body("User not authenticated");
    }
    // 2. Passer le userId au service pour vérification d'ownership
    service.doAction(resourceId, quantity, userId);
}
```

```java
// Dans le service : vérifier que la ressource appartient au joueur
Player owner = entity.getPlayer();
if (owner == null || !authenticatedUserId.equals(owner.getId())) {
    throw new SecurityException("Access denied: resource does not belong to authenticated user");
}
```

### Pattern Endpoint Admin
Pour les opérations de gestion/destruction, ajouter `@PreAuthorize` directement sur la méthode du controller.
Les endpoints admin sont regroupés dans `AdminController` (`/api/admin/**`), protégé à la fois par :
- `SecurityConfig` : `.requestMatchers("/api/admin/**").hasRole("ADMIN")` (URL-level)
- `@PreAuthorize("hasRole('ADMIN')")` au niveau de la **classe** (défense en profondeur)

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/{id}")
public void delete(@PathVariable Long id) { /* ... */ }
```

### Séparation des controllers

| Controller | Responsabilité | Endpoints |
|------------|---------------|-----------|
| `PlayerController` | Opérations du joueur sur SES données | `GET /api/players`, `GET /api/players/{name}`, `POST /api/players/resources/sell/{id}` |
| `AdminController` | CRUD admin (import/export/suppression joueurs) | `/api/admin/**` |
| `BoardController` | Lecture boards (joueur) + CRUD boards (admin) | `GET /api/boards/**`, `POST/DELETE /api/boards` (admin) |
| `EquipmentController` | Lecture catalogue (joueur) + CRUD catalogue (admin) | `GET /api/equipment/**`, `POST/DELETE /api/equipment` (admin) |

**Règle** : Ne **jamais** dupliquer un endpoint admin dans un controller joueur. Les opérations admin doivent passer par `AdminController` ou être protégées par `@PreAuthorize("hasRole('ADMIN')")` dans le controller métier.

### Endpoints protégés actuels

| Endpoint | Protection | Raison |
|----------|-----------|--------|
| `POST /api/players/resources/sell/{id}` | Ownership | Le joueur vend SES ressources |
| `POST /api/boards` | `ADMIN` | Création de board |
| `DELETE /api/boards/{id}` | `ADMIN` | Suppression de board |
| `PUT /api/boards/.../owner` | `ADMIN` | Réassignation de secteur |
| `POST /api/equipment` | `ADMIN` | Création d'équipement catalogue |
| `DELETE /api/equipment/{id}` | `ADMIN` | Suppression d'équipement catalogue |
| `/api/admin/**` | `ADMIN` (classe) | Import/export/suppression joueurs |
| `GET /api/**` | Authentifié | Lecture pour tout joueur connecté |

### Règles pour les futurs endpoints

1. **Le joueur gère ses propres données** (troupes, équipements, déplacements) → **Ownership check** via `HttpServletRequest` + `userId`, pas de `@PreAuthorize`
2. **Opération admin** (CRUD catalogue, gestion monde, suppression joueurs) → **`@PreAuthorize("hasRole('ADMIN')")`**
3. **Ne jamais** accepter un `playerId` depuis le body/params pour une action joueur — toujours utiliser le `userId` du JWT
4. **SecurityException** → 403, **RuntimeException** → 404, **IllegalArgumentException** → 400

---

## 📝 Créer une fonctionnalité

### Backend
1. Entité dans `domain/model/<feature>/`
2. DTO dans `api/dto/`
3. Mapper dans `mapper/`
4. Repository dans `infrastructure/repository/`
5. Service dans `domain/service/`
6. Controller dans `api/controller/`

### Frontend
1. Interface dans `models/index.ts`
2. Méthode API dans `services/api.service.ts`
3. Slice NgRx dans `store/<feature>/` (actions, reducer, selectors, effects)
4. Enregistrer reducer dans `store/index.ts`
5. Enregistrer effects dans `app.config.ts`
6. Page dans `pages/<feature>/`
7. Route dans `app.routes.ts`

---

## 🚀 Commandes

| Backend (`nml-ms/`) | Frontend (`nml-ui-bst-angular/`) |
|---------------------|----------------------------------|
| `./mvnw spring-boot:run` | `npm start` |
| `./mvnw test` | `npm test` |
| `./mvnw clean package` | `npm run build` |

**Docker** : `docker build -t nml-online . && docker run -p 8080:8080 nml-online`

---

## ⚠️ Points d'attention

1. **Single Source of Truth** : Propriété des secteurs via `Sector.ownerId`
2. **Relations JPA** : `@JsonIgnore` côté "many" pour éviter les boucles
3. **Maps transient** : `Board.sectors` initialisée par `@PostLoad`
4. **NgRx** : État immutable, utiliser le spread operator
5. **Lazy loading** : Préférer `loadComponent` pour les pages

---

## 📁 Fichiers clés

| Fichier | Rôle |
|---------|------|
| `nml-ms/.../application.properties` | Config Spring |
| `nml-ui-bst-angular/proxy.conf.json` | Proxy dev API |
| `nml-ui-bst-angular/.../app.config.ts` | Providers Angular |
| `nml-ui-bst-angular/.../store/index.ts` | Export store NgRx |
| `nml-ui-bst-angular/.../models/index.ts` | Types TypeScript |
| `nml-ui-bst-angular/src/styles/_shared.scss` | Variables et mixins SCSS partagés |

---

## 📚 Documentation

- `nml-ui-bst-angular/GUIDE_ANGULAR_POUR_DEVS_REACT.md` : Guide React → Angular

## 🌐 Langue

Toutes les interactions avec GitHub Copilot doivent se faire **en français**. Les réponses sont générées en français par défaut.