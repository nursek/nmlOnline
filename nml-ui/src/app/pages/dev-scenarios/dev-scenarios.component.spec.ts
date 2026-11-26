import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DevScenariosComponent } from './dev-scenarios.component';
import { DevScenarioService } from '../../services/dev-scenario.service';
import { PlayerStatusService } from '../../services/player-status.service';

describe('DevScenariosComponent', () => {
  const scenarios = {
    available: signal(true),
    loading: signal(false),
    seeding: signal(false),
    seedReport: signal(null),
    demoReport: signal(null),
    demoAvailable: signal(true),
    error: signal<string | null>(null),
    combatScenarios: signal([
      { code: 'MIXED_5050', name: 'Mixte', eliminatesPlayer: false },
      { code: 'MAIN_CHARACTER_ELIMINATION', name: 'Élimination', eliminatesPlayer: true },
    ]),
    clearSeedReport: jest.fn(),
    seedCombatScenario: jest.fn().mockResolvedValue(undefined),
    seedExchangeScenario: jest.fn().mockResolvedValue(undefined),
    seedAllianceScenario: jest.fn().mockResolvedValue(undefined),
    seedResourceSaleScenario: jest.fn().mockResolvedValue(undefined),
    seedDevScenario: jest.fn().mockResolvedValue(undefined),
    seedStandoffScenario: jest.fn().mockResolvedValue(undefined),
    seedEndGame: jest.fn().mockResolvedValue(undefined),
    playDemoTurns: jest.fn().mockResolvedValue(undefined),
  };
  const playerStatus = { reload: jest.fn() };
  const snack = { open: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    scenarios.seedReport.set(null);
    TestBed.configureTestingModule({
      providers: [
        { provide: DevScenarioService, useValue: scenarios },
        { provide: PlayerStatusService, useValue: playerStatus },
        { provide: MatSnackBar, useValue: snack },
      ],
    });
  });

  function component(): DevScenariosComponent {
    return TestBed.runInInjectionContext(() => new DevScenariosComponent());
  }

  it('trie les scénarios qui éliminent un joueur en premier', () => {
    const page = component();

    expect(page.sortedCombatScenarios().map((scenario) => scenario.code)).toEqual([
      'MAIN_CHARACTER_ELIMINATION',
      'MIXED_5050',
    ]);
    expect(page.combatIcon('MIXED_5050')).toBe('swords');
    expect(page.combatIcon('UNKNOWN')).toBe('swords');
  });

  it('déclenche les seeders et libère l’action en cours', async () => {
    const page = component();

    page.onSeedCombatScenario('MIXED_5050');
    expect(page.pendingAction()).toBe('MIXED_5050');
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(scenarios.seedCombatScenario).toHaveBeenCalledWith('MIXED_5050');
    expect(page.pendingAction()).toBeNull();

    page.onSeedExchangeScenario();
    page.onSeedAllianceScenario();
    page.onSeedResourceSaleScenario();
    page.onSeedScenario();
    page.onSeedStandoffScenario();
    page.onPlayDemoTurns();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(scenarios.seedExchangeScenario).toHaveBeenCalled();
    expect(scenarios.seedAllianceScenario).toHaveBeenCalled();
    expect(scenarios.seedResourceSaleScenario).toHaveBeenCalled();
    expect(scenarios.seedDevScenario).toHaveBeenCalled();
    expect(scenarios.seedStandoffScenario).toHaveBeenCalled();
    expect(scenarios.playDemoTurns).toHaveBeenCalled();
  });

  it('termine la partie puis recharge le statut joueur', async () => {
    const page = component();

    page.onSeedEndGame();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(scenarios.seedEndGame).toHaveBeenCalled();
    expect(playerStatus.reload).toHaveBeenCalled();
  });

  it('affiche le rapport de seed et le consomme', () => {
    component();
    scenarios.seedReport.set({ message: 'Scénario prêt' } as never);
    TestBed.tick();

    expect(snack.open).toHaveBeenCalledWith('Scénario prêt', 'OK', {
      duration: 6000,
      panelClass: 'toast-success',
    });
    expect(scenarios.clearSeedReport).toHaveBeenCalled();
  });
});
