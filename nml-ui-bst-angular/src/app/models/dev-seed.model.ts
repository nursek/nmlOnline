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

export interface ScenarioSummary {
  turn: number;
  attacker: ScenarioActor;
  defender: ScenarioActor;
  attackerUnit: ScenarioUnit;
  defendersAdded: number;
  route: number[];
  orderId: number;
  message: string | null;
}
