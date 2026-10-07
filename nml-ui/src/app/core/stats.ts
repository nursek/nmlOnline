import type { GameCharacter } from '../models';

const num = (value: number | null | undefined): number => value ?? 0;

export interface StatToken {
  value: number;
  label: string;
  separator: string;
}

type StatEntry = readonly [value: number, label: string];

/** « 120 Atk + 390 Pdf / 120 Def + 392.5 Arm » : stats à 0 masquées, « / » marque offensif → défensif. */
export function statLine(
  offensive: readonly StatEntry[],
  defensive: readonly StatEntry[],
): StatToken[] {
  const off = offensive.filter(([value]) => value !== 0);
  const def = defensive.filter(([value]) => value !== 0);

  return [...off, ...def].map(([value, label], i) => ({
    value,
    label,
    separator: i === 0 ? '' : i === off.length && off.length > 0 ? ' / ' : ' + ',
  }));
}

export function characterStats(c: GameCharacter): StatToken[] {
  return statLine(
    [
      [num(c.baseAttack), 'Atk'],
      [num(c.basePdf), 'Pdf'],
      [num(c.basePdc), 'Pdc'],
    ],
    [
      [num(c.baseDefense), 'Def'],
      [num(c.baseArmor), 'Arm'],
    ],
  );
}
