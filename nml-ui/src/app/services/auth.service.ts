import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { TokenService } from './token.service';
import { InstanceService } from './instance.service';
import { LoginRequest, RegisterRequest, User } from '../models';

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
  // Le MJ n'existe que par partie : son rôle vient de la partie choisie, jamais du jeton.
  readonly canAdmin = computed(() => {
    const role = this.instance.selectedRole();
    return this.isAdmin() || role === 'ADMIN' || role === 'MJ';
  });

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
    } catch (error) {
      // Panne passagère (redéploiement, 5xx, timeout) : garder la session pour retenter au prochain chargement.
      const status = (error as { status?: number })?.status;
      if (status === 401 || status === 403) this.tokenService.clearAuth();
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
      const user = {
        id: response.id,
        username: response.name,
        role: response.role,
        avatarUrl: response.avatarUrl ?? null,
      };
      this.tokenService.setUser(user);
      this._user.set(user);
      this.instance.clear();
      this.instance.invalidate();
      // Une liste indisponible ne doit pas annuler une authentification réussie : /parties propose de réessayer.
      const instances = await this.instance.ensureLoaded().catch(() => []);
      const mine = instances.filter((candidate) => candidate.participant);
      if (mine.length === 1) {
        // « //hôte » sortirait du site : enter() passe par location.assign.
        const returnUrl = this.router.routerState.snapshot.root.queryParamMap.get('returnUrl');
        if (returnUrl && /^\/(?!\/)/.test(returnUrl)) {
          this.instance.enter(mine[0], returnUrl);
        } else {
          this.instance.enter(mine[0]);
        }
        return;
      }
      void this.router.navigate(['/parties']).catch(() => undefined);
    } catch (error) {
      const retryAfterSeconds = this.extractRetryAfterSeconds(error);
      if (retryAfterSeconds > 0) {
        this.startRetryCountdown(retryAfterSeconds);
        throw new Error('Trop de tentatives de connexion');
      }
      const message = this.loginErrorMessage(error);
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
    } catch (error) {
      const message = this.registerErrorMessage(error);
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._loading.set(false);
    }
    // Hors du try : un échec de connexion garde son propre message, l'inscription a réussi.
    await this.login({
      username: payload.username,
      password: payload.password,
      rememberMe: false,
    });
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

  updateUser(patch: Partial<User>): void {
    const user = this._user();
    if (!user) return;
    const updated = { ...user, ...patch };
    this._user.set(updated);
    this.tokenService.setUser(updated);
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

  private loginErrorMessage(error: unknown): string {
    const status = (error as { status?: number })?.status ?? 0;
    // Détail technique (statut, corps du backend) : console uniquement, jamais dans l'alerte joueur.
    console.warn(`[connexion] échec (${status})`, error);
    if (status === 400) {
      return 'Vérifiez le nom d’utilisateur et le mot de passe.';
    }
    if (status === 401) {
      return "Nom d'utilisateur ou mot de passe incorrect.";
    }
    if (status === 403) {
      return 'Connexion refusée. Contactez l’organisateur de la partie.';
    }
    return this.failureMessage(
      error,
      'La connexion est impossible pour le moment. Réessayez plus tard.',
    );
  }

  private registerErrorMessage(error: unknown): string {
    const status = (error as { status?: number })?.status ?? 0;
    console.warn(`[inscription] échec (${status})`, error);
    if (status === 400) {
      return 'Vérifiez les informations du formulaire.';
    }
    if (status === 403) {
      // Seules les erreurs 403 de /api/register portent un détail rédigé pour le joueur (invitation).
      const detail = (error as { error?: { detail?: unknown } })?.error?.detail;
      return typeof detail === 'string' && detail.trim() !== ''
        ? detail
        : 'Inscription refusée. Contactez l’organisateur de la partie.';
    }
    if (status === 409) {
      return 'Ce nom d’utilisateur est déjà pris.';
    }
    return this.failureMessage(
      error,
      'L’inscription est impossible pour le moment. Réessayez plus tard.',
    );
  }

  private failureMessage(error: unknown, fallback: string): string {
    const status = (error as { status?: number })?.status ?? 0;
    if ((error as { name?: string })?.name === 'TimeoutError') {
      return 'La connexion a pris trop de temps. Veuillez réessayer.';
    }
    if (status === 0) {
      return 'Le serveur ne répond pas. Vérifiez votre connexion puis réessayez.';
    }
    if (status === 429) {
      return 'Trop de tentatives. Réessayez dans quelques instants.';
    }
    if (status >= 500) {
      return 'Le serveur est momentanément indisponible. Réessayez dans quelques instants.';
    }
    return fallback;
  }
}
