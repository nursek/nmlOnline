import { Equipment, VehicleTypeInfo } from '../../models';
import {
  compareEquipments,
  equipmentBonusSummary,
  equipmentClassLabel,
  equipmentSummary,
  sortEquipments,
  sortVehiclesByCost,
  vehicleSummary,
} from './boutique.helpers';

function eq(
  name: string,
  cost: number,
  category: string,
  unitClass: string,
  bonuses: Partial<Equipment> = {},
): Equipment {
  return {
    name,
    cost,
    pdfBonus: 0,
    pdcBonus: 0,
    armBonus: 0,
    evasionBonus: 0,
    compatibleClass: [
      {
        name: unitClass,
        code: 'X',
        criticalChance: null,
        criticalMultiplier: null,
        damageReductionPdf: null,
        damageReductionPdc: null,
        maxMovementHops: 1,
      },
    ],
    category,
    ...bonuses,
  };
}

function vt(name: string, cost: number, basePdf: number, baseDefense: number): VehicleTypeInfo {
  return {
    name,
    displayName: name,
    cost,
    basePdf,
    baseDefense,
    speed: 2,
    capacity: 0,
    resistance: 0,
    firesInTransit: false,
    aerial: false,
  };
}

describe('boutique.helpers — tri par défaut', () => {
  it('trie par classe (léger→élémentaire) puis catégorie (mêlée→arme à feu→défensif) puis prix', () => {
    const items = [
      eq('Pistolet 9mm', 400, 'FIREARM', 'LEGER'), // Léger, FIREARM, 400
      eq('Poing américain', 100, 'MELEE', 'LEGER'), // Léger, MELEE, 100  ← 1er
      eq('Matraque télescopique', 200, 'MELEE', 'LEGER'), // Léger, MELEE, 200  ← 2e
      eq('Tenue ultra légère', 750, 'DEFENSIVE', 'LEGER'), // Léger, DEFENSIVE
      eq('Hache de bûcheron', 450, 'MELEE', 'MASTODONTE'), // Mastodonte, MELEE
      eq('Couteau de combat', 300, 'MELEE', 'SNIPER'), // Sniper, MELEE
      eq('Bombes collantes', 3400, 'FIREARM', 'PILOTE_DESTRUCTEUR'),
      eq('Gantelet électrique', 1000, 'MELEE', 'ELEMENTAIRE'), // Élémentaire → dernier
    ];
    const sorted = sortEquipments(items);
    expect(sorted.map((i) => i.name)).toEqual([
      'Poing américain',
      'Matraque télescopique',
      'Pistolet 9mm',
      'Tenue ultra légère',
      'Hache de bûcheron',
      'Couteau de combat',
      'Bombes collantes',
      'Gantelet électrique',
    ]);
  });

  it('tri véhicules par coût croissant', () => {
    const items = [
      vt('Tank', 7500, 125, 250),
      vt('Tourelle', 1300, 25, 40),
      vt('Avion', 15000, 0, 1000),
    ];
    expect(sortVehiclesByCost(items).map((i) => i.name)).toEqual(['Tourelle', 'Tank', 'Avion']);
  });

  it('compareEquipments est un comparateur cohérent (total order)', () => {
    const a = eq('A', 100, 'MELEE', 'LEGER');
    const b = eq('B', 200, 'MELEE', 'LEGER');
    expect(compareEquipments(a, b)).toBeLessThan(0);
    expect(compareEquipments(b, a)).toBeGreaterThan(0);
    expect(compareEquipments(a, a)).toBe(0);
  });
});

describe('boutique.helpers — résumés compacts', () => {
  it('résumé équipement : « Nom (Catégorie FR) : +n % Xxx. coût ₡. »', () => {
    expect(equipmentSummary(eq('Poing américain', 100, 'MELEE', 'LEGER', { pdcBonus: 20 }))).toBe(
      'Poing américain (Arme de corps-à-corps) : +20 % Pdc. 100 ₡.',
    );
    expect(
      equipmentSummary(
        eq('Pistolet-mitrailleur', 850, 'FIREARM', 'LEGER', { pdfBonus: 150, armBonus: 25 }),
      ),
    ).toBe('Pistolet-mitrailleur (Arme à feu) : +150 % Pdf ; +25 % Arm. 850 ₡.');
  });

  it('résumé équipement sans bonus', () => {
    expect(equipmentSummary(eq('Bidule', 50, 'DEFENSIVE', 'LEGER'))).toBe(
      'Bidule (Équipement défensif). 50 ₡.',
    );
  });

  it('résumé bonus : uniquement bonus > 0, « ; »-séparés', () => {
    expect(
      equipmentBonusSummary(
        eq('X', 1, 'FIREARM', 'LEGER', {
          pdfBonus: 80,
          pdcBonus: 0,
          armBonus: 25,
          evasionBonus: 0,
        }),
      ),
    ).toBe('+80 % Pdf ; +25 % Arm');
    expect(equipmentBonusSummary(eq('Y', 1, 'MELEE', 'LEGER'))).toBe('');
  });

  it('résumé véhicule : « Nom (Véhicule) : Pdf ; Def. coût ₡. »', () => {
    expect(vehicleSummary(vt('VTT léger', 4000, 0, 50))).toBe(
      'VTT léger (Véhicule) : 50 Def. 4000 ₡.',
    );
    expect(vehicleSummary(vt('Tank', 7500, 125, 250))).toBe(
      'Tank (Véhicule) : 125 Pdf ; 250 Def. 7500 ₡.',
    );
  });

  it('equipmentClassLabel donne le libellé FR de la 1re classe', () => {
    expect(equipmentClassLabel(eq('X', 1, 'FIREARM', 'PILOTE_DESTRUCTEUR'))).toBe(
      'Pilote destructeur',
    );
  });
});
