import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { TokenService } from './token.service';
import { InstanceService } from './instance.service';
import { LoginRequest, RegisterRequest, User } from '../models';
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
  private readonly instance = inject(InstanceService);

  // Reactive state — restored from session storage on first instantiation.
  private readonly _user = signal<User | null>(this.tokenService.getUser());
  private readonly _error = signal<string | null>(null);
  private readonly _loading = signal(false);
  private readonly _initialized = signal(false);
  private readonly _retryAt = signal<number | null>(null);
  private readonly _tick = signal(Date.now());
  private retryTimer: ReturnType<typeof setInterval> | null = null;

  readonly user = this._user.asReadonly();
  readonly error = this._error.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly initialized = this._initialized.asReadonly();
  readonly retryInSeconds = computed(() => {
    const retryAt = this._retryAt();
    return retryAt === null ? 0 : Math.max(0, Math.ceil((retryAt - this._tick()) / 1000));
  });

  readonly isAuthenticated = computed(() => this._user() !== null);
  readonly isAdmin = computed(() => this._user()?.role === 'ADMIN');

  /** Initialize session: try a silent refresh on app bootstrap. */
  async initSession(): Promise<void> {
    if (this._initialized()) return;
    this._loading.set(true);

    if (!this.tokenService.hasStoredToken() && !this.tokenService.isRemembered()) {
      this._markInitializedUnauthenticated();
      return;
    }

    try {
      const token = await firstValueFrom(this.tokenService.refreshToken());
      const user = this.tokenService.getUser();
      if (token && user) {
        this._user.set(user);
        // Chargement de fond : le menu compte et le garde restent utilisables sans bloquer le boot.
        void this.instance.ensureLoaded().catch(() => undefined);
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
      this.stopRetryCountdown();
      this.tokenService.setRememberMe(credentials.rememberMe);
      this.tokenService.setAccessToken(response.token);
      this.tokenService.setUser({
        id: response.id,
        username: response.name,
        role: response.role,
      });
      this._user.set({ id: response.id, username: response.name, role: response.role });
      this.instance.clear();
      this.instance.invalidate();
      // Une liste indisponible ne doit pas annuler une authentification réussie : /parties propose de réessayer.
      const instances = await this.instance.ensureLoaded().catch(() => []);
      const mine = instances.filter((candidate) => candidate.participant);
      if (mine.length === 1) {
        this.instance.enter(mine[0]);
        return;
      }
      void this.router.navigate(['/parties']).catch(() => undefined);
    } catch (error) {
      const retryAfterSeconds = this.extractRetryAfterSeconds(error);
      if (retryAfterSeconds > 0) {
        this.startRetryCountdown(retryAfterSeconds);
        throw new Error('Trop de tentatives de connexion');
      }
      const message = this.extractErrorMessage(error);
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._loading.set(false);
    }
  }

  /** Crée le compte puis connecte immédiatement (parcours d'invitation). */
  async register(payload: RegisterRequest): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      await firstValueFrom(this.api.register(payload));
      await this.login({
        username: payload.username,
        password: payload.password,
        rememberMe: false,
      });
    } catch (error) {
      const message = httpErrorMessage(error, "Une erreur est survenue lors de l'inscription");
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
    this.instance.clear();
    this.instance.invalidate();
    this._user.set(null);
    this._error.set(null);
  }

  reportForbidden(message: string): void {
    this._error.set(message);
  }

  private _markInitializedUnauthenticated(): void {
    this._user.set(null);
    this._loading.set(false);
    this._initialized.set(true);
  }

  private startRetryCountdown(seconds: number): void {
    this._tick.set(Date.now());
    this._retryAt.set(this._tick() + seconds * 1000);
    if (this.retryTimer !== null) return;
    this.retryTimer = setInterval(() => {
      this._tick.set(Date.now());
      if (this.retryInSeconds() === 0) {
        this.stopRetryCountdown();
      }
    }, 1000);
  }

  private stopRetryCountdown(): void {
    if (this.retryTimer !== null) {
      clearInterval(this.retryTimer);
      this.retryTimer = null;
    }
    this._retryAt.set(null);
  }

  private extractRetryAfterSeconds(error: unknown): number {
    const response = error as { status?: number; headers?: { get(name: string): string | null } };
    if (response?.status !== 429) return 0;
    const raw = response.headers?.get('Retry-After');
    const seconds = raw ? Number(raw) : NaN;
    return Number.isFinite(seconds) && seconds > 0 ? Math.ceil(seconds) : 0;
  }

  private extractErrorMessage(error: unknown): string {
    if ((error as { name?: string })?.name === 'TimeoutError') {
      return 'La connexion a pris trop de temps. Veuillez réessayer.';
    }
    return httpErrorMessage(error, 'Une erreur est survenue lors de la connexion');
  }
}
