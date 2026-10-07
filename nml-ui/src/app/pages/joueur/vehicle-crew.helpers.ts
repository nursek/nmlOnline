import type { Sector, Unit, Vehicle } from '../../models';

export interface CrewCandidate {
  id: number;
  name: string;
}

const isPilotClass = (unit: Unit): boolean => (unit.classes ?? []).some((c) => c.code === 'P');

const unitName = (unit: Unit): string => `${unit.type.name} n°${unit.number}`;

const occupiedElsewhere = (sector: Sector, vehicle: Vehicle): ReadonlySet<number> => {
  const occupied = new Set<number>();
  for (const other of sector.vehicles ?? []) {
    if (other.id === vehicle.id) {
      continue;
    }
    if (other.pilotId != null) {
      occupied.add(other.pilotId);
    }
    other.passengerIds.forEach((id) => occupied.add(id));
  }
  return occupied;
};

export function pilotCandidates(
  sector: Sector,
  vehicle: Vehicle,
  playerId: number,
): CrewCandidate[] {
  const candidates: CrewCandidate[] = [];
  const busy = occupiedElsewhere(sector, vehicle);

  for (const unit of sector.army ?? []) {
    if (unit.playerId !== playerId || busy.has(unit.id) || !isPilotClass(unit)) continue;
    candidates.push({ id: unit.id, name: unitName(unit) });
  }

  const character = sector.character;
  if (
    character?.id != null &&
    character.playerId === playerId &&
    !busy.has(character.id) &&
    !candidates.some((candidate) => candidate.id === character.id)
  ) {
    candidates.push({ id: character.id, name: character.name });
  }

  return candidates;
}

export function passengerCandidates(
  sector: Sector,
  vehicle: Vehicle,
  playerId: number,
  pilotId: number | null,
): CrewCandidate[] {
  const candidates: CrewCandidate[] = [];
  const busy = occupiedElsewhere(sector, vehicle);

  for (const unit of sector.army ?? []) {
    if (unit.playerId !== playerId || busy.has(unit.id) || unit.id === pilotId) continue;
    candidates.push({ id: unit.id, name: unitName(unit) });
  }

  const character = sector.character;
  if (
    character?.id != null &&
    character.playerId === playerId &&
    character.id !== pilotId &&
    !busy.has(character.id) &&
    !candidates.some((candidate) => candidate.id === character.id)
  ) {
    candidates.push({ id: character.id, name: character.name });
  }

  return candidates;
}
