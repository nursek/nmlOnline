import { characterStats, statLine } from './stats';

describe('statLine', () => {
  const render = (tokens: ReturnType<typeof statLine>) =>
    tokens.map((t) => `${t.separator}${t.value} ${t.label}`).join('');

  it('masque les stats à 0 et place le « / » entre offensif et défensif', () => {
    expect(
      render(
        statLine(
          [
            [120, 'Atk'],
            [390, 'Pdf'],
            [0, 'Pdc'],
          ],
          [
            [120, 'Def'],
            [392.5, 'Arm'],
          ],
        ),
      ),
    ).toBe('120 Atk + 390 Pdf / 120 Def + 392.5 Arm');
  });

  it('masque aussi un groupe entier, sans séparateur orphelin', () => {
    expect(render(statLine([[0, 'Atk']], [[50, 'Def']]))).toBe('50 Def');
    expect(
      render(
        statLine(
          [[10, 'Atk']],
          [
            [0, 'Def'],
            [0, 'Arm'],
          ],
        ),
      ),
    ).toBe('10 Atk');
    expect(render(statLine([[0, 'Atk']], [[0, 'Def']]))).toBe('');
  });
});

describe('characterStats', () => {
  it('aligne les stats de base du personnage, offensif puis défensif', () => {
    const tokens = characterStats({
      id: 1,
      playerId: 1,
      name: 'Lurio',
      baseAttack: 100,
      baseDefense: 150,
      basePdf: 50,
      basePdc: 300,
      baseArmor: 300,
      baseEvasion: 5,
      sectorNumber: 13,
    });

    expect(tokens).toEqual([
      { value: 100, label: 'Atk', separator: '' },
      { value: 50, label: 'Pdf', separator: ' + ' },
      { value: 300, label: 'Pdc', separator: ' + ' },
      { value: 150, label: 'Def', separator: ' / ' },
      { value: 300, label: 'Arm', separator: ' + ' },
    ]);
  });
});
