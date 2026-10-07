// Correspondant à MovementOrderDto du backend.
export interface MovementOrder {
  id: number;
  turn: number;
  playerId: number;
  status: 'PENDING' | 'RESOLVED' | 'BLOCKED' | 'CANCELLED' | string;
  fromSectorNumber: number;
  toSectorNumber: number;
  route: number[];
  entityIds: number[];
  vehicleId?: number | null;
  statusMessage?: string | null;
}
