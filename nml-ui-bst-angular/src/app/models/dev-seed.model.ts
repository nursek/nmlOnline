// Mirror du ScenarioSummaryDto backend (dev scenario seeding).
export interface ScenarioActor {
  id: number;
  name: string;
}

export interface ScenarioUnit {
  id: number;
  unitClass: string;
  fromSector: number;
}

export interface ScenarioOrder {
  playerId: number;
  playerName: string;
  unitId: number;
  unitClass: string;
  fromSector: number;
  route: number[];
  orderId: number;
}

export interface SeedReport {
  turn?: number;
  route?: number[];
  message: string | null;
}

export interface ScenarioSummary extends SeedReport {
  turn: number;
  standoff: boolean;
  attacker?: ScenarioActor;
  defender: ScenarioActor;
  attackerUnit?: ScenarioUnit;
  defendersAdded: number;
  route?: number[];
  orderId?: number;
  orders?: ScenarioOrder[];
  message: string | null;
}

export interface ExchangeScenarioSummary extends SeedReport {
  turn: number;
  pendingOfferId: number | null;
  acceptedOfferId: number | null;
  message: string;
}
