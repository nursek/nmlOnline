import { UnitClass } from './player.model';

/**
 * Équipement
 * Correspond à EquipmentDto du backend
 */
export interface Equipment {
  name: string;
  cost: number;
  pdfBonus: number;
  pdcBonus: number;
  armBonus: number;
  evasionBonus: number;
  compatibleClass?: UnitClass[];
  category: string;
}
