export interface PodiumPlace {
  place: number;
  playerId: number;
  playerName: string;
  race: string | null;
  characterName: string | null;
  eliminatedTurn: number | null;
  eliminationCause: string | null;
  definitive: boolean;
  killerPlayerId: number | null;
  killerName: string | null;
  kills: number;
  assists: number;
  alive: boolean;
  victims: string[];
  assistedVictims: string[];
}

export interface GamePodium {
  finished: boolean;
  finalTurn: number | null;
  finishedAt: string | null;
  totalPlayers: number;
  activePlayers: number;
  posthumous: boolean;
  championAdjusted: boolean;
  lastGazetteTurn: number | null;
  champion: PodiumPlace | null;
  places: PodiumPlace[];
}
