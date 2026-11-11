import type { BattleLogEntry, ResolvedBattle, TurnExport, TurnFinalizeResult } from '../../models';

export type ExportFormat = 'json' | 'log';

export interface BattleLogGroup {
  phase: string;
  entries: BattleLogEntry[];
}

export interface BattleExport {
  turn: number;
  exportedAt: string;
  battle: ResolvedBattle;
}

export interface TurnExportInput {
  turn: number;
  battles: ResolvedBattle[];
  finalizeResult: TurnFinalizeResult | null;
  transitCombatsCount?: number;
  exportedAt?: Date;
}

export function groupBattleLog(entries: BattleLogEntry[] | null): BattleLogGroup[] {
  const groups: BattleLogGroup[] = [];
  for (const entry of entries ?? []) {
    let group = groups.at(-1);
    if (!group || group.phase !== entry.phase) {
      group = { phase: entry.phase, entries: [] };
      groups.push(group);
    }
    group.entries.push(entry);
  }
  return groups;
}

export function eliminationCauseLabel(cause: string): string {
  switch (cause) {
    case 'HEADQUARTERS_CAPTURED':
      return 'QG capturé';
    case 'MAIN_CHARACTER_KILLED':
      return 'personnage principal tué';
    default:
      return cause;
  }
}

export function buildBattleExport(
  battle: ResolvedBattle,
  turn: number,
  exportedAt = new Date(),
): BattleExport {
  return { turn, exportedAt: exportedAt.toISOString(), battle };
}

export function buildTurnExport(input: TurnExportInput): TurnExport {
  const { finalizeResult } = input;
  const captures = finalizeResult?.capturedSectors ?? [];
  const transitCombats = finalizeResult?.transitCombats ?? [];
  const eliminatedPlayers = finalizeResult?.eliminatedPlayers ?? [];
  return {
    turn: input.turn,
    exportedAt: (input.exportedAt ?? new Date()).toISOString(),
    finalized: finalizeResult !== null,
    summary: {
      battles: input.battles.length,
      resolvedOrders: finalizeResult?.resolvedOrders ?? null,
      blockedOrders: finalizeResult?.blockedOrders ?? null,
      captures: captures.length,
      eliminatedPlayers: eliminatedPlayers.length,
      transitCombats: finalizeResult?.transitCombatsCount ?? input.transitCombatsCount ?? 0,
    },
    battles: input.battles,
    captures,
    transitCombats,
    eliminatedPlayers,
  };
}

export function formatBattleLog(battle: ResolvedBattle, turn?: number): string {
  const lines: string[] = [];
  lines.push(
    `Combat — secteur ${battle.sectorNumber}${turn === undefined ? '' : ` — tour ${turn}`}`,
  );
  if (battle.standoff) {
    const camps = (battle.participants ?? []).map((p) => p.playerName ?? `#${p.playerId}`);
    lines.push(`Impasse mexicaine : ${camps.join(', ')}`);
  } else {
    lines.push(
      `${battle.attackerName ?? '#' + battle.attackerPlayerId} vs ` +
        `${battle.defenderName ?? '#' + battle.defenderPlayerId}`,
    );
  }
  lines.push(`Vainqueur : ${battle.winnerName ?? 'aucun'}`);
  for (const elimination of battle.eliminatedPlayers ?? []) {
    lines.push(
      `Éliminé : ${elimination.playerName ?? '#' + elimination.playerId} ` +
        `(${eliminationCauseLabel(elimination.cause)})`,
    );
  }
  for (const group of groupBattleLog(battle.battleLog)) {
    lines.push('', `=== ${group.phase} ===`);
    for (const entry of group.entries) {
      lines.push(`[${entry.outcome}] ${entry.message}`);
    }
  }
  return lines.join('\n');
}

export function formatTurnLog(data: TurnExport): string {
  const lines: string[] = [];
  lines.push(`Journal du tour ${data.turn}`, `Exporté le ${data.exportedAt}`);
  lines.push(
    data.finalized
      ? 'Statut : finalisé'
      : 'Statut : non finalisé — captures, éliminés et combats de transit seront ajoutés après finalisation.',
  );

  const summary = [`Batailles : ${data.summary.battles}`];
  if (data.summary.resolvedOrders !== null) {
    summary.push(`Ordres résolus : ${data.summary.resolvedOrders}`);
  }
  if (data.summary.blockedOrders !== null) {
    summary.push(`Ordres bloqués : ${data.summary.blockedOrders}`);
  }
  summary.push(
    `Captures : ${data.summary.captures}`,
    `Éliminés : ${data.summary.eliminatedPlayers}`,
    `Combats de transit : ${data.summary.transitCombats}`,
  );
  lines.push(summary.join(' | '));

  data.battles.forEach((battle, index) => {
    lines.push('', `=== Bataille ${index + 1}/${data.battles.length} ===`);
    lines.push(formatBattleLog(battle, data.turn));
  });

  if (data.captures.length) {
    lines.push('', '=== Captures ===');
    for (const capture of data.captures) {
      const suffix = capture.onTheFly ? ' (à la volée)' : '';
      lines.push(
        `- Secteur ${capture.sectorNumber} : ${capture.playerName ?? '#' + capture.playerId}${suffix}`,
      );
    }
  }

  if (data.transitCombats.length) {
    lines.push('', '=== Combats de transit ===');
    for (const combat of data.transitCombats) {
      lines.push(
        `- Secteur ${combat.sectorNumber} : véhicule #${combat.vehicleId} ` +
          `(${combat.vehicleFired ? 'a tiré' : 'passif'})`,
      );
    }
  }

  if (data.eliminatedPlayers.length) {
    lines.push('', '=== Éliminés ===');
    for (const name of data.eliminatedPlayers) {
      lines.push(`- ${name}`);
    }
  }

  return lines.join('\n');
}
