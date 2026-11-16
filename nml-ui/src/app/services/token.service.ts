import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize, map, shareReplay, timeout } from 'rxjs/operators';
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

  private refresh$: Observable<string> | null = null;

  private readonly REFRESH_TIMEOUT_MS = 10_000;

  /**
   * Récupère le token d'accès depuis le sessionStorage.
   * Le sessionStorage est préféré au localStorage pour le token afin de limiter
   * la durée de vie en cas d'attaque XSS et de réduire la surface d'exposition.
   */
  getAccessToken(): string | null {
    return sessionStorage.getItem('accessToken');
  }

  setAccessToken(token: string): void {
    sessionStorage.setItem('accessToken', token);
  }

  removeAccessToken(): void {
    sessionStorage.removeItem('accessToken');
  }

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

  setUser(user: { id: number; username: string; role?: string }): void {
    sessionStorage.setItem('user', JSON.stringify(user));
  }

  removeUser(): void {
    sessionStorage.removeItem('user');
  }

  clearAuth(): void {
    this.removeAccessToken();
    this.removeUser();
    // Un autre compte ne doit pas hériter de la partie choisie.
    sessionStorage.removeItem('instanceId');
    sessionStorage.removeItem('instanceRole');
    this.removeRememberMe();
  }

  /** Drapeau non secret : autorise une tentative de refresh cookie au prochain démarrage. */
  setRememberMe(remember: boolean): void {
    if (remember) {
      localStorage.setItem('rememberMe', '1');
    } else {
      localStorage.removeItem('rememberMe');
    }
  }

  isRemembered(): boolean {
    return localStorage.getItem('rememberMe') === '1';
  }

  removeRememberMe(): void {
    localStorage.removeItem('rememberMe');
  }

  // Une seule requête de refresh à la fois : les appels concurrents partagent son résultat, succès comme échec.
  refreshToken(): Observable<string> {
    this.refresh$ ??= this.http
      .post<RefreshResponse>(`${this.baseUrl}/auth/refresh`, {}, { withCredentials: true })
      .pipe(
        timeout(this.REFRESH_TIMEOUT_MS),
        map((response) => {
          if (!response.valid || !response.token) {
            this.clearAuth();
            throw new Error('Invalid refresh token');
          }
          this.setAccessToken(response.token);
          if (response.id && response.name) {
            this.setUser({ id: response.id, username: response.name, role: response.role });
          }
          return response.token;
        }),
        finalize(() => (this.refresh$ = null)),
        shareReplay(1),
      );
    return this.refresh$;
  }

  /**
   * Vérifie si l'utilisateur a un token stocké (peut être expiré).
   */
  hasStoredToken(): boolean {
    return !!this.getAccessToken();
  }
}
