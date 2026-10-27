export type ProposalKind = 'ALLIANCE' | 'RUPTURE';
export type AnnouncementType = 'ALLIANCE_FORMED' | 'ALLIANCE_BROKEN' | 'BETRAYAL';

export interface AllianceMe {
  headquartersOperational: boolean;
  alliances: AllianceInfo[];
  incomingProposals: AllianceProposal[];
  outgoingProposals: AllianceProposal[];
  alliedPlayers: AlliedPlayer[];
  activeBetrayalBonuses: BetrayalBonus[];
}

export interface AllianceInfo {
  id: number;
  allyPlayerId: number;
  allyName: string;
  createdTurn: number;
}

export interface AlliedPlayer {
  playerId: number;
  name: string;
}

export interface BetrayalBonus {
  victimPlayerId: number;
  victimName: string;
  bonusPercent: number;
}

export interface AllianceProposal {
  id: number;
  kind: ProposalKind;
  status: string;
  fromPlayerId: number;
  fromPlayerName: string;
  toPlayerId: number;
  toPlayerName: string;
  createdTurn: number;
  direction: 'IN' | 'OUT';
}

export interface AllianceMessage {
  id: number;
  senderPlayerId: number;
  senderName: string;
  body: string;
  turn: number;
  createdAt: string;
}

export interface AdminAllianceEntry {
  id: number;
  playerOneId: number;
  playerOneName: string | null;
  playerTwoId: number;
  playerTwoName: string | null;
  createdTurn: number;
  endedTurn: number | null;
  endedByPlayerId: number | null;
  endedByName: string | null;
  betrayal: boolean;
}

export interface AdminAllianceProposalEntry {
  id: number;
  kind: ProposalKind;
  status: string;
  fromPlayerId: number;
  fromPlayerName: string | null;
  toPlayerId: number;
  toPlayerName: string | null;
  createdTurn: number;
  resolvedTurn: number | null;
}

export interface AdminAllianceHistory {
  alliances: AdminAllianceEntry[];
  proposals: AdminAllianceProposalEntry[];
}

export interface Announcement {
  id: number;
  type: AnnouncementType;
  actorPlayerId: number;
  actorName: string;
  targetPlayerId: number | null;
  targetName: string | null;
  turnCreated: number;
  visibleAtTurn: number;
}

export interface PendingCapture {
  id: number;
  boardId: number;
  sectorNumber: number;
  turn: number;
  candidates: PendingCaptureCandidate[];
}

export interface PendingCaptureCandidate {
  playerId: number;
  playerName: string;
}
