import type { Sector } from '../../models';
import { routeForSteps, routeStepOptions, visibleRouteSteps } from './route-picker.helpers';

function sector(number: number, neighbors: number[]): Sector {
  return {
    number,
    name: `Secteur ${number}`,
    income: 0,
    army: [],
    stats: undefined,
    buildings: [],
    character: null,
    vehicles: [],
    ownerId: null,
    boardId: 1,
    color: null,
    resource: null,
    neighbors,
    x: 0,
    y: 0,
  };
}

// Graphe : 1-2, 2-3, 3-4, 2-5.
const graph = [
  sector(1, [2]),
  sector(2, [1, 3, 5]),
  sector(3, [2, 4]),
  sector(4, [3]),
  sector(5, [2]),
];

describe('route-picker.helpers', () => {
  it('construit le chemin jusqu’à la première étape vide', () => {
    expect(routeForSteps(1, [])).toEqual([1]);
    expect(routeForSteps(1, [2])).toEqual([1, 2]);
    expect(routeForSteps(1, [2, null])).toEqual([1, 2]);
    expect(routeForSteps(1, [2, 3])).toEqual([1, 2, 3]);
  });

  it('affiche l’étape suivante seulement après une étape choisie', () => {
    expect(visibleRouteSteps(0, [])).toBe(0);
    expect(visibleRouteSteps(1, [])).toBe(1);
    expect(visibleRouteSteps(2, [])).toBe(1);
    expect(visibleRouteSteps(2, [2])).toBe(2);
    expect(visibleRouteSteps(2, [2, null])).toBe(2);
    expect(visibleRouteSteps(3, [2])).toBe(2);
    expect(visibleRouteSteps(3, [2, 3])).toBe(3);
  });

  it('propose les voisins sans revenir sur le départ ni sur une étape déjà choisie', () => {
    expect(routeStepOptions(graph, 1, [null], 0)).toEqual([2]);
    expect(routeStepOptions(graph, 1, [2], 1)).toEqual([3, 5]);
    expect(routeStepOptions(graph, 1, [2, 3], 2)).toEqual([4]);
    expect(routeStepOptions(graph, 1, [2, null], 1)).toEqual([3, 5]);
    expect(routeStepOptions(graph, 1, [2, null], 2)).toEqual([]);
  });
});
