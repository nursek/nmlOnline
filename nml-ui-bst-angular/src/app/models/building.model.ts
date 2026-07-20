import type { PlayerResource } from './player.model';
import type { EquipmentStack } from './unit.model';

// Types pour les bâtiments
/**
 * IMPORTANT: This type is coupled to the BuildingType enum on the backend.
 * The string values here must exactly match the enum constants used by the API.
 *
 * If you add, remove or rename a building type in the backend enum,
 * you MUST update this union accordingly to keep the frontend and backend in sync.
 */
export const BUILDING_TYPES = ['HEADQUARTERS', 'WEAPON_CACHE', 'BANK'] as const;

export type BuildingType = (typeof BUILDING_TYPES)[number];

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
