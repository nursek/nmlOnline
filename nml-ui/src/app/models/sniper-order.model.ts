export type SniperOrderAction = 'TIR_PRECISION' | 'SURVEILLANCE';

// Correspondant à SniperOrderDto du backend.
export interface SniperOrder {
  id: number;
  turn: number;
  playerId: number;
  unitId: number;
  fromSectorNumber: number;
  targetSectorNumber: number;
  status: 'PENDING' | 'RESOLVED' | 'CANCELLED' | 'BLOCKED' | string;
  statusMessage?: string | null;
  action: SniperOrderAction | string;
}
