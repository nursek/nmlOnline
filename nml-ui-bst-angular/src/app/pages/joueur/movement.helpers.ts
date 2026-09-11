import type { GameCharacter, Sector, Unit } from '../../models';

export type SectorKind = 'own' | 'neutral' | 'enemy' | 'unknown';

export interface MovableEntities {
  units: Unit[];
  character: GameCharacter | null;
}

/** Portée d'une unité = max de ses classes (défaut 1). */
export function unitMaxHops(unit: Unit): number {
  const classes = unit.classes ?? [];
  if (classes.length === 0) return 1;
  return classes.reduce((max, c) => Math.max(max, c.maxMovementHops ?? 1), 1);
}

/** Portée du groupe = min des entités : le backend valide la route hop par hop. */
export function groupMaxHops(units: Unit[], character: GameCharacter | null): number {
  const hops = units.map(unitMaxHops);
  // Le personnage n'a pas de classe : 1 secteur, comme MovementService.validateFootHops.
  if (character) hops.push(1);
  return hops.length ? Math.min(...hops) : 0;
}

const sectorOf = (sectors: Sector[], number: number): Sector | null =>
  sectors.find((s) => s.number === number) ?? null;

const neighborsOf = (sectors: Sector[], number: number): number[] =>
  sectorOf(sectors, number)?.neighbors ?? [];

/** Secteurs atteignables en <= maxHops via une route adjacente (BFS), `from` exclu. */
export function reachableTargets(sectors: Sector[], from: number, maxHops: number): number[] {
  if (maxHops < 1) return [];

  const result = new Set<number>();
  let frontier = new Set<number>(neighborsOf(sectors, from));
  frontier.forEach((n) => result.add(n));

  for (let hop = 2; hop <= maxHops; hop++) {
    const next = new Set<number>();
    for (const current of frontier) {
      for (const n of neighborsOf(sectors, current)) {
        if (n !== from && !result.has(n)) next.add(n);
      }
    }
    next.forEach((n) => result.add(n));
    frontier = next;
  }

  return [...result].sort((a, b) => a - b);
}

/** Route adjacente la plus courte de `from` vers `to`, bornée par maxHops (BFS). */
export function findRoute(sectors: Sector[], from: number, to: number, maxHops: number): number[] {
  if (from === to || maxHops < 1) return [];

  const queue: number[][] = [[from]];
  const visited = new Set<number>([from]);

  while (queue.length > 0) {
    const path = queue.shift()!;
    if (path.length - 1 >= maxHops) continue;

    for (const next of neighborsOf(sectors, path[path.length - 1])) {
      if (visited.has(next)) continue;

      if (next === to) return [...path, next];

      visited.add(next);
      queue.push([...path, next]);
    }
  }

  return [];
}

export function sectorKind(sector: Sector | null | undefined, playerId: number | null): SectorKind {
  if (!sector || playerId == null) return 'unknown';
  if (sector.ownerId == null) return 'neutral';
  return sector.ownerId === playerId ? 'own' : 'enemy';
}

/**
 * Entités déplaçables d'un secteur : propriété stricte (le backend exige
 * `playerId.equals`) et hors des ordres PENDING existants.
 */
export function movableEntities(
  sector: Sector,
  playerId: number,
  pendingIds: ReadonlySet<number>,
): MovableEntities {
  const units = (sector.army ?? []).filter((u) => u.playerId === playerId && !pendingIds.has(u.id));
  const character = sector.character;
  const movableCharacter =
    character &&
    character.playerId === playerId &&
    character.id != null &&
    !pendingIds.has(character.id)
      ? character
      : null;
  return { units, character: movableCharacter };
}
