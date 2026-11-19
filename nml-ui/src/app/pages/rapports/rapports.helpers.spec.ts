import type { BattlePlayerForce, BattleReport, BattleReportCamp } from '../../models';
import {
  battleStatTokens,
  casualtyGroups,
  evasionPercent,
  experienceGroups,
  forceSummary,
  reportOpponentsLabel,
  reportResultLabel,
  reportSides,
  reportTone,
} from './rapports.helpers';

function camp(
  playerId: number,
  playerName: string,
  overrides: Partial<BattleReportCamp> = {},
): BattleReportCamp {
  return {
    playerId,
    playerName,
    eliminated: false,
    characterLost: false,
    playerEliminated: false,
    eliminationCause: null,
    ...overrides,
  };
}

function report(overrides: Partial<BattleReport> = {}): BattleReport {
  return {
    turn: 3,
    sectorNumber: 15,
    kind: 'ASSAUT',
    standoff: false,
    ranged: false,
    rangedFire: null,
    winnerPlayerId: 1,
    winnerName: 'Alice',
    capturedBuildings: 0,
    camps: [camp(1, 'Alice'), camp(2, 'Bob')],
    casualties: [],
    experienceGains: [],
    forces: [],
    highlights: [],
    viewerPlayerId: 1,
    ...overrides,
  };
}

function force(overrides: Partial<BattlePlayerForce> = {}): BattlePlayerForce {
  return {
    playerId: 1,
    playerName: 'Alice',
    side: 'ATTAQUANT',
    characters: [],
    vehicles: [],
    infantry: [],
    buildings: [],
    ...overrides,
  };
}

describe('rapports de combat — synthèse repliée', () => {
  it('affiche une victoire contre le seul opposant', () => {
    const duel = report();

    expect(reportTone(duel)).toBe('win');
    expect(reportResultLabel(duel)).toBe('Victoire');
    expect(reportOpponentsLabel(duel)).toBe('Bob');
  });

  it('affiche une défaite du point de vue du joueur qui consulte', () => {
    const duel = report({ viewerPlayerId: 2 });

    expect(reportTone(duel)).toBe('loss');
    expect(reportResultLabel(duel)).toBe('Défaite');
    expect(reportOpponentsLabel(duel)).toBe('Alice');
  });

  it('ne compte pas les alliés parmi les opposants d’un assaut à plusieurs camps', () => {
    const duel = report({
      camps: [camp(1, 'Alice'), camp(2, 'Alliée'), camp(3, 'Bob'), camp(4, 'Eve')],
      forces: [
        force({ playerId: 1, playerName: 'Alice', side: 'ATTAQUANT' }),
        force({ playerId: 2, playerName: 'Alliée', side: 'ATTAQUANT' }),
        force({ playerId: 3, playerName: 'Bob', side: 'DEFENSEUR' }),
        force({ playerId: 4, playerName: 'Eve', side: 'DEFENSEUR' }),
      ],
    });

    expect(reportOpponentsLabel(duel)).toBe('Bob, Eve');
    expect(reportSides(duel)).toEqual([
      { side: 'ATTAQUANT', players: ['Alice', 'Alliée'] },
      { side: 'DEFENSEUR', players: ['Bob', 'Eve'] },
    ]);
  });

  it('accorde la victoire au joueur dont le camp gagne', () => {
    const allied = report({
      camps: [
        camp(1, 'Alice'),
        camp(2, 'Alliée'),
        camp(3, 'Bob', { eliminated: true }),
        camp(4, 'Eve', { eliminated: true }),
      ],
      forces: [
        force({ playerId: 1, playerName: 'Alice', side: 'ATTAQUANT' }),
        force({ playerId: 2, playerName: 'Alliée', side: 'ATTAQUANT' }),
        force({ playerId: 3, playerName: 'Bob', side: 'DEFENSEUR' }),
        force({ playerId: 4, playerName: 'Eve', side: 'DEFENSEUR' }),
      ],
      winnerPlayerId: 1,
      viewerPlayerId: 2,
    });

    expect(reportTone(allied)).toBe('win');
    expect(reportResultLabel(allied)).toBe('Victoire');

    const opposing = { ...allied, viewerPlayerId: 4 };
    expect(reportTone(opposing)).toBe('loss');
    expect(reportResultLabel(opposing)).toBe('Défaite');
  });

  it('retombe sur le camp éliminé quand les côtés manquent', () => {
    const legacy = report({
      camps: [camp(1, 'Alice'), camp(2, 'Alliée'), camp(3, 'Bob', { eliminated: true })],
      forces: [],
      winnerPlayerId: 1,
      viewerPlayerId: 2,
    });

    expect(reportTone(legacy)).toBe('win');
    expect(reportResultLabel(legacy)).toBe('Victoire');
  });

  it('nomme l’impasse et liste tous les autres camps', () => {
    const standoff = report({
      kind: 'IMPASSE',
      standoff: true,
      winnerPlayerId: 1,
      camps: [camp(1, 'Alice'), camp(2, 'Bob'), camp(3, 'Cid')],
    });

    expect(reportTone(standoff)).toBe('standoff');
    expect(reportResultLabel(standoff)).toBe('Impasse mexicaine');
    expect(reportOpponentsLabel(standoff)).toBe('Bob, Cid');
  });

  it('distingue un tir à distance sans vainqueur', () => {
    const ranged = report({
      kind: 'TIR',
      ranged: true,
      winnerPlayerId: null,
      winnerName: null,
      camps: [camp(1, 'Alice'), camp(2, 'Bob')],
    });

    expect(reportTone(ranged)).toBe('ranged');
    expect(reportResultLabel(ranged)).toBe('Tir à distance');
  });

  it('retombe sur le vainqueur quand la vue n’est pas signée', () => {
    const legacy = report({ viewerPlayerId: null });

    expect(reportTone(legacy)).toBe('none');
    expect(reportResultLabel(legacy)).toBe('Vainqueur : Alice');
  });
});

describe('rapports de combat — pertes et expérience par joueur', () => {
  it('sépare les pertes par joueur, y compris les camps sans perte', () => {
    const groups = casualtyGroups(
      report({
        casualties: [
          {
            playerId: 2,
            playerName: 'Bob',
            label: 'Larbin n°1',
            category: 'INFANTRY',
            unitType: 'LARBIN',
            unitNumber: 1,
            experience: 0,
          },
          {
            playerId: 2,
            playerName: 'Bob',
            label: 'Voyou n°2',
            category: 'INFANTRY',
            unitType: 'VOYOU',
            unitNumber: 2,
            experience: 2,
          },
          {
            playerId: 9,
            playerName: 'Cid',
            label: 'Brute n°1',
            category: 'INFANTRY',
            unitType: 'BRUTE',
            unitNumber: 1,
            experience: 8,
          },
        ],
      }),
    );

    expect(groups.map((group) => [group.playerId, group.entries.length])).toEqual([
      [1, 0],
      [2, 2],
      [9, 1],
    ]);
    expect(groups[1].playerName).toBe('Bob');
    expect(groups[2].playerName).toBe('Cid');
  });

  it('groupe les gains d’expérience par joueur', () => {
    const groups = experienceGroups(
      report({
        camps: [camp(1, 'Alice'), camp(2, 'Bob')],
        experienceGains: [
          {
            playerId: 1,
            playerName: 'Alice',
            unitNumber: 1,
            typeBefore: 'LARBIN',
            experienceBefore: 0,
            gained: 1,
            typeAfter: 'LARBIN',
            experienceAfter: 1,
          },
          {
            playerId: 1,
            playerName: 'Alice',
            unitNumber: 2,
            typeBefore: 'LARBIN',
            experienceBefore: 0,
            gained: 1,
            typeAfter: 'LARBIN',
            experienceAfter: 1,
          },
          {
            playerId: 2,
            playerName: 'Bob',
            unitNumber: 3,
            typeBefore: 'VOYOU',
            experienceBefore: 2,
            gained: 0.5,
            typeAfter: 'VOYOU',
            experienceAfter: 2.5,
          },
        ],
      }),
    );

    expect(groups.map((group) => [group.playerId, group.entries.length])).toEqual([
      [1, 2],
      [2, 1],
    ]);
  });
});

describe('rapports de combat — statistiques', () => {
  it('met en forme les statistiques d’une unité en masquant les zéros', () => {
    const tokens = battleStatTokens({
      attack: 10,
      pdf: 15,
      pdc: 0,
      defense: 10,
      armor: 7.5,
      evasion: 10,
    });

    expect(tokens.map((token) => [token.value, token.label, token.separator])).toEqual([
      [10, 'Atk', ''],
      [15, 'Pdf', ' + '],
      [10, 'Def', ' / '],
      [7.5, 'Arm', ' + '],
    ]);
  });

  it('arrondit l’esquive au pourcentage supérieur et l’omet à zéro', () => {
    const stats = { attack: 0, pdf: 0, pdc: 0, defense: 0, armor: 0, evasion: 10.2 };

    expect(evasionPercent(stats)).toBe(11);
    expect(evasionPercent({ ...stats, evasion: 0 })).toBeNull();
    expect(evasionPercent(null)).toBeNull();
  });

  it('résume la composition visible d’un camp', () => {
    expect(
      forceSummary(
        force({
          characters: [
            { number: null, type: null, displayName: 'Alice', experience: null, injured: false },
          ],
          infantry: [
            { number: 1, type: 'LARBIN', displayName: 'Larbin n°1', experience: 0, injured: false },
            { number: 2, type: 'LARBIN', displayName: 'Larbin n°2', experience: 0, injured: false },
          ],
          vehicles: [
            {
              displayName: 'VTT léger',
              aerial: false,
              crew: [
                {
                  number: 3,
                  type: 'LARBIN',
                  displayName: 'Larbin n°3',
                  experience: 0,
                  injured: false,
                },
              ],
            },
          ],
          buildings: [{ displayName: 'QG', category: 'HEADQUARTERS' }],
        }),
      ),
    ).toBe('1 personnage(s) · 2 unité(s) · 1 véhicule(s) · 1 bâtiment(s)');
  });
});
