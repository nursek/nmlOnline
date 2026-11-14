import {
  GazetteAdminIssue,
  GazetteArticle,
  GazetteEditorNotes,
  GazetteIssue,
  GazetteIssueSummary,
  GazetteMasthead,
} from '../../models';

export const MASTHEAD: GazetteMasthead = {
  journalName: 'Brigade des Morts',
  subtitle: 'Organe des conflits de Carte Principale',
  fakeUrl: 'www.brigade-des-morts.nml/vol-5/n-2/le-front',
  volume: 5,
  editionDate: '2026-11-13T20:14:00Z',
  dateLabel: '13 novembre 2026',
  boardName: 'Carte Principale',
  weather: {
    condition: 'Ciel de plomb',
    icon: 'cloud',
    temperature: 14,
    precipitation: 4,
    humidity: 62,
    wind: 12,
    windDirection: 'NNE',
  },
};

export const LEAD_ARTICLE: GazetteArticle = {
  kind: 'ELIMINATION',
  section: 'LA UNE',
  headline: 'LA CHUTE DE LURIO',
  quote: '« Le QG est tombé au crépuscule. »',
  body: 'Premier paragraphe du récit.\n\nDeuxième paragraphe, plus sombre.',
  author: 'Frère Caïn',
  photoPlayerName: 'lurio',
  photoCaption: 'Lurio, dernier rempart de la ruche.',
};

export const SECOND_ARTICLE: GazetteArticle = {
  kind: 'BATTLE',
  section: 'LA UNE',
  headline: 'LE FRONT SE CRAQUELE',
  quote: null,
  body: 'Les lignes tiennent encore, mais pour combien de temps ?',
  author: 'Sœur Vex',
  photoPlayerName: null,
  photoCaption: null,
};

export const BRIEF_ARTICLE: GazetteArticle = {
  kind: 'CAPTURE',
  section: 'LES AUTRES FAITS',
  headline: 'AUSSI — TRAZYN TOMBE',
  quote: null,
  body: 'Le secteur 12 a changé de mains dans la nuit.',
  author: 'La Rédaction',
  photoPlayerName: null,
  photoCaption: null,
};

export const GAZETTE_ISSUE: GazetteIssue = {
  schemaVersion: 1,
  turn: 5,
  issueNumber: 2,
  publishedAt: '2026-11-13T20:15:00Z',
  updatedAt: '2026-11-13T20:15:00Z',
  finalEdition: false,
  masthead: MASTHEAD,
  frontPage: [LEAD_ARTICLE, SECOND_ARTICLE],
  briefs: [BRIEF_ARTICLE],
};

export const EDITOR_NOTES: GazetteEditorNotes = {
  eliminations: [
    {
      victimName: 'lurio',
      eliminatedTurn: 5,
      cause: 'MAIN_CHARACTER_KILLED',
      killerName: 'nursek',
      assists: ['trazyn'],
      characterName: 'Lurio',
      battleSector: 43,
    },
  ],
  battles: [
    {
      sectorNumber: 43,
      standoff: false,
      winnerName: 'nursek',
      attackerName: 'nursek',
      defenderName: 'lurio',
      attackerCasualties: 2,
      defenderCasualties: 5,
      attackerCharacterLost: false,
      defenderCharacterLost: true,
      headquartersCaptured: true,
      log: ['Le QG tombe.'],
    },
  ],
  captures: [{ sectorNumber: 43, ownerName: 'nursek', onTheFly: false }],
  announcements: [
    { type: 'FINAL_DUEL', actorName: 'nursek', targetName: 'trazyn', turnCreated: 5 },
  ],
  players: [
    { name: 'nursek', alive: true, sectorCount: 3 },
    { name: 'lurio', alive: false, sectorCount: 0 },
  ],
  outcome: { finished: false, winnerName: null, posthumous: false, adjusted: false },
};

export const GAZETTE_ADMIN_ISSUE: GazetteAdminIssue = {
  issue: { ...GAZETTE_ISSUE, publishedAt: null, updatedAt: '2026-11-13T20:14:00Z' },
  status: 'DRAFT',
  editorNotes: EDITOR_NOTES,
  mainHeadline: null,
  publishedAt: null,
  updatedAt: '2026-11-13T20:14:00Z',
};

export const GAZETTE_SUMMARIES: GazetteIssueSummary[] = [
  {
    turn: 5,
    issueNumber: 2,
    status: 'DRAFT',
    mainHeadline: null,
    publishedAt: null,
    updatedAt: '2026-11-13T20:14:00Z',
    finalEdition: false,
  },
  {
    turn: 4,
    issueNumber: 1,
    status: 'PUBLISHED',
    mainHeadline: 'LA CHUTE DE LURIO',
    publishedAt: '2026-11-12T20:15:00Z',
    updatedAt: '2026-11-12T20:15:00Z',
    finalEdition: false,
  },
];
