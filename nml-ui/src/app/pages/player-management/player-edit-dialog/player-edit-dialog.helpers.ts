import type { AdminPlayerEdit, Player, Sector, Vehicle } from '../../../models';

/** Le dialog d'équipage du joueur ne connaît que le secteur : un véhicule hors secteur possédé n'est pas équipable. */
export function sectorOfVehicle(player: Player | null, vehicle: Vehicle): Sector | null {
  if (!player || vehicle.sectorNumber === null || vehicle.sectorNumber === undefined) {
    return null;
  }
  return player.sectors.find((sector) => sector.number === vehicle.sectorNumber) ?? null;
}

/** Noms des occupants via l'édition complète : le VehicleDto ne transporte que des ids. */
export function crewLabel(edit: AdminPlayerEdit | null, vehicle: Vehicle): string {
  if (!edit) return '';
  const byId = new Map<number, string>();
  for (const unit of edit.units) {
    byId.set(unit.id, `${unit.type.name} n°${unit.number}`);
  }
  const character = edit.player.character;
  if (character?.id != null) {
    byId.set(character.id, character.name);
  }
  const pilot =
    vehicle.pilotId === null ? '—' : (byId.get(vehicle.pilotId) ?? `#${vehicle.pilotId}`);
  const passengers = vehicle.passengerIds.map((id) => byId.get(id) ?? `#${id}`);
  return `Pilote ${pilot}` + (passengers.length > 0 ? ` · ${passengers.join(', ')}` : '');
}
