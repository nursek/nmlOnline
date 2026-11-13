import type { PodiumPlace } from '../../models';

export function causeLabel(place: PodiumPlace): string {
  if (place.alive) {
    return '—';
  }
  return place.definitive ? 'Mort définitive' : 'QG conquis — retour possible';
}

export function championLabel(place: PodiumPlace | null): string {
  if (!place) {
    return '';
  }
  return place.alive ? 'Dernier survivant' : 'Vainqueur posthume';
}

export function medalLabel(place: number): string {
  switch (place) {
    case 1:
      return 'Or';
    case 2:
      return 'Argent';
    case 3:
      return 'Bronze';
    default:
      return `${place}ᵉ place`;
  }
}

export function medalEmoji(place: number): string {
  switch (place) {
    case 1:
      return '🥇';
    case 2:
      return '🥈';
    case 3:
      return '🥉';
    default:
      return '•';
  }
}
