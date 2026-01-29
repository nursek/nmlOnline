# 🎓 Guide complet : Apprendre Angular en venant de React

Ce guide explique les concepts Angular en les comparant à React, basé sur la migration du projet `nml-ui-copilot` (React) vers `nml-ui-copilot-angular`.

---

## 📚 Table des matières

1. [Vue d'ensemble des différences](#1-vue-densemble-des-différences)
2. [Les librairies utilisées](#2-les-librairies-utilisées)
3. [Structure du projet](#3-structure-du-projet)
4. [Les concepts clés](#4-les-concepts-clés)
   - [4.1 Les Composants](#41-les-composants)
   - [4.2 Le Data Binding](#42-le-data-binding-liaison-de-données)
   - [4.3 Les Directives](#43-les-directives-équivalent-des-conditionsboucles-jsx)
   - [4.4 L'Injection de Dépendances](#44-linjection-de-dépendances-di)
   - [4.5 Les Services](#45-les-services)
   - [4.6 Les Observables (RxJS)](#46-les-observables-rxjs-vs-promises)
   - [4.7 NgRx vs Redux Toolkit](#47-ngrx-vs-redux-toolkit)
   - [4.8 Le Routing](#48-le-routing)
   - [4.9 Les Guards](#49-les-guards-protection-de-routes)
   - [4.10 Les Intercepteurs HTTP](#410-les-intercepteurs-http)
5. [Comparaison du code React vs Angular](#5-comparaison-du-code-react-vs-angular)
6. [Résumé des points clés](#6-résumé-des-points-clés)

---

## 1. Vue d'ensemble des différences

| Concept | React | Angular |
|---------|-------|---------|
| Type | Librairie UI | Framework complet |
| Langage | JSX (JS + HTML mélangé) | TypeScript + Templates HTML séparés |
| Gestion d'état | Redux / Zustand / Context | NgRx (basé sur Redux) |
| Routing | react-router-dom | @angular/router (intégré) |
| HTTP | axios / fetch | HttpClient (intégré) |
| Styles | CSS-in-JS, styled-components | SCSS avec encapsulation |
| Composants | Fonctions | Classes ou Standalone functions |

---

## 2. Les librairies utilisées

### **@angular/core** - Le cœur d'Angular

```typescript
// C'est comme React lui-même
import { Component, inject, OnInit } from '@angular/core';
```

Fournit : composants, injection de dépendances, lifecycle hooks.

### **@angular/material** - Équivalent de MUI (Material-UI)

```typescript
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
```

Composants UI pré-stylés : boutons, cards, inputs, dialogs, etc.

### **@angular/router** - Équivalent de react-router-dom

```typescript
import { RouterOutlet, RouterLink } from '@angular/router';
```

Gestion des routes, navigation, lazy loading.

### **@ngrx/store** - Équivalent de Redux Toolkit

```typescript
import { Store } from '@ngrx/store';
import { createAction, createReducer } from '@ngrx/store';
```

Gestion d'état globale avec le pattern Redux.

### **@ngrx/effects** - Pour les side effects (comme redux-thunk/saga)

```typescript
import { Actions, createEffect, ofType } from '@ngrx/effects';
```

Gère les appels API et actions asynchrones.

### **RxJS** - Librairie de programmation réactive

```typescript
import { Observable, map, catchError, switchMap } from 'rxjs';
```

**C'est LA grosse différence avec React !** Angular utilise des Observables partout.

---

## 3. Structure du projet

```
nml-ui-copilot-angular/
├── angular.json          # Config du projet (comme vite.config.ts)
├── tsconfig.json         # Config TypeScript
├── proxy.conf.json       # Proxy pour l'API (comme Vite proxy)
│
├── src/
│   ├── main.ts           # Point d'entrée (comme main.tsx)
│   ├── index.html        # HTML principal
│   ├── styles.scss       # Styles globaux
│   │
│   ├── environments/     # Config par environnement
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   │
│   ├── app/
│   │   ├── app.ts              # Composant racine (comme App.tsx)
│   │   ├── app.routes.ts       # Définition des routes
│   │   ├── app.config.ts       # Configuration de l'app
│   │   │
│   │   ├── models/             # Types TypeScript (comme types/)
│   │   │   └── index.ts
│   │   │
│   │   ├── services/           # Services HTTP (comme services/)
│   │   │   ├── api.service.ts
│   │   │   └── auth.interceptor.ts
│   │   │
│   │   ├── store/              # NgRx (comme store/)
│   │   │   ├── index.ts
│   │   │   ├── auth/
│   │   │   │   ├── auth.actions.ts
│   │   │   │   ├── auth.reducer.ts
│   │   │   │   ├── auth.selectors.ts
│   │   │   │   └── auth.effects.ts
│   │   │   ├── player/
│   │   │   └── shop/
│   │   │
│   │   ├── guards/             # Protection de routes
│   │   │   └── auth.guard.ts
│   │   │
│   │   ├── components/         # Composants réutilisables
│   │   │   └── navbar/
│   │   │
│   │   └── pages/              # Pages (comme pages/)
│   │       ├── login/
│   │       ├── carte/
│   │       ├── joueur/
│   │       ├── boutique/
│   │       └── regles/
```

### Comparaison React vs Angular

| React (nml-ui-copilot) | Angular (nml-ui-copilot-angular) |
|------------------------|----------------------------------|
| `src/types/index.ts` | `src/app/models/index.ts` |
| `src/services/api.ts` | `src/app/services/api.service.ts` |
| `src/store/authSlice.ts` | `src/app/store/auth/*.ts` (4 fichiers) |
| `src/components/Navbar.tsx` | `src/app/components/navbar/navbar.component.ts` |
| `src/pages/LoginPage.tsx` | `src/app/pages/login/login.component.ts` |

---

## 4. Les concepts clés

### 4.1 Les Composants

**React** : Une fonction qui retourne du JSX

```tsx
// React - LoginPage.tsx
import { useState } from 'react';

function LoginPage() {
  const [email, setEmail] = useState('');
  
  return (
    <div>
      <input 
        value={email} 
        onChange={e => setEmail(e.target.value)} 
      />
    </div>
  );
}

export default LoginPage;
```

**Angular** : Un décorateur `@Component` + classe

```typescript
// Angular - login.component.ts
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-login',           // Nom de la balise HTML <app-login>
  standalone: true,                // Composant autonome (Angular 17+)
  imports: [FormsModule],          // Modules/composants utilisés
  template: `
    <div>
      <input [(ngModel)]="email" />
    </div>
  `,
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email = '';  // Propriété (équivalent useState)
}
```

### Points clés :

| Aspect | React | Angular |
|--------|-------|---------|
| Définition | Fonction | Classe avec `@Component` |
| État local | `useState()` | Propriétés de classe |
| Template | JSX dans le return | Template séparé ou `template:` |
| Export | `export default` | `export class` |

---

### 4.2 Le Data Binding (liaison de données)

| Type | React | Angular | Description |
|------|-------|---------|-------------|
| Interpolation | `{value}` | `{{value}}` | Afficher une valeur |
| Property binding | `attr={value}` | `[attr]="value"` | Lier un attribut |
| Event binding | `onClick={fn}` | `(click)="fn()"` | Écouter un événement |
| Two-way binding | `value + onChange` | `[(ngModel)]="value"` | Liaison bidirectionnelle |

**Exemple concret :**

```tsx
// React
<button 
  disabled={isLoading} 
  className={isActive ? 'active' : ''} 
  onClick={() => handleSubmit()}
>
  {isLoading ? 'Chargement...' : 'Connexion'}
</button>
```

```html
<!-- Angular -->
<button 
  [disabled]="isLoading" 
  [class.active]="isActive" 
  (click)="handleSubmit()"
>
  {{isLoading ? 'Chargement...' : 'Connexion'}}
</button>
```

---

### 4.3 Les Directives (équivalent des conditions/boucles JSX)

**React** : Logique JS dans le JSX

```tsx
// Condition
{isLoggedIn && <UserMenu />}
{isLoggedIn ? <Dashboard /> : <Login />}

// Boucle
{items.map(item => (
  <Item key={item.id} data={item} />
))}
```

**Angular** : Directives structurelles (nouvelle syntaxe Angular 17+)

```html
<!-- Condition avec @if -->
@if (isLoggedIn) {
  <app-user-menu />
}

@if (isLoggedIn) {
  <app-dashboard />
} @else {
  <app-login />
}

<!-- Boucle avec @for -->
@for (item of items; track item.id) {
  <app-item [data]="item" />
}

<!-- Switch avec @switch -->
@switch (status) {
  @case ('loading') {
    <app-spinner />
  }
  @case ('error') {
    <app-error />
  }
  @default {
    <app-content />
  }
}
```

> **Note** : Avant Angular 17, on utilisait `*ngIf`, `*ngFor`, `*ngSwitch` (encore supportés).

---

### 4.4 L'Injection de Dépendances (DI)

**C'est un concept fondamental d'Angular qui n'existe pas vraiment en React !**

**React** : Tu importes directement

```tsx
// React
import { api } from '../services/api';
import { useDispatch, useSelector } from 'react-redux';

function MyComponent() {
  const dispatch = useDispatch();
  const user = useSelector(state => state.auth.user);
  
  const handleClick = () => {
    api.getUsers();  // Import direct
  };
}
```

**Angular** : Tu **injectes** les dépendances

```typescript
// Angular
import { Component, inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { ApiService } from '../services/api.service';

@Component({...})
export class MyComponent {
  // Méthode moderne (Angular 14+) - RECOMMANDÉE
  private api = inject(ApiService);
  private store = inject(Store);
  
  // OU via le constructeur (ancienne méthode)
  constructor(
    private api: ApiService,
    private store: Store
  ) {}
  
  handleClick() {
    this.api.getUsers();  // Utilise le service injecté
  }
}
```

**Pourquoi l'injection de dépendances ?**

1. **Testabilité** : Facile de mocker les services dans les tests
2. **Singleton** : Une seule instance partagée dans toute l'app
3. **Découplage** : Le composant ne sait pas comment le service est créé
4. **Flexibilité** : On peut changer l'implémentation sans modifier les composants

---

### 4.5 Les Services

**React** : Simples modules JS avec des fonctions

```typescript
// React - services/api.ts
import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
});

export const api = {
  login: (email: string, password: string) => 
    apiClient.post('/login', { email, password }),
    
  getPlayers: () => 
    apiClient.get('/players'),
};
```

**Angular** : Classes avec le décorateur `@Injectable`

```typescript
// Angular - services/api.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ 
  providedIn: 'root'  // Disponible partout, singleton automatique
})
export class ApiService {
  private http = inject(HttpClient);
  private baseUrl = '/api';

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, { email, password });
  }

  getPlayers(): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.baseUrl}/players`);
  }
}
```

### Points clés :

| Aspect | React | Angular |
|--------|-------|---------|
| Définition | Module avec fonctions | Classe avec `@Injectable` |
| HTTP client | axios (externe) | HttpClient (intégré) |
| Retour | Promise | Observable |
| Singleton | Manuel | Automatique avec `providedIn: 'root'` |

---

### 4.6 Les Observables (RxJS) vs Promises

**C'est le concept le plus déroutant quand on vient de React !**

#### Différences fondamentales

| Promise | Observable |
|---------|------------|
| S'exécute immédiatement | S'exécute au `subscribe()` |
| Émet une seule valeur | Peut émettre plusieurs valeurs |
| Non annulable | Annulable avec `unsubscribe()` |
| Natif JavaScript | Librairie RxJS |
| `async/await` | Opérateurs (`map`, `filter`, etc.) |

#### Exemple : Appel API

**React avec Promise**

```typescript
// React
const [users, setUsers] = useState([]);
const [loading, setLoading] = useState(false);

useEffect(() => {
  const fetchUsers = async () => {
    setLoading(true);
    try {
      const response = await api.getUsers();
      setUsers(response.data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };
  
  fetchUsers();
}, []);
```

**Angular avec Observable - Option 1 : Subscribe**

```typescript
// Angular - Dans le composant
export class UsersComponent implements OnInit {
  private api = inject(ApiService);
  
  users: User[] = [];
  loading = false;

  ngOnInit() {
    this.loading = true;
    this.api.getUsers().subscribe({
      next: (users) => {
        this.users = users;
        this.loading = false;
      },
      error: (error) => {
        console.error(error);
        this.loading = false;
      }
    });
  }
}
```

**Angular avec Observable - Option 2 : Async Pipe (RECOMMANDÉ)**

```typescript
// Angular - Composant
export class UsersComponent {
  private api = inject(ApiService);
  
  users$ = this.api.getUsers();  // Observable, pas encore exécuté !
}
```

```html
<!-- Template -->
@if (users$ | async; as users) {
  @for (user of users; track user.id) {
    <div>{{user.name}}</div>
  }
} @else {
  <app-loading />
}
```

> **Avantage du `| async`** : Angular gère automatiquement le `subscribe` et `unsubscribe` !

#### Opérateurs RxJS utiles

```typescript
import { map, filter, catchError, switchMap, tap } from 'rxjs/operators';
import { of } from 'rxjs';

// Transformer les données
this.api.getUsers().pipe(
  map(users => users.filter(u => u.isActive)),  // Filtrer les actifs
  tap(users => console.log('Users:', users)),    // Debug (side effect)
  catchError(error => {
    console.error(error);
    return of([]);  // Retourner un tableau vide en cas d'erreur
  })
);

// Chaîner des appels
this.api.getUser(id).pipe(
  switchMap(user => this.api.getOrders(user.id))  // Appel suivant
);
```

---

### 4.7 NgRx vs Redux Toolkit

La structure est très similaire, mais la syntaxe diffère.

#### Actions

**Redux Toolkit** : Actions + Thunks combinés

```typescript
// React - store/authSlice.ts
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';

export const login = createAsyncThunk(
  'auth/login',
  async (credentials: { email: string; password: string }, { rejectWithValue }) => {
    try {
      const response = await api.login(credentials.email, credentials.password);
      return response.data;
    } catch (error) {
      return rejectWithValue(error.message);
    }
  }
);
```

**NgRx** : Actions séparées

```typescript
// Angular - store/auth/auth.actions.ts
import { createActionGroup, props, emptyProps } from '@ngrx/store';

export const AuthActions = createActionGroup({
  source: 'Auth',
  events: {
    'Login': props<{ email: string; password: string }>(),
    'Login Success': props<{ user: User; token: string }>(),
    'Login Failure': props<{ error: string }>(),
    'Logout': emptyProps(),
  },
});

// Utilisation : AuthActions.login({ email, password })
```

#### Reducer

**Redux Toolkit**

```typescript
// React
const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    logout: (state) => {
      state.user = null;
      state.token = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.loading = true;
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false;
        state.user = action.payload.user;
        state.token = action.payload.token;
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false;
        state.error = action.payload;
      });
  },
});
```

**NgRx**

```typescript
// Angular - store/auth/auth.reducer.ts
import { createReducer, on } from '@ngrx/store';
import { AuthActions } from './auth.actions';

export const authReducer = createReducer(
  initialState,
  
  on(AuthActions.login, (state) => ({
    ...state,
    loading: true,
    error: null,
  })),
  
  on(AuthActions.loginSuccess, (state, { user, token }) => ({
    ...state,
    loading: false,
    user,
    token,
    isAuthenticated: true,
  })),
  
  on(AuthActions.loginFailure, (state, { error }) => ({
    ...state,
    loading: false,
    error,
  })),
  
  on(AuthActions.logout, () => initialState),
);
```

#### Effects (équivalent des Thunks)

```typescript
// Angular - store/auth/auth.effects.ts
import { Injectable, inject } from '@angular/core';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { of } from 'rxjs';
import { map, exhaustMap, catchError, tap } from 'rxjs/operators';

@Injectable()
export class AuthEffects {
  private actions$ = inject(Actions);
  private api = inject(ApiService);
  private router = inject(Router);

  // Effect pour le login
  login$ = createEffect(() =>
    this.actions$.pipe(
      ofType(AuthActions.login),                    // Écoute l'action login
      exhaustMap(({ email, password }) =>
        this.api.login(email, password).pipe(
          map(response => AuthActions.loginSuccess(response)),
          catchError(error => of(AuthActions.loginFailure({ error: error.message })))
        )
      )
    )
  );

  // Effect pour rediriger après login
  loginSuccess$ = createEffect(
    () =>
      this.actions$.pipe(
        ofType(AuthActions.loginSuccess),
        tap(() => this.router.navigate(['/dashboard']))
      ),
    { dispatch: false }  // Pas de nouvelle action à dispatcher
  );
}
```

#### Sélecteurs

```typescript
// React
export const selectUser = (state: RootState) => state.auth.user;
export const selectIsLoading = (state: RootState) => state.auth.loading;

// Angular (identique)
import { createFeatureSelector, createSelector } from '@ngrx/store';

export const selectAuthState = createFeatureSelector<AuthState>('auth');
export const selectUser = createSelector(selectAuthState, state => state.user);
export const selectIsLoading = createSelector(selectAuthState, state => state.loading);
```

#### Utilisation dans un composant

**React**

```tsx
import { useDispatch, useSelector } from 'react-redux';
import { login } from '../store/authSlice';

function LoginPage() {
  const dispatch = useDispatch();
  const user = useSelector(selectUser);
  const loading = useSelector(selectIsLoading);

  const handleSubmit = () => {
    dispatch(login({ email, password }));
  };
}
```

**Angular**

```typescript
import { Store } from '@ngrx/store';
import { AuthActions, selectUser, selectIsLoading } from '../store';

@Component({...})
export class LoginComponent {
  private store = inject(Store);

  user$ = this.store.select(selectUser);        // Observable !
  loading$ = this.store.select(selectIsLoading);

  handleSubmit() {
    this.store.dispatch(AuthActions.login({ email: this.email, password: this.password }));
  }
}
```

```html
<!-- Template avec async pipe -->
@if (loading$ | async) {
  <app-spinner />
}

@if (user$ | async; as user) {
  <p>Bienvenue {{user.name}}</p>
}
```

---

### 4.8 Le Routing

**React Router**

```tsx
// Configuration (dans App.tsx)
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';

function App() {
  return (
    <BrowserRouter>
      <Navbar />
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/joueur" element={
          <ProtectedRoute>
            <JoueurPage />
          </ProtectedRoute>
        } />
        <Route path="/" element={<Navigate to="/carte" />} />
      </Routes>
    </BrowserRouter>
  );
}

// Navigation programmatique
import { useNavigate } from 'react-router-dom';
const navigate = useNavigate();
navigate('/login');
```

**Angular Router**

```typescript
// app.routes.ts
import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { 
    path: 'login', 
    loadComponent: () => import('./pages/login/login.component')
      .then(m => m.LoginComponent)  // Lazy loading !
  },
  { 
    path: 'joueur', 
    loadComponent: () => import('./pages/joueur/joueur.component')
      .then(m => m.JoueurComponent),
    canActivate: [authGuard]  // Protection de route
  },
  { path: '', redirectTo: 'carte', pathMatch: 'full' },
  { path: '**', redirectTo: 'carte' }  // Wildcard pour 404
];

// Navigation programmatique
import { Router } from '@angular/router';
private router = inject(Router);
this.router.navigate(['/login']);

// Ou avec des paramètres
this.router.navigate(['/user', userId]);
this.router.navigate(['/search'], { queryParams: { q: 'test' } });
```

```html
<!-- Template - Liens -->
<a routerLink="/login">Login</a>
<a [routerLink]="['/user', user.id]">Profil</a>

<!-- Classe active -->
<a routerLink="/login" routerLinkActive="active">Login</a>

<!-- Outlet (équivalent de <Outlet /> en React Router) -->
<router-outlet></router-outlet>
```

---

### 4.9 Les Guards (Protection de routes)

**React** : Composant wrapper

```tsx
// React - components/ProtectedRoute.tsx
import { Navigate } from 'react-router-dom';
import { useSelector } from 'react-redux';

function ProtectedRoute({ children }) {
  const isAuthenticated = useSelector(state => state.auth.isAuthenticated);
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return children;
}

// Utilisation
<Route path="/dashboard" element={
  <ProtectedRoute>
    <Dashboard />
  </ProtectedRoute>
} />
```

**Angular** : Guard fonctionnel

```typescript
// Angular - guards/auth.guard.ts
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { map, take } from 'rxjs/operators';
import { selectIsAuthenticated } from '../store';

export const authGuard: CanActivateFn = (route, state) => {
  const store = inject(Store);
  const router = inject(Router);

  return store.select(selectIsAuthenticated).pipe(
    take(1),  // Prend une seule valeur et complete
    map(isAuthenticated => {
      if (isAuthenticated) {
        return true;  // Autoriser l'accès
      }
      router.navigate(['/login']);
      return false;  // Bloquer l'accès
    })
  );
};

// Utilisation dans les routes
{ 
  path: 'dashboard', 
  component: DashboardComponent,
  canActivate: [authGuard]  // Applique le guard
}
```

---

### 4.10 Les Intercepteurs HTTP

**Concept qui n'existe pas directement en React** (tu le fais manuellement avec axios interceptors)

**React avec Axios**

```typescript
// React - services/api.ts
import axios from 'axios';

const apiClient = axios.create({ baseURL: '/api' });

// Intercepteur pour ajouter le token
apiClient.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercepteur pour gérer le refresh token
apiClient.interceptors.response.use(
  response => response,
  async error => {
    if (error.response?.status === 401) {
      // Refresh token logic...
    }
    return Promise.reject(error);
  }
);
```

**Angular avec HttpInterceptor**

```typescript
// Angular - services/auth.interceptor.ts
import { HttpInterceptorFn, HttpHandlerFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { switchMap, take } from 'rxjs/operators';
import { selectToken } from '../store';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(Store);

  return store.select(selectToken).pipe(
    take(1),
    switchMap(token => {
      if (token) {
        // Clone la requête et ajoute le header
        const authReq = req.clone({
          setHeaders: { Authorization: `Bearer ${token}` }
        });
        return next(authReq);
      }
      return next(req);
    })
  );
};

// Enregistrement dans app.config.ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';

export const appConfig = {
  providers: [
    provideHttpClient(withInterceptors([authInterceptor])),
  ]
};
```

---

## 5. Comparaison du code React vs Angular

### Page de Login complète

**React (`LoginPage.tsx`)**

```tsx
import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import { TextField, Button, Card, CircularProgress, Alert } from '@mui/material';
import { login, clearError } from '../store/authSlice';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [rememberMe, setRememberMe] = useState(false);
  
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { loading, error, isAuthenticated } = useSelector(state => state.auth);

  // Redirect si déjà connecté
  useEffect(() => {
    if (isAuthenticated) {
      navigate('/carte');
    }
  }, [isAuthenticated, navigate]);

  // Clear error on unmount
  useEffect(() => {
    return () => {
      dispatch(clearError());
    };
  }, [dispatch]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    dispatch(login({ email, password, rememberMe }));
  };

  return (
    <div className="login-page">
      <Card className="login-card">
        <form onSubmit={handleSubmit}>
          {error && (
            <Alert severity="error">{error}</Alert>
          )}
          
          <TextField
            label="Email"
            value={email}
            onChange={e => setEmail(e.target.value)}
            disabled={loading}
            required
          />
          
          <TextField
            label="Mot de passe"
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            disabled={loading}
            required
          />
          
          <Button type="submit" disabled={loading}>
            {loading ? <CircularProgress size={20} /> : 'Connexion'}
          </Button>
        </form>
      </Card>
    </div>
  );
}
```

**Angular (`login.component.ts`)**

```typescript
import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthActions } from '../../store/auth/auth.actions';
import { selectAuthLoading, selectAuthError, selectIsAuthenticated } from '../../store/auth/auth.selectors';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <div class="login-page">
      <mat-card class="login-card">
        <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
          
          @if (error$ | async; as error) {
            <div class="error-alert">{{error}}</div>
          }
          
          <mat-form-field>
            <mat-label>Email</mat-label>
            <input matInput formControlName="email" />
          </mat-form-field>
          
          <mat-form-field>
            <mat-label>Mot de passe</mat-label>
            <input matInput type="password" formControlName="password" />
          </mat-form-field>
          
          <button mat-raised-button 
                  type="submit" 
                  [disabled]="loginForm.invalid || (loading$ | async)">
            @if (loading$ | async) {
              <mat-spinner diameter="20"></mat-spinner>
            } @else {
              Connexion
            }
          </button>
        </form>
      </mat-card>
    </div>
  `,
  styleUrl: './login.component.scss'
})
export class LoginComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private store = inject(Store);
  private router = inject(Router);
  private destroy$ = new Subject<void>();

  // Observables depuis le store
  loading$ = this.store.select(selectAuthLoading);
  error$ = this.store.select(selectAuthError);

  // Formulaire réactif (alternative à useState pour chaque champ)
  loginForm = this.fb.group({
    email: ['', Validators.required],
    password: ['', Validators.required],
    rememberMe: [false],
  });

  ngOnInit(): void {
    // Redirect si déjà connecté
    this.store.select(selectIsAuthenticated)
      .pipe(takeUntil(this.destroy$))
      .subscribe(isAuth => {
        if (isAuth) {
          this.router.navigate(['/carte']);
        }
      });
  }

  ngOnDestroy(): void {
    // Cleanup (équivalent du return dans useEffect)
    this.destroy$.next();
    this.destroy$.complete();
    this.store.dispatch(AuthActions.clearError());
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.store.dispatch(AuthActions.login({
        credentials: this.loginForm.value
      }));
    }
  }
}
```

---

## 6. Résumé des points clés

### Checklist pour un dev React qui apprend Angular

| ✅ Concept | React | Angular |
|-----------|-------|---------|
| **Composants** | Fonctions avec hooks | Classes avec `@Component` |
| **État local** | `useState()` | Propriétés de classe ou Signals |
| **Side effects** | `useEffect()` | `ngOnInit()`, `ngOnDestroy()` |
| **Props** | `props.value` | `@Input() value` |
| **Events** | `props.onClick` | `@Output() click = new EventEmitter()` |
| **Templates** | JSX inline | Template séparé ou `template:` |
| **Conditions** | `{condition && <Comp />}` | `@if (condition) { <comp /> }` |
| **Boucles** | `{arr.map(x => <Comp />)}` | `@for (x of arr; track x.id) { <comp /> }` |
| **Services** | Modules importés | Classes `@Injectable` injectées |
| **HTTP** | axios/fetch → Promise | HttpClient → Observable |
| **State global** | Redux/Zustand | NgRx Store |
| **Async actions** | Thunks/Sagas | NgRx Effects |
| **Routing** | react-router-dom | @angular/router |
| **Route guards** | Composant wrapper | `canActivate` guard |
| **HTTP interceptors** | axios interceptors | `HttpInterceptorFn` |

### Les 5 choses les plus importantes à retenir

1. **Observables partout** : Utilise `| async` dans les templates au lieu de subscribe manuellement

2. **Injection de dépendances** : Utilise `inject(Service)` au lieu d'importer directement

3. **Standalone components** : Angular moderne (17+) n'a plus besoin de NgModules

4. **Formulaires réactifs** : Préfère `FormBuilder` à `[(ngModel)]` pour les formulaires complexes

5. **Déclaration des imports** : Chaque composant déclare ses dépendances dans `imports: []`

---

## Ressources pour aller plus loin

- [Documentation officielle Angular](https://angular.dev)
- [NgRx Documentation](https://ngrx.io)
- [RxJS Guide](https://rxjs.dev/guide/overview)
- [Angular Material](https://material.angular.io)

---

*Ce guide a été créé lors de la migration de `nml-ui-copilot` (React) vers `nml-ui-copilot-angular`.*
