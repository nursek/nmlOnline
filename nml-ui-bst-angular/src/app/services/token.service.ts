import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, filter, finalize, switchMap, take, timeout } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { RefreshResponse } from '../models';

/**
 * Service singleton pour gérer le refresh des tokens JWT.
 * Gère les race conditions et le queueing des requêtes pendant un refresh :
 * si un refresh est en cours, les autres requêtes attendent le résultat.
 */
@Injectable({
  providedIn: 'root',
})
export class TokenService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  // État du refresh - partagé entre toutes les requêtes de cette instance
  private isRefreshing = false;
  private readonly refreshTokenSubject = new BehaviorSubject<string | null>(null);

  // Timeout pour le refresh (10 secondes max)
  private readonly REFRESH_TIMEOUT_MS = 10_000;

  /**
   * Récupère le token d'accès depuis le sessionStorage.
   * Le sessionStorage est préféré au localStorage pour le token afin de limiter
   * la durée de vie en cas d'attaque XSS et de réduire la surface d'exposition.
   */
  getAccessToken(): string | null {
    return sessionStorage.getItem('accessToken');
  }

  /**
   * Stocke le token d'accès dans le sessionStorage.
   */
  setAccessToken(token: string): void {
    sessionStorage.setItem('accessToken', token);
  }

  /**
   * Supprime le token d'accès du sessionStorage.
   */
  removeAccessToken(): void {
    sessionStorage.removeItem('accessToken');
  }

  /**
   * Récupère l'utilisateur depuis le sessionStorage.
   */
  getUser(): { id: number; username: string; role?: string } | null {
    const stored = sessionStorage.getItem('user');
    if (!stored) return null;

    try {
      const parsed = JSON.parse(stored);
      if (parsed && typeof parsed.id === 'number' && typeof parsed.username === 'string') {
        return parsed;
      }
    } catch {
      // Ignore parsing errors
    }

    sessionStorage.removeItem('user');
    return null;
  }

  /**
   * Stocke l'utilisateur dans le sessionStorage.
   */
  setUser(user: { id: number; username: string; role?: string }): void {
    sessionStorage.setItem('user', JSON.stringify(user));
  }

  /**
   * Supprime l'utilisateur du sessionStorage.
   */
  removeUser(): void {
    sessionStorage.removeItem('user');
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
  }

  /**
   * Effectue un refresh du token.
   * Si un refresh est déjà en cours, attend son résultat ; sinon en lance un nouveau.
   *
   * @returns Observable<string> Le nouveau token d'accès
   */
  refreshToken(): Observable<string> {
    // Si un refresh est déjà en cours, attendre son résultat
    if (this.isRefreshing) {
      return this.waitForRefresh();
    }

    // Démarrer un nouveau refresh
    this.isRefreshing = true;
    this.refreshTokenSubject.next(null);

    return this.http
      .post<RefreshResponse>(`${this.baseUrl}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        timeout(this.REFRESH_TIMEOUT_MS),
        switchMap((response) => {
          if (response.valid && response.token) {
            // Stocker le nouveau token
            this.setAccessToken(response.token);

            // Stocker l'utilisateur si présent
            if (response.id && response.name) {
              this.setUser({ id: response.id, username: response.name, role: response.role });
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
          // Toujours réinitialiser l'état
          this.isRefreshing = false;
        }),
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
      }),
    );
  }

  /**
   * Vérifie si l'utilisateur a un token stocké (peut être expiré).
   */
  hasStoredToken(): boolean {
    return !!this.getAccessToken();
  }
}
