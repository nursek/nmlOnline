export interface GazetteWeather {
  condition: string;
  icon: string;
  temperature: number;
  precipitation: number;
  humidity: number;
  wind: number;
  windDirection: string;
}

export interface GazetteMasthead {
  journalName: string;
  subtitle: string;
  fakeUrl: string;
  volume: number;
  editionDate: string;
  dateLabel: string;
  boardName: string;
  weather: GazetteWeather;
}

export interface GazetteArticle {
  kind: string;
  section: string;
  headline: string;
  quote: string | null;
  body: string;
  author: string;
  photoPlayerName: string | null;
  photoCaption: string | null;
}

export interface GazetteIssue {
  schemaVersion: number;
  turn: number;
  issueNumber: number;
  publishedAt: string | null;
  updatedAt: string | null;
  finalEdition: boolean;
  masthead: GazetteMasthead;
  frontPage: GazetteArticle[];
  briefs: GazetteArticle[];
}

export interface GazetteIssueSummary {
  turn: number;
  issueNumber: number;
  status: string;
  mainHeadline: string | null;
  publishedAt: string | null;
  updatedAt: string | null;
  finalEdition: boolean;
}

export interface GazetteEliminationNote {
  victimName: string;
  eliminatedTurn: number;
  cause: string | null;
  killerName: string | null;
  assists: string[];
  characterName: string | null;
  battleSector: number | null;
}

export interface GazetteBattleNote {
  sectorNumber: number;
  standoff: boolean;
  winnerName: string | null;
  attackerName: string | null;
  defenderName: string | null;
  attackerCasualties: number;
  defenderCasualties: number;
  attackerCharacterLost: boolean;
  defenderCharacterLost: boolean;
  headquartersCaptured: boolean;
  log: string[];
}

export interface GazetteCaptureNote {
  sectorNumber: number;
  ownerName: string;
  onTheFly: boolean;
}

export interface GazetteAnnouncementNote {
  type: string;
  actorName: string;
  targetName: string | null;
  turnCreated: number;
}

export interface GazettePlayerNote {
  name: string;
  alive: boolean;
  sectorCount: number;
}

export interface GazetteOutcomeNote {
  finished: boolean;
  winnerName: string | null;
  posthumous: boolean;
  adjusted: boolean;
}

export interface GazetteEditorNotes {
  eliminations: GazetteEliminationNote[];
  battles: GazetteBattleNote[];
  captures: GazetteCaptureNote[];
  announcements: GazetteAnnouncementNote[];
  players: GazettePlayerNote[];
  outcome: GazetteOutcomeNote;
}

export interface GazetteAdminIssue {
  issue: GazetteIssue;
  status: string;
  editorNotes: GazetteEditorNotes | null;
  mainHeadline: string | null;
  publishedAt: string | null;
  updatedAt: string | null;
}

export interface GazetteContent {
  frontPage: GazetteArticle[];
  briefs: GazetteArticle[];
}
