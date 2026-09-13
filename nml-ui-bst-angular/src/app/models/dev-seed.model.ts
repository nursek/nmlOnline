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

export interface ScenarioSummary {
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
