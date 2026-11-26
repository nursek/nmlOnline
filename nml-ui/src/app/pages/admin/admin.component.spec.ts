import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AdminComponent } from './admin.component';
import { AdminService } from '../../services/admin.service';
import { AuthService } from '../../services/auth.service';
import type { AdminAllianceEntry, ExchangeOffer } from '../../models';

describe('AdminComponent', () => {
  const admin = {
    currentTurn: signal(3),
    orders: signal([]),
    ordersLoading: signal(false),
    orderStatusFilter: signal('ALL'),
    exchanges: signal([]),
    exchangesLoading: signal(false),
    exchangeStatusFilter: signal('ALL'),
    allianceEntries: signal([]),
    allianceProposals: signal([]),
    alliancesLoading: signal(false),
    loadCurrentTurn: jest.fn().mockResolvedValue(undefined),
    reloadOrders: jest.fn(),
    reloadExchanges: jest.fn(),
    reloadAlliances: jest.fn(),
  };
  const auth = { isAdmin: signal(true) };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: AdminService, useValue: admin },
        { provide: AuthService, useValue: auth },
      ],
    });
  });

  function component(): AdminComponent {
    return TestBed.runInInjectionContext(() => new AdminComponent());
  }

  it('charge tour, ordres, échanges et alliances à la création', () => {
    const page = component();

    expect(admin.loadCurrentTurn).toHaveBeenCalled();
    expect(admin.reloadOrders).toHaveBeenCalled();
    expect(admin.reloadExchanges).toHaveBeenCalled();
    expect(admin.reloadAlliances).toHaveBeenCalled();
    expect(page.currentTurnLabel()).toBe('Tour 3');
  });

  it('met à jour les filtres et recharge les listes', () => {
    const page = component();

    page.onOrderStatusChange('PENDING');
    page.onExchangeStatusChange('EXPIRED');
    page.onReloadExchanges();
    page.onReloadAlliances();

    expect(admin.orderStatusFilter()).toBe('PENDING');
    expect(admin.exchangeStatusFilter()).toBe('EXPIRED');
    expect(admin.reloadExchanges).toHaveBeenCalledTimes(2);
    expect(admin.reloadAlliances).toHaveBeenCalledTimes(2);
  });

  it('formate l’argent et le contenu d’un échange', () => {
    const page = component();
    const offer = {
      money: 2500,
      resources: [{ quantity: 2, resourceName: 'Auramite' }],
    } as ExchangeOffer;

    expect(page.formatMoney(2_500)).toBe('2.5k');
    expect(page.formatMoney(1_200_000)).toBe('1.2M');
    expect(page.formatMoney(42)).toBe('42');
    expect(page.exchangeItemsLabel(offer)).toBe('2.5k ₡, 2 × Auramite');
    expect(page.exchangeItemsLabel({ money: 0, resources: [] } as unknown as ExchangeOffer)).toBe('—');
  });

  it('labellise statuts et propositions', () => {
    const page = component();
    const active = { endedTurn: null, betrayal: false } as AdminAllianceEntry;
    const betrayed = { endedTurn: 3, betrayal: true } as AdminAllianceEntry;
    const broken = { endedTurn: 3, betrayal: false } as AdminAllianceEntry;

    expect(page.allianceStatusLabel(active)).toBe('Active');
    expect(page.allianceStatusLabel(betrayed)).toBe('Trahison');
    expect(page.allianceStatusLabel(broken)).toBe('Rompue');
    expect(page.allianceStatusClass(active)).toBe('status-resolved');
    expect(page.allianceStatusClass(betrayed)).toBe('status-blocked');
    expect(page.proposalStatusClass('PENDING')).toBe('status-pending');
    expect(page.proposalStatusClass('INCONNU')).toBe('status-cancelled');
    expect(page.proposalStatusLabel('ACCEPTED')).toBe('Acceptée');
    expect(page.proposalStatusLabel('INCONNU')).toBe('INCONNU');
    expect(page.proposalKindLabel('RUPTURE')).toBe('Rupture');
    expect(page.proposalKindLabel('INCONNU')).toBe('INCONNU');
    expect(page.exchangeStatusClass('PENDING')).toBe('status-pending');
    expect(page.exchangeStatusClass('CANCELLED')).toBe('status-cancelled');
  });

  it('résout les acteurs d’une proposition', () => {
    const page = component();

    expect(
      page.proposalActor({
        fromPlayerId: 1,
        fromPlayerName: 'lurio',
        toPlayerId: 2,
        toPlayerName: 'nursek',
      } as never),
    ).toBe('lurio → nursek');
    expect(
      page.proposalActor({ fromPlayerId: 1, toPlayerId: 2 } as never),
    ).toBe('#1 → #2');
  });
});
