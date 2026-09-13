export interface BattleReport {
  turn: number;
  sectorNumber: number;
  standoff: boolean;
  winnerPlayerId: number | null;
  winnerName: string | null;
  capturedBuildings: number;
  camps: BattleReportCamp[];
  casualties: BattleReportCasualty[];
  experienceGains: BattleReportExperienceGain[];
}

export interface BattleReportCamp {
  playerId: number;
  playerName: string | null;
  eliminated: boolean;
  characterLost: boolean;
}

export interface BattleReportCasualty {
  playerId: number;
  playerName: string | null;
  label: string;
  category: string;
  unitType: string | null;
  unitNumber: number | null;
  experience: number | null;
}

export interface BattleReportExperienceGain {
  playerId: number;
  playerName: string | null;
  unitNumber: number;
  typeBefore: string;
  experienceBefore: number;
  gained: number;
  typeAfter: string;
  experienceAfter: number;
}
