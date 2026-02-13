import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, filter, finalize, switchMap, take, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { RefreshResponse } from '../models';

/**
 * Service singleton pour gérer le refresh des tokens JWT.
 * Gère les race conditions et le queueing des requêtes pendant un refresh.
 *
 * Protection contre le spam F5 :
 * - Utilise sessionStorage pour persister l'état entre les rechargements
 * - Cooldown de 3 secondes entre chaque refresh
 * - Si un refresh est en cours, les autres requêtes attendent le résultat
 */
@Injectable({
  providedIn: 'root'
})
export class TokenService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  // État du refresh - partagé entre toutes les requêtes de cette instance
  private isRefreshing = false;
  private refreshTokenSubject = new BehaviorSubject<string | null>(null);

  // Timeout pour le refresh (10 secondes max)
  private readonly REFRESH_TIMEOUT_MS = 10000;

  // Cooldown minimum entre les refreshes (éviter le spam F5)
  // 3 secondes = assez long pour éviter le spam, assez court pour ne pas gêner
  private readonly REFRESH_COOLDOWN_MS = 3000;

  // Clés pour le stockage
  private readonly REFRESH_LOCK_KEY = 'nml_refresh_lock';
  private readonly REFRESH_TIME_KEY = 'nml_last_refresh';

  constructor() {
    // Nettoyer les locks obsolètes au démarrage (plus vieux que 10 secondes)
    this.cleanupStaleLock();
  }

  /**
   * Nettoie un lock de refresh qui serait resté bloqué (ex: crash du navigateur)
   */
  private cleanupStaleLock(): void {
    const lockTime = sessionStorage.getItem(this.REFRESH_LOCK_KEY);
    if (lockTime) {
      const elapsed = Date.now() - parseInt(lockTime, 10);
      if (elapsed > this.REFRESH_TIMEOUT_MS) {
        // Lock trop vieux, le supprimer
        sessionStorage.removeItem(this.REFRESH_LOCK_KEY);
      }
    }
  }

  /**
   * Vérifie si un refresh est en cours (même dans un autre onglet/rechargement)
   */
  private isRefreshLocked(): boolean {
    const lockTime = sessionStorage.getItem(this.REFRESH_LOCK_KEY);
    if (!lockTime) return false;

    const elapsed = Date.now() - parseInt(lockTime, 10);
    // Lock valide pendant 10 secondes max
    return elapsed < this.REFRESH_TIMEOUT_MS;
  }

  /**
   * Vérifie si on est dans le cooldown après un refresh récent
   */
  private isInCooldown(): boolean {
    const lastRefresh = sessionStorage.getItem(this.REFRESH_TIME_KEY);
    if (!lastRefresh) return false;

    const elapsed = Date.now() - parseInt(lastRefresh, 10);
    return elapsed < this.REFRESH_COOLDOWN_MS;
  }

  /**
   * Acquiert le lock de refresh
   */
  private acquireRefreshLock(): boolean {
    if (this.isRefreshLocked()) {
      return false; // Quelqu'un d'autre a le lock
    }
    sessionStorage.setItem(this.REFRESH_LOCK_KEY, Date.now().toString());
    return true;
  }

  /**
   * Libère le lock de refresh et enregistre le temps
   */
  private releaseRefreshLock(): void {
    sessionStorage.removeItem(this.REFRESH_LOCK_KEY);
    sessionStorage.setItem(this.REFRESH_TIME_KEY, Date.now().toString());
  }

  /**
   * Récupère le token d'accès depuis le localStorage.
   */
  getAccessToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  /**
   * Stocke le token d'accès dans le localStorage.
   */
  setAccessToken(token: string): void {
    localStorage.setItem('accessToken', token);
  }

  /**
   * Supprime le token d'accès du localStorage.
   */
  removeAccessToken(): void {
    localStorage.removeItem('accessToken');
  }

  /**
   * Récupère l'utilisateur depuis le localStorage.
   */
  getUser(): { id: number; username: string } | null {
    const stored = localStorage.getItem('user');
    if (!stored) return null;

    try {
      const parsed = JSON.parse(stored);
      if (parsed && typeof parsed.id === 'number' && typeof parsed.username === 'string') {
        return parsed;
      }
    } catch {
      // Ignore parsing errors
    }

    localStorage.removeItem('user');
    return null;
  }

  /**
   * Stocke l'utilisateur dans le localStorage.
   */
  setUser(user: { id: number; username: string }): void {
    localStorage.setItem('user', JSON.stringify(user));
  }

  /**
   * Supprime l'utilisateur du localStorage.
   */
  removeUser(): void {
    localStorage.removeItem('user');
  }

  /**
   * Nettoie toutes les données d'authentification.
   */
  clearAuth(): void {
    this.removeAccessToken();
    this.removeUser();
    this.resetRefreshState();
  }

  /**
   * Réinitialise l'état du refresh.
   */
  private resetRefreshState(): void {
    this.isRefreshing = false;
    this.refreshTokenSubject.next(null);
    sessionStorage.removeItem(this.REFRESH_LOCK_KEY);
  }

  /**
   * Effectue un refresh du token.
   *
   * Protection contre le spam F5 :
   * 1. Si on est dans le cooldown (3s après dernier refresh), on utilise le token actuel
   * 2. Si un refresh est en cours, on attend son résultat
   * 3. Sinon, on lance un nouveau refresh
   *
   * @returns Observable<string> Le nouveau token d'accès
   */
  refreshToken(): Observable<string> {
    // Protection 1 : Si on est dans le cooldown et qu'on a déjà un token, l'utiliser
    if (this.isInCooldown()) {
      const existingToken = this.getAccessToken();
      if (existingToken) {
        // On a un token récent, pas besoin de refresh
        return of(existingToken);
      }
      // Pas de token mais dans le cooldown = attendre un peu
      if (this.isRefreshLocked() || this.isRefreshing) {
        return this.waitForRefresh();
      }
    }

    // Protection 2 : Si un refresh est déjà en cours (cette instance), attendre
    if (this.isRefreshing) {
      return this.waitForRefresh();
    }

    // Protection 3 : Si un refresh est en cours (autre onglet/rechargement), attendre
    if (this.isRefreshLocked()) {
      // Attendre un peu puis réessayer avec le token existant
      return this.waitAndRetry();
    }

    // Acquérir le lock
    if (!this.acquireRefreshLock()) {
      return this.waitAndRetry();
    }

    // Démarrer un nouveau refresh
    this.isRefreshing = true;
    this.refreshTokenSubject.next(null);

    return this.http.post<RefreshResponse>(`${this.baseUrl}/auth/refresh`, {}, { withCredentials: true }).pipe(
      timeout(this.REFRESH_TIMEOUT_MS),
      switchMap((response) => {
        if (response.valid && response.token) {
          // Stocker le nouveau token
          this.setAccessToken(response.token);

          // Stocker l'utilisateur si présent
          if (response.id && response.name) {
            this.setUser({ id: response.id, username: response.name });
          }

          // Notifier les requêtes en attente
          this.refreshTokenSubject.next(response.token);

          return of(response.token);
        } else {
          // Token invalide
          this.clearAuth();
          return throwError(() => new Error('Invalid refresh token'));
        }
      }),
      catchError((error) => {
        // Nettoyer en cas d'erreur
        this.clearAuth();
        return throwError(() => error);
      }),
      finalize(() => {
        // Toujours libérer le lock et réinitialiser l'état
        this.releaseRefreshLock();
        this.isRefreshing = false;
      })
    );
  }

  /**
   * Attend qu'un refresh en cours se termine.
   */
  private waitForRefresh(): Observable<string> {
    return this.refreshTokenSubject.pipe(
      filter((token): token is string => token !== null),
      take(1),
      // Timeout si le refresh prend trop longtemps
      timeout(this.REFRESH_TIMEOUT_MS),
      catchError(() => {
        // Timeout atteint, essayer avec le token existant
        const existingToken = this.getAccessToken();
        if (existingToken) {
          return of(existingToken);
        }
        return throwError(() => new Error('Refresh timeout'));
      })
    );
  }

  /**
   * Attend un peu puis retourne le token existant ou lance un refresh.
   * Utilisé quand un autre onglet/rechargement fait déjà un refresh.
   */
  private waitAndRetry(): Observable<string> {
    return new Observable<string>(subscriber => {
      // Attendre 500ms puis vérifier
      setTimeout(() => {
        const token = this.getAccessToken();
        if (token) {
          subscriber.next(token);
          subscriber.complete();
        } else {
          // Toujours pas de token, réessayer le refresh
          this.refreshToken().subscribe({
            next: (t) => { subscriber.next(t); subscriber.complete(); },
            error: (e) => subscriber.error(e)
          });
        }
      }, 500);
    });
  }

  /**
   * Vérifie si un refresh est actuellement en cours.
   */
  isRefreshInProgress(): boolean {
    return this.isRefreshing || this.isRefreshLocked();
  }

  /**
   * Vérifie si l'utilisateur a un token stocké (peut être expiré).
   */
  hasStoredToken(): boolean {
    return !!this.getAccessToken();
  }

  /**
   * Vérifie si l'utilisateur semble authentifié (token + user présents).
   */
  isLoggedIn(): boolean {
    return this.hasStoredToken() && !!this.getUser();
  }
}
