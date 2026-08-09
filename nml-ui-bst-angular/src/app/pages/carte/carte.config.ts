export const MAP_THEME = {
  // Palette affectée par index sur les joueurs triés par id (stable au refresh,
  // contrairement à l'ordre d'insertion précédent).
  playerPalette: [
    '#6366f1',
    '#ef4444',
    '#10b981',
    '#f59e0b',
    '#5f5c32',
    '#ec4899',
    '#f97316',
    '#06b6d4',
    '#84cc16',
    '#14b8a6',
    '#f43f5e',
    '#a855f7',
  ],

  // Couleur unique des secteurs neutres (fond, hachures, label, légende).
  neutralColor: '#94a3b8',

  // Motif de hachures des secteurs neutres (injecté en <defs> au chargement).
  neutralPattern: {
    width: 8,
    height: 8,
    rotateDeg: 45,
    stripeWidth: 1,
    background: '#ffffff',
  },

  // Canal alpha hex (sur 2 chiffres) appliqué à la couleur propriétaire.
  fill: {
    normalAlpha: '66', // ~40%
    hoverAlpha: 'B3', // ~70%
    dimmedOpacity: 0.25,
  },

  // Épaisseur de contour (unités utilisateur ; non-scaling-stroke garde le net).
  stroke: {
    normal: 2,
    hover: 3,
    selected: 3,
  },

  // Effets appliqués au secteur sélectionné.
  selection: {
    // Rayon de dilatation feMorphology : produit un contour net qui déborde
    // des limites géométriques du path (effet « halo crisp » hors bordures).
    overflowRadius: 0,
    // Rayon du drop-shadow (glow flou autour). 0 = désactivé.
    glowRadius: 0,
  },

  overlay: {
    offsetX: 0,
    offsetY: 0,
  },

  // Rendu des numéros de secteur.
  label: {
    fontPx: 20,
    weight: 800,
    strokeColor: '#000000', // halo du chiffre (lisible sur tout fond)
    strokeWidthPx: 4,
    contrastOnSelect: true,
    selectedStroke: '#ffffff',
  },
} as const;
