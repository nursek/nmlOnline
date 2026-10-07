import type { AllianceInfo } from '../../models';

/** Sélection stable : garde l'alliance courante si elle existe encore, sinon la première, sinon aucune. */
export function nextSelectedAllianceId(
  alliances: AllianceInfo[],
  currentId: number | null,
): number | null {
  if (currentId != null && alliances.some((alliance) => alliance.id === currentId)) {
    return currentId;
  }
  return alliances[0]?.id ?? null;
}
