// Types pour l'authentification
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

export interface AuthResponse {
  accessToken: string;
  userId: number;
  username: string;
}

export interface User {
  id: number;
  username: string;
}

// Types pour les joueurs - Correspondant à PlayerStatsDto du backend
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

// Types pour les unités
export interface UnitType {
  name: string;
  code: string;
}

export interface Unit {
  id: number;
  type: UnitType;
  health: number;
  maxHealth: number;
  movement: number;
  maxMovement: number;
}

export interface EquipmentStack {
  equipment: Equipment;
  quantity: number;
  available: number;
}

// Correspondant à SectorDto du backend
export interface Sector {
  number: number | null;
  name: string;
  income: number | null;
  army: Unit[] | null;
  stats?: SectorStats;
}

export interface SectorStats {
  defenseBonus: number;
  resourceProduction: number;
}

export interface Player {
  id: number | null;
  name: string;
  stats: PlayerStats;
  equipments: EquipmentStack[];
  sectors: Sector[];
}

// Types pour les équipements et classes d'unités
export interface UnitClass {
  name: string;
  code: string;
  criticalChance: number | null;
  criticalMultiplier: number | null;
  damageReductionPdf: number | null;
  damageReductionPdc: number | null;
}

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

// Types pour le panier
export interface CartItem {
  equipment: Equipment;
  quantity: number;
}

