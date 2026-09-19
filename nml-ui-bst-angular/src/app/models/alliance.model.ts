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
