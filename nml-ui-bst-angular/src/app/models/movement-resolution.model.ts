// Correspondant à AdminMovementOrderDto du backend : MovementOrder enrichi
// du nom du joueur, exposé à la console admin.
export interface AdminMovementOrder {
  id: number;
  turn: number;
  playerId: number;
  playerName: string | null;
  status: 'PENDING' | 'RESOLVED' | 'BLOCKED' | 'CANCELLED' | string;
  fromSectorNumber: number;
  toSectorNumber: number;
  route: number[];
  entityIds: number[];
  vehicleId?: number | null;
  statusMessage?: string | null;
}

// Conflit à la destination (mirror de DestinationConflictDto).
export interface DestinationConflict {
  sectorNumber: number;
  attackerPlayerId: number;
  attackerName: string | null;
  defenderPlayerId: number;
  defenderName: string | null;
}

// Combat de transit (mirror de TransitCombatResultDto).
export interface TransitCombatResult {
  sectorNumber: number;
  vehicleId: number;
  vehicleFired: boolean;
}

// Compte-rendu de résolution des mouvements (mirror de MovementResolutionResultDto).
export interface MovementResolutionResult {
  turn: number;
  resolved: AdminMovementOrder[];
  blocked: AdminMovementOrder[];
  conflicts: DestinationConflict[];
  transitCombats: TransitCombatResult[];
  hasConflicts: boolean;
  hasTransitCombats: boolean;
}

export type MovementStatusFilter = 'ALL' | 'PENDING' | 'RESOLVED' | 'BLOCKED' | 'CANCELLED';
