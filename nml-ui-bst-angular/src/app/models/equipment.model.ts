import type { UnitClass } from './unit.model';

export interface Equipment {
  name: string;
  cost: number;
  pdfBonus: number;
  pdcBonus: number;
  armBonus: number;
  evasionBonus: number;
  compatibleClass: UnitClass[];
  category: string;
}
