import { equipmentDescription, equipmentImageCandidates, equipmentLabel } from './equipment-label';

describe('equipmentLabel', () => {
  const equipment = { name: 'Pistolet-mitrailleur', displayNames: { NECRONS: 'Éclateur gauss' } };

  it('renvoie le libellé de la race quand il existe', () => {
    expect(equipmentLabel(equipment, 'NECRONS')).toBe('Éclateur gauss');
  });

  it('replie sur le nom anglais sans race ou sans traduction', () => {
    expect(equipmentLabel(equipment, null)).toBe('Pistolet-mitrailleur');
    expect(equipmentLabel(equipment, 'ORKS')).toBe('Pistolet-mitrailleur');
  });
});

describe('equipmentDescription', () => {
  const equipment = {
    name: 'Pistolet-mitrailleur',
    displayDescriptions: { NECRONS: "L'éclateur gauss arrache la matière atome par atome." },
  };

  it('renvoie la description de la race quand elle existe', () => {
    expect(equipmentDescription(equipment, 'NECRONS')).toBe(
      "L'éclateur gauss arrache la matière atome par atome.",
    );
  });

  it('renvoie une chaîne vide sans race ou sans description', () => {
    expect(equipmentDescription(equipment, null)).toBe('');
    expect(equipmentDescription(equipment, 'ORKS')).toBe('');
  });
});

describe('equipmentImageCandidates', () => {
  it('préfixe le dossier de la race, replie sur le fichier commun', () => {
    expect(equipmentImageCandidates({ name: 'Pistolet-mitrailleur' }, 'NECRONS')).toEqual([
      'assets/shop/equipment/necrons/pistolet-mitrailleur.png',
      'assets/shop/equipment/pistolet-mitrailleur.png',
    ]);
  });

  it('sans race, uniquement le fichier commun', () => {
    expect(equipmentImageCandidates({ name: 'Pistolet-mitrailleur' })).toEqual([
      'assets/shop/equipment/pistolet-mitrailleur.png',
    ]);
  });
});
