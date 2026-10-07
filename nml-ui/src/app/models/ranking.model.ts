export interface RankingEntry {
  playerId: number;
  name: string;
  power: number;
  comment: string | null;
}

export interface Rankings {
  turn: number;
  military: RankingEntry[];
  economic: RankingEntry[];
}
