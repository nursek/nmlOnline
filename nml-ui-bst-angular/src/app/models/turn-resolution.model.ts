// Mirror des DTO backend de la résolution pas-à-pas par hop
// (TurnResolutionStateDto, PendingConflictDto, ResolvedBattleDto, TurnFinalizeResultDto).

export interface PendingConflict {
  conflictId: number;
  sectorNumber: number;
  attackerPlayerId: number;
  attackerName: string | null;
  defenderPlayerId: number;
  defenderName: string | null;
}

export interface ResolvedBattle {
  sectorNumber: number;
  attackerPlayerId: number;
  attackerName: string | null;
  defenderPlayerId: number;
  defenderName: string | null;
  success: boolean;
  message: string | null;
  winnerId: number | null;
  winnerName: string | null;
  attackerCasualties: number;
  defenderCasualties: number;
  attackerInjured: number;
  defenderInjured: number;
}

export interface TurnResolutionState {
  active: boolean;
  turnEnding: number;
  currentStep: number;
  maxSteps: number;
  pendingConflicts: PendingConflict[];
  resolvedConflicts: ResolvedBattle[];
  transitCombatsCount: number;
  canAdvance: boolean;
  canFinalize: boolean;
  allDone: boolean;
}

export interface TurnFinalizeResult {
  newTurn: number;
  turnEnding: number;
  resolvedOrders: number;
  blockedOrders: number;
  conflictsResolved: number;
  transitCombats: number;
  message: string | null;
}
