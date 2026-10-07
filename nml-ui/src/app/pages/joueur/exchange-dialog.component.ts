import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  ExchangeOffer,
  ExchangeOfferItem,
  ExchangeOfferStatus,
  PageResult,
  Player,
  PlayerResource,
} from '../../models';
import { ExchangeService } from '../../services/exchange.service';
import { environment } from '../../../environments/environment';

export interface ExchangeDialogData {
  playerId: number;
  resources: PlayerResource[];
  transferableMoney: number;
  startingMoneyRemaining: number;
}

const STATUS_LABELS: Record<ExchangeOfferStatus, string> = {
  PENDING: 'En attente',
  ACCEPTED: 'Acceptée',
  DECLINED: 'Refusée',
  CANCELLED: 'Annulée',
  EXPIRED: 'Expirée',
};

@Component({
  selector: 'app-exchange-dialog',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    DecimalPipe,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
  ],
  templateUrl: './exchange-dialog.component.html',
  styleUrls: ['./exchange-dialog.component.scss'],
})
export class ExchangeDialogComponent {
  private readonly dialogRef = inject(MatDialogRef<ExchangeDialogComponent>);
  private readonly snackBar = inject(MatSnackBar);
  private readonly exchange = inject(ExchangeService);
  readonly data = inject<ExchangeDialogData>(MAT_DIALOG_DATA);

  readonly received = this.exchange.received;
  readonly sent = this.exchange.sent;
  readonly saving = this.exchange.saving;
  readonly error = this.exchange.error;

  private readonly playersRef = httpResource<PageResult<Player>>(
    () => `${environment.apiBaseUrl}/players?size=500`,
  );

  readonly partners = computed(() => {
    const page = this.playersRef.value();
    if (!page) return [];
    return page.content
      .filter((player) => player.id !== this.data.playerId)
      .map((player) => ({ id: player.id, name: player.name }))
      .sort((a, b) => a.name.localeCompare(b.name));
  });

  readonly receiverId = signal<number | null>(null);
  readonly money = signal(0);
  readonly resourceName = signal<string | null>(null);
  readonly resourceQuantity = signal(1);
  readonly items = signal<ExchangeOfferItem[]>([]);

  readonly selectedResource = computed(
    () => this.data.resources.find((resource) => resource.name === this.resourceName()) ?? null,
  );
  readonly canAdd = computed(() => {
    const name = this.resourceName();
    const selected = this.selectedResource();
    return (
      name != null &&
      selected != null &&
      this.resourceQuantity() >= 1 &&
      this.resourceQuantity() <= selected.quantity &&
      !this.items().some((item) => item.resourceName === name)
    );
  });
  readonly canSubmit = computed(
    () =>
      this.receiverId() != null &&
      this.money() <= this.data.transferableMoney &&
      (this.money() > 0 || this.items().length > 0) &&
      !this.saving(),
  );

  constructor() {
    void this.exchange.loadOffers();
  }

  setMoney(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    const capped = Number.isFinite(value) && value > 0 ? Math.trunc(value) : 0;
    this.money.set(Math.min(capped, Math.max(0, Math.trunc(this.data.transferableMoney))));
  }

  setResourceQuantity(event: Event): void {
    const value = Number((event.target as HTMLInputElement).value);
    this.resourceQuantity.set(Number.isFinite(value) && value > 0 ? Math.trunc(value) : 1);
  }

  addItem(): void {
    const name = this.resourceName();
    if (!this.canAdd() || name == null) return;
    this.items.update((items) => [
      ...items,
      { resourceName: name, quantity: this.resourceQuantity() },
    ]);
    this.resourceName.set(null);
    this.resourceQuantity.set(1);
  }

  removeItem(resourceName: string): void {
    this.items.update((items) => items.filter((item) => item.resourceName !== resourceName));
  }

  async submit(): Promise<void> {
    const receiverId = this.receiverId();
    if (receiverId == null || !this.canSubmit()) return;
    const ok = await this.exchange.create({
      receiverPlayerId: receiverId,
      money: this.money(),
      resources: this.items(),
    });
    if (!ok) return;
    this.snackBar.open('Offre envoyée', 'Fermer', { duration: 3000 });
    this.receiverId.set(null);
    this.money.set(0);
    this.resourceName.set(null);
    this.items.set([]);
  }

  async accept(offer: ExchangeOffer): Promise<void> {
    if (await this.exchange.accept(offer.id)) {
      this.snackBar.open('Échange accepté', 'Fermer', { duration: 3000 });
    }
  }

  async decline(offer: ExchangeOffer): Promise<void> {
    if (await this.exchange.decline(offer.id)) {
      this.snackBar.open('Offre refusée', 'Fermer', { duration: 3000 });
    }
  }

  async cancel(offer: ExchangeOffer): Promise<void> {
    if (await this.exchange.cancel(offer.id)) {
      this.snackBar.open('Offre annulée', 'Fermer', { duration: 3000 });
    }
  }

  statusLabel(status: ExchangeOfferStatus): string {
    return STATUS_LABELS[status];
  }

  itemsLabel(offer: ExchangeOffer): string {
    return offer.resources.map((item) => `${item.quantity} × ${item.resourceName}`).join(', ');
  }

  close(): void {
    this.dialogRef.close();
  }
}
