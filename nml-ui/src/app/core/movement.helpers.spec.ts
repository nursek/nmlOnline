import {
  findRoute,
  groupMaxHops,
  movableEntities,
  reachableTargets,
  sectorKind,
  unitMaxHops,
} from './movement.helpers';
import type { GameCharacter, Sector, Unit, Vehicle } from '../../models';

function unit(id: number, playerId: number | null, maxHops: number): Unit {
  return {
    id,
    playerId,
    number: id,
    experience: 0,
    type: {
      name: 'LARBIN',
      level: 1,
      baseAttack: 10,
      baseDefense: 10,
      maxFirearms: 1,
      maxMeleeWeapons: 1,
      maxDefensiveEquipment: 1,
    },
    classes: [
      {
        name: 'Léger',
        code: 'L',
        criticalChance: null,
        criticalMultiplier: null,
        damageReductionPdf: null,
        damageReductionPdc: null,
        maxMovementHops: maxHops,
      },
    ],
    isInjured: false,
    equipments: [],
    attack: 10,
    defense: 10,
    pdf: 0,
    pdc: 0,
    armor: 0,
    evasion: 0,
  };
}

function character(id: number | null, playerId: number | null): GameCharacter {
  return {
    id,
    playerId,
    name: 'Mortarion',
    baseAttack: 100,
    baseDefense: 250,
    basePdf: 100,
    basePdc: 50,
    baseArmor: 20,
    baseEvasion: 0,
    sectorNumber: 1,
  };
}

function sector(
  number: number,
  ownerId: number | null,
  neighbors: number[] = [],
  opts: Partial<Sector> = {},
): Sector {
  return {
    number,
    name: `Secteur ${number}`,
    income: 2000,
    army: [],
    stats: undefined,
    buildings: [],
    character: null,
    vehicles: [],
    ownerId,
    boardId: 1,
    color: null,
    resource: null,
    neighbors,
    x: 0,
    y: 0,
    ...opts,
  };
}

function vehicle(id: number, playerId: number, extra: Partial<Vehicle> = {}): Vehicle {
  return {
    id,
    playerId,
    vehicleType: 'VTT_LEGER',
    displayName: 'VTT léger',
    pdf: 0,
    defense: 50,
    isDestroyed: false,
    speed: 2,
    capacity: 10,
    passengerCount: 0,
    hasPilot: true,
    pilotId: 99,
    pilotName: 'PILOTE n°1',
    passengerIds: [],
    sectorNumber: 1,
    boardId: 1,
    ...extra,
  };
}

// Graphe : 1-2, 2-3, 3-4, 2-5.
const graph: Sector[] = [
  sector(1, 1, [2]),
  sector(2, 2, [1, 3, 5]),
  sector(3, 2, [2, 4]),
  sector(4, 2, [3]),
  sector(5, 2, [2]),
];

describe('movement.helpers', () => {
  describe('portée', () => {
    it('utilise la portée maximale des classes', () => {
      expect(unitMaxHops(unit(1, 1, 2))).toBe(2);
    });

    it('borne le groupe par la portée minimale', () => {
      expect(groupMaxHops([unit(1, 1, 2), unit(2, 1, 1)], null)).toBe(1);
      expect(groupMaxHops([unit(1, 1, 2)], null)).toBe(2);
    });

    it('compte le personnage comme 1 secteur', () => {
      expect(groupMaxHops([unit(1, 1, 2)], character(9, 1))).toBe(1);
      expect(groupMaxHops([], null)).toBe(0);
    });

    it('borne le groupe par la vitesse du véhicule', () => {
      expect(groupMaxHops([], null, [vehicle(50, 1)])).toBe(2);
      expect(groupMaxHops([unit(1, 1, 1)], null, [vehicle(50, 1)])).toBe(1);
    });
  });

  describe('reachableTargets', () => {
    it('ne retient que les voisins à 1 hop', () => {
      expect(reachableTargets(graph, 1, 1)).toEqual([2]);
    });

    it('étend la frontière sur plusieurs hops sans inclure le départ', () => {
      expect(reachableTargets(graph, 1, 2)).toEqual([2, 3, 5]);
      expect(reachableTargets(graph, 1, 3)).toEqual([2, 3, 4, 5]);
    });
  });

  describe('findRoute', () => {
    it('retourne la route directe', () => {
      expect(findRoute(graph, 1, 2, 1)).toEqual([1, 2]);
    });

    it('trouve un chemin à 2 hops', () => {
      expect(findRoute(graph, 1, 3, 2)).toEqual([1, 2, 3]);
    });

    it('retourne un chemin vide si la portée est insuffisante', () => {
      expect(findRoute(graph, 1, 3, 1)).toEqual([]);
      expect(findRoute(graph, 1, 4, 2)).toEqual([]);
    });
  });

  describe('sectorKind', () => {
    it('distingue interne, neutre et ennemi', () => {
      expect(sectorKind(sector(1, 1), 1)).toBe('own');
      expect(sectorKind(sector(2, null), 1)).toBe('neutral');
      expect(sectorKind(sector(3, 2), 1)).toBe('enemy');
      expect(sectorKind(null, 1)).toBe('unknown');
    });

    it('marque le secteur d’un allié', () => {
      expect(sectorKind(sector(3, 2), 1, new Set([2]))).toBe('ally');
    });
  });

  describe('movableEntities', () => {
    it('exclut les entités non possédées ou déjà ordonnées', () => {
      const s = sector(1, 1, [], {
        army: [unit(10, 1, 1), unit(11, 1, 1), unit(12, null, 1), unit(13, 2, 1)],
        character: character(99, 1),
      });

      const movable = movableEntities(s, 1, new Set([11]));

      expect(movable.units.map((u) => u.id)).toEqual([10]);
      expect(movable.character?.id).toBe(99);
    });

    it('ignore le personnage sans id ou déjà ordonné', () => {
      const noId = sector(1, 1, [], { character: character(null, 1) });
      expect(movableEntities(noId, 1, new Set()).character).toBeNull();

      const ordered = sector(1, 1, [], { character: character(99, 1) });
      expect(movableEntities(ordered, 1, new Set([99])).character).toBeNull();
    });

    it('ne retient que les véhicules possédés, pilotés et hors ordre', () => {
      const s = sector(1, 1, [], {
        vehicles: [
          vehicle(50, 1),
          vehicle(51, 1, { hasPilot: false }),
          vehicle(52, 2),
          vehicle(53, 1, { isDestroyed: true }),
        ],
      });

      const movable = movableEntities(s, 1, new Set(), new Set([50]));

      expect(movable.vehicles.map((v) => v.id)).toEqual([]);
      expect(movableEntities(s, 1, new Set(), new Set()).vehicles.map((v) => v.id)).toEqual([50]);
    });

    it('exclut pilote et passagers du déplacement à pied', () => {
      const s = sector(1, 1, [], {
        army: [unit(10, 1, 1), unit(11, 1, 1), unit(12, 1, 1)],
        vehicles: [vehicle(50, 1, { pilotId: 10, passengerIds: [11] })],
      });

      const movable = movableEntities(s, 1, new Set(), new Set());

      expect(movable.units.map((u) => u.id)).toEqual([12]);
    });
  });
});
