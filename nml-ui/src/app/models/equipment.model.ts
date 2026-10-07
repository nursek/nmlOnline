import type { UnitClass } from './unit.model';

export interface Equipment {
  name: string;
  displayNames?: Record<string, string> | null;
  cost: number;
  pdfBonus: number;
  pdcBonus: number;
  armBonus: number;
  evasionBonus: number;
  vehicleBonus?: number;
  vehicleBonusTarget?: string | null;
  compatibleClass: UnitClass[];
  category: string;
}
