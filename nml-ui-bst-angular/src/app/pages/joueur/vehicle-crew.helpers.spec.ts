import { passengerCandidates, pilotCandidates } from './vehicle-crew.helpers';
import type { GameCharacter, Sector, Unit, Vehicle } from '../../models';

function unit(id: number, playerId: number, code: string): Unit {
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
        name: code,
        code,
        criticalChance: null,
        criticalMultiplier: null,
        damageReductionPdf: null,
        damageReductionPdc: null,
        maxMovementHops: 1,
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

function character(id: number, playerId: number, name: string): GameCharacter {
  return {
    id,
    playerId,
    name,
    baseAttack: 100,
    baseDefense: 250,
    basePdf: 100,
    basePdc: 50,
    baseArmor: 20,
    baseEvasion: 0,
    sectorNumber: 1,
  };
}

function vehicle(extra: Partial<Vehicle> = {}): Vehicle {
  return {
    id: 50,
    playerId: 1,
    vehicleType: 'VTT_LEGER',
    displayName: 'VTT léger',
    pdf: 0,
    defense: 50,
    isDestroyed: false,
    speed: 2,
    capacity: 10,
    passengerCount: 0,
    hasPilot: false,
    pilotId: null,
    pilotName: null,
    passengerIds: [],
    sectorNumber: 1,
    boardId: 1,
    ...extra,
  };
}

function sector(
  army: Unit[],
  characterUnit: GameCharacter | null = null,
  vehicles: Vehicle[] = [],
): Sector {
  return {
    number: 1,
    name: 'Secteur 1',
    income: 2000,
    army,
    buildings: [],
    character: characterUnit,
    vehicles,
    ownerId: 1,
    boardId: 1,
    color: null,
    resource: null,
    neighbors: [],
    x: null,
    y: null,
  };
}

describe('vehicle-crew helpers', () => {
  it('propose en pilote le personnage et les unités classe P, hors véhicules occupés', () => {
    const busyVehicle = vehicle({ id: 60, pilotId: 4 });
    const s = sector(
      [unit(1, 1, 'P'), unit(2, 1, 'L'), unit(3, 2, 'P'), unit(4, 1, 'P')],
      character(9, 1, 'Mortarion'),
      [busyVehicle],
    );

    const pilots = pilotCandidates(s, vehicle(), 1);

    expect(pilots.map((p) => p.id).sort()).toEqual([1, 9]);
  });

  it('propose tous les occupants du secteur comme passagers, sauf le pilote et les autres véhicules', () => {
    const busyVehicle = vehicle({ id: 60, pilotId: 4, passengerIds: [5] });
    const s = sector(
      [unit(1, 1, 'P'), unit(2, 1, 'L'), unit(4, 1, 'P'), unit(5, 1, 'L')],
      character(9, 1, 'Mortarion'),
      [busyVehicle],
    );

    const passengers = passengerCandidates(s, vehicle(), 1, 1);

    expect(passengers.map((p) => p.id).sort()).toEqual([2, 9]);
  });

  it("garde les passagers actuels du véhicule dans les candidats", () => {
    const current = vehicle({ pilotId: 1, passengerIds: [2] });
    const s = sector([unit(1, 1, 'P'), unit(2, 1, 'L')], null, [current]);

    const passengers = passengerCandidates(s, current, 1, 1);

    expect(passengers.map((p) => p.id)).toEqual([2]);
  });
});
