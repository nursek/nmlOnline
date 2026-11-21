import type {
  BattlePlayerForce,
  BattleReport,
  BattleReportCasualty,
  BattleReportExperienceGain,
  BattleStats,
} from '../../models';
import { statLine, StatToken } from '../../core/stats';

export type ReportTone = 'win' | 'loss' | 'none' | 'standoff' | 'ranged';

function viewerWon(report: BattleReport): boolean {
  const winnerId = report.winnerPlayerId;
  const viewerId = report.viewerPlayerId;
  if (winnerId == null || viewerId == null) return false;

  const sides = new Map((report.forces ?? []).map((force) => [force.playerId, force.side]));
  const viewerSide = sides.get(viewerId);
  const winnerSide = sides.get(winnerId);
  if (viewerSide && winnerSide) return viewerSide === winnerSide;

  const camps = report.camps ?? [];
  const viewerCamp = camps.find((camp) => camp.playerId === viewerId);
  if (viewerCamp && camps.some((camp) => camp.eliminated)) {
    return !viewerCamp.eliminated;
  }
  return winnerId === viewerId;
}

export function reportTone(report: BattleReport): ReportTone {
  if (report.ranged || report.kind === 'TIR') return 'ranged';
  if (report.standoff || report.kind === 'IMPASSE') return 'standoff';
  if (report.winnerPlayerId == null || report.viewerPlayerId == null) return 'none';
  return viewerWon(report) ? 'win' : 'loss';
}

export function reportResultLabel(report: BattleReport): string {
  if (report.ranged || report.kind === 'TIR') return 'Tir à distance';
  if (report.standoff || report.kind === 'IMPASSE') return 'Impasse mexicaine';
  if (report.winnerPlayerId == null) return 'Aucun vainqueur';
  if (report.viewerPlayerId == null) {
    return `Vainqueur : ${report.winnerName ?? '#' + report.winnerPlayerId}`;
  }
  return viewerWon(report) ? 'Victoire' : 'Défaite';
}

export function reportOpponents(report: BattleReport): string[] {
  const sideByPlayer = new Map((report.forces ?? []).map((force) => [force.playerId, force.side]));
  const viewerSide =
    report.viewerPlayerId == null ? null : (sideByPlayer.get(report.viewerPlayerId) ?? null);
  return (report.camps ?? [])
    .filter((camp) => camp.playerId !== report.viewerPlayerId)
    .filter((camp) => viewerSide === null || sideByPlayer.get(camp.playerId) !== viewerSide)
    .map((camp) => camp.playerName ?? `#${camp.playerId}`);
}

export function reportOpponentsLabel(report: BattleReport): string {
  return reportOpponents(report).join(', ');
}

export interface ReportSideGroup {
  side: string;
  players: string[];
}

export function reportSides(report: BattleReport): ReportSideGroup[] {
  const groups = new Map<string, string[]>();
  for (const force of report.forces ?? []) {
    const players = groups.get(force.side) ?? [];
    players.push(force.playerName ?? `#${force.playerId}`);
    groups.set(force.side, players);
  }
  if (groups.size) {
    return [...groups.entries()].map(([side, players]) => ({ side, players }));
  }
  return (report.camps ?? []).map((camp) => ({
    side: '',
    players: [camp.playerName ?? `#${camp.playerId}`],
  }));
}

export interface ReportPlayerGroup<T> {
  playerId: number | null;
  playerName: string;
  eliminated: boolean;
  characterLost: boolean;
  entries: T[];
}

interface PlayerEntry {
  playerId: number | null;
  playerName: string | null;
}

export function casualtyGroups(report: BattleReport): ReportPlayerGroup<BattleReportCasualty>[] {
  return groupByPlayer(report, report.casualties ?? []);
}

export function experienceGroups(
  report: BattleReport,
): ReportPlayerGroup<BattleReportExperienceGain>[] {
  return groupByPlayer(report, report.experienceGains ?? []);
}

function groupByPlayer<T extends PlayerEntry>(
  report: BattleReport,
  entries: T[],
): ReportPlayerGroup<T>[] {
  const groups = new Map<number | null, ReportPlayerGroup<T>>();
  for (const camp of report.camps ?? []) {
    groups.set(camp.playerId, {
      playerId: camp.playerId,
      playerName: camp.playerName ?? `#${camp.playerId}`,
      eliminated: camp.eliminated,
      characterLost: camp.characterLost,
      entries: [],
    });
  }
  for (const entry of entries) {
    let group = groups.get(entry.playerId);
    if (!group) {
      group = {
        playerId: entry.playerId,
        playerName: entry.playerName ?? `#${entry.playerId}`,
        eliminated: false,
        characterLost: false,
        entries: [],
      };
      groups.set(entry.playerId, group);
    }
    group.entries.push(entry);
  }
  return [...groups.values()];
}

export function battleStatTokens(stats: BattleStats | null | undefined): StatToken[] {
  if (!stats) return [];
  return statLine(
    [
      [stats.attack, 'Atk'],
      [stats.pdf, 'Pdf'],
      [stats.pdc, 'Pdc'],
    ],
    [
      [stats.defense, 'Def'],
      [stats.armor, 'Arm'],
    ],
  );
}

export function evasionPercent(stats: BattleStats | null | undefined): number | null {
  if (!stats || stats.evasion <= 0) return null;
  return Math.ceil(stats.evasion);
}

export function forceSummary(force: BattlePlayerForce): string {
  const parts: string[] = [];
  if (force.characters.length) parts.push(`${force.characters.length} personnage(s)`);
  if (force.infantry.length) parts.push(`${force.infantry.length} unité(s)`);
  if (force.vehicles.length) parts.push(`${force.vehicles.length} véhicule(s)`);
  if (force.buildings.length) parts.push(`${force.buildings.length} bâtiment(s)`);
  return parts.length ? parts.join(' · ') : 'Aucune force';
}

export function forceHasStats(force: BattlePlayerForce): boolean {
  if (force.totals) return true;
  const entities = [...force.characters, ...force.infantry, ...force.vehicles, ...force.buildings];
  return (
    entities.some((entity) => entity.stats != null) ||
    force.vehicles.some((vehicle) => vehicle.crew.some((unit) => unit.stats != null))
  );
}
