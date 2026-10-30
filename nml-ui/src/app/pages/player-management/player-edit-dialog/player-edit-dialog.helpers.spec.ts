import { crewLabel, sectorOfVehicle } from './player-edit-dialog.helpers';
import type { AdminPlayerEdit, Player, Sector, Unit, Vehicle } from '../../../models';

const character = {
  id: 9,
  playerId: 1,
  name: 'Lurio',
  baseAttack: 1,
  baseDefense: 1,
  basePdf: 0,
  basePdc: 0,
  baseArmor: 0,
  baseEvasion: 0,
  sectorNumber: 41,
};

const unit = (id: number, number: number): Unit => ({
  id,
  number,
  experience: 0,
  type: {
    name: 'LARBIN',
    level: 1,
    baseAttack: 1,
    baseDefense: 1,
    maxFirearms: 0,
    maxMeleeWeapons: 0,
    maxDefensiveEquipment: 0,
  },
  classes: [],
  isInjured: false,
  equipments: [],
  attack: 1,
  defense: 1,
  pdf: 0,
  pdc: 0,
  armor: 0,
  evasion: 0,
  sectorNumber: 41,
});

const vehicle = (overrides: Partial<Vehicle> = {}): Vehicle => ({
  id: 50,
  playerId: 1,
  vehicleType: 'VTT_LEGER',
  displayName: 'VTT léger',
  pdf: 10,
  defense: 20,
  isDestroyed: false,
  speed: 3,
  capacity: 2,
  passengerCount: 0,
  hasPilot: false,
  pilotId: null,
  pilotName: null,
  passengerIds: [],
  sectorNumber: 41,
  boardId: 1,
  ...overrides,
});

const sector = (number: number): Sector => ({
  number,
  name: `Secteur ${number}`,
  income: 100,
  army: [],
  ownerId: 1,
  color: '#fff',
  resource: null,
  neighbors: [],
  x: null,
  y: null,
});

describe('player-edit-dialog helpers', () => {
  it('résout le secteur du véhicule chez le joueur', () => {
    const player = { sectors: [sector(41), sector(13)] } as Player;
    expect(sectorOfVehicle(player, vehicle())?.number).toBe(41);
    expect(sectorOfVehicle(player, vehicle({ sectorNumber: 13 }))?.number).toBe(13);
    expect(sectorOfVehicle(player, vehicle({ sectorNumber: 7 }))).toBeNull();
    expect(sectorOfVehicle(player, vehicle({ sectorNumber: null }))).toBeNull();
    expect(sectorOfVehicle(null, vehicle())).toBeNull();
  });

  it('nomme le pilote et les passagers depuis l’édition complète', () => {
    const edit = {
      player: { character },
      units: [unit(1, 12), unit(2, 7)],
    } as AdminPlayerEdit;
    const equipped = vehicle({ pilotId: 9, passengerIds: [1, 2] });
    expect(crewLabel(edit, equipped)).toBe('Pilote Lurio · LARBIN n°12, LARBIN n°7');
  });

  it('retombe sur les ids quand l’entité n’est plus listée', () => {
    const edit = { player: { character: null }, units: [] } as unknown as AdminPlayerEdit;
    expect(crewLabel(edit, vehicle({ pilotId: 99, passengerIds: [42] }))).toBe('Pilote #99 · #42');
    expect(crewLabel(null, vehicle())).toBe('');
  });
});
