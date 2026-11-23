import type { Sector } from '../../models';
import { routeAfterSectorClick, routeForSteps } from './route-picker.helpers';

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

// Graphe : 1-2, 1-6, 2-3, 3-4, 2-5.
const graph = [
  sector(1, [2, 6]),
  sector(2, [1, 3, 5]),
  sector(3, [2, 4]),
  sector(4, [3]),
  sector(5, [2]),
  sector(6, [1]),
];

describe('route-picker.helpers', () => {
  it('construit le chemin jusqu’à la première étape vide', () => {
    expect(routeForSteps(1, [])).toEqual([1]);
    expect(routeForSteps(1, [2])).toEqual([1, 2]);
    expect(routeForSteps(1, [2, null])).toEqual([1, 2]);
    expect(routeForSteps(1, [2, 3])).toEqual([1, 2, 3]);
  });

  it('construit un chemin en cliquant les secteurs de la carte', () => {
    expect(routeAfterSectorClick([], 2, 1, graph, 2)).toEqual({ route: [1, 2], error: null });
    expect(routeAfterSectorClick([1, 2], 3, 1, graph, 2)).toEqual({
      route: [1, 2, 3],
      error: null,
    });
    expect(routeAfterSectorClick([1, 2, 3], 2, 1, graph, 3)).toEqual({
      route: [1, 2],
      error: null,
    });
    expect(routeAfterSectorClick([1, 2], 1, 1, graph, 2)).toEqual({ route: [], error: null });
  });

  it('repart du départ quand on clique un voisin du départ hors du chemin', () => {
    expect(routeAfterSectorClick([1, 2], 6, 1, graph, 2)).toEqual({ route: [1, 6], error: null });
  });

  it('refuse un secteur non adjacent ou une portée dépassée', () => {
    expect(routeAfterSectorClick([], 4, 1, graph, 2).route).toEqual([]);
    expect(routeAfterSectorClick([], 4, 1, graph, 2).error).toContain('adjacent');
    expect(routeAfterSectorClick([1, 2], 5, 1, graph, 1).error).toContain('Portée maximale');
  });
});
