import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { GameInstance, InstanceRole } from '../models';
import { environment } from '../../environments/environment';

const ID_KEY = 'instanceId';
const ROLE_KEY = 'instanceRole';

/** Partie sélectionnée après login : deux onglets peuvent jouer deux parties (sessionStorage). */
@Injectable({ providedIn: 'root' })
export class InstanceService {
  private readonly http = inject(HttpClient);

  private readonly _instances = signal<GameInstance[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _selectedId = signal<number | null>(readStoredId());
  private readonly _selectedRole = signal<InstanceRole | null>(readStoredRole());
  private loaded = false;
  private generation = 0;
  private pendingLoad: Promise<GameInstance[]> | null = null;

  readonly instances = this._instances.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly selectedId = this._selectedId.asReadonly();
  readonly selectedRole = this._selectedRole.asReadonly();
  readonly selected = computed(
    () => this.instances().find((instance) => instance.id === this._selectedId()) ?? null,
  );

  /** Charge la liste une seule fois : le login tranche la route avant la première navigation. */
  async ensureLoaded(): Promise<GameInstance[]> {
    if (this.loaded) return this._instances();
    if (this.pendingLoad) return this.pendingLoad;
    const pending = this.load().finally(() => {
      if (this.pendingLoad === pending) this.pendingLoad = null;
    });
    this.pendingLoad = pending;
    return pending;
  }

  // httpResource n'expose pas de promesse, or la route post-login exige la liste avant navigation.
  async load(): Promise<GameInstance[]> {
    const generation = this.generation;
    this._loading.set(true);
    this._error.set(null);
    try {
      const instances = await firstValueFrom(
        this.http.get<GameInstance[]>(`${environment.apiBaseUrl}/instances`),
      );
      if (generation === this.generation) {
        this._instances.set(instances);
        this.loaded = true;
      }
      return instances;
    } catch (error) {
      if (generation === this.generation) {
        this.loaded = false;
        this._error.set('Impossible de charger vos parties.');
      }
      throw error;
    } finally {
      this._loading.set(false);
    }
  }

  async refresh(): Promise<GameInstance[]> {
    this.invalidate();
    return this.ensureLoaded();
  }

  invalidate(): void {
    this.generation++;
    this.loaded = false;
    this.pendingLoad = null;
    this._instances.set([]);
    this._error.set(null);
  }

  enter(instance: GameInstance, target = routeForInstance(instance)): void {
    // Rechargement complet : les ressources racine et les httpResource ne se rechargent pas en SPA.
    this.select(instance);
    window.location.assign(target);
  }

  select(instance: GameInstance): void {
    sessionStorage.setItem(ID_KEY, String(instance.id));
    if (instance.role) {
      sessionStorage.setItem(ROLE_KEY, instance.role);
    } else {
      sessionStorage.removeItem(ROLE_KEY);
    }
    this._selectedId.set(instance.id);
    this._selectedRole.set(instance.role);
  }

  clear(): void {
    sessionStorage.removeItem(ID_KEY);
    sessionStorage.removeItem(ROLE_KEY);
    this._selectedId.set(null);
    this._selectedRole.set(null);
  }
}

export function routeForInstance(instance: GameInstance): string {
  const admin = instance.role === 'ADMIN' || instance.role === 'MJ';
  if (instance.currentTurn === 0) {
    if (instance.role === 'ADMIN') return '/admin/partie';
    if (instance.role === 'MJ') return '/admin';
    return '/joueur';
  }
  return admin ? '/admin' : '/accueil';
}

function readStoredId(): number | null {
  const raw = sessionStorage.getItem(ID_KEY);
  if (raw === null) return null;
  const parsed = Number(raw);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : null;
}

function readStoredRole(): InstanceRole | null {
  const raw = sessionStorage.getItem(ROLE_KEY);
  return raw === 'ADMIN' || raw === 'MJ' || raw === 'PLAYER' ? raw : null;
}
