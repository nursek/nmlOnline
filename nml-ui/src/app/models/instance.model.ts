export type InstanceRole = 'ADMIN' | 'MJ' | 'PLAYER';

export interface GameInstance {
  id: number;
  name: string;
  currentTurn: number;
  playerCount: number;
  playerId: number | null;
  playerName: string | null;
  eliminated: boolean;
  role: InstanceRole | null;
}
