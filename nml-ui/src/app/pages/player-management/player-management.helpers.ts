import type { Player } from '../../models';

export interface PlayerGroup {
  race: string | null;
  label: string;
  players: Player[];
}

const RACE_LABELS: Record<string, string> = {
  ORKS: 'Orks',
  NECRONS: 'Nécrons',
};

const RACE_ORDER = ['ORKS', 'NECRONS'];

function byName(players: readonly Player[]): Player[] {
  return [...players].sort((a, b) => a.name.localeCompare(b.name));
}

export function groupPlayersByRace(players: readonly Player[]): PlayerGroup[] {
  const groups = new Map<string, Player[]>();
  const unraced: Player[] = [];
  for (const player of players) {
    const race = player.race?.trim().toUpperCase();
    if (!race) {
      unraced.push(player);
      continue;
    }
    const bucket = groups.get(race);
    if (bucket) bucket.push(player);
    else groups.set(race, [player]);
  }

  const orderOf = (race: string): number => {
    const index = RACE_ORDER.indexOf(race);
    return index === -1 ? RACE_ORDER.length : index;
  };
  const result: PlayerGroup[] = [...groups.entries()]
    .sort(([a], [b]) => orderOf(a) - orderOf(b) || a.localeCompare(b))
    .map(([race, list]) => ({ race, label: RACE_LABELS[race] ?? race, players: byName(list) }));

  if (unraced.length) {
    result.push({ race: null, label: 'Sans race / faction', players: byName(unraced) });
  }
  return result;
}
