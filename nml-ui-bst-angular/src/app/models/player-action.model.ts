export type PlayerActionType =
  | 'BUY_EQUIPMENT'
  | 'SELL_RESOURCE'
  | 'EQUIP_UNIT'
  | 'UNEQUIP_UNIT'
  | 'BUY_VEHICLE'
  | 'PLACE_VEHICLE'
  | 'MOVE_BUILDING'
  | 'SET_VEHICLE_CREW'
  | 'HARVEST_MONEY'
  | 'HARVEST_RESOURCE';

export type HarvestChoice = 'HARVEST_MONEY' | 'HARVEST_RESOURCE';

export type PlayerActionStatus = 'ACTIVE' | 'UNDONE';

export interface PlayerAction {
  id: number;
  turn: number;
  type: PlayerActionType;
  status: PlayerActionStatus;
  label: string;
  money: number | null;
  quantity: number | null;
  equipmentName: string | null;
  resourceName: string | null;
  unitId: number | null;
  vehicleId: number | null;
  buildingId: number | null;
  fromSectorNumber: number | null;
  toSectorNumber: number | null;
}
