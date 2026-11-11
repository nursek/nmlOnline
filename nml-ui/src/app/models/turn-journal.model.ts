import type { SectorCapture, TransitCombatResult } from './movement-resolution.model';
import type { ResolvedBattle } from './turn-resolution.model';

export interface TurnExportSummary {
  battles: number;
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
  captures: SectorCapture[];
  transitCombats: TransitCombatResult[];
  eliminatedPlayers: string[];
}
