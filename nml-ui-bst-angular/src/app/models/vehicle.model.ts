// Types pour les véhicules
export interface Vehicle {
  id: number | null;
  playerId: number | null;
  vehicleType: string;
  displayName: string;
  pdf: number;
  defense: number;
  isDestroyed: boolean;
  speed: number;
  capacity: number;
  passengerCount: number;
  hasPilot: boolean;
  sectorNumber: number | null;
  boardId: number | null;
}

// Types pour la boutique véhicules
export interface VehicleTypeInfo {
  name: string;
  displayName: string;
  cost: number;
  basePdf: number;
  baseDefense: number;
  speed: number;
  capacity: number;
  resistance: number;
  firesInTransit: boolean;
  aerial: boolean;
}
