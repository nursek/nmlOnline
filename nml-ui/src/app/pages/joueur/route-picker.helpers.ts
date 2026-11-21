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

/** Nombre de sélecteurs visibles : l'étape suivante apparaît après chaque étape choisie. */
export function visibleRouteSteps(maxHops: number, steps: readonly (number | null)[]): number {
  if (maxHops < 1) return 0;
  let count = 1;
  while (count < maxHops && steps[count - 1] != null) count++;
  return count;
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

/** Voisins éligibles pour l'étape `index`, sans revenir sur le départ ni sur une étape déjà choisie. */
export function routeStepOptions(
  sectors: readonly Sector[],
  from: number,
  steps: readonly (number | null)[],
  index: number,
): number[] {
  const previous = index === 0 ? from : steps[index - 1];
  if (previous == null) return [];

  const visited = new Set<number>([from]);
  for (const step of steps.slice(0, index)) {
    if (step == null) break;
    visited.add(step);
  }

  return neighborsOf(sectors, previous)
    .filter((number) => !visited.has(number))
    .sort((a, b) => a - b);
}
