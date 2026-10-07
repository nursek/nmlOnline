import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import {
  AllianceMe,
  AllianceMessage,
  Announcement,
  ProposalKind,
} from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

@Injectable({ providedIn: 'root' })
export class AllianceStateService {
  private readonly api = inject(ApiService);

  private readonly _me = signal<AllianceMe | null>(null);
  private readonly _messages = signal<AllianceMessage[]>([]);
  private readonly _announcements = signal<Announcement[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly me = this._me.asReadonly();
  readonly messages = this._messages.asReadonly();
  readonly announcements = this._announcements.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  readonly alliedPlayerIds = computed(
    () => new Set((this._me()?.alliedPlayers ?? []).map((ally) => ally.playerId)),
  );

  async loadMe(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      this._me.set(await firstValueFrom(this.api.getAllianceMe()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors du chargement de l'alliance"));
    } finally {
      this._loading.set(false);
    }
  }

  async refreshMe(): Promise<void> {
    try {
      this._me.set(await firstValueFrom(this.api.getAllianceMe()));
    } catch {
      // Rafraîchissement silencieux du dialogue : la prochaine action remontera l'erreur.
    }
  }

  async propose(kind: ProposalKind, targetPlayerId: number): Promise<boolean> {
    return this.run(() => this.api.proposeAlliance(kind, targetPlayerId), 'Proposition envoyée');
  }

  async accept(proposalId: number): Promise<boolean> {
    return this.run(() => this.api.acceptAllianceProposal(proposalId), 'Proposition acceptée');
  }

  async decline(proposalId: number): Promise<boolean> {
    return this.run(() => this.api.declineAllianceProposal(proposalId), 'Proposition refusée');
  }

  async withdraw(proposalId: number): Promise<boolean> {
    return this.run(() => this.api.withdrawAllianceProposal(proposalId), 'Proposition retirée');
  }

  async betray(allianceId: number): Promise<boolean> {
    return this.run(() => this.api.betrayAlliance(allianceId), 'Alliance trahie');
  }

  async loadMessages(allianceId: number): Promise<void> {
    this._error.set(null);
    try {
      this._messages.set(await firstValueFrom(this.api.getAllianceMessages(allianceId)));
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors du chargement du chat'));
    }
  }

  async sendMessage(allianceId: number, body: string): Promise<boolean> {
    if (!body.trim()) return false;
    try {
      await firstValueFrom(this.api.postAllianceMessage(allianceId, body.trim()));
      await this.loadMessages(allianceId);
      return true;
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors de l'envoi du message"));
      return false;
    }
  }

  async loadAnnouncements(): Promise<void> {
    try {
      this._announcements.set(await firstValueFrom(this.api.getAnnouncements()));
    } catch {
      // Annonces secondaires : pas d'erreur bloquante.
    }
  }

  private async run(request: () => Observable<unknown>, fallback: string): Promise<boolean> {
    this._error.set(null);
    try {
      await firstValueFrom(request());
      await this.loadMe();
      await this.loadAnnouncements();
      return true;
    } catch (error) {
      this._error.set(httpErrorMessage(error, fallback));
      return false;
    }
  }
}
