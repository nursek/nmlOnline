// Types pour l'authentification
export interface LoginRequest {
  username: string;
  password: string;
  rememberMe: boolean;
}

// Correspond à AuthResponse du backend (token, id, name)
export interface AuthResponse {
  token: string;
  id: number;
  name: string;
}

export interface RefreshResponse {
  valid: boolean;
  token?: string;
  id?: number;
  name?: string;
  error?: string;
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
  level: number;
  baseAttack: number;
  baseDefense: number;
}

export interface Unit {
  id: number;
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

// Correspondant à SectorDto du backend
export interface Sector {
  number: number | null;
  name: string;
  income: number | null;
  army: Unit[] | null;
  stats?: SectorStats;
  // Propriétés pour la carte
  ownerId: number | null;
  color: string | null;
  resource: string | null;
  neighbors: number[];
  // Coordonnées pour le positionnement
  x: number | null;
  y: number | null;
}

export interface SectorStats {
  defenseBonus: number;
  resourceProduction: number;
  totalAtk?: number;
  totalPdf?: number;
  totalPdc?: number;
  totalDef?: number;
  totalArmor?: number;
  totalOffensive?: number;
  totalDefensive?: number;
  globalStats?: number;
}

export interface Player {
  id: number | null;
  name: string;
  stats: PlayerStats;
  equipments: EquipmentStack[];
  sectors: Sector[];
}

// Types pour la Board (carte du jeu)
export interface Board {
  id: number;
  name: string;
  mapImageUrl: string | null;
  svgOverlayUrl: string | null;
  sectors: { [key: number]: Sector };
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
