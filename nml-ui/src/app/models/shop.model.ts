import type { Equipment } from './equipment.model';
import type { PlayerResource, ResourceSaleResponse } from './player.model';
import type { UnitCatalogEntry, UnitClass } from './unit.model';
import type { VehicleTypeInfo } from './vehicle.model';

export interface CartItem {
  equipment: Equipment;
  quantity: number;
}

export interface BuyEquipmentItem {
  name: string;
  quantity: number;
}

export interface VehicleCartItem {
  vehicleType: VehicleTypeInfo;
  quantity: number;
}

export interface SellCartItem {
  resource: PlayerResource;
  quantity: number;
}

export interface BuyVehicleBatchItem {
  vehicleType: string;
  quantity: number;
}

export interface UnitCartItem {
  unitType: UnitCatalogEntry;
  unitClass: UnitClass;
  quantity: number;
}

export interface BuyUnitBatchItem {
  unitType: string;
  unitClass: string;
  quantity: number;
}

export interface SellResourceBatchItem {
  playerResourceId: number;
  quantity: number;
}

export interface ResourceBatchSaleResponse {
  message: string;
  totalValue: number;
  sales: ResourceSaleResponse[];
}
