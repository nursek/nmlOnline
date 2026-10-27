import type { Equipment } from './equipment.model';
import type { Player } from './player.model';
import type { Unit, UnitClass } from './unit.model';
import type { Vehicle, VehicleTypeInfo } from './vehicle.model';

export interface AdminResourceInfo {
  name: string;
  baseValue: number;
}

export interface AdminUnitTypeInfo {
  name: string;
  level: number;
  minExp: number;
  maxExp: number;
  baseAttack: number;
  baseDefense: number;
  maxFirearms: number;
  maxMeleeWeapons: number;
  maxDefensiveEquipment: number;
}

export interface AdminCatalog {
  equipments: Equipment[];
  resources: AdminResourceInfo[];
  unitTypes: AdminUnitTypeInfo[];
  unitClasses: UnitClass[];
  vehicleTypes: VehicleTypeInfo[];
}

export interface AdminPlayerEdit {
  player: Player;
  units: Unit[];
  vehicles: Vehicle[];
}
