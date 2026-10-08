import type { GameCharacter, MovementOrder, Player, Unit, Vehicle } from '../../models';
import type { ForcesTotals } from '../joueur/joueur.helpers';
import { entityTotals, powerOf } from '../joueur/joueur.helpers';

export interface OrderEntities {
  order: MovementOrder;
  units: Unit[];
  character: GameCharacter | null;
  vehicles: Vehicle[];
  unknownIds: number[];
  power: number;
}

export interface DestinationGroup {
  toSectorNumber: number;
  orders: OrderEntities[];
  units: Unit[];
  characters: GameCharacter[];
  vehicles: Vehicle[];
  totals: ForcesTotals;
  power: number;
}

type PlayerEntities = Pick<Player, 'sectors' | 'character'>;

interface EntityIndex {
  units: Map<number, Unit>;
  characters: Map<number, GameCharacter>;
  vehicles: Map<number, Vehicle>;
}

function indexEntities(player: PlayerEntities | null): EntityIndex {
  const index: EntityIndex = { units: new Map(), characters: new Map(), vehicles: new Map() };
  for (const sector of player?.sectors ?? []) {
    for (const unit of sector.army ?? []) index.units.set(unit.id, unit);
    if (sector.character?.id != null) index.characters.set(sector.character.id, sector.character);
    for (const vehicle of sector.vehicles ?? []) {
      if (vehicle.id != null) index.vehicles.set(vehicle.id, vehicle);
    }
  }
  if (player?.character?.id != null) index.characters.set(player.character.id, player.character);
  return index;
}

function resolveOrder(order: MovementOrder, index: EntityIndex): OrderEntities {
  const units: Unit[] = [];
  let character: GameCharacter | null = null;
  const unknownIds: number[] = [];
  for (const id of order.entityIds ?? []) {
    const unit = index.units.get(id);
    if (unit) {
      units.push(unit);
      continue;
    }
    const found = index.characters.get(id);
    if (found) {
      character = found;
      continue;
    }
    unknownIds.push(id);
  }

  const vehicles: Vehicle[] = [];
  if (order.vehicleId != null) {
    const vehicle = index.vehicles.get(order.vehicleId);
    if (vehicle) vehicles.push(vehicle);
    else unknownIds.push(order.vehicleId);
  }

  const totals = entityTotals(units, character ? [character] : [], vehicles);
  return { order, units, character, vehicles, unknownIds, power: powerOf(totals) };
}

export function groupOrders(
  orders: MovementOrder[],
  player: PlayerEntities | null,
): DestinationGroup[] {
  const index = indexEntities(player);
  const byDestination = new Map<number, OrderEntities[]>();
  for (const order of orders) {
    const detail = resolveOrder(order, index);
    const bucket = byDestination.get(order.toSectorNumber);
    if (bucket) bucket.push(detail);
    else byDestination.set(order.toSectorNumber, [detail]);
  }

  return [...byDestination.entries()]
    .sort(([a], [b]) => a - b)
    .map(([toSectorNumber, details]) => {
      details.sort((a, b) => a.order.fromSectorNumber - b.order.fromSectorNumber);
      const units = details.flatMap((d) => d.units);
      const characters = details.flatMap((d) => (d.character ? [d.character] : []));
      const vehicles = details.flatMap((d) => d.vehicles);
      const totals = entityTotals(units, characters, vehicles);
      return {
        toSectorNumber,
        orders: details,
        units,
        characters,
        vehicles,
        totals,
        power: powerOf(totals),
      };
    });
}
