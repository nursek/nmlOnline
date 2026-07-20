import type { Building } from './building.model';
import type { GameCharacter, Unit } from './unit.model';
import type { Vehicle } from './vehicle.model';

// Correspondant à SectorDto du backend
export interface Sector {
  number: number | null;
  name: string;
  income: number | null;
  army: Unit[] | null;
  stats?: SectorStats;
  // Entités combattantes du secteur
  buildings?: Building[] | null;
  character?: GameCharacter | null;
  vehicles?: Vehicle[] | null;
  // Propriétés pour la carte
  ownerId: number | null;
  boardId?: number | null;
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
