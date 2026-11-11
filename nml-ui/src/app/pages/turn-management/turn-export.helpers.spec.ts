import type { ResolvedBattle, TurnFinalizeResult } from '../../models';
import { buildTurnExport, formatBattleLog } from './turn-export.helpers';

function battle(overrides: Partial<ResolvedBattle> = {}): ResolvedBattle {
  return {
    sectorNumber: 5,
    standoff: false,
    participants: null,
    attackerPlayerId: 1,
    attackerName: 'Alice',
    defenderPlayerId: 2,
    defenderName: 'Bob',
    success: true,
    message: null,
    winnerId: 1,
    winnerName: 'Alice',
    attackerCasualties: 0,
    defenderCasualties: 2,
    attackerInjured: 0,
    defenderInjured: 0,
    capturedBuildings: 0,
    attackerCharacterLost: false,
    defenderCharacterLost: false,
    defenderHeadquartersCaptured: false,
    eliminatedPlayers: null,
    battleLog: [
      { phase: 'PDF', outcome: 'INFO', message: '=== Phase PDF ===' },
      { phase: 'PDF', outcome: 'DAMAGE', message: 'Alice · X touche Bob · Y' },
      { phase: 'ATK', outcome: 'DESTROYED', message: 'Alice · X détruit Bob · Y' },
    ],
    ...overrides,
  };
}

function finalizedTurn(): TurnFinalizeResult {
  return {
    newTurn: 13,
    turnEnding: 12,
    resolvedOrders: 4,
    blockedOrders: 1,
    conflictsResolved: 1,
    transitCombatsCount: 1,
    transitCombats: [{ sectorNumber: 7, vehicleId: 3, vehicleFired: true }],
    capturedSectors: [{ sectorNumber: 8, playerId: 1, playerName: 'Alice', onTheFly: false }],
    eliminatedPlayers: ['Bob'],
    message: null,
  };
}

describe('buildTurnExport', () => {
  it('fusionne les batailles et le résultat de finalisation', () => {
    const finalizeResult = finalizedTurn();
    const exported = buildTurnExport({
      turn: 12,
      battles: [battle()],
      finalizeResult,
      exportedAt: new Date('2026-01-02T03:04:05Z'),
    });

    expect(exported.finalized).toBe(true);
    expect(exported.exportedAt).toBe('2026-01-02T03:04:05.000Z');
    expect(exported.summary).toEqual({
      battles: 1,
      resolvedOrders: 4,
      blockedOrders: 1,
      captures: 1,
      eliminatedPlayers: 1,
      transitCombats: 1,
    });
    expect(exported.battles).toHaveLength(1);
    expect(exported.captures).toEqual(finalizeResult.capturedSectors);
    expect(exported.transitCombats).toEqual(finalizeResult.transitCombats);
    expect(exported.eliminatedPlayers).toEqual(['Bob']);
  });

  it('marque un tour non finalisé et reprend le compte de transit de la session', () => {
    const exported = buildTurnExport({
      turn: 12,
      battles: [battle()],
      finalizeResult: null,
      transitCombatsCount: 2,
    });

    expect(exported.finalized).toBe(false);
    expect(exported.summary.transitCombats).toBe(2);
    expect(exported.captures).toEqual([]);
    expect(exported.eliminatedPlayers).toEqual([]);
  });
});

describe('formatBattleLog', () => {
  it('regroupe les entrées par phase dans l’ordre du journal', () => {
    const text = formatBattleLog(battle(), 12);

    expect(text).toContain('Combat — secteur 5 — tour 12');
    expect(text).toContain('Vainqueur : Alice');
    expect(text.indexOf('=== ATK ===')).toBeGreaterThan(text.indexOf('=== PDF ==='));
    expect(text).toContain('[DAMAGE] Alice · X touche Bob · Y');
    expect(text).toContain('[DESTROYED] Alice · X détruit Bob · Y');
  });
});
