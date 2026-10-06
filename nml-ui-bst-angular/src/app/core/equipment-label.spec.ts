import { equipmentImageCandidates, equipmentLabel } from './equipment-label';

describe('equipmentLabel', () => {
  const equipment = { name: 'Gauss Blaster', displayNames: { NECRONS: 'Éclateur gauss' } };

  it('renvoie le libellé de la race quand il existe', () => {
    expect(equipmentLabel(equipment, 'NECRONS')).toBe('Éclateur gauss');
  });

  it('replie sur le nom anglais sans race ou sans traduction', () => {
    expect(equipmentLabel(equipment, null)).toBe('Gauss Blaster');
    expect(equipmentLabel(equipment, 'ORKS')).toBe('Gauss Blaster');
  });
});

describe('equipmentImageCandidates', () => {
  it('préfixe le dossier de la race, replie sur le fichier commun', () => {
    expect(equipmentImageCandidates({ name: 'Gauss Blaster' }, 'NECRONS')).toEqual([
      'assets/shop/equipment/necrons/gauss-blaster.png',
      'assets/shop/equipment/gauss-blaster.png',
    ]);
  });

  it('sans race, uniquement le fichier commun', () => {
    expect(equipmentImageCandidates({ name: 'Gauss Blaster' })).toEqual([
      'assets/shop/equipment/gauss-blaster.png',
    ]);
  });
});
