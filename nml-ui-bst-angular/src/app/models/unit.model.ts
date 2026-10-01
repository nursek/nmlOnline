import type { Equipment } from './equipment.model';

export interface UnitType {
  name: string;
  level: number;
  baseAttack: number;
  baseDefense: number;
  maxFirearms: number;
  maxMeleeWeapons: number;
  maxDefensiveEquipment: number;
}

export interface Unit {
  id: number;
  playerId?: number | null;
  number: number;
  experience: number;
  type: UnitType;
  classes: UnitClass[];
  isInjured: boolean;
  equipments: Equipment[];
  // Stats calculées
  attack: number;
  defense: number;
  pdf: number;
  pdc: number;
  armor: number;
  evasion: number;
}

export interface EquipmentStack {
  equipment: Equipment;
  quantity: number;
  available: number;
}

// Types pour les personnages principaux (leaders)
export interface GameCharacter {
  id: number | null;
  playerId: number | null;
  name: string;
  // Stats de base (fixes)
  baseAttack: number;
  baseDefense: number;
  basePdf: number;
  basePdc: number;
  baseArmor: number;
  baseEvasion: number;
  sectorNumber: number | null;
}

export interface UnitClass {
  name: string;
  code: string;
  criticalChance: number | null;
  criticalMultiplier: number | null;
  damageReductionPdf: number | null;
  damageReductionPdc: number | null;
  /** Nombre max de secteurs parcourus par tour (LEGER = 2, autres = 1). */
  maxMovementHops: number;
}

// Catalogue de recrutement
export interface UnitCatalogEntry {
  name: string;
  cost: number;
  baseAttack: number;
  baseDefense: number;
  availableFromTurn: number;
  maxPerTurn: number;
  purchasedThisTurn: number;
  availableNow: boolean;
}

export interface UnitCatalog {
  classes: UnitClass[];
  entries: UnitCatalogEntry[];
}
