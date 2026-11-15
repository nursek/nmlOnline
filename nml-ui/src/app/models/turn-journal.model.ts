import type { BattleReport } from './battle-report.model';
import type { SectorCapture, TransitCombatResult } from './movement-resolution.model';
import type { ResolvedBattle } from './turn-resolution.model';

export interface TurnExportSummary {
  battles: number;
  rangedCombats?: number;
  resolvedOrders: number | null;
  blockedOrders: number | null;
  captures: number;
  eliminatedPlayers: number;
  transitCombats: number;
}

export interface TurnExport {
  turn: number;
  exportedAt: string;
  finalized: boolean;
  summary: TurnExportSummary;
  battles: ResolvedBattle[];
  rangedCombats?: BattleReport[];
  captures: SectorCapture[];
  transitCombats: TransitCombatResult[];
  eliminatedPlayers: string[];
  gameFinished?: boolean;
  winnerPlayerId?: number | null;
  winnerName?: string | null;
  winnerPosthumous?: boolean;
  winnerAdjusted?: boolean;
  gazetteDraftTurn?: number | null;
}
