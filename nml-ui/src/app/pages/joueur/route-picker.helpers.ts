import type { Sector } from '../../models';
import { neighborsOf } from '../../core/movement.helpers';

/** Chemin final : départ + étapes non nulles, jusqu'à la première étape vide. */
export function routeForSteps(from: number, steps: readonly (number | null)[]): number[] {
  const route = [from];
  for (const step of steps) {
    if (step == null) break;
    route.push(step);
  }
  return route;
}

export interface RouteClickResult {
  route: number[];
  error: string | null;
}

/** Étend/raccourcit le chemin en cliquant un secteur de la carte. */
export function routeAfterSectorClick(
  currentRoute: readonly number[],
  clicked: number,
  from: number,
  sectors: readonly Sector[],
  maxHops: number,
): RouteClickResult {
  const route = currentRoute.length ? [...currentRoute] : [from];
  if (clicked === from) return { route: [], error: null };

  const existing = route.indexOf(clicked);
  if (existing > 0) return { route: route.slice(0, existing + 1), error: null };

  const last = route[route.length - 1];
  const adjacentToLast = neighborsOf(sectors, last).includes(clicked);
  if (!adjacentToLast && route.length > 1 && neighborsOf(sectors, from).includes(clicked)) {
    return { route: [from, clicked], error: null };
  }

  if (route.length - 1 >= maxHops) {
    return { route: [...currentRoute], error: `Portée maximale : ${maxHops} secteur(s) atteinte.` };
  }

  if (!adjacentToLast) {
    return {
      route: [...currentRoute],
      error: 'Ce secteur n’est pas adjacent à la dernière étape.',
    };
  }
  return { route: [...route, clicked], error: null };
}
