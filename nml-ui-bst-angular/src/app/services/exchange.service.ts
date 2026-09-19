import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { PlayerService } from './player.service';
import { CreateExchangeOfferPayload, ExchangeOffer } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

@Injectable({ providedIn: 'root' })
export class ExchangeService {
  private readonly api = inject(ApiService);
  private readonly playerService = inject(PlayerService);

  private readonly _offers = signal<ExchangeOffer[]>([]);
  private readonly _loading = signal(false);
  private readonly _saving = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly offers = this._offers.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly saving = this._saving.asReadonly();
  readonly error = this._error.asReadonly();

  private readonly playerId = computed(() => this.playerService.player()?.id ?? null);

  readonly received = computed(() => {
    const id = this.playerId();
    return this._offers().filter((offer) => offer.receiverPlayerId === id);
  });

  readonly sent = computed(() => {
    const id = this.playerId();
    return this._offers().filter((offer) => offer.senderPlayerId === id);
  });

  readonly pendingSentCount = computed(
    () => this.sent().filter((offer) => offer.status === 'PENDING').length,
  );

  async loadOffers(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      this._offers.set(await firstValueFrom(this.api.getExchangeOffers()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors du chargement des échanges'));
    } finally {
      this._loading.set(false);
    }
  }

  async create(payload: CreateExchangeOfferPayload): Promise<boolean> {
    this._saving.set(true);
    this._error.set(null);
    try {
      const offer = await firstValueFrom(this.api.createExchangeOffer(payload));
      this._offers.update((offers) => [offer, ...offers]);
      return true;
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors de la création de l'offre"));
      return false;
    } finally {
      this._saving.set(false);
    }
  }

  async accept(offerId: number): Promise<boolean> {
    return this.mutate(
      () => this.api.acceptExchangeOffer(offerId),
      "Erreur lors de l'acceptation de l'échange",
    );
  }

  async decline(offerId: number): Promise<boolean> {
    return this.mutate(
      () => this.api.declineExchangeOffer(offerId),
      "Erreur lors du refus de l'échange",
    );
  }

  async cancel(offerId: number): Promise<boolean> {
    return this.mutate(
      () => this.api.cancelExchangeOffer(offerId),
      "Erreur lors de l'annulation de l'échange",
    );
  }

  private async mutate(
    request: () => ReturnType<ApiService['acceptExchangeOffer']>,
    fallback: string,
  ): Promise<boolean> {
    this._saving.set(true);
    this._error.set(null);
    try {
      const offer = await firstValueFrom(request());
      this._offers.update((offers) => offers.map((o) => (o.id === offer.id ? offer : o)));
      void this.playerService.loadCurrent();
      return true;
    } catch (error) {
      this._error.set(httpErrorMessage(error, fallback));
      return false;
    } finally {
      this._saving.set(false);
    }
  }
}
