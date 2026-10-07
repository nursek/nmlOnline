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

// Conflit groupé (mirror de SectorConflictDto) : duel [arrivant, défenseur] ou impasse 3+.
export interface SectorConflictParticipant {
  playerId: number;
  playerName: string | null;
}

export interface SectorConflict {
  sectorNumber: number;
  standoff: boolean;
  participants: SectorConflictParticipant[];
}

// Combat de transit (mirror de TransitCombatResultDto).
export interface TransitCombatResult {
  sectorNumber: number;
  vehicleId: number;
  vehicleFired: boolean;
}

// Capture de secteur en fin de tour (mirror de SectorCaptureDto).
// onTheFly = secteur traversé capturé par une unité à double déplacement.
export interface SectorCapture {
  sectorNumber: number;
  playerId: number;
  playerName?: string | null;
  onTheFly: boolean;
}

// Compte-rendu de résolution des mouvements (mirror de MovementResolutionResultDto).
export interface MovementResolutionResult {
  turn: number;
  resolved: AdminMovementOrder[];
  blocked: AdminMovementOrder[];
  conflicts: SectorConflict[];
  transitCombats: TransitCombatResult[];
  capturedSectors: SectorCapture[];
  hasConflicts: boolean;
  hasTransitCombats: boolean;
}

export type MovementStatusFilter = 'ALL' | 'PENDING' | 'RESOLVED' | 'BLOCKED' | 'CANCELLED';
