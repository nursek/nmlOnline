import { Equipment } from './equipment.model';

export interface PlayerStats {
  money: number;
  totalIncome: number;
  totalVehiclesValue: number;
  totalEquipmentValue: number;
  totalOffensivePower: number;
  totalDefensivePower: number;
  globalPower: number;
  totalEconomyPower: number;
  totalAtk: number;
  totalPdf: number;
  totalPdc: number;
  totalDef: number;
  totalArmor: number;
}

export interface UnitType {
  id: number;
  name: string;
}

export interface UnitClass {
  id: number;
  name: string;
}

export interface Unit {
  id: number;
  name: string;
  number: number;
  experience: number;
  type: UnitType;
  classes: UnitClass[];
  isInjured: boolean;
  equipments: Equipment[];
  attack: number;
  defense: number;
  pdf: number;
  pdc: number;
  armor: number;
  evasion: number;
}

export interface SectorStats {
  atk: number;
  def: number;
  pdf: number;
  pdc: number;
  armor: number;
}

export interface Sector {
  number: number;
  name: string;
  income: number;
  army: Unit[];
  stats: SectorStats;
}

export interface EquipmentStack {
  equipment: Equipment;
  quantity: number;
  available: number;
}

export interface Player {
  id?: number;
  name: string;
  stats: PlayerStats;
  equipments: EquipmentStack[];
  sectors: Sector[];
}
