import { mkdirSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';

const args = process.argv.slice(2);
const option = (name, fallback) => {
  const index = args.indexOf(`--${name}`);
  return index >= 0 && args[index + 1] ? args[index + 1] : fallback;
};

const WIDTH = Number(option('width', 1600));
const HEIGHT = Number(option('height', 1000));
const OUT = resolve(option('out', 'carte-25'));
const VARIANT = option('variant', 'spokes');

if (!['arcs', 'spokes', 'rings'].includes(VARIANT)) {
  throw new Error("--variant doit valoir 'arcs', 'spokes' ou 'rings'");
}
if (!Number.isFinite(WIDTH) || !Number.isFinite(HEIGHT) || WIDTH < 320 || HEIGHT < 320) {
  throw new Error('--width et --height doivent être des nombres ≥ 320');
}

const CENTER_X = WIDTH / 2;
const CENTER_Y = HEIGHT / 2;
const RADIUS_X = WIDTH * 0.46;
const RADIUS_Y = HEIGHT * 0.46;
const HUB_RATIO = 0.22;
const RING1_RATIO = 0.61;
const CAP_RATIO = 0.1;
const MID_RATIO = (HUB_RATIO + RING1_RATIO) / 2;

const point = (ratio, degrees) => {
  const angle = (degrees * Math.PI) / 180;
  return [
    CENTER_X + ratio * RADIUS_X * Math.cos(angle),
    CENTER_Y + ratio * RADIUS_Y * Math.sin(angle),
  ];
};
const fmt = (value) => value.toFixed(2).replace(/\.?0+$/, '');
const pathData = (points) =>
  `M ${points.map(([x, y]) => `${fmt(x)},${fmt(y)}`).join(' L ')} Z`;

const annulus = (ratioIn, ratioOut, degreesStart, degreesEnd) => [
  point(ratioIn, degreesStart),
  point(ratioOut, degreesStart),
  point(ratioOut, degreesEnd),
  point(ratioIn, degreesEnd),
];
const hubPolygon = (slices) =>
  Array.from({ length: slices }, (_, index) => point(HUB_RATIO, (index * 360) / slices));

function buildSlicesLayout(variant) {
  const RINGS = variant === 'spokes' ? 2 : 4;
  const SLICES = variant === 'spokes' ? 12 : 6;
  const ANGLE = 360 / SLICES;
  const HUB = SLICES * RINGS + 1;
  const RADII = Array.from({ length: RINGS + 1 }, (_, index) => HUB_RATIO + (1 - HUB_RATIO) * (index / RINGS));
  const isPlayerSlice = (slice) => variant === 'rings' || slice % 2 === 0;

  const cells = [];
  for (let slice = 0; slice < SLICES; slice++) {
    for (let ring = 1; ring <= RINGS; ring++) {
      const start = slice * ANGLE;
      const end = start + ANGLE;
      cells.push({
        number: slice * RINGS + ring,
        slice,
        ring,
        points: annulus(RADII[ring - 1], RADII[ring], start, end),
      });
    }
  }
  cells.push({
    number: HUB,
    ring: 0,
    points: Array.from({ length: SLICES }, (_, index) => point(HUB_RATIO, index * ANGLE)),
  });

  const hubNeighbors = Array.from({ length: SLICES }, (_, slice) => slice * RINGS + 1);
  const neighborsOf = (number) => {
    if (number === HUB) return [...hubNeighbors];
    const slice = Math.floor((number - 1) / RINGS);
    const ring = ((number - 1) % RINGS) + 1;
    const result = [ring === 1 ? HUB : number - 1];
    if (ring < RINGS) result.push(number + 1);
    result.push(((slice + SLICES - 1) % SLICES) * RINGS + ring);
    result.push(((slice + 1) % SLICES) * RINGS + ring);
    return result.sort((a, b) => a - b);
  };

  const ringResources = ['Prométhéum', 'Plastacier', 'Conteneur blindé', "Caisse d'armes"];
  const playerResources = [
    ['Prométhéum', 2000],
    ['Plastacier', 2000],
  ];
  const neutralResources = [
    ['Auramite', 2500],
    ['Noctilithe', 3000],
  ];
  const hubMeta =
    variant === 'spokes'
      ? { name: 'Cité Centrale', income: 4000, resource: 'Noctilithe' }
      : { name: 'Cité Centrale', income: 3000, resource: 'Noctilithe' };
  const ringTones = ['#3c4854', '#333d48', '#2b343e', '#232b34'];
  const spokeTones = {
    player: ['#3d4956', '#35404b'],
    neutral: ['#242c35', '#1d242b'],
  };

  const sectors = cells.map((cell) => {
    let meta;
    let tone;
    if (cell.number === HUB) {
      meta = hubMeta;
      tone = '#4b5763';
    } else if (variant === 'rings') {
      meta = {
        name: `Secteur ${cell.number}`,
        income: 2000,
        resource: ringResources[cell.ring - 1],
      };
      tone = ringTones[cell.ring - 1];
    } else {
      const [resource, income] = (isPlayerSlice(cell.slice) ? playerResources : neutralResources)[
        cell.ring - 1
      ];
      meta = { name: `Secteur ${cell.number}`, income, resource };
      tone = isPlayerSlice(cell.slice)
        ? spokeTones.player[cell.ring - 1]
        : spokeTones.neutral[cell.ring - 1];
    }
    return { ...cell, ...meta, neighbors: neighborsOf(cell.number), tone };
  });

  const players = [];
  let neutrals = [];
  if (variant === 'spokes') {
    for (let player = 0; player < SLICES / 2; player++) {
      const slice = player * 2;
      const numbers = Array.from({ length: RINGS }, (_, index) => slice * RINGS + index + 1);
      players.push(`J${player + 1} ${numbers.join(',')} (QG ${numbers[0]})`);
    }
    neutrals = cells
      .filter((cell) => cell.number !== HUB && !isPlayerSlice(cell.slice))
      .map((cell) => cell.number);
  }

  return {
    sectors,
    hub: sectors.find((sector) => sector.number === HUB),
    boardName: 'Carte 6 Joueurs',
    players,
    neutrals,
    summary:
      variant === 'spokes'
        ? `${cells.length} secteurs : 6 joueurs × ${RINGS} secteurs + 6 bandes neutres × ${RINGS} + hub`
        : `${cells.length} secteurs : 6 joueurs × ${RINGS} secteurs + 1 hub neutre`,
  };
}

function buildArcsLayout() {
  const SLICES = 12;
  const ANGLE = 30;
  const HUB = 25;
  const players = [];
  const playerNumbers = new Set();
  const sectors = [];
  const byNumber = new Map();
  const add = (number, points, name, income, resource, neighbors, tone) => {
    const sector = { number, points, name, income, resource, neighbors, tone };
    sectors.push(sector);
    byNumber.set(number, sector);
    return sector;
  };

  const playerSlices = [0, 2, 4, 6, 8, 10];
  const arcs = [
    { center: 0, number: 13 },
    { center: 4, number: 14 },
    { center: 8, number: 15 },
  ];
  const gapCells = {
    1: [16, 17],
    3: [18],
    5: [19, 20],
    7: [21],
    9: [22, 23],
    11: [24],
  };

  playerSlices.forEach((slice, index) => {
    const inner = index * 2 + 1;
    const outer = index * 2 + 2;
    const start = slice * ANGLE;
    const end = start + ANGLE;
    const capped = arcs.some((arc) => arc.center === slice);
    const leftGap = gapCells[(slice + 11) % 12];
    const rightGap = gapCells[(slice + 1) % 12];
    const arcNumber = capped ? arcs.find((arc) => arc.center === slice).number : null;
    const nextArc = arcs.find((arc) => arc.center === (slice + 2) % 12)?.number;
    const prevArc = arcs.find((arc) => arc.center === (slice + 10) % 12)?.number;

    add(inner, annulus(HUB_RATIO, RING1_RATIO, start, end), `Secteur ${inner}`, 2000, 'Prométhéum',
      [outer, ...leftGap, ...rightGap, HUB].sort((a, b) => a - b), '#3d4956');
    add(outer, annulus(RING1_RATIO, capped ? 1 - CAP_RATIO : 1, start, end), `Secteur ${outer}`,
      2000, 'Plastacier',
      capped ? [inner, arcNumber].sort((a, b) => a - b)
        : [inner, prevArc, nextArc].sort((a, b) => a - b),
      '#35404b');
    playerNumbers.add(inner).add(outer);
    players.push(`J${index + 1} ${inner},${outer} (QG ${inner})`);
  });

  for (const arc of arcs) {
    const slice = arc.center;
    const a0 = (slice - 1) * ANGLE;
    const a1 = (slice + 2) * ANGLE;
    const aLeft = slice * ANGLE;
    const aRight = (slice + 1) * ANGLE;
    const points = [
      point(1, a0),
      point(1, aLeft),
      point(1, aRight),
      point(1, a1),
      point(RING1_RATIO, a1),
      point(RING1_RATIO, aRight),
      point(1 - CAP_RATIO, aRight),
      point(1 - CAP_RATIO, aLeft),
      point(RING1_RATIO, aLeft),
      point(RING1_RATIO, a0),
    ];
    const centerInner = (slice / 2) * 2 + 1;
    const prevOuter = ((slice + 10) % 12) / 2 * 2 + 2;
    const nextOuter = ((slice + 2) % 12) / 2 * 2 + 2;
    const leftGap = gapCells[(slice + 11) % 12];
    const rightGap = gapCells[(slice + 1) % 12];
    add(arc.number, points, `Secteur ${arc.number}`, 3000, 'Noctilithe',
      [centerInner + 1, prevOuter, nextOuter, leftGap.at(-1), rightGap.at(-1)]
        .sort((a, b) => a - b), '#2a2f3a');
  }

  for (const [gap, numbers] of Object.entries(gapCells)) {
    const slice = Number(gap);
    const start = slice * ANGLE;
    const end = start + ANGLE;
    const leftPlayer = ((slice - 1) / 2) * 2 + 1;
    const rightPlayer = (((slice + 1) % 12) / 2) * 2 + 1;
    const arc = arcs.find((item) => item.center === (slice + 1) % 12 || item.center === (slice - 1) % 12);
    const arcNumber = arc.number;
    if (numbers.length === 2) {
      const [inner, outer] = numbers;
      add(inner, annulus(HUB_RATIO, MID_RATIO, start, end), `Secteur ${inner}`, 2200, 'Adamentium',
        [leftPlayer, rightPlayer, outer, HUB].sort((a, b) => a - b), '#242c35');
      add(outer, annulus(MID_RATIO, RING1_RATIO, start, end), `Secteur ${outer}`, 2200, 'Adamentium',
        [leftPlayer, rightPlayer, inner, arcNumber].sort((a, b) => a - b), '#232a32');
    } else {
      const [sector] = numbers;
      add(sector, annulus(HUB_RATIO, RING1_RATIO, start, end), `Secteur ${sector}`, 2500, 'Auramite',
        [leftPlayer, rightPlayer, arcNumber, HUB].sort((a, b) => a - b), '#242c35');
    }
  }

  const hub = add(HUB, hubPolygon(SLICES), 'Cité Centrale', 4000, 'Noctilithe',
    [1, 3, 5, 7, 9, 11, 16, 18, 19, 21, 22, 24], '#4b5763');
  sectors.sort((a, b) => a.number - b.number);

  const neutralNumbers = sectors
    .filter((sector) => !playerNumbers.has(sector.number))
    .map((sector) => sector.number);

  return {
    sectors,
    hub,
    boardName: 'Carte 6 Joueurs (Arcs)',
    players,
    neutrals: neutralNumbers,
    summary: `${sectors.length} secteurs : 6 joueurs × 2 secteurs + 3 arcs neutres à 3 joueurs + 9 neutres à 2 joueurs + hub 6`,
  };
}

const layout = VARIANT === 'arcs' ? buildArcsLayout() : buildSlicesLayout(VARIANT);
const board = {
  name: layout.boardName,
  mapImageUrl: '/assets/maps/carte-25-fond.png',
  svgOverlayUrl: '/assets/maps/carte-25-overlay.svg',
  sectors: layout.sectors.map((sector) => ({
    number: sector.number,
    name: sector.name,
    income: sector.income,
    army: [],
    neighbors: sector.neighbors,
    resource: sector.resource,
  })),
};

const byNumber = new Map(board.sectors.map((sector) => [sector.number, sector]));
if (byNumber.size !== layout.sectors.length) throw new Error('Numéros de secteur dupliqués');
for (const sector of board.sectors) {
  for (const neighbor of sector.neighbors) {
    const target = byNumber.get(neighbor);
    if (!target) throw new Error(`Secteur ${sector.number} : voisin ${neighbor} inconnu`);
    if (!target.neighbors.includes(sector.number)) {
      throw new Error(`Voisinage asymétrique ${sector.number}/${neighbor}`);
    }
  }
}
const reached = new Set([1]);
const queue = [1];
while (queue.length) {
  for (const neighbor of byNumber.get(queue.shift()).neighbors) {
    if (!reached.has(neighbor)) {
      reached.add(neighbor);
      queue.push(neighbor);
    }
  }
}
if (reached.size !== layout.sectors.length) {
  throw new Error(`Graphe non connexe : ${reached.size}/${layout.sectors.length} secteurs atteints`);
}

const overlay = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}">
${layout.sectors.map((sector) => `  <path id="path${sector.number}" d="${pathData(sector.points)}" />`).join('\n')}
</svg>
`;

const background = `<?xml version="1.0" encoding="UTF-8"?>
<svg xmlns="http://www.w3.org/2000/svg" width="${WIDTH}" height="${HEIGHT}" viewBox="0 0 ${WIDTH} ${HEIGHT}">
  <defs>
    <radialGradient id="vignette" cx="50%" cy="50%" r="65%">
      <stop offset="0%" stop-color="#26303a" />
      <stop offset="100%" stop-color="#0d1116" />
    </radialGradient>
  </defs>
  <rect width="${WIDTH}" height="${HEIGHT}" fill="url(#vignette)" />
  <g fill="none" stroke="#0b0e12" stroke-width="3" stroke-linejoin="round">
${layout.sectors
  .filter((sector) => sector.number !== layout.hub.number)
  .map((sector) => `    <path d="${pathData(sector.points)}" fill="${sector.tone}" />`)
  .join('\n')}
    <path d="${pathData(layout.hub.points)}" fill="${layout.hub.tone}" />
  </g>
</svg>
`;

mkdirSync(OUT, { recursive: true });
writeFileSync(join(OUT, 'overlay.svg'), overlay, 'utf8');
writeFileSync(join(OUT, 'fond.svg'), background, 'utf8');
writeFileSync(join(OUT, 'board.json'), JSON.stringify(board, null, 2) + '\n', 'utf8');

const slash = (path) => path.split('\\').join('/');
console.log(layout.summary);
if (layout.players.length) console.log(layout.players.join(' | '));
if (layout.neutrals.length) console.log(`Neutres : ${layout.neutrals.join(',')}`);
console.log(`écrit dans ${OUT} : overlay.svg, fond.svg, board.json`);
console.log('rendu PNG du fond (sharp déjà installé dans nml-ui) :');
console.log(
  `  cd nml-ui && node -e "require('sharp')('${slash(join(OUT, 'fond.svg'))}').png().toFile('${slash(join(OUT, 'fond.png'))}')"`,
);
