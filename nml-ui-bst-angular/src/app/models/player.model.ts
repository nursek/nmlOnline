import type { Building } from './building.model';
import type { EquipmentStack, GameCharacter } from './unit.model';
import type { Sector } from './sector.model';

// Types pour les joueurs - Correspondant à PlayerStatsDto du backend
export interface PlayerStats {
  money: number;
  startingMoneyTotal?: number;
  startingMoneyRemaining?: number;
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

export interface Player {
  id: number | null;
  name: string;
  // Présent sur la vue publique (autres joueurs) : état privé absent.
  sectorCount?: number;
  stats: PlayerStats;
  equipments: EquipmentStack[];
  resources: PlayerResource[];
  sectors: Sector[];
  character: GameCharacter | null;
  buildings: Building[];
}
