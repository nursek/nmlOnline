import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  AdminAllianceEntry,
  AdminAllianceProposalEntry,
  ExchangeOffer,
  ExchangeOfferStatus,
  ExchangeOfferStatusFilter,
  MovementStatusFilter,
} from '../../models';
import { AdminService } from '../../services/admin.service';

@Component({
  selector: 'app-admin',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.scss'],
})
export class AdminComponent {
  private readonly admin = inject(AdminService);

  readonly currentTurn = this.admin.currentTurn;

  readonly orders = this.admin.orders;
  readonly ordersLoading = this.admin.ordersLoading;
  readonly orderStatusFilter = this.admin.orderStatusFilter;

  readonly exchanges = this.admin.exchanges;
  readonly exchangesLoading = this.admin.exchangesLoading;
  readonly exchangeStatusFilter = this.admin.exchangeStatusFilter;

  readonly alliances = this.admin.allianceEntries;
  readonly allianceProposals = this.admin.allianceProposals;
  readonly alliancesLoading = this.admin.alliancesLoading;

  readonly statusFilters: ReadonlyArray<MovementStatusFilter> = [
    'ALL',
    'PENDING',
    'RESOLVED',
    'BLOCKED',
    'CANCELLED',
  ];

  readonly exchangeFilters: ReadonlyArray<ExchangeOfferStatusFilter> = [
    'ALL',
    'PENDING',
    'ACCEPTED',
    'DECLINED',
    'CANCELLED',
    'EXPIRED',
  ];

  readonly exchangeStatusLabels: Record<ExchangeOfferStatus, string> = {
    PENDING: 'En attente',
    ACCEPTED: 'Acceptée',
    DECLINED: 'Refusée',
    CANCELLED: 'Annulée',
    EXPIRED: 'Expirée',
  };

  readonly proposalStatusLabels: Record<string, string> = {
    PENDING: 'En attente',
    ACCEPTED: 'Acceptée',
    DECLINED: 'Refusée',
    WITHDRAWN: 'Retirée',
  };

  readonly proposalKindLabels: Record<string, string> = {
    ALLIANCE: 'Alliance',
    RUPTURE: 'Rupture',
  };

  readonly currentTurnLabel = computed(() => {
    const t = this.currentTurn();
    return t === null || t === undefined ? '—' : `Tour ${t}`;
  });

  constructor() {
    void this.admin.loadCurrentTurn();
    this.admin.reloadOrders();
    this.admin.reloadExchanges();
    this.admin.reloadAlliances();
  }

  onOrderStatusChange(status: MovementStatusFilter): void {
    this.orderStatusFilter.set(status);
  }

  onExchangeStatusChange(status: ExchangeOfferStatusFilter): void {
    this.exchangeStatusFilter.set(status);
  }

  onReloadExchanges(): void {
    this.admin.reloadExchanges();
  }

  onReloadAlliances(): void {
    this.admin.reloadAlliances();
  }

  exchangeItemsLabel(offer: ExchangeOffer): string {
    const parts = offer.resources.map((item) => `${item.quantity} × ${item.resourceName}`);
    if (offer.money > 0) parts.unshift(`${this.formatMoney(offer.money)} ₡`);
    return parts.length > 0 ? parts.join(', ') : '—';
  }

  exchangeStatusClass(status: ExchangeOfferStatus): string {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'ACCEPTED':
        return 'status-resolved';
      case 'DECLINED':
      case 'EXPIRED':
        return 'status-blocked';
      case 'CANCELLED':
        return 'status-cancelled';
    }
  }

  allianceStatusLabel(alliance: AdminAllianceEntry): string {
    if (alliance.endedTurn === null) return 'Active';
    return alliance.betrayal ? 'Trahison' : 'Rompue';
  }

  allianceStatusClass(alliance: AdminAllianceEntry): string {
    if (alliance.endedTurn === null) return 'status-resolved';
    return alliance.betrayal ? 'status-blocked' : 'status-cancelled';
  }

  proposalStatusClass(status: string): string {
    switch (status) {
      case 'PENDING':
        return 'status-pending';
      case 'ACCEPTED':
        return 'status-resolved';
      case 'DECLINED':
        return 'status-blocked';
      default:
        return 'status-cancelled';
    }
  }

  proposalStatusLabel(status: string): string {
    return this.proposalStatusLabels[status] ?? status;
  }

  proposalKindLabel(kind: string): string {
    return this.proposalKindLabels[kind] ?? kind;
  }

  formatMoney(amount: number): string {
    if (amount >= 1_000_000) return (amount / 1_000_000).toFixed(1) + 'M';
    if (amount >= 1_000) return (amount / 1_000).toFixed(1) + 'k';
    return amount.toFixed(0);
  }

  proposalActor(proposal: AdminAllianceProposalEntry): string {
    return `${proposal.fromPlayerName ?? '#' + proposal.fromPlayerId} → ${proposal.toPlayerName ?? '#' + proposal.toPlayerId}`;
  }
}
