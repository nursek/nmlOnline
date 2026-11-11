export interface BattleReport {
  turn: number;
  sectorNumber: number;
  kind?: string | null;
  standoff: boolean;
  ranged?: boolean;
  rangedFire?: string | null;
  winnerPlayerId: number | null;
  winnerName: string | null;
  capturedBuildings: number;
  camps: BattleReportCamp[];
  casualties: BattleReportCasualty[];
  experienceGains: BattleReportExperienceGain[];
  forces?: BattlePlayerForce[] | null;
  highlights?: BattleHighlight[] | null;
}

export interface BattleReportCamp {
  playerId: number;
  playerName: string | null;
  eliminated: boolean;
  characterLost: boolean;
  playerEliminated: boolean;
  eliminationCause: string | null;
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

export interface BattleHighlight {
  kind: string;
  priority: number;
  visibility?: string | null;
  message: string;
}

export interface BattleUnitForce {
  number: number | null;
  type: string | null;
  displayName: string;
  experience: number | null;
  injured: boolean;
}

export interface BattleVehicleForce {
  displayName: string;
  aerial: boolean;
  crew: BattleUnitForce[];
}

export interface BattleBuildingForce {
  displayName: string;
  category: string | null;
}

export interface BattlePlayerForce {
  playerId: number;
  playerName: string | null;
  side: string;
  characters: BattleUnitForce[];
  vehicles: BattleVehicleForce[];
  infantry: BattleUnitForce[];
  buildings: BattleBuildingForce[];
}
