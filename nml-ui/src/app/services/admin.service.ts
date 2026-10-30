import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { firstValueFrom, Observable } from 'rxjs';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from './api.service';
import {
  AdminCatalog,
  AdminMovementOrder,
  AdminPlayerEdit,
  MovementStatusFilter,
  PageResult,
  Player,
  ExchangeOffer,
  ExchangeOfferStatusFilter,
  AdminAllianceHistory,
} from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';
import { environment } from '../../environments/environment';

/** État partagé console admin (ordres, échanges, alliances) et gestion des joueurs. */
@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly api = inject(ApiService);
  private readonly snackBar = inject(MatSnackBar);

  // Chaque resource n'est chargée que par la page qui la consomme (cf. reload*).
  private readonly playersRequested = signal(false);
  private readonly playersRef = httpResource<PageResult<Player>>(() =>
    this.playersRequested()
      ? { url: `${environment.apiBaseUrl}/admin/players`, params: { page: '0', size: '50' } }
      : undefined,
  );

  readonly players = computed(() => this.playersRef.value()?.content ?? []);
  readonly playersTruncated = computed(() => {
    const page = this.playersRef.value();
    return !!page && page.content.length < (page.totalElements ?? page.content.length);
  });
  readonly loading = computed(() => this.playersRef.isLoading());

  // Filtre réactif : la resource se recharge à chaque changement de statut.
  readonly orderStatusFilter = signal<MovementStatusFilter>('ALL');
  private readonly ordersRequested = signal(false);
  private readonly ordersRef = httpResource<AdminMovementOrder[]>(() => {
    if (!this.ordersRequested()) return undefined;
    const filter = this.orderStatusFilter();
    return {
      url: `${environment.apiBaseUrl}/admin/turn/orders`,
      params: (filter !== 'ALL' ? { status: filter } : {}) as Record<string, string>,
    };
  });
  readonly orders = computed(() => this.ordersRef.value() ?? []);
  readonly ordersLoading = computed(() => this.ordersRef.isLoading());

  readonly exchangeStatusFilter = signal<ExchangeOfferStatusFilter>('ALL');
  private readonly exchangesRequested = signal(false);
  private readonly exchangesRef = httpResource<PageResult<ExchangeOffer>>(() => {
    if (!this.exchangesRequested()) return undefined;
    const filter = this.exchangeStatusFilter();
    return {
      url: `${environment.apiBaseUrl}/admin/exchanges`,
      params: (filter !== 'ALL' ? { status: filter } : {}) as Record<string, string>,
    };
  });
  readonly exchanges = computed(() => this.exchangesRef.value()?.content ?? []);
  readonly exchangesLoading = computed(() => this.exchangesRef.isLoading());

  private readonly editPlayerIdState = signal<number | null>(null);
  private readonly editRef = httpResource<AdminPlayerEdit>(() => {
    const id = this.editPlayerIdState();
    return id === null ? undefined : { url: `${environment.apiBaseUrl}/admin/players/${id}` };
  });
  readonly edit = computed(() => this.editRef.value() ?? null);
  readonly editLoading = computed(() => this.editRef.isLoading());

  private readonly catalogRef = httpResource<AdminCatalog>(() =>
    this.editPlayerIdState() === null
      ? undefined
      : { url: `${environment.apiBaseUrl}/admin/catalog` },
  );
  readonly catalog = computed(() => this.catalogRef.value() ?? null);

  private readonly alliancesRequested = signal(false);
  private readonly alliancesRef = httpResource<AdminAllianceHistory>(() =>
    this.alliancesRequested() ? { url: `${environment.apiBaseUrl}/admin/alliances` } : undefined,
  );
  readonly allianceEntries = computed(() => this.alliancesRef.value()?.alliances ?? []);
  readonly allianceProposals = computed(() => this.alliancesRef.value()?.proposals ?? []);
  readonly alliancesLoading = computed(() => this.alliancesRef.isLoading());

  private readonly _importing = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _successMessage = signal<string | null>(null);
  private readonly _currentTurn = signal<number | null>(null);

  readonly importing = this._importing.asReadonly();
  readonly currentTurn = this._currentTurn.asReadonly();

  constructor() {
    // Un seul abonné aux messages : les deux pages admin partagent le même service root.
    effect(() => {
      const message = this._successMessage();
      if (!message) return;
      this.snackBar.open(message, 'OK', { duration: 4000, panelClass: 'success-snackbar' });
      this._successMessage.set(null);
    });
    effect(() => {
      const error = this._error();
      if (!error) return;
      this.snackBar.open(error, 'Fermer', { duration: 6000, panelClass: 'error-snackbar' });
      this._error.set(null);
    });
  }

  reloadPlayers(): void {
    if (this.playersRequested()) this.playersRef.reload();
    else this.playersRequested.set(true);
  }

  reloadOrders(): void {
    if (this.ordersRequested()) this.ordersRef.reload();
    else this.ordersRequested.set(true);
  }

  reloadExchanges(): void {
    if (this.exchangesRequested()) this.exchangesRef.reload();
    else this.exchangesRequested.set(true);
  }

  reloadAlliances(): void {
    if (this.alliancesRequested()) this.alliancesRef.reload();
    else this.alliancesRequested.set(true);
  }

  openEdit(playerId: number): void {
    this.editPlayerIdState.set(playerId);
  }

  closeEdit(): void {
    this.editPlayerIdState.set(null);
    this.reloadPlayers();
  }

  async mutate(call: Observable<unknown>, successMessage: string): Promise<void> {
    this._error.set(null);
    try {
      await firstValueFrom(call);
      this._successMessage.set(successMessage);
      this.editRef.reload();
      this.reloadPlayers();
    } catch (error) {
      const message = httpErrorMessage(error, 'Erreur lors de la modification');
      this._error.set(message);
      throw new Error(message);
    }
  }

  /** Charge le tour courant depuis la source unique de vérité (TurnService). */
  async loadCurrentTurn(): Promise<void> {
    try {
      const res = await firstValueFrom(this.api.adminGetCurrentTurn());
      this._currentTurn.set(res.currentTurn);
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors de la récupération du tour courant'));
    }
  }

  async importPlayer(file: File, password?: string): Promise<Player> {
    this._importing.set(true);
    this._error.set(null);
    this._successMessage.set(null);
    try {
      const player = await firstValueFrom(this.api.adminImportPlayer(file, password));
      this._successMessage.set(`Joueur "${player.name}" importé avec succès`);
      this.reloadPlayers();
      return player;
    } catch (error) {
      const message = httpErrorMessage(error, "Erreur lors de l'import");
      this._error.set(message);
      throw new Error(message);
    } finally {
      this._importing.set(false);
    }
  }

  async deletePlayer(playerId: number): Promise<void> {
    this._error.set(null);
    this._successMessage.set(null);
    try {
      await firstValueFrom(this.api.adminDeletePlayer(playerId));
      this._successMessage.set('Joueur supprimé avec succès');
      this.reloadPlayers();
    } catch (error) {
      const message = httpErrorMessage(error, 'Erreur lors de la suppression');
      this._error.set(message);
      throw new Error(message);
    }
  }
}
