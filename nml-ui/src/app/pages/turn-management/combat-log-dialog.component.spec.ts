import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { CombatLogDialogComponent } from './combat-log-dialog.component';
import type { BattlePlayerForce, ResolvedBattle } from '../../models';

describe('CombatLogDialogComponent', () => {
  const dialogRef = { close: jest.fn() };
  const battle = {
    sectorNumber: 5,
    standoff: false,
    attackerName: 'lurio',
    defenderName: 'nursek',
    winnerName: 'lurio',
    highlights: [{ kind: 'WINNER', message: 'Victoire', visibility: 'PUBLIC' }],
    forces: [],
    battleLog: [],
  } as unknown as ResolvedBattle;

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: battle },
      ],
    });
  });

  function component(): CombatLogDialogComponent {
    return TestBed.runInInjectionContext(() => new CombatLogDialogComponent());
  }

  it('résume les forces par catégorie', () => {
    const page = component();
    const force = {
      characters: [{ displayName: 'Héros' }],
      vehicles: [{ displayName: 'VTT' }],
      infantry: [{ displayName: 'LARBIN n°1' }],
      buildings: [{ displayName: 'QG' }],
    } as unknown as BattlePlayerForce;

    expect(page.forceSummary(force)).toBe(
      'Personnages : Héros · Véhicules : VTT · Infanterie : LARBIN n°1 · Bâtiments : QG',
    );
    expect(page.forceSummary({ characters: [], vehicles: [], infantry: [], buildings: [] } as unknown as BattlePlayerForce)).toBe('');
  });

  it('groupe le journal de combat et ferme', () => {
    const page = component();
    expect(page.groups()).toEqual([]);

    page.close();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
