import { Equipment, UnitCartItem, UnitClass, VehicleTypeInfo } from '../../models';
import {
  clampUnitQuantity,
  compareEquipments,
  equipmentBonusSummary,
  equipmentClassLabel,
  equipmentSummary,
  matchesEquipmentSearch,
  sortEquipments,
  sortVehiclesByCost,
  unitCartQuantityForType,
  unitClassBonusSummary,
  unitPendingQuantityForType,
  unitQuotaRemaining,
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
    availableFromTurn: 4,
  };
}

describe('boutique.helpers — tri par défaut', () => {
  it('trie par classe (léger→élémentaire) puis catégorie (mêlée→arme à feu→défensif) puis prix', () => {
    const items = [
      eq('Enmitic Disintegrator Pistol', 400, 'FIREARM', 'LEGER'),
      eq('Flensing Claw', 100, 'MELEE', 'LEGER'),
      eq('Voidblade', 200, 'MELEE', 'LEGER'),
      eq('Phylactery', 750, 'DEFENSIVE', 'LEGER'),
      eq('Hyperphase Glaive', 450, 'MELEE', 'MASTODONTE'),
      eq('Hyperphase Thresher', 300, 'MELEE', 'SNIPER'),
      eq('Gauss Cannon', 3400, 'FIREARM', 'PILOTE_DESTRUCTEUR'),
      eq('Warscythe', 1000, 'MELEE', 'ELEMENTAIRE'),
    ];
    const sorted = sortEquipments(items);
    expect(sorted.map((i) => i.name)).toEqual([
      'Flensing Claw',
      'Voidblade',
      'Enmitic Disintegrator Pistol',
      'Phylactery',
      'Hyperphase Glaive',
      'Hyperphase Thresher',
      'Gauss Cannon',
      'Warscythe',
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

describe('boutique.helpers — recherche', () => {
  const gauss = eq('Gauss Blaster', 850, 'FIREARM', 'LEGER', {
    displayNames: { ORKS: 'Shoota Dakka-Dakka', NECRONS: 'Éclateur gauss' },
  });

  it("ne matche que le libellé de la race du joueur", () => {
    expect(matchesEquipmentSearch(gauss, 'éclateur', 'NECRONS')).toBe(true);
    expect(matchesEquipmentSearch(gauss, 'shoota', 'NECRONS')).toBe(false);
    expect(matchesEquipmentSearch(gauss, 'shoota', 'ORKS')).toBe(true);
  });

  it('matche toujours le nom technique et replie dessus sans race', () => {
    expect(matchesEquipmentSearch(gauss, 'gauss', 'NECRONS')).toBe(true);
    expect(matchesEquipmentSearch(gauss, 'gauss', null)).toBe(true);
    expect(matchesEquipmentSearch(gauss, 'shoota', null)).toBe(false);
  });
});

describe('boutique.helpers — résumés compacts', () => {
  it('résumé équipement : « Nom (Catégorie FR) : +n % Xxx. coût ₡. »', () => {
    expect(equipmentSummary(eq('Flensing Claw', 100, 'MELEE', 'LEGER', { pdcBonus: 20 }))).toBe(
      'Flensing Claw (Arme de corps-à-corps) : +20 % Pdc. 100 ₡.',
    );
    expect(
      equipmentSummary(
        eq('Gauss Blaster', 850, 'FIREARM', 'LEGER', { pdfBonus: 150, armBonus: 25 }),
      ),
    ).toBe('Gauss Blaster (Arme à feu) : +150 % Pdf ; +25 % Arm. 850 ₡.');
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

  describe('bonus de classe des unités', () => {
    const uc = (name: string, overrides: Partial<UnitClass> = {}): UnitClass => ({
      name,
      code: name.charAt(0),
      criticalChance: null,
      criticalMultiplier: null,
      damageReductionPdf: null,
      damageReductionPdc: null,
      maxMovementHops: 1,
      ...overrides,
    });

    it('dérive les effets des champs du DTO, cumulés dans l’ordre', () => {
      expect(unitClassBonusSummary(uc('LEGER', { maxMovementHops: 2 }))).toBe(
        'Peut parcourir 2 secteurs par tour.',
      );
      expect(
        unitClassBonusSummary(uc('MASTODONTE', { damageReductionPdf: 0.25, damageReductionPdc: 0.25 })),
      ).toBe('Réduit de 25 % les dégâts PdF et PdC reçus.');
      expect(unitClassBonusSummary(uc('TIREUR', { criticalChance: 0.1, criticalMultiplier: 1.5 }))).toBe(
        'Critique : 10 % de chances, dégâts ×1,5.',
      );
      expect(
        unitClassBonusSummary(uc('HYBRIDE', { maxMovementHops: 2, damageReductionPdf: 0.25 })),
      ).toBe('Peut parcourir 2 secteurs par tour. Réduit de 25 % les dégâts PdF et PdC reçus.');
    });

    it('texte dédié pour PILOTE_DESTRUCTEUR, sinon « Aucun effet de combat. »', () => {
      expect(unitClassBonusSummary(uc('PILOTE_DESTRUCTEUR'))).toBe(
        'Tire en priorité sur les véhicules ; obligatoire pour piloter un véhicule avec une unité.',
      );
      expect(unitClassBonusSummary(uc('ELEMENTAIRE'))).toBe('Aucun effet de combat.');
    });
  });

  describe('quota du panier unités', () => {
    const unitLine = (typeName: string, className: string, quantity: number): UnitCartItem => ({
      unitType: {
        name: typeName,
        cost: 400,
        baseAttack: 10,
        baseDefense: 10,
        availableFromTurn: 2,
        maxPerTurn: 20,
        purchasedThisTurn: 0,
        availableNow: true,
      },
      unitClass: {
        name: className,
        code: className.charAt(0),
        criticalChance: null,
        criticalMultiplier: null,
        damageReductionPdf: null,
        damageReductionPdc: null,
        maxMovementHops: 1,
      },
      quantity,
    });

    it('cumule le panier par type, toutes classes confondues', () => {
      const cart = [
        unitLine('LARBIN', 'LEGER', 3),
        unitLine('LARBIN', 'ELEMENTAIRE', 2),
        unitLine('VOYOU', 'LEGER', 4),
      ];
      expect(unitCartQuantityForType(cart, 'LARBIN')).toBe(5);
      expect(unitCartQuantityForType(cart, 'VOYOU')).toBe(4);
      expect(unitCartQuantityForType(cart, 'MALFRAT')).toBe(0);
    });

    it('borne la quantité au quota restant et bloque à zéro', () => {
      expect(clampUnitQuantity(50, 8)).toBe(8);
      expect(clampUnitQuantity(3, 8)).toBe(3);
      expect(clampUnitQuantity(0, 8)).toBe(1);
      expect(clampUnitQuantity(3, 0)).toBe(0);
      expect(clampUnitQuantity(3, -1)).toBe(0);
    });

    it('déduit du quota les achats du tour et le panier déjà rempli', () => {
      expect(unitQuotaRemaining(20, 18, 2)).toBe(0);
      expect(unitQuotaRemaining(20, 10, 2)).toBe(8);
      expect(unitQuotaRemaining(20, 0, 25)).toBe(0);
      expect(clampUnitQuantity(5, unitQuotaRemaining(20, 18, 2))).toBe(0);
      expect(clampUnitQuantity(5, unitQuotaRemaining(20, 10, 2))).toBe(5);
    });

    it('somme les saisies par type sans mélanger les classes ni les préfixes', () => {
      const pending = {
        'LARBIN#LEGER': 10,
        'LARBIN#SNIPER': 10,
        'LARBIN2#LEGER': 3,
        'VOYOU#LEGER': 4,
      };
      expect(unitPendingQuantityForType(pending, 'LARBIN')).toBe(20);
      expect(unitPendingQuantityForType(pending, 'LARBIN2')).toBe(3);
      expect(unitPendingQuantityForType(pending, 'MALFRAT')).toBe(0);
    });
  });
});
