export const MAP_THEME = {
  // Palette affectée par index sur les joueurs triés par id (stable au refresh,
  // contrairement à l'ordre d'insertion précédent).
  playerPalette: [
    '#c03e1d',
    '#264079',
    '#205737',
    '#e59826',
    '#832e53',
    '#0b5557',
    '#a56d38',
    '#581f0e',
    '#192d5a',
    '#5a6723',
    '#b53d6d',
    '#3b3529',
  ],

  // Couleur unique des secteurs neutres (fond, hachures, label, légende).
  neutralColor: '#7c6a47',

  // Motif de hachures des secteurs neutres (injecté en <defs> au chargement).
  neutralPattern: {
    width: 8,
    height: 8,
    rotateDeg: 45,
    stripeWidth: 1,
    background: '#fff5db',
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
    strokeColor: '#17150f', // halo du chiffre (lisible sur tout fond)
    strokeWidthPx: 4,
    contrastOnSelect: true,
    selectedStroke: '#fff5db',
  },
} as const;
