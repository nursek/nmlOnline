import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { TokenService } from './token.service';
import { LoginRequest, User } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

/**
 * Single source of truth for authentication state.
 *
 * Pure signals + services — no NgRx. The service exposes reactive state
 * (`user`, `isAuthenticated`, `isAdmin`, ...) as signals and async methods
 * (`login`, `logout`, `initSession`) that resolve when the underlying HTTP
 * call is finished so callers can chain UI feedback (snackbar, dialog, ...).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokenService = inject(TokenService);
  private readonly router = inject(Router);

  // Reactive state — restored from session storage on first instantiation.
  private readonly _user = signal<User | null>(this.tokenService.getUser());
  private readonly _error = signal<string | null>(null);
  private readonly _loading = signal(false);
  private readonly _initialized = signal(false);

  readonly user = this._user.asReadonly();
  readonly error = this._error.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly initialized = this._initialized.asReadonly();

  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  /** Initialize session: try a silent refresh on app bootstrap. */
  async initSession(): Promise<void> {
    if (this._initialized()) return;
    this._loading.set(true);

    if (!this.tokenService.hasStoredToken()) {
      this._markInitializedUnauthenticated();
      return;
    }

    try {
      const token = await firstValueFrom(this.tokenService.refreshToken());
      const user = this.tokenService.getUser();
      if (token && user) {
        this._user.set(user);
      } else {
        this.tokenService.clearAuth();
        this._user.set(null);
      }
    } catch {
      this.tokenService.clearAuth();
      this._user.set(null);
    } finally {
      this._loading.set(false);
      this._initialized.set(true);
    }
  }

  /** Log the user in. Resolves on success, rejects with a readable message. */
  async login(credentials: LoginRequest): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      const response = await firstValueFrom(this.api.login(credentials));
      this.tokenService.setAccessToken(response.token);
      this.tokenService.setUser({
        id: response.id,
        username: response.name,
        role: response.role,
      });
      this._user.set({ id: response.id, username: response.name, role: response.role });
      void this.router.navigate(['/carte']);
    } catch (error) {
      const message = this.extractErrorMessage(error);
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._loading.set(false);
    }
  }

  /** Log the user out (server + local). Never rejects — always clears locally. */
  async logout(): Promise<void> {
    try {
      await firstValueFrom(this.api.logout());
    } catch {
      // Even on server error, clear local session.
    }
    this.clear();
    void this.router.navigate(['/login']);
  }

  /** Clear local auth state (no HTTP). Used by the 401 interceptor path. */
  clear(): void {
    this.tokenService.clearAuth();
    this._user.set(null);
    this._error.set(null);
  }

  /** Surface a 403 'forbidden' message to the UI. */
  reportForbidden(message: string): void {
    this._error.set(message);
  }

  private _markInitializedUnauthenticated(): void {
    this._user.set(null);
    this._loading.set(false);
    this._initialized.set(true);
  }

  private extractErrorMessage(error: unknown): string {
    if ((error as { name?: string })?.name === 'TimeoutError') {
      return 'La connexion a pris trop de temps. Veuillez réessayer.';
    }
    return httpErrorMessage(error, 'Une erreur est survenue lors de la connexion');
  }
}
