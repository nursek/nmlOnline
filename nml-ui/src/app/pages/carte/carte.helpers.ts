import type { Sector } from '../../models';
import { optimizedImage } from '../../core/optimized-image';
import { slugify } from '../../core/slug';

export interface FrontBorders {
  mine: number[];
  enemies: number[];
}

// Bordure = contact avec un joueur non allié ; les neutres ne comptent jamais comme front.
export function frontBorders(
  sectors: readonly Sector[],
  playerId: number | null,
  alliedIds: ReadonlySet<number>,
): FrontBorders {
  if (playerId == null) return { mine: [], enemies: [] };
  const byNumber = new Map<number, Sector>();
  sectors.forEach((sector) => {
    if (sector.number !== null) byNumber.set(sector.number, sector);
  });
  const mine: number[] = [];
  const enemies = new Set<number>();
  for (const sector of sectors) {
    if (sector.ownerId !== playerId || sector.number === null) continue;
    mine.push(sector.number);
    for (const neighborNumber of sector.neighbors) {
      const owner = byNumber.get(neighborNumber)?.ownerId;
      if (owner != null && owner !== playerId && !alliedIds.has(owner)) {
        enemies.add(neighborNumber);
      }
    }
  }
  return { mine, enemies: [...enemies].sort((a, b) => a - b) };
}

// Vignette du calque « Ressources » : dérivé WebP, les PNG d'origine pèsent jusqu'à 2 Mo.
export function resourceIconSrc(name: string): string {
  return optimizedImage(`assets/shop/resources/${slugify(name)}.png`).src;
}
