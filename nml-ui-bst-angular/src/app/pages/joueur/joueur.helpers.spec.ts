import {
  buildingMoveStatus,
  economyBreakdown,
  equipmentByClass,
  equipmentStackCost,
  incomeTotal,
  playerForces,
  reserveGroups,
  sectorForces,
  sectorHarvestStates,
  troopSummaries,
  unitClassCodes,
  unitEquipmentLabel,
  vehicleLabels,
} from './joueur.helpers';
import type {
  Building,
  Equipment,
  EquipmentStack,
  GameCharacter,
  Player,
  PlayerAction,
  Sector,
  Unit,
  Vehicle,
} from '../../models';

const typeOf = (name: string) => ({
  name,
  level: 1,
  baseAttack: 10,
  baseDefense: 10,
  maxFirearms: 1,
  maxMeleeWeapons: 1,
  maxDefensiveEquipment: 1,
});

function equipment(name: string, category: string = 'FIREARM'): Equipment {
  return {
    name,
    cost: 100,
    pdfBonus: 10,
    pdcBonus: 0,
    armBonus: 0,
    evasionBonus: 0,
    compatibleClass: [],
    category,
  };
}

function stack(
  name: string,
  category: string,
  opts: { cost?: number; compatibleClass?: string[] } = {},
): EquipmentStack {
  return {
    equipment: {
      ...equipment(name, category),
      cost: opts.cost ?? 100,
      compatibleClass: (opts.compatibleClass ?? []).map((className) => ({
        name: className,
        code: className[0],
        criticalChance: null,
        criticalMultiplier: null,
        damageReductionPdf: null,
        damageReductionPdc: null,
        maxMovementHops: 1,
      })),
    },
    quantity: 3,
    available: 1,
  };
}

function unit(id: number, number: number, playerId: number, opts: Partial<Unit> = {}): Unit {
  return {
    id,
    playerId,
    number,
    experience: 0,
    type: typeOf('LARBIN'),
    classes: [
      {
        name: 'LEGER',
        code: 'L',
        criticalChance: null,
        criticalMultiplier: null,
        damageReductionPdf: null,
        damageReductionPdc: null,
        maxMovementHops: 2,
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
    ...opts,
  };
}

function building(id: number, playerId: number, opts: Partial<Building> = {}): Building {
  return {
    id,
    playerId,
    buildingType: 'HEADQUARTERS',
    displayName: 'Quartier Général',
    attack: 100,
    defense: 200,
    isDestroyed: false,
    isCaptured: false,
    capturedByPlayerId: null,
    capturedTurn: null,
    lastMovedTurn: null,
    canMove: false,
    moveCooldown: 0,
    sectorNumber: 1,
    ...opts,
  };
}

function vehicle(id: number, playerId: number): Vehicle {
  return {
    id,
    playerId,
    vehicleType: 'TANK',
    displayName: 'Tank de combat',
    pdf: 125,
    defense: 250,
    isDestroyed: false,
    speed: 1,
    capacity: 0,
    passengerCount: 0,
    hasPilot: false,
    pilotId: null,
    pilotName: null,
    passengerIds: [],
    sectorNumber: 1,
    boardId: 1,
  };
}

const character = (playerId: number): GameCharacter => ({
  id: 99,
  playerId,
  name: 'Mortarion',
  baseAttack: 100,
  baseDefense: 250,
  basePdf: 100,
  basePdc: 50,
  baseArmor: 20,
  baseEvasion: 0,
  sectorNumber: 1,
});

function sector(number: number, playerId: number, opts: Partial<Sector> = {}): Sector {
  return {
    number,
    name: `Secteur ${number}`,
    income: 2000,
    army: [],
    stats: undefined,
    buildings: [],
    character: null,
    vehicles: [],
    ownerId: playerId,
    boardId: 1,
    color: null,
    resource: null,
    neighbors: [],
    x: 0,
    y: 0,
    ...opts,
  };
}

describe('joueur.helpers', () => {
  const me = 1;
  const enemy = 2;

  describe("libellés d'unité", () => {
    it('expose les codes de classes et les équipements portés', () => {
      const u = unit(1, 1, me, {
        classes: [
          {
            name: 'MASTODONTE',
            code: 'M',
            criticalChance: null,
            criticalMultiplier: null,
            damageReductionPdf: null,
            damageReductionPdc: null,
            maxMovementHops: 1,
          },
          {
            name: 'LEGER',
            code: 'L',
            criticalChance: null,
            criticalMultiplier: null,
            damageReductionPdf: null,
            damageReductionPdc: null,
            maxMovementHops: 2,
          },
        ],
        equipments: [equipment('Gilet'), equipment('Pistolet')],
      });

      expect(unitClassCodes(u)).toBe('LM');
      expect(unitEquipmentLabel(u)).toBe('Gilet, Pistolet');
      expect(unitEquipmentLabel(unit(2, 2, me))).toBe('Aucun équipement');
    });
  });

  describe('troopSummaries', () => {
    it('distingue les troupes par type et par expérience, sans les unités ennemies', () => {
      const sectors = [
        sector(1, me, {
          army: [
            unit(1, 1, me),
            unit(2, 2, me),
            unit(3, 3, me, { experience: 2.5 }),
            unit(4, 4, me, { type: typeOf('BRUTE'), experience: 8 }),
            unit(5, 5, enemy),
          ],
        }),
      ];

      expect(troopSummaries(sectors, me)).toEqual([
        { type: 'BRUTE', experience: 8, count: 1 },
        { type: 'LARBIN', experience: 2.5, count: 1 },
        { type: 'LARBIN', experience: 0, count: 2 },
      ]);
    });
  });

  describe('sectorForces', () => {
    it('ne compte que les entités du joueur, avec Def et Arm distincts', () => {
      const s = sector(1, me, {
        army: [unit(1, 1, me, { armor: 5 }), unit(2, 2, enemy)],
        buildings: [building(10, me), building(11, enemy)],
        character: character(me),
        vehicles: [vehicle(20, me)],
      });

      const sf = sectorForces(s, me);

      expect(sf.units).toHaveLength(1);
      expect(sf.buildings).toHaveLength(1);
      expect(sf.vehicles).toHaveLength(1);
      expect(sf.empty).toBe(false);
      // 1 unité (10 atk / 10 def / 5 arm) + QG (100/200) + personnage (100+100+50 / 250+20) + tank (125 pdf / 250 def)
      expect(sf.totals).toEqual({ atk: 210, pdf: 225, pdc: 50, def: 710, armor: 25 });
    });

    it('marque vide un secteur sans forces', () => {
      expect(sectorForces(sector(2, me), me).empty).toBe(true);
    });

    it('trie les unités par exp décroissante, puis type et numéro', () => {
      const s = sector(1, me, {
        army: [
          unit(1, 1, me, { experience: 0 }),
          unit(2, 2, me, { experience: 8, type: typeOf('BRUTE') }),
          unit(3, 3, me, { experience: 2.5 }),
          unit(4, 4, me, { experience: 8, type: typeOf('BRUTE') }),
          unit(5, 5, me, { experience: 8 }),
        ],
      });

      const sf = sectorForces(s, me);

      expect(sf.units.map((u) => u.experience)).toEqual([8, 8, 8, 2.5, 0]);
      expect(sf.units.map((u) => u.type.name)).toEqual([
        'BRUTE',
        'BRUTE',
        'LARBIN',
        'LARBIN',
        'LARBIN',
      ]);
      expect(sf.units[0].number).toBe(2);
      expect(sf.units[1].number).toBe(4);
    });
  });

  describe('playerForces', () => {
    it('trie les secteurs par numéro et calcule la puissance globale = (off + def) / 2', () => {
      const sectors = [
        sector(3, me, { army: [unit(1, 1, me, { armor: 5 })] }),
        sector(1, me, { army: [unit(2, 2, me)] }),
      ];

      const pf = playerForces(sectors, me);

      expect(pf.sectors.map((sf) => sf.sector.number)).toEqual([1, 3]);
      // 2 unités à 10 Atk / 10 Def, dont une avec 5 Arm
      expect(pf.totals).toEqual({ atk: 20, pdf: 0, pdc: 0, def: 20, armor: 5 });
      // (20 atk + 20 def + 5 arm) / 2
      expect(pf.offensive).toBe(20);
      expect(pf.defensive).toBe(25);
      expect(pf.militaryTotal).toBe(45);
      expect(pf.globalPower).toBe(22.5);
    });

    it('ignore les véhicules ennemis stationnés sur nos secteurs', () => {
      const sectors = [
        sector(1, me, { vehicles: [vehicle(20, me), vehicle(21, enemy)] }),
        sector(2, me, { vehicles: [vehicle(22, enemy)] }),
      ];

      const deployed = playerForces(sectors, me).sectors.reduce(
        (n, sf) => n + sf.vehicles.length,
        0,
      );

      expect(deployed).toBe(1);
    });
  });

  describe('equipmentByClass', () => {
    it('groupe par classe (ordre Léger → Élémentaire) puis par catégorie, coût croissant', () => {
      const groups = equipmentByClass([
        stack('Gilet pare-balles', 'DEFENSIVE', { compatibleClass: ['LEGER'] }),
        stack('Pistolet cascade', 'FIREARM', { compatibleClass: ['SNIPER'] }),
        stack('Particle Caster', 'FIREARM', { compatibleClass: ['LEGER'], cost: 900 }),
        stack('Pistolet 9mm', 'FIREARM', { compatibleClass: ['LEGER'], cost: 400 }),
        stack('Flensing Claw', 'MELEE', { compatibleClass: ['MASTODONTE'] }),
        stack('Hyperphase Sword', 'MELEE'),
      ]);

      expect(groups.map((g) => g.label)).toEqual(['Léger', 'Mastodonte', 'Sniper', 'Sans classe']);
      expect(groups.map((g) => g.count)).toEqual([3, 1, 1, 1]);

      // Léger : mêlée → arme à feu → défensif, coût croissant dans chaque catégorie.
      expect(groups[0].categories.map((c) => c.label)).toEqual([
        'Arme à feu',
        'Équipement défensif',
      ]);
      expect(groups[0].categories[0].stacks.map((s) => s.equipment.name)).toEqual([
        'Pistolet 9mm',
        'Particle Caster',
      ]);

      // Mastodonte : une seule catégorie (mêlée).
      expect(groups[1].categories.map((c) => c.label)).toEqual(['Arme de corps-à-corps']);
    });
  });

  describe('equipmentStackCost', () => {
    it('chiffre le stock possédé : « 3 x 850 = 2550 ₡ »', () => {
      expect(equipmentStackCost(stack('9mm', 'FIREARM', { cost: 850 }))).toBe('3 x 850 = 2550 ₡');
    });
  });

  describe('économie', () => {
    it('somme les revenus des secteurs et décompose la puissance économique', () => {
      const sectors = [
        sector(1, me),
        sector(2, me, { income: null }),
        sector(3, me, { income: 19 }),
      ];
      expect(incomeTotal(sectors)).toBe(2019);

      const player = {
        id: me,
        name: 'mortarion',
        stats: {
          money: 20000,
          totalIncome: 6000,
          totalVehiclesValue: 0,
          totalEquipmentValue: 500,
          totalOffensivePower: 0,
          totalDefensivePower: 0,
          globalPower: 0,
          totalEconomyPower: 0,
          totalAtk: 0,
          totalPdf: 0,
          totalPdc: 0,
          totalDef: 0,
          totalArmor: 0,
        },
        equipments: [],
        resources: [],
        sectors,
        character: null,
        buildings: [],
      } satisfies Player;

      const breakdown = economyBreakdown(player, incomeTotal(sectors));

      expect(breakdown).toEqual({
        money: 20000,
        income: 2019,
        equipmentValue: 500,
        vehicleValue: 0,
        total: 22519,
      });
    });
  });

  describe('sectorHarvestStates', () => {
    const harvestAction = (
      id: number,
      type: PlayerAction['type'],
      sectorNumber: number,
    ): PlayerAction => ({
      id,
      turn: 2,
      type,
      status: 'ACTIVE',
      label: '',
      money: null,
      quantity: null,
      equipmentName: null,
      resourceName: null,
      unitId: null,
      vehicleId: null,
      buildingId: null,
      fromSectorNumber: sectorNumber,
      toSectorNumber: null,
    });

    it('marque les secteurs récoltés (argent ou ressource) et ignore les autres actions', () => {
      const states = sectorHarvestStates(
        [
          sector(1, me, { income: 100, resource: 'Or' }),
          sector(2, me, { income: 200, resource: 'Ivoire' }),
          sector(3, me, { income: 300, resource: 'Cigares' }),
        ],
        [
          harvestAction(10, 'HARVEST_MONEY', 1),
          harvestAction(11, 'HARVEST_RESOURCE', 3),
          harvestAction(12, 'SELL_RESOURCE', 2),
        ],
      );

      expect(states.map((state) => state.choice)).toEqual(['MONEY', null, 'RESOURCE']);
      expect(states[0].money).toBe(100);
      expect(states[2].resource).toBe('Cigares');
      expect(states[0].action?.id).toBe(10);
    });
  });

  describe('vehicleLabels', () => {
    it('numérote les véhicules par type dans l’ordre des ids et tague pilote/passagers', () => {
      const vehicles = [
        {
          ...vehicle(9, me),
          vehicleType: 'VTT_LEGER',
          displayName: 'VTT léger',
          pilotId: 10,
          passengerIds: [12, 13],
        },
        { ...vehicle(3, me), vehicleType: 'VTT_LEGER', displayName: 'VTT léger', pilotId: 11 },
        { ...vehicle(5, me), vehicleType: 'TANK', displayName: 'Tank', pilotId: null },
      ];

      const { labels, pilotTags, passengerTags } = vehicleLabels(vehicles);

      expect(labels.get(3)).toBe('VTT léger n°1');
      expect(labels.get(9)).toBe('VTT léger n°2');
      expect(labels.get(5)).toBe('Tank n°1');
      expect(pilotTags.get(11)).toBe('pilote · VTT léger n°1');
      expect(pilotTags.get(10)).toBe('pilote · VTT léger n°2');
      expect(passengerTags.get(12)).toBe('passager · VTT léger n°2');
      expect(passengerTags.get(13)).toBe('passager · VTT léger n°2');
      expect([...pilotTags.keys()]).toHaveLength(2);
      expect([...passengerTags.keys()]).toHaveLength(2);
    });
  });

  describe('buildingMoveStatus', () => {
    it('affiche la recharge restante du QG (cooldown 5)', () => {
      const hq = building(1, me, {
        canMove: false,
        moveCooldown: 5,
        lastMovedTurn: 8,
        isOperational: true,
      });

      expect(buildingMoveStatus(hq, 10)).toEqual({ canMove: false, label: 'Recharge : 3 tours' });
      expect(buildingMoveStatus({ ...hq, canMove: true }, 13)).toEqual({
        canMove: true,
        label: 'Déplacement disponible',
      });
      expect(buildingMoveStatus(hq, null).label).toBe('Déplacement indisponible');
    });

    it('QG jamais déplacé : disponible à partir du tour 5', () => {
      const hq = building(1, me, { canMove: false, moveCooldown: 5, isOperational: true });

      expect(buildingMoveStatus(hq, 3).label).toBe('Déplacement disponible au tour 5');
    });

    it('gère le déplacement unique de la banque (tour 5, puis définitif)', () => {
      const bank = building(2, me, {
        buildingType: 'BANK',
        canMove: false,
        moveCooldown: -1,
        hasMoved: false,
      });

      expect(buildingMoveStatus(bank, 3).label).toBe('Déplacement disponible au tour 5');
      expect(buildingMoveStatus(bank, 6).label).toBe('Déplacement indisponible');
      expect(buildingMoveStatus({ ...bank, hasMoved: true }, 6).label).toBe(
        'Déplacement unique utilisé',
      );
    });

    it('priorise détruit, capturé et QG inopérant', () => {
      expect(buildingMoveStatus(building(1, me, { isDestroyed: true }), 10).label).toBe('Détruit');
      expect(buildingMoveStatus(building(1, me, { isCaptured: true }), 10).label).toBe('Capturé');
      expect(
        buildingMoveStatus(building(1, me, { isOperational: false, canMove: false }), 10).label,
      ).toBe('QG inopérant');
    });
  });

  describe('reserveGroups', () => {
    it('groupe par type + classe et conserve les ids à déployer', () => {
      const elementaire = {
        name: 'ELEMENTAIRE',
        code: 'E',
        criticalChance: null,
        criticalMultiplier: null,
        damageReductionPdf: null,
        damageReductionPdc: null,
        maxMovementHops: 1,
      };
      const groups = reserveGroups([
        unit(1, 0, me, { attack: 10, defense: 10 }),
        unit(2, 0, me, { attack: 10, defense: 10 }),
        unit(3, 0, me, { classes: [elementaire], attack: 12, defense: 14 }),
      ]);

      expect(groups.map((g) => g.key)).toEqual(['LARBIN#LEGER', 'LARBIN#ELEMENTAIRE']);
      expect(groups[0]).toMatchObject({ count: 2, unitIds: [1, 2], attack: 10, defense: 10 });
      expect(groups[1]).toMatchObject({ count: 1, unitIds: [3], attack: 12, defense: 14 });
    });
  });
});
