import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ExchangeDialogComponent } from './exchange-dialog.component';
import { ExchangeService } from '../../services/exchange.service';
import type { ExchangeOffer, Player } from '../../models';

describe('ExchangeDialogComponent', () => {
  const exchange = {
    received: signal([]),
    sent: signal([]),
    saving: signal(false),
    error: signal<string | null>(null),
    loadOffers: jest.fn().mockResolvedValue(undefined),
    create: jest.fn().mockResolvedValue(true),
    accept: jest.fn().mockResolvedValue(true),
    decline: jest.fn().mockResolvedValue(true),
    cancel: jest.fn().mockResolvedValue(true),
  };
  const snackBar = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };
  const data = {
    playerId: 1,
    playerName: 'lurio',
    transferableMoney: 1000,
    resources: [{ name: 'Auramite', quantity: 5 }],
  };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: ExchangeService, useValue: exchange },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
  });

  function component(): ExchangeDialogComponent {
    return TestBed.runInInjectionContext(() => new ExchangeDialogComponent());
  }

  it('liste les partenaires triés sans le joueur courant', async () => {
    const page = component();
    const http = TestBed.inject(HttpTestingController);
    page.partners();
    TestBed.tick();

    http
      .expectOne('/api/players?size=500')
      .flush({
        content: [
          { id: 1, name: 'lurio' },
          { id: 3, name: 'trazyn' },
          { id: 2, name: 'nursek' },
        ],
        totalElements: 3,
      } satisfies Partial<{ content: Partial<Player>[]; totalElements: number }>);
    await TestBed.inject(ApplicationRef).whenStable();
    TestBed.tick();

    expect(page.partners().map((partner) => partner.name)).toEqual(['nursek', 'trazyn']);
    expect(exchange.loadOffers).toHaveBeenCalled();
  });

  it('borne l’argent au transférable et gère les ressources du panier', () => {
    const page = component();

    page.setMoney({ target: { value: '9999' } } as unknown as Event);
    expect(page.money()).toBe(1000);
    page.setMoney({ target: { value: '-5' } } as unknown as Event);
    expect(page.money()).toBe(0);

    page.resourceName.set('Auramite');
    page.setResourceQuantity({ target: { value: '3' } } as unknown as Event);
    expect(page.canAdd()).toBe(true);
    page.addItem();
    expect(page.items()).toEqual([{ resourceName: 'Auramite', quantity: 3 }]);
    expect(page.canAdd()).toBe(false);

    page.removeItem('Auramite');
    expect(page.items()).toEqual([]);
    page.setResourceQuantity({ target: { value: '0' } } as unknown as Event);
    expect(page.resourceQuantity()).toBe(1);
  });

  it('envoie une offre valide puis réinitialise le formulaire', async () => {
    const page = component();
    page.receiverId.set(2);
    page.money.set(500);
    page.resourceName.set('Auramite');
    page.resourceQuantity.set(2);
    page.addItem();

    await page.submit();

    expect(exchange.create).toHaveBeenCalledWith({
      receiverPlayerId: 2,
      money: 500,
      resources: [{ resourceName: 'Auramite', quantity: 2 }],
    });
    expect(snackBar.open).toHaveBeenCalledWith('Offre envoyée', 'Fermer', { duration: 3000 });
    expect(page.receiverId()).toBeNull();
    expect(page.money()).toBe(0);
    expect(page.items()).toEqual([]);
  });

  it('ignore une offre sans destinataire ou sans contenu', async () => {
    const page = component();

    await page.submit();
    page.receiverId.set(2);
    await page.submit();

    expect(exchange.create).not.toHaveBeenCalled();
  });

  it('accepte, refuse et annule avec notification conditionnelle', async () => {
    const page = component();
    const offer = { id: 7, resources: [{ quantity: 2, resourceName: 'Auramite' }] } as ExchangeOffer;

    await page.accept(offer);
    await page.decline(offer);
    await page.cancel(offer);

    expect(snackBar.open).toHaveBeenCalledWith('Échange accepté', 'Fermer', { duration: 3000 });
    expect(snackBar.open).toHaveBeenCalledWith('Offre refusée', 'Fermer', { duration: 3000 });
    expect(snackBar.open).toHaveBeenCalledWith('Offre annulée', 'Fermer', { duration: 3000 });
    expect(page.itemsLabel(offer)).toBe('2 × Auramite');
    expect(page.statusLabel('PENDING')).toBeTruthy();

    exchange.accept.mockResolvedValueOnce(false);
    snackBar.open.mockClear();
    await page.accept(offer);
    expect(snackBar.open).not.toHaveBeenCalled();
  });

  it('ferme la fenêtre', () => {
    const page = component();
    page.close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
