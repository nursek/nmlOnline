import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import { TurnManagementComponent } from './turn-management.component';
import { TurnManagementService } from '../../services/turn-management.service';
import { ConflictPreviewService } from '../../services/conflict-preview.service';
import { DevScenarioService } from '../../services/dev-scenario.service';
import { PlayerStatusService } from '../../services/player-status.service';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

describe('TurnManagementComponent — actions de résolution', () => {
  const turnManagement = {
    state: signal(null),
    busy: signal(false),
    loading: signal(false),
    error: signal<string | null>(null),
    lastReport: signal(null),
    finalizeResult: signal(null),
    active: signal(true),
    currentTurn: signal(2),
    navigating: signal(false),
    loadState: jest.fn().mockResolvedValue(undefined),
    loadCurrentTurn: jest.fn().mockResolvedValue(undefined),
    startSession: jest.fn().mockResolvedValue(undefined),
    advanceHop: jest.fn().mockResolvedValue(undefined),
    resolveElimination: jest.fn().mockResolvedValue(undefined),
    resolveBattle: jest.fn().mockResolvedValue(undefined),
    finalizeResolution: jest.fn().mockResolvedValue(undefined),
    abort: jest.fn().mockResolvedValue(undefined),
    navigateToTurn: jest.fn().mockResolvedValue(undefined),
    clearLastReport: jest.fn(),
    clearFinalizeResult: jest.fn(),
  };
  const preview = {
    report: signal(null),
    previewing: signal(false),
    error: signal<string | null>(null),
    loadPreview: jest.fn().mockResolvedValue(undefined),
  };
  const api = {
    adminGetPendingCaptures: jest.fn(() => of([])),
    adminGetTurnJournal: jest.fn(() =>
      of({
        turn: 2,
        exportedAt: '2026-11-25T12:00:00Z',
        finalized: true,
        summary: {
          battles: 0,
          resolvedOrders: 0,
          blockedOrders: 0,
          captures: 0,
          eliminatedPlayers: 0,
          rangedCombats: 0,
          transitCombats: 0,
        },
        battles: [],
        captures: [],
        eliminatedPlayers: [],
        rangedCombats: [],
        transitCombats: [],
      }),
    ),
    adminResolvePendingCapture: jest.fn(() => of(undefined)),
    adminDismissPendingCapture: jest.fn(() => of(undefined)),
  };
  const snack = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: TurnManagementService, useValue: turnManagement },
        { provide: ConflictPreviewService, useValue: preview },
        { provide: DevScenarioService, useValue: { available: signal(false) } },
        { provide: PlayerStatusService, useValue: { gameFinished: signal(false) } },
        { provide: ApiService, useValue: api },
        { provide: AuthService, useValue: { isAdmin: signal(true) } },
        {
          provide: MatDialog,
          useValue: { open: jest.fn().mockReturnValue({ afterClosed: () => of(true) }) },
        },
        { provide: MatSnackBar, useValue: snack },
      ],
    });
  });

  function component(): TurnManagementComponent {
    return TestBed.runInInjectionContext(() => new TurnManagementComponent());
  }

  it('déclenche démarrage, hop, bataille et élimination puis relit l’aperçu', async () => {
    const page = component();

    page.onStart();
    page.onAdvanceHop();
    page.onResolveBattle(12);
    page.onResolveElimination(3, 4);
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(turnManagement.startSession).toHaveBeenCalled();
    expect(turnManagement.advanceHop).toHaveBeenCalled();
    expect(turnManagement.resolveBattle).toHaveBeenCalledWith(12);
    expect(turnManagement.resolveElimination).toHaveBeenCalledWith(3, 4);
    expect(preview.loadPreview).toHaveBeenCalled();
  });

  it('ouvre le détail de combat et compose les éliminations', () => {
    const page = component();
    page.onShowDetail({ sectorNumber: 5 } as never);

    expect(page.eliminationsLabel({
      eliminatedPlayers: [
        { playerId: 3, playerName: 'trazyn', cause: 'MAIN_CHARACTER_KILLED' },
        { playerId: 4, playerName: null, cause: 'HEADQUARTERS_CAPTURED' },
      ],
    } as never)).toContain('trazyn');
  });

  it('télécharge le tour et une archive', async () => {
    const createObjectURL = jest.fn(() => 'blob:test');
    Object.defineProperty(URL, 'createObjectURL', { value: createObjectURL, configurable: true });
    Object.defineProperty(URL, 'revokeObjectURL', { value: jest.fn(), configurable: true });
    jest.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
    turnManagement.state.set({
      active: true,
      turnEnding: 2,
      resolvedConflicts: [],
      transitCombatsCount: 0,
    } as never);
    const page = component();

    page.onDownloadTurn('json');
    expect(createObjectURL).toHaveBeenCalledTimes(1);

    page.onArchiveTurnChange('3');
    await page.onDownloadArchive('log');
    expect(api.adminGetTurnJournal).toHaveBeenCalledWith(3);
    expect(createObjectURL).toHaveBeenCalledTimes(2);
  });

  it('attribue ou ignore une capture en attente', async () => {
    const page = component();
    const pending = { id: 7, sectorNumber: 5 } as never;

    await page.onResolvePendingCapture(pending, 2, 'nursek');
    expect(api.adminResolvePendingCapture).toHaveBeenCalledWith(7, 2);
    expect(snack.open).toHaveBeenCalledWith('Secteur 5 attribué à nursek', 'OK', { duration: 4000 });

    await page.onDismissPendingCapture(pending);
    expect(api.adminDismissPendingCapture).toHaveBeenCalledWith(7);
  });

  it('finalise le tour après confirmation puis relit l’aperçu', async () => {
    const page = component();

    page.onFinalize();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(turnManagement.finalizeResolution).toHaveBeenCalled();
    expect(preview.loadPreview).toHaveBeenCalled();
  });

  it('abandonne la session après confirmation', () => {
    component().onAbort();

    expect(turnManagement.abort).toHaveBeenCalled();
  });

  it('navigue vers un tour valide et ignore une saisie invalide', async () => {
    const page = component();

    page.onTargetTurnChange('4');
    page.onNavigateToTurn();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(turnManagement.navigateToTurn).toHaveBeenCalledWith(4);

    page.onTargetTurnChange('abc');
    page.onNavigateToTurn();
    expect(turnManagement.navigateToTurn).toHaveBeenCalledTimes(1);
  });
});
