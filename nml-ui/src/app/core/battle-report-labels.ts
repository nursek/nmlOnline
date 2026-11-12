export function highlightIcon(kind: string): string {
  switch (kind) {
    case 'WINNER':
      return 'emoji_events';
    case 'CHARACTER_LOST':
      return 'heart_broken';
    case 'HEADQUARTERS_CAPTURED':
      return 'flag';
    case 'VEHICLE_DESTROYED':
      return 'directions_car';
    case 'PROMOTION':
      return 'military_tech';
    case 'BUILDING_CAPTURED':
      return 'domain';
    case 'HEAVY_LOSSES':
      return 'warning';
    case 'CRIT':
      return 'bolt';
    default:
      return 'info';
  }
}

export function highlightTone(kind: string): string {
  return kind.toLowerCase();
}

export function sideLabel(side: string): string {
  if (side === 'ATTAQUANT') {
    return 'Attaquant';
  }
  if (side === 'DEFENSEUR') {
    return 'Défenseur';
  }
  if (side.startsWith('CAMP_')) {
    return `Camp ${side.slice(5)}`;
  }
  return side;
}

export function kindLabel(kind: string | null | undefined): string {
  switch (kind) {
    case 'ASSAUT':
      return 'Assaut';
    case 'IMPASSE':
      return 'Impasse mexicaine';
    case 'TIR':
      return 'Tir à distance';
    default:
      return 'Combat';
  }
}
