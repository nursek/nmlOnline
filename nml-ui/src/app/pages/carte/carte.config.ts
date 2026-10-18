import type { Player } from '../../models';

export const DARK_TEXT = '#060809';
export const LIGHT_TEXT = '#e6decb';

function relativeLuminance(hex: string): number {
  const clean = hex.replace('#', '');
  const full = clean.length === 3 ? [...clean].map((c) => c + c).join('') : clean;
  const channel = (offset: number) => {
    const value = parseInt(full.slice(offset, offset + 2), 16) / 255;
    return value <= 0.03928 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
  };
  return 0.2126 * channel(0) + 0.7152 * channel(2) + 0.0722 * channel(4);
}

// Contraste du texte posé sur une couleur de secteur (meilleure des deux options, seuil calculé).
export function contrastColor(hex: string): string {
  const luminance = relativeLuminance(hex);
  const contrastWithDark = (luminance + 0.05) / (relativeLuminance(DARK_TEXT) + 0.05);
  const contrastWithLight = (relativeLuminance(LIGHT_TEXT) + 0.05) / (luminance + 0.05);
  return contrastWithDark >= contrastWithLight ? DARK_TEXT : LIGHT_TEXT;
}

export function haloColor(hex: string): string {
  return contrastColor(hex) === DARK_TEXT ? LIGHT_TEXT : DARK_TEXT;
}

// Couleur stable au refresh : indexation par id trié, indépendante de l'ordre d'insertion de l'API.
export function assignPlayerColors(players: readonly Player[]): ReadonlyMap<number, string> {
  const map = new Map<number, string>();
  const sorted = [...players]
    .filter((p) => p.id != null)
    .sort((a, b) => (a.id as number) - (b.id as number));
  const palette = MAP_THEME.playerPalette;
  sorted.forEach((player, index) => {
    map.set(player.id as number, palette[index % palette.length]);
  });
  return map;
}

export const MAP_THEME = {
  playerPalette: [
    '#e8bb45',
    '#6f93c9',
    '#6fae6a',
    '#d96a3a',
    '#b47ccb',
    '#4fc3c9',
    '#d4a86a',
    '#ef6a57',
    '#8f9ad6',
    '#9fbf4a',
    '#e07ab0',
    '#8d8f96',
  ],

  // Couleur unique des secteurs neutres (fond, hachures, label, légende).
  neutralColor: '#6e7885',

  // Motif de hachures des secteurs neutres (injecté en <defs> au chargement).
  neutralPattern: {
    width: 8,
    height: 8,
    rotateDeg: 45,
    stripeWidth: 1,
    background: '#4e565f',
  },

  // Canal alpha hex (sur 2 chiffres) appliqué à la couleur propriétaire.
  fill: {
    normalAlpha: '80', // ~50%
    hoverAlpha: 'C0', // ~75%
    dimmedOpacity: 0.22,
  },

  // Épaisseur de contour (unités utilisateur ; non-scaling-stroke garde le net).
  stroke: {
    normal: 2,
    hover: 3,
    selected: 3,
  },

  selection: {
    // Rayon de dilatation feMorphology : produit un contour net qui déborde
    // des limites géométriques du path (effet « halo crisp » hors bordures).
    overflowRadius: 0,
    // Rayon du drop-shadow (glow flou autour). 0 = désactivé.
    glowRadius: 4,
  },

  overlay: {
    offsetX: 0,
    offsetY: 0,
  },

  // Rendu des numéros de secteur.
  label: {
    fontPx: 20,
    weight: 800,
    strokeColor: '#060809', // halo du chiffre (lisible sur tout fond)
    strokeWidthPx: 4,
    contrastOnSelect: true,
  },
} as const;
