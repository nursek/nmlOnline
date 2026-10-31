import { frontBorders, resourceIconSrc } from './carte.helpers';
import type { Sector } from '../../models';

function sector(number: number, ownerId: number | null, neighbors: number[]): Sector {
  return {
    number,
    name: `S${number}`,
    ownerId,
    neighbors,
    income: null,
    army: null,
    color: null,
    resource: null,
    x: null,
    y: null,
  };
}

describe('frontBorders', () => {
  const sectors = [
    sector(1, 10, [2, 3, 4]), // mien
    sector(2, 20, [1]), // ennemi
    sector(3, null, [1]), // neutre
    sector(4, 30, [1]), // allié
    sector(5, 20, [6]), // ennemi sans contact
    sector(6, 10, [5]), // mien
  ];

  it('garde les contacts ennemis, ignore neutres et alliés', () => {
    expect(frontBorders(sectors, 10, new Set([30]))).toEqual({ mine: [1, 6], enemies: [2, 5] });
  });

  it('sans joueur identifié, aucun front', () => {
    expect(frontBorders(sectors, null, new Set())).toEqual({ mine: [], enemies: [] });
  });
});

describe('resourceIconSrc', () => {
  it('résout la vignette optimisée depuis le nom affiché', () => {
    expect(resourceIconSrc("Caisse d'armes")).toBe(
      '/assets/_opt/shop/resources/caisse-d-armes.320.webp',
    );
    expect(resourceIconSrc('Prométhéum')).toBe('/assets/_opt/shop/resources/prometheum.320.webp');
  });
});
