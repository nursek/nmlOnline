import { Injectable, computed, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { GameInstance, InstanceRole } from '../models';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';

const ID_KEY = 'instanceId';
const ROLE_KEY = 'instanceRole';

/** Partie sélectionnée après login : deux onglets peuvent jouer deux parties (sessionStorage). */
@Injectable({ providedIn: 'root' })
export class InstanceService {
  private readonly auth = inject(AuthService);

  private readonly _selectedId = signal<number | null>(readStoredId());
  private readonly _selectedRole = signal<InstanceRole | null>(readStoredRole());
  readonly selectedId = this._selectedId.asReadonly();
  readonly selectedRole = this._selectedRole.asReadonly();

  private readonly instancesRef = httpResource<GameInstance[]>(() =>
    this.auth.user() ? { url: `${environment.apiBaseUrl}/instances` } : undefined,
  );

  readonly instances = computed(() => this.instancesRef.value() ?? []);
  readonly selected = computed(
    () => this.instances().find((instance) => instance.id === this._selectedId()) ?? null,
  );
  readonly loading = computed(() => this.instancesRef.isLoading());

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

  reload(): void {
    this.instancesRef.reload();
  }
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
