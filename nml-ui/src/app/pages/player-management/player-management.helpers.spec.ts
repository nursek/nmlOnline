import type { Player, PlayerStats } from '../../models';
import { groupPlayersByRace } from './player-management.helpers';

function player(id: number, name: string, race?: string | null): Player {
  return {
    id,
    name,
    race,
    stats: {} as PlayerStats,
    equipments: [],
    resources: [],
    sectors: [],
    character: null,
    buildings: [],
  };
}

describe('groupPlayersByRace', () => {
  it('groupe par race, trie les groupes puis les joueurs, et isole les sans-race', () => {
    const groups = groupPlayersByRace([
      player(1, 'Zed', 'necrons'),
      player(2, 'Bob'),
      player(3, 'Ara', 'ORKS'),
      player(4, 'Ann', 'ORKS'),
    ]);

    expect(groups.map((group) => group.label)).toEqual(['Orks', 'Nécrons', 'Sans race / faction']);
    expect(groups[0].players.map((p) => p.name)).toEqual(['Ann', 'Ara']);
    expect(groups[2].players.map((p) => p.name)).toEqual(['Bob']);
  });

  it('ne crée pas de panneau sans-race quand tous les joueurs en ont une', () => {
    const groups = groupPlayersByRace([player(1, 'Ara', 'ORKS')]);
    expect(groups.map((group) => group.race)).toEqual(['ORKS']);
  });

  it('place une race inconnue après les races connues, avec son code brut', () => {
    const groups = groupPlayersByRace([player(1, 'Zed', 'TAU'), player(2, 'Ara', ' ORKS ')]);
    expect(groups.map((group) => group.label)).toEqual(['Orks', 'TAU']);
  });
});
