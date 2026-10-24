export interface GameStatEntry {
  key: string;
  label: string;
  count: number;
}

export interface GameStatistics {
  units: GameStatEntry[];
  characters: number;
  vehicles: GameStatEntry[];
}
