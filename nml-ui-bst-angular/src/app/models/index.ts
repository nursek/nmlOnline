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

// Correspondant à PlayerResourceDto du backend
export interface PlayerResource {
  id?: number;
  name: string;
  quantity: number;
  baseValue: number | null;
}

// Correspondant à ResourceSaleResponseDto du backend
export interface ResourceSaleResponse {
  message: string;
  saleValue: number;
  resourceName: string | null;
  quantitySold: number;
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
  // Localisation
  sectorNumber: number | null;
}

// Types pour les bâtiments
/**
 * IMPORTANT: This type is coupled to the BuildingType enum on the backend.
 * The string values here must exactly match the enum constants used by the API.
 *
 * If you add, remove or rename a building type in the backend enum,
 * you MUST update this union accordingly to keep the frontend and backend in sync.
 */
export type BuildingType = 'HEADQUARTERS' | 'WEAPON_CACHE' | 'BANK';

export interface Building {
  id: number | null;
  playerId: number | null;
  buildingType: BuildingType;
  displayName: string;
  // Stats de combat
  attack: number;
  defense: number;
  // État
  isDestroyed: boolean;
  isCaptured: boolean;
  capturedByPlayerId: number | null;
  capturedTurn: number | null;
  // Déplacement
  lastMovedTurn: number | null;
  canMove: boolean;
  moveCooldown: number;
  // Localisation
  sectorNumber: number | null;
  // Spécifique au QG
  isOperational?: boolean;
  storedWealth?: number;
  // Spécifique à la Cache d'armes
  maxCapacity?: number;
  currentCapacity?: number;
  availableCapacity?: number;
  fillPercentage?: number;
  storedEquipments?: EquipmentStack[];
  // Spécifique à la Banque
  hasMoved?: boolean;
  storedMoney?: number;
  currentVampirizeRate?: number;
  storedResources?: PlayerResource[];
}

export interface Player {
  id: number | null;
  name: string;
  stats: PlayerStats;
  equipments: EquipmentStack[];
  resources: PlayerResource[];
  sectors: Sector[];
  // Nouveaux champs
  character: GameCharacter | null;
  buildings: Building[];
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
