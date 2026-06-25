/**
 * SVG Neighbor Detector
 *
 * Analyse un fichier SVG contenant des paths de secteurs et détecte
 * automatiquement les voisins en fonction de la proximité des formes.
 *
 * Usage: node svg-neighbor-detector.js [options]
 *   --svg <path>       Chemin vers le fichier SVG (défaut: ../nml-ui-bst-angular/src/assets/maps/main-map-overlay.svg)
 *   --threshold <px>   Distance max pour considérer deux secteurs comme voisins (défaut: 17)
 *   --output <path>    Fichier de sortie JSON (défaut: neighbors-output.json)
 *   --samples <n>      Nombre de points échantillonnés par path pour calcul de distance (défaut: 10000)
 *   --board <path>     Chemin vers board.json à mettre à jour (optionnel)
 *   --generate         Génère un nouveau board.json avec tous les secteurs du SVG
 */

import { readFileSync, writeFileSync } from 'node:fs';

const DEFAULT_CONFIG = {
  svgPath: '../nml-ui-bst-angular/src/assets/maps/main-map-overlay.svg',
  threshold: 17,
  outputPath: 'neighbors-output.json',
  samples: 10000,
  boardPath: '../nml-ms/src/main/resources/boards/board.json',
  generate: false
};

const SECTOR_NAMES = ['QG Central', 'Place du Marché', 'Entrepôt Abandonné', 'Zone Industrielle', 'Marché Noir', "Dépôt d'Armes", 'Terrain Vague', 'Bar Clandestin', 'Raffinerie', 'Parc Abandonné', 'Station Service', 'Garage Mécanique', 'Base Militaire', 'Quartier Résidentiel', 'Casino', 'Port de Contrebande', 'Arsenal', 'Usine Chimique', 'Entrepôt Naval', 'Centre Commercial', 'Bunker Souterrain', 'Tour de Contrôle', 'Prison Abandonnée', 'Aéroport', 'Gare de Triage', 'Centrale Électrique', 'Silos à Grain', 'Dock de Pêche', 'Mine de Charbon', 'Forêt Dense', 'Marais', 'Carrière', 'Champ de Tir', 'Camp Militaire', 'Observatoire', 'Phare', 'Scierie', 'Ferme', 'Château d\'Eau', 'Station Radio', 'Pont Suspendu', 'Tunnel', 'Dépôt de Bus', 'Stade', 'Hôpital', 'Université', 'Musée', 'Cathédrale', 'Hôtel de Ville', 'Commissariat', 'Caserne de Pompiers', 'Cimetière', 'Zoo', 'Aquarium', 'Jardin Botanique', 'Théâtre', 'Opéra', 'Bibliothèque', 'Archives'];

const RESOURCES = ['or', 'marchandises', 'ferraille', 'acier', 'joyaux', 'munitions', 'alcool', 'cigares', 'essence', 'pièces', 'uranium', 'ivoire'];

// Définition des commandes SVG : nombre de paramètres et fonction de mise à jour.
// Les fonctions capturent currentX/currentY à l'exécution, ce qui gère les commandes relatives.
function buildCommandTable() {
  let currentX = 0, currentY = 0, startX = 0, startY = 0;
  const withStart = (fn) => (params) => {
    [currentX, currentY] = fn(params);
    startX = currentX;
    startY = currentY;
    return [currentX, currentY];
  };
  const abs = ([x, y]) => [x, y];
  const rel = ([dx, dy]) => [currentX + dx, currentY + dy];
  const endCubic = (c) => [c[4], c[5]];
  const endSmoothCubic = (c) => [c[2], c[3]];
  const endQuad = (c) => [c[2], c[3]];
  const endArc = (c) => [c[5], c[6]];

  return {
    current: { get x() { return currentX; }, get y() { return currentY; }, set x(v) { currentX = v; }, set y(v) { currentY = v; } },
    start: { get x() { return startX; }, get y() { return startY; }, set x(v) { startX = v; }, set y(v) { startY = v; } },
    cmds: {
      M: { n: 2, update: withStart(([x, y]) => [x, y]), next: 'L' },
      m: { n: 2, update: withStart(([dx, dy]) => [currentX + dx, currentY + dy]), next: 'l' },
      L: { n: 2, update: abs },
      l: { n: 2, update: rel },
      H: { n: 1, update: ([x]) => [x, currentY] },
      h: { n: 1, update: ([dx]) => [currentX + dx, currentY] },
      V: { n: 1, update: ([y]) => [currentX, y] },
      v: { n: 1, update: ([dy]) => [currentX, currentY + dy] },
      C: { n: 6, update: endCubic },
      c: { n: 6, update: (c) => [currentX + c[4], currentY + c[5]] },
      S: { n: 4, update: endSmoothCubic },
      s: { n: 4, update: (c) => [currentX + c[2], currentY + c[3]] },
      Q: { n: 4, update: endQuad },
      q: { n: 4, update: (c) => [currentX + c[2], currentY + c[3]] },
      T: { n: 2, update: abs },
      t: { n: 2, update: rel },
      A: { n: 7, update: endArc },
      a: { n: 7, update: (c) => [currentX + c[5], currentY + c[6]] }
    }
  };
}

function parseArgs() {
  const args = process.argv.slice(2);
  const config = { ...DEFAULT_CONFIG };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--svg': config.svgPath = args[++i]; break;
      case '--threshold': config.threshold = Number.parseInt(args[++i], 10); break;
      case '--output': config.outputPath = args[++i]; break;
      case '--samples': config.samples = Number.parseInt(args[++i], 10); break;
      case '--board': config.boardPath = args[++i]; break;
      case '--generate': config.generate = true; break;
    }
  }

  return config;
}

function extractSectorNumber(pathId) {
  if (!pathId) return null;
  const match = pathId.match(/^(?:sector-|path)(\d+)$/);
  return match ? Number.parseInt(match[1], 10) : null;
}

function samplePointsFromPath(d, numSamples) {
  const tokens = d.match(/[MmLlHhVvCcSsQqTtAaZz]|-?(?:\d+\.?\d*|\.\d+)(?:e[+-]?\d+)?/gi);
  if (!tokens) return [];

  const points = [];
  const table = buildCommandTable();
  const { current, start, cmds } = table;
  let command = '';

  for (let i = 0; i < tokens.length; ) {
    const token = tokens[i];

    if (token === 'Z' || token === 'z') {
      current.x = start.x;
      current.y = start.y;
      i++;
      continue;
    }

    if (/^[MmLlHhVvCcSsQqTtAaZz]$/.test(token)) {
      command = token;
      i++;
      continue;
    }

    const cmd = cmds[command];
    if (!cmd) { i++; continue; }

    const params = [Number.parseFloat(token)];
    for (let k = 1; k < cmd.n; k++) {
      params.push(Number.parseFloat(tokens[++i]));
    }

    const [x, y] = cmd.update(params);
    current.x = x;
    current.y = y;
    points.push({ x, y });
    if (cmd.next) command = cmd.next;
    i++;
  }

  if (points.length <= numSamples) return points;

  const sampled = [];
  const step = points.length / numSamples;
  for (let j = 0; j < numSamples; j++) {
    sampled.push(points[Math.min(Math.floor(j * step), points.length - 1)]);
  }
  return sampled;
}

function computeBoundingBox(points) {
  if (!points.length) return null;

  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const { x, y } of points) {
    if (x < minX) minX = x;
    if (y < minY) minY = y;
    if (x > maxX) maxX = x;
    if (y > maxY) maxY = y;
  }

  return { minX, minY, maxX, maxY };
}

function bboxDistance(b1, b2) {
  const dx = b1.maxX < b2.minX ? b2.minX - b1.maxX : b2.maxX < b1.minX ? b1.minX - b2.maxX : 0;
  const dy = b1.maxY < b2.minY ? b2.minY - b1.maxY : b2.maxY < b1.minY ? b1.minY - b2.maxY : 0;
  return Math.sqrt(dx * dx + dy * dy);
}

function minPointDistance(points1, points2, threshold) {
  const thresholdSq = threshold * threshold;

  // Pour de petits ensembles, force brute simple suffit.
  if (points1.length * points2.length < 4096) {
    let minDistSq = Infinity;
    for (const p1 of points1) {
      for (const p2 of points2) {
        const dx = p1.x - p2.x;
        const dy = p1.y - p2.y;
        const distSq = dx * dx + dy * dy;
        if (distSq < minDistSq) minDistSq = distSq;
        if (minDistSq <= thresholdSq) return Math.sqrt(minDistSq);
      }
    }
    return Math.sqrt(minDistSq);
  }

  // Hash spatial pour éviter les comparaisons O(n×m).
  const grid = new Map();
  const cellSize = threshold || 1;
  const keyOf = (x, y) => `${Math.floor(x / cellSize)},${Math.floor(y / cellSize)}`;

  for (const p of points2) {
    const key = keyOf(p.x, p.y);
    if (!grid.has(key)) grid.set(key, []);
    grid.get(key).push(p);
  }

  let minDistSq = Infinity;
  for (const p1 of points1) {
    const cx = Math.floor(p1.x / cellSize);
    const cy = Math.floor(p1.y / cellSize);
    for (let dx = -1; dx <= 1; dx++) {
      for (let dy = -1; dy <= 1; dy++) {
        const cell = grid.get(`${cx + dx},${cy + dy}`);
        if (!cell) continue;
        for (const p2 of cell) {
          const ddx = p1.x - p2.x;
          const ddy = p1.y - p2.y;
          const distSq = ddx * ddx + ddy * ddy;
          if (distSq < minDistSq) minDistSq = distSq;
          if (minDistSq <= thresholdSq) return Math.sqrt(minDistSq);
        }
      }
    }
  }

  return Math.sqrt(minDistSq);
}

function extractSectors(svgContent, numSamples) {
  const sectors = [];
  const pathPattern = /<path\s+[^>]*?>/g;
  let pathMatch;

  while ((pathMatch = pathPattern.exec(svgContent)) !== null) {
    const pathElement = pathMatch[0];
    const idMatch = pathElement.match(/id="([^"]+)"/);
    const dMatch = pathElement.match(/d="([^"]+)"/);
    if (!idMatch || !dMatch) continue;

    const id = idMatch[1];
    const sectorNumber = extractSectorNumber(id);
    if (sectorNumber === null) continue;

    const points = samplePointsFromPath(dMatch[1], numSamples);
    const bbox = computeBoundingBox(points);

    if (bbox && points.length > 0) {
      sectors.push({ id, number: sectorNumber, points, bbox });
    }
  }

  return sectors;
}

function detectNeighbors(sectors, threshold) {
  const neighbors = Object.fromEntries(sectors.map(s => [s.number, []]));

  for (let i = 0; i < sectors.length; i++) {
    for (let j = i + 1; j < sectors.length; j++) {
      const s1 = sectors[i];
      const s2 = sectors[j];

      // Si les bounding boxes sont trop éloignées, inutile de vérifier les points.
      if (bboxDistance(s1.bbox, s2.bbox) > threshold) continue;

      if (minPointDistance(s1.points, s2.points, threshold) <= threshold) {
        neighbors[s1.number].push(s2.number);
        neighbors[s2.number].push(s1.number);
      }
    }
  }

  for (const key of Object.keys(neighbors)) {
    neighbors[key].sort((a, b) => a - b);
  }

  return neighbors;
}

function generateOutput(neighbors, sectors, threshold) {
  return {
    generatedAt: new Date().toISOString(),
    totalSectors: sectors.length,
    threshold,
    sectors: Object.entries(neighbors)
      .map(([number, n]) => ({ number: Number.parseInt(number, 10), neighbors: n }))
      .sort((a, b) => a.number - b.number)
  };
}

function generateBoardJsonFormat(neighbors) {
  console.log('\n📋 Format pour board.json (copier-coller dans chaque secteur):');
  console.log('='.repeat(60));
  for (const num of Object.keys(neighbors).map(n => Number.parseInt(n, 10)).sort((a, b) => a - b)) {
    console.log(`Secteur ${num}: "neighbors": [${neighbors[num].join(', ')}]`);
  }
}

function updateBoardJson(boardPath, neighbors) {
  let board;
  try {
    board = JSON.parse(readFileSync(boardPath, 'utf-8'));
  } catch (err) {
    console.error(`❌ Erreur lors de la lecture de ${boardPath}: ${err.message}`);
    return false;
  }

  const notFound = [];
  for (const sector of board.sectors) {
    if (neighbors[sector.number]) {
      sector.neighbors = neighbors[sector.number];
    } else {
      notFound.push(sector.number);
    }
  }

  try {
    writeFileSync(boardPath, JSON.stringify(board, null, 2));
    console.log(`✅ ${board.sectors.length - notFound.length} secteurs mis à jour dans ${boardPath}`);
    if (notFound.length > 0) {
      console.log(`⚠️  Secteurs non trouvés dans le SVG: [${notFound.join(', ')}]`);
    }
    return true;
  } catch (err) {
    console.error(`❌ Erreur lors de l'écriture: ${err.message}`);
    return false;
  }
}

function generateNewBoardJson(boardPath, neighbors, sectors) {
  const sortedNumbers = Object.keys(neighbors)
    .map(n => Number.parseInt(n, 10))
    .sort((a, b) => a - b);

  const board = {
    name: 'Carte Principale',
    mapImageUrl: '/assets/maps/main-map.jpg',
    svgOverlayUrl: '/assets/maps/main-map-overlay.svg',
    sectors: sortedNumbers.map((num, i) => {
      const sector = {
        number: num,
        name: SECTOR_NAMES[i] || `Secteur ${num}`,
        income: 1000 + Math.floor(Math.random() * 4000),
        army: [],
        neighbors: neighbors[num]
      };
      if (i % 3 === 0) sector.resource = RESOURCES[i % RESOURCES.length];
      return sector;
    })
  };

  try {
    writeFileSync(boardPath, JSON.stringify(board, null, 2));
    console.log(`✅ Nouveau board.json généré: ${boardPath}`);
    console.log(`   ${board.sectors.length} secteurs créés`);
    return true;
  } catch (err) {
    console.error(`❌ Erreur lors de l'écriture: ${err.message}`);
    return false;
  }
}

async function main() {
  const config = parseArgs();

  console.log('🗺️  SVG Neighbor Detector');
  console.log('='.repeat(40));
  console.log(`📁 Fichier SVG: ${config.svgPath}`);
  console.log(`📏 Seuil de distance: ${config.threshold}px`);
  console.log(`🎯 Points échantillonnés: ${config.samples}`);
  if (config.generate) console.log(`🆕 Mode: Génération d'un nouveau board.json`);
  console.log();

  let svgContent;
  try {
    svgContent = readFileSync(config.svgPath, 'utf-8');
    console.log('✅ Fichier SVG chargé');
  } catch (err) {
    console.error(`❌ Erreur lors de la lecture du SVG: ${err.message}`);
    process.exit(1);
  }

  const sectors = extractSectors(svgContent, config.samples);
  console.log(`✅ ${sectors.length} secteurs trouvés`);

  if (sectors.length === 0) {
    console.error('❌ Aucun secteur trouvé. Vérifiez que les paths ont des IDs au format "pathX" ou "sector-X"');
    process.exit(1);
  }

  const neighbors = detectNeighbors(sectors, config.threshold);
  console.log('✅ Détection des voisins terminée');

  const output = generateOutput(neighbors, sectors, config.threshold);

  try {
    writeFileSync(config.outputPath, JSON.stringify(output, null, 2));
    console.log(`✅ Résultat écrit dans: ${config.outputPath}`);
  } catch (err) {
    console.error(`❌ Erreur lors de l'écriture: ${err.message}`);
  }

  console.log('\n📊 Résumé:');
  console.log('-'.repeat(40));

  let totalNeighbors = 0;
  for (const sector of output.sectors) {
    totalNeighbors += sector.neighbors.length;
    console.log(`  Secteur ${sector.number.toString().padStart(2)}: ${sector.neighbors.length} voisins → [${sector.neighbors.join(', ')}]`);
  }

  console.log('-'.repeat(40));
  console.log(`Total: ${output.sectors.length} secteurs, ${totalNeighbors / 2} connexions`);

  if (config.generate) {
    console.log('\n🔧 Génération du board.json...');
    generateNewBoardJson(config.boardPath, neighbors, sectors);
  } else {
    console.log('\n💡 Pour mettre à jour board.json, relancez avec:');
    console.log(`   node svg-neighbor-detector.js --generate --board "${config.boardPath}"`);
    generateBoardJsonFormat(neighbors);
  }
}

main().catch(console.error);
