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