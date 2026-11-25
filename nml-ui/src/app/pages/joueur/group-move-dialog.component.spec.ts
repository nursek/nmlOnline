import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { GroupMoveDialogComponent } from './group-move-dialog.component';
import { MovementStateService } from '../../services/movement-state.service';
import { ActiveBoardService } from '../../services/active-board.service';
import type { Sector, Unit } from '../../models';

describe('GroupMoveDialogComponent', () => {
  const unit = (id: number) =>
    ({
      id,
      number: id,
      playerId: 1,
      type: { name: 'LARBIN' },
      classes: [{ name: 'LEGER', maxMovementHops: 1 }],
      attack: 10,
      defense: 10,
      pdf: 0,
      pdc: 0,
      armor: 0,
      evasion: 0,
      experience: 0,
      equipments: [],
      isInjured: false,
    }) as unknown as Unit;
  const sector = {
    number: 5,
    name: 'Cinq',
    ownerId: 1,
    neighbors: [6],
    army: [unit(1), unit(2)],
    character: { id: 3, name: 'Héros', playerId: 1 },
    vehicles: [
      { id: 4, displayName: 'VTT', sectorNumber: 5, playerId: 1, isDestroyed: false, hasPilot: true, speed: 2 },
    ],
    buildings: [],
  } as unknown as Sector;
  const sectors = [
    sector,
    { number: 6, name: 'Six', ownerId: null, neighbors: [5], army: [], buildings: [], vehicles: [], characters: [] },
    { number: 7, name: 'Sept', ownerId: 9, neighbors: [6], army: [], buildings: [], vehicles: [], characters: [] },
  ] as unknown as Sector[];
  const movement = {
    pendingEntityIds: signal(new Set<number>()),
    pendingVehicleIds: signal(new Set<number>()),
    error: signal<string | null>(null),
    loadOrders: jest.fn().mockResolvedValue(undefined),
    placeFootOrder: jest.fn().mockResolvedValue({ id: 9 }),
    placeVehicleOrder: jest.fn().mockResolvedValue({ id: 10 }),
  };
  const snackBar = { open: jest.fn() };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    movement.pendingEntityIds.set(new Set());
    movement.pendingVehicleIds.set(new Set());
    TestBed.configureTestingModule({
      providers: [
        { provide: MovementStateService, useValue: movement },
        { provide: ActiveBoardService, useValue: { sectors: signal(sectors) } },
        { provide: MatSnackBar, useValue: snackBar },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { sector, sectorNumber: 5, playerId: 1, allyPlayerIds: new Set<number>() },
        },
      ],
    });
  });

  function component(): GroupMoveDialogComponent {
    return TestBed.runInInjectionContext(() => new GroupMoveDialogComponent());
  }

  it('sélectionne toutes les entités déplaçables par défaut', () => {
    const page = component();

    expect(page.movable().units.map((entity) => entity.id)).toEqual([1, 2]);
    expect(page.selectedCount()).toBe(4);
    expect(page.totalMovableCount()).toBe(4);
    expect(page.maxHops()).toBe(1);

    page.toggle(1, false);
    expect(page.isSelected(1)).toBe(false);
    expect(page.selectedCount()).toBe(3);
    page.selectAll();
    expect(page.selectedCount()).toBe(4);
    page.clearSelection();
    expect(page.selectedCount()).toBe(0);
  });

  it('exclut les entités déjà engagées dans un ordre', () => {
    movement.pendingEntityIds.set(new Set([1]));
    movement.pendingVehicleIds.set(new Set([4]));
    const page = component();

    expect(page.movable().units.map((entity) => entity.id)).toEqual([2]);
    expect(page.selectedVehicles()).toEqual([]);
  });

  it('liste les cibles atteignables avec leur nature', () => {
    const page = component();

    expect(page.targets().map((target) => target.number)).toEqual([6]);
    expect(page.kindLabel(page.targets()[0].kind)).toBe('Neutre');
    expect(page.kindLabel('own')).toBe('Interne');
    expect(page.kindLabel('enemy')).toBe('Ennemi');
  });

  it('envoie un ordre groupé pour les unités, le personnage et les véhicules', async () => {
    const page = component();
    page.targetSector.set(6);

    await page.submit();

    expect(movement.placeFootOrder).toHaveBeenCalledWith([1, 2, 3], [5, 6]);
    expect(movement.placeVehicleOrder).toHaveBeenCalledWith(4, [5, 6]);
    expect(snackBar.open).toHaveBeenCalledWith('Ordre groupé : secteur 5 → 6 (4 entité(s))', 'OK', {
      duration: 3000,
    });
    expect(dialogRef.close).toHaveBeenCalledWith(true);
  });

  it('refuse une destination sans route et ne bouge rien', async () => {
    const page = component();
    page.targetSector.set(7);

    await page.submit();

    expect(movement.placeFootOrder).not.toHaveBeenCalled();
    expect(snackBar.open).toHaveBeenCalledWith(
      'Aucune route adjacente trouvée vers la destination',
      'Fermer',
      { duration: 3000 },
    );
  });

  it('ferme sans déplacer', () => {
    component().close();
    expect(dialogRef.close).toHaveBeenCalledWith(false);
  });
});
