import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { PlayerManagementComponent } from './player-management.component';
import { AdminService } from '../../services/admin.service';
import { ApiService } from '../../services/api.service';
import type { Player } from '../../models';

describe('PlayerManagementComponent', () => {
  const players = [
    { id: 1, name: 'lurio', race: 'ORKS', sectors: [] },
    { id: 2, name: 'nursek', race: 'ORKS', sectors: [] },
  ] as Player[];
  const admin = {
    players: signal(players),
    playersTruncated: signal(false),
    loading: signal(false),
    importing: signal(false),
    reloadPlayers: jest.fn(),
    deletePlayer: jest.fn().mockResolvedValue(undefined),
    closeEdit: jest.fn(),
    importPlayer: jest.fn().mockResolvedValue(undefined),
  };
  const api = { adminExportPlayer: jest.fn(() => of({ name: 'lurio' })) };
  const snackBar = { open: jest.fn() };
  const dialog = { open: jest.fn().mockReturnValue({ afterClosed: () => of(true) }) };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: AdminService, useValue: admin },
        { provide: ApiService, useValue: api },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialog, useValue: dialog },
      ],
    });
  });

  function component(): PlayerManagementComponent {
    return TestBed.runInInjectionContext(() => new PlayerManagementComponent());
  }

  it('filtre les joueurs par nom et recharge à la création', () => {
    const page = component();
    expect(admin.reloadPlayers).toHaveBeenCalled();

    page.onSearchChange('LUR');
    expect(page.filteredPlayers().map((player) => player.name)).toEqual(['lurio']);
  });

  it('exporte un joueur et confirme le téléchargement', async () => {
    const createObjectURL = jest.fn(() => 'blob:joueur');
    const revokeObjectURL = jest.fn();
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectURL, configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: revokeObjectURL, configurable: true });
    jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    const page = component();

    await page.exportPlayer(players[0]);

    expect(api.adminExportPlayer).toHaveBeenCalledWith(1);
    expect(createObjectURL).toHaveBeenCalled();
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:joueur');
    expect(snackBar.open).toHaveBeenCalledWith('Export de lurio téléchargé', 'OK', {
      duration: 3000,
    });
  });

  it('formate argent, effectifs et libellés', () => {
    const page = component();
    const player = {
      id: 1,
      name: 'lurio',
      sectors: [
        { army: [{ id: 1 }, { id: 2 }] },
        { army: [{ id: 3 }] },
      ],
      equipments: [],
    } as never;

    expect(page.getTotalArmySize(player)).toBe(3);
    expect(page.formatMoney(2_500)).toBe('2.5k');
    expect(page.formatMoney(1_200_000)).toBe('1.2M');
    expect(page.formatMoney(42)).toBe('42');
    expect(page.getUnitTypeLabel(null)).toBe('?');
    expect(page.getUnitTypeLabel('LARBIN')).toBe('LARBIN');
    expect(page.getUnitTypeLabel({ name: 'BRUTE' } as never)).toBe('BRUTE');
    expect(page.getClassNames(null)).toBe('');
    expect(page.getClassNames([{ name: 'LEGER' }, { name: 'TIREUR' }] as never)).toBe(
      'LEGER / TIREUR',
    );
    expect(page.getEquipmentCount(player)).toBe(0);
  });

  it('referme la session d’édition à la fermeture du dialogue', () => {
    const page = component();

    page.openEdit(players[0]);
    expect(dialog.open).toHaveBeenCalled();
    expect(admin.closeEdit).toHaveBeenCalled();
  });

  it('supprime un joueur après confirmation', async () => {
    const page = component();

    page.confirmDelete(players[0]);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(admin.deletePlayer).toHaveBeenCalledWith(1);
  });

  it('ignore export, suppression et édition sans id', async () => {
    const page = component();
    const withoutId = { ...players[0], id: null } as Player;

    await page.exportPlayer(withoutId);
    page.confirmDelete(withoutId);
    page.openEdit(withoutId);

    expect(api.adminExportPlayer).not.toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
  });
});
