// Mirror des DTO backend de la résolution pas-à-pas par hop
// (TurnResolutionStateDto, PendingConflictDto, ResolvedBattleDto, TurnFinalizeResultDto).

export interface PendingConflictParticipant {
  playerId: number;
  playerName: string | null;
  submittedAt: string | null;
}

export interface PendingConflict {
  conflictId: number;
  sectorNumber: number;
  standoff: boolean;
  participants: PendingConflictParticipant[];
  attackerPlayerId: number | null;
  attackerName: string | null;
  defenderPlayerId: number | null;
  defenderName: string | null;
}

export interface StandoffParticipantResult {
  playerId: number;
  playerName: string | null;
  casualties: number;
  injured: number;
  characterLost: boolean;
  eliminated: boolean;
}

export interface BattleLogEntry {
  phase: string;
  outcome: string;
  message: string;
}

export interface ResolvedBattle {
  sectorNumber: number;
  standoff: boolean;
  participants: StandoffParticipantResult[] | null;
  attackerPlayerId: number | null;
  attackerName: string | null;
  defenderPlayerId: number | null;
  defenderName: string | null;
  success: boolean;
  message: string | null;
  winnerId: number | null;
  winnerName: string | null;
  attackerCasualties: number;
  defenderCasualties: number;
  attackerInjured: number;
  defenderInjured: number;
  capturedBuildings: number;
  attackerCharacterLost: boolean;
  defenderCharacterLost: boolean;
  defenderHeadquartersCaptured: boolean;
  battleLog: BattleLogEntry[] | null;
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
