import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SellResourceDialogComponent } from './sell-resource-dialog.component';
import { ShopService } from '../../services/shop.service';
import { PurchaseSuccessDialogComponent } from '../../shared/purchase-success-dialog/purchase-success-dialog.component';
import type { PlayerResource } from '../../models';

describe('SellResourceDialogComponent', () => {
  const shop = {
    purchaseLoading: signal(false),
    sellResource: jest.fn().mockResolvedValue(1800),
    error: signal<string | null>(null),
  };
  const snackBar = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };
  const dialog = { open: jest.fn() };
  const resource = { id: 7, name: 'Bois', quantity: 20, baseValue: 2 } as PlayerResource;

  beforeEach(() => {
    jest.clearAllMocks();
    shop.error.set(null);
    shop.sellResource.mockResolvedValue(1800);
    TestBed.configureTestingModule({
      providers: [
        { provide: ShopService, useValue: shop },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { resource } },
      ],
    });
  });

  function component(): SellResourceDialogComponent {
    return TestBed.runInInjectionContext(() => new SellResourceDialogComponent());
  }

  it('plafonne la quantité à 9 et calcule la valeur avec le multiplicateur', () => {
    const page = component();
    expect(page.maxQuantity()).toBe(9);
    expect(page.quantity()).toBe(9);
    expect(page.multiplier()).toBe(45);
    expect(page.totalValue()).toBe(90);

    page.setMax();
    expect(page.quantity()).toBe(9);
    page.onInput({ target: { value: '0' } } as unknown as Event);
    expect(page.quantity()).toBe(1);
    expect(page.totalValue()).toBe(2);
  });

  it('vend et ouvre le dialogue de succès', async () => {
    const page = component();
    page.onInput({ target: { value: '4' } } as unknown as Event);

    await page.confirm();

    expect(shop.sellResource).toHaveBeenCalledWith(7, 4);
    expect(dialogRef.close).toHaveBeenCalled();
    expect(dialog.open).toHaveBeenCalledWith(PurchaseSuccessDialogComponent, {
      width: '360px',
      data: expect.objectContaining({ isSale: true, totalCost: 1800 }),
    });
  });

  it('affiche l’erreur et garde la fenêtre ouverte en cas d’échec', async () => {
    shop.sellResource.mockRejectedValueOnce(new Error('refus'));
    shop.error.set('Stock insuffisant');
    const page = component();

    await page.confirm();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith('Stock insuffisant', 'Fermer', { duration: 5000 });
  });

  it('ignore une ressource sans id', async () => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        { provide: ShopService, useValue: shop },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { resource: { ...resource, id: null } } },
      ],
    });
    const page = TestBed.runInInjectionContext(() => new SellResourceDialogComponent());

    await page.confirm();

    expect(shop.sellResource).not.toHaveBeenCalled();
  });
});
