import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TurnManagementService } from './turn-management.service';
import { ApiService } from './api.service';

describe('TurnManagementService', () => {
  const api = {
    adminGetResolutionState: jest.fn(),
    adminGetCurrentTurn: jest.fn(),
    adminNavigateToTurn: jest.fn(),
    adminStartResolution: jest.fn(),
    adminAdvanceHop: jest.fn(),
    adminResolveElimination: jest.fn(),
    adminResolveBattle: jest.fn(),
    adminFinalizeResolution: jest.fn(),
    adminAbortResolution: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    api.adminGetResolutionState.mockReturnValue(of({ active: false, currentStep: 0 }));
    TestBed.configureTestingModule({
      providers: [TurnManagementService, { provide: ApiService, useValue: api }],
    });
  });

  function service(): TurnManagementService {
    return TestBed.inject(TurnManagementService);
  }

  it('finalise le tour : résultat, tour courant et relecture de l’état', async () => {
    api.adminFinalizeResolution.mockReturnValue(of({ newTurn: 4, captures: [] }));

    await service().finalizeResolution();

    expect(api.adminFinalizeResolution).toHaveBeenCalled();
    expect(service().finalizeResult()).toEqual({ newTurn: 4, captures: [] });
    expect(service().currentTurn()).toBe(4);
    expect(service().lastReport()).toBeNull();
    expect(api.adminGetResolutionState).toHaveBeenCalled();
  });

  it('résout une bataille puis recharge l’état', async () => {
    api.adminResolveBattle.mockReturnValue(of({ success: true }));

    await service().resolveBattle(12);

    expect(api.adminResolveBattle).toHaveBeenCalledWith(12);
    expect(service().lastReport()).toEqual({ success: true });
    expect(api.adminGetResolutionState).toHaveBeenCalled();
  });

  it('abandonne la session et efface le dernier rapport', async () => {
    api.adminAbortResolution.mockReturnValue(of(undefined));

    await service().abort();

    expect(api.adminAbortResolution).toHaveBeenCalled();
    expect(service().lastReport()).toBeNull();
    expect(service().busy()).toBe(false);
    expect(api.adminGetResolutionState).toHaveBeenCalled();
  });


  it('démarre, avance et résout une élimination en publiant l’état', async () => {
    api.adminStartResolution.mockReturnValue(of({ active: true, currentStep: 0 }));
    api.adminAdvanceHop.mockReturnValue(of({ active: true, currentStep: 1 }));
    api.adminResolveElimination.mockReturnValue(of({ active: true, currentStep: 1 }));

    await service().startSession();
    expect(service().state()).toEqual({ active: true, currentStep: 0 });

    await service().advanceHop();
    expect(service().state()).toEqual({ active: true, currentStep: 1 });

    await service().resolveElimination(3, 4);
    expect(api.adminResolveElimination).toHaveBeenCalledWith(3, 4);
    expect(service().busy()).toBe(false);
  });

  it('charge le tour courant et signale son erreur', async () => {
    api.adminGetCurrentTurn.mockReturnValue(of({ currentTurn: 4 }));
    await service().loadCurrentTurn();
    expect(service().currentTurn()).toBe(4);

    api.adminGetCurrentTurn.mockReturnValue(throwError(() => ({})));
    await service().loadCurrentTurn();
    expect(service().error()).toBe('Erreur lors de la récupération du tour courant');
  });

  it('navigue vers un tour puis recharge l’état', async () => {
    api.adminNavigateToTurn.mockReturnValue(of({ currentTurn: 5 }));
    await service().navigateToTurn(5);

    expect(api.adminNavigateToTurn).toHaveBeenCalledWith(5);
    expect(service().currentTurn()).toBe(5);
    expect(api.adminGetResolutionState).toHaveBeenCalled();
    expect(service().navigating()).toBe(false);

    api.adminNavigateToTurn.mockReturnValue(throwError(() => ({})));
    await service().navigateToTurn(6);
    expect(service().error()).toBe('Erreur lors de la navigation vers le tour');
  });

  it('efface les résultats et erreurs', async () => {
    api.adminFinalizeResolution.mockReturnValue(of({ newTurn: 4 }));
    api.adminResolveBattle.mockReturnValue(of({ success: true }));
    const page = service();
    await page.finalizeResolution();
    await page.resolveBattle(1);

    page.clearLastReport();
    page.clearFinalizeResult();
    page.clearError();

    expect(page.lastReport()).toBeNull();
    expect(page.finalizeResult()).toBeNull();
    expect(page.error()).toBeNull();
  });

  it('expose le message de repli quand un appel échoue', async () => {
    api.adminGetResolutionState.mockReturnValue(throwError(() => ({})));

    await service().loadState();

    expect(service().error()).toBe('Erreur lors du chargement de la session');
    expect(service().loading()).toBe(false);
  });
});
