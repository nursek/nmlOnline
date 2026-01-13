import { Equipment } from './equipment.model';

/**
 * Statistiques globales du joueur
 * Correspond à PlayerStatsDto du backend
 */
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

/**
 * Type d'unité (BRUTE, MALFRAT, VOYOU, LARBIN)
 * Correspond à UnitTypeDto du backend
 */
export interface UnitType {
  name: string;
  level?: number;
  minExp?: number;
  maxExp?: number;
  baseAttack?: number;
  baseDefense?: number;
  maxFirearms?: number;
  maxMeleeWeapons?: number;
  maxDefensiveEquipment?: number;
}

/**
 * Classe d'unité (TIREUR, MASTODONTE, etc.)
 * Correspond à UnitClassDto du backend
 */
export interface UnitClass {
  name: string;
  code?: string;
  criticalChance?: number;
  criticalMultiplier?: number;
  damageReductionPdf?: number;
  damageReductionPdc?: number;
}

/**
 * Unité de combat
 * Correspond à UnitDto du backend
 */
export interface Unit {
  id: number;
  name?: string;
  number?: number;
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

/**
 * Statistiques d'un secteur
 * Correspond à SectorStatsDto du backend
 */
export interface SectorStats {
  totalAtk: number;
  totalPdf: number;
  totalPdc: number;
  totalDef: number;
  totalArmor: number;
  totalOffensive: number;
  totalDefensive: number;
  globalStats: number;
}

/**
 * Secteur contrôlé par le joueur
 * Correspond à SectorDto du backend
 */
export interface Sector {
  number: number;
  name: string;
  income: number;
  army: Unit[];
  stats: SectorStats;
}

/**
 * Stock d'équipement du joueur
 * Correspond à EquipmentStackDto du backend
 */
export interface EquipmentStack {
  equipment: Equipment;
  quantity: number;
  available: number;
}

/**
 * Joueur complet
 * Correspond à PlayerDto du backend
 */
export interface Player {
  id?: number;
  name: string;
  stats: PlayerStats;
  equipments: EquipmentStack[];
  sectors: Sector[];
}
