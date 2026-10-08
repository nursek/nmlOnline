import { groupOrders } from './ordres.helpers';
import type { GameCharacter, MovementOrder, Sector, Unit, Vehicle } from '../../models';

function unit(id: number, opts: Partial<Unit> = {}): Unit {
  return {
    id,
    playerId: 1,
    number: id,
    experience: 0,
    type: {
      name: 'LARBIN',
      level: 1,
      baseAttack: 10,
      baseDefense: 20,
      maxFirearms: 1,
      maxMeleeWeapons: 1,
      maxDefensiveEquipment: 1,
    },
    classes: [],
    isInjured: false,
    equipments: [],
    attack: 10,
    defense: 20,
    pdf: 0,
    pdc: 0,
    armor: 5,
    evasion: 0,
    ...opts,
  };
}

function character(): GameCharacter {
  return {
    id: 99,
    playerId: 1,
    name: 'Mortarion',
    baseAttack: 100,
    baseDefense: 250,
    basePdf: 100,
    basePdc: 50,
    baseArmor: 20,
    baseEvasion: 0,
    sectorNumber: 25,
  };
}

function vehicle(id: number): Vehicle {
  return {
    id,
    playerId: 1,
    vehicleType: 'TANK',
    displayName: 'Tank de combat',
    pdf: 125,
    defense: 250,
    isDestroyed: false,
    speed: 1,
    capacity: 0,
    passengerCount: 0,
    hasPilot: false,
    pilotId: null,
    pilotName: null,
    passengerIds: [],
    sectorNumber: 25,
    boardId: 1,
  };
}

function sector(number: number, opts: Partial<Sector> = {}): Sector {
  return {
    number,
    name: `Secteur ${number}`,
    income: 0,
    army: [],
    buildings: [],
    character: null,
    vehicles: [],
    ownerId: 1,
    color: null,
    resource: null,
    neighbors: [],
    x: 0,
    y: 0,
    ...opts,
  };
}

function order(
  id: number,
  from: number,
  to: number,
  opts: Partial<MovementOrder> = {},
): MovementOrder {
  return {
    id,
    turn: 1,
    playerId: 1,
    status: 'PENDING',
    fromSectorNumber: from,
    toSectorNumber: to,
    route: [from, to],
    entityIds: [],
    vehicleId: null,
    ...opts,
  };
}

describe('groupOrders', () => {
  it('regroupe par secteur de destination et résout unités, personnage et véhicules', () => {
    const player = {
      sectors: [
        sector(25, { army: [unit(1)], character: character(), vehicles: [vehicle(20)] }),
        sector(24, { army: [unit(2)] }),
      ],
      character: null,
    };
    const orders = [
      order(1, 25, 26, { entityIds: [1, 99] }),
      order(2, 24, 26, { entityIds: [2] }),
      order(3, 25, 26, { vehicleId: 20 }),
      order(4, 26, 30, { entityIds: [7] }),
    ];

    const groups = groupOrders(orders, player);

    expect(groups.map((g) => g.toSectorNumber)).toEqual([26, 30]);

    const group26 = groups[0];
    expect(group26.orders.map((d) => d.order.fromSectorNumber)).toEqual([24, 25, 25]);
    expect(group26.units.map((u) => u.id)).toEqual([2, 1]);
    expect(group26.characters.map((c) => c.id)).toEqual([99]);
    expect(group26.vehicles.map((v) => v.id)).toEqual([20]);
    expect(group26.totals).toEqual({ atk: 120, pdf: 225, pdc: 50, def: 540, armor: 30 });
    expect(group26.power).toBe(482.5);

    const footOrder = group26.orders.find((d) => d.order.id === 1);
    expect(footOrder?.units.map((u) => u.id)).toEqual([1]);
    expect(footOrder?.character?.id).toBe(99);
    expect(footOrder?.unknownIds).toEqual([]);
    expect(footOrder?.power).toBe(277.5);

    const vehicleOrder = group26.orders.find((d) => d.order.id === 3);
    expect(vehicleOrder?.power).toBe(187.5);

    const group30 = groups[1];
    expect(group30.units).toEqual([]);
    expect(group30.orders[0].unknownIds).toEqual([7]);
    expect(group30.power).toBe(0);
  });

  it('résout le personnage hors secteur et signale un véhicule introuvable', () => {
    const player = { sectors: [sector(24, { army: [unit(2)] })], character: character() };
    const orders = [
      order(1, 24, 26, { entityIds: [99] }),
      order(2, 24, 26, { vehicleId: 999 }),
    ];

    const groups = groupOrders(orders, player);
    const [leaderOrder, unknownVehicle] = groups[0].orders;

    expect(leaderOrder.character?.id).toBe(99);
    expect(leaderOrder.unknownIds).toEqual([]);
    expect(leaderOrder.power).toBe(260);
    expect(unknownVehicle.vehicles).toEqual([]);
    expect(unknownVehicle.unknownIds).toEqual([999]);
  });

  it('ne casse pas sans joueur chargé', () => {
    const groups = groupOrders([order(1, 25, 26, { entityIds: [1, 2] })], null);

    expect(groups[0].orders[0].unknownIds).toEqual([1, 2]);
    expect(groups[0].power).toBe(0);
  });
});
