/**
 * SVG Neighbor Detector
 *
 * Analyse un fichier SVG contenant des paths de secteurs et détecte
 * automatiquement les voisins en fonction de la proximité des formes.
 *
 * Usage: node svg-neighbor-detector.js [options]
 *   --svg <path>       Chemin vers le fichier SVG (défaut: ../nml-ui-bst-angular/src/assets/maps/main-map-overlay.svg)
 *   --threshold <px>   Distance max pour considérer deux secteurs comme voisins (défaut: 30)
 *   --output <path>    Fichier de sortie JSON (défaut: neighbors-output.json)
 *   --samples <n>      Nombre de points échantillonnés par path pour calcul de distance (défaut: 100)
 *   --board <path>     Chemin vers board.json à mettre à jour (optionnel)
 *   --generate         Génère un nouveau board.json avec tous les secteurs du SVG
 */

import { readFileSync, writeFileSync } from 'node:fs';

// Configuration par défaut
const DEFAULT_CONFIG = {
  svgPath: '../nml-ui-bst-angular/src/assets/maps/main-map-overlay.svg',
  threshold: 17,      // Distance en pixels pour considérer deux secteurs comme voisins
  outputPath: 'neighbors-output.json',
  samples: 10000,       // Nombre de points à échantillonner sur chaque path
  boardPath: '../nml-ms/src/main/resources/boards/board.json',
  generate: false     // Si true, génère un nouveau board.json
};

// Parse les arguments de ligne de commande
function parseArgs() {
  const args = process.argv.slice(2);
  const config = { ...DEFAULT_CONFIG };

  for (let i = 0; i < args.length; i++) {
    switch (args[i]) {
      case '--svg':
        config.svgPath = args[++i];
        break;
      case '--threshold':
        config.threshold = Number.parseInt(args[++i], 10);
        break;
      case '--output':
        config.outputPath = args[++i];
        break;
      case '--samples':
        config.samples = Number.parseInt(args[++i], 10);
        break;
      case '--board':
        config.boardPath = args[++i];
        break;
      case '--generate':
        config.generate = true;
        break;
    }
  }

  return config;
}

/**
 * Extrait le numéro de secteur depuis l'ID du path
 * Supporte les formats: "path1", "path59", "sector-1", "sector-16"
 */
function extractSectorNumber(pathId) {
  if (!pathId) return null;

  // Format "sector-X"
  const sectorMatch = pathId.match(/^sector-(\d+)$/);
  if (sectorMatch) return parseInt(sectorMatch[1], 10);

  // Format "pathX"
  const pathMatch = pathId.match(/^path(\d+)$/);
  if (pathMatch) return parseInt(pathMatch[1], 10);

  return null;
}

/**
 * Parse le chemin SVG 'd' et extrait des points échantillonnés
 * Gère correctement les commandes SVG (M, m, L, l, C, c, etc.)
 */
function samplePointsFromPath(d, numSamples) {
  const points = [];

  // Tokenize le path en commandes et nombres
  const tokens = d.match(/[MmLlHhVvCcSsQqTtAaZz]|-?\d*\.?\d+(?:e[+-]?\d+)?/gi);
  if (!tokens) return points;

  let currentX = 0;
  let currentY = 0;
  let startX = 0;
  let startY = 0;
  let command = '';
  let i = 0;

  while (i < tokens.length) {
    const token = tokens[i];

    // Vérifier si c'est une commande
    if (/^[MmLlHhVvCcSsQqTtAaZz]$/.test(token)) {
      command = token;
      i++;
      continue;
    }

    // Parser les nombres selon la commande courante
    const num = parseFloat(token);

    switch (command) {
      case 'M': // Moveto absolu
        currentX = num;
        currentY = parseFloat(tokens[++i]);
        startX = currentX;
        startY = currentY;
        points.push({ x: currentX, y: currentY });
        command = 'L'; // Les coords suivantes sont des lineto
        break;

      case 'm': // Moveto relatif
        currentX += num;
        currentY += parseFloat(tokens[++i]);
        startX = currentX;
        startY = currentY;
        points.push({ x: currentX, y: currentY });
        command = 'l';
        break;

      case 'L': // Lineto absolu
        currentX = num;
        currentY = parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'l': // Lineto relatif
        currentX += num;
        currentY += parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'H': // Horizontal absolu
        currentX = num;
        points.push({ x: currentX, y: currentY });
        break;

      case 'h': // Horizontal relatif
        currentX += num;
        points.push({ x: currentX, y: currentY });
        break;

      case 'V': // Vertical absolu
        currentY = num;
        points.push({ x: currentX, y: currentY });
        break;

      case 'v': // Vertical relatif
        currentY += num;
        points.push({ x: currentX, y: currentY });
        break;

      case 'C': // Cubic bezier absolu (6 params: x1,y1,x2,y2,x,y)
        i += 4; // Skip control points
        currentX = parseFloat(tokens[i]);
        currentY = parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'c': // Cubic bezier relatif
        i += 4; // Skip control points relatifs
        currentX += parseFloat(tokens[i]);
        currentY += parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'S': // Smooth cubic absolu (4 params)
        i += 2; // Skip control point
        currentX = parseFloat(tokens[i]);
        currentY = parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 's': // Smooth cubic relatif
        i += 2;
        currentX += parseFloat(tokens[i]);
        currentY += parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'Q': // Quadratic bezier absolu (4 params)
        i += 2; // Skip control point
        currentX = parseFloat(tokens[i]);
        currentY = parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'q': // Quadratic bezier relatif
        i += 2;
        currentX += parseFloat(tokens[i]);
        currentY += parseFloat(tokens[++i]);
        break;

      case 'T': // Smooth quadratic absolu
        currentX = num;
        currentY = parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 't': // Smooth quadratic relatif
        currentX += num;
        currentY += parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'A': // Arc absolu (7 params)
        i += 5; // Skip arc params
        currentX = parseFloat(tokens[i]);
        currentY = parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'a': // Arc relatif
        i += 5;
        currentX += parseFloat(tokens[i]);
        currentY += parseFloat(tokens[++i]);
        points.push({ x: currentX, y: currentY });
        break;

      case 'Z':
      case 'z': // Closepath
        currentX = startX;
        currentY = startY;
        break;

      default:
        // Commande inconnue, on skip
        break;
    }

    i++;
  }

  if (points.length === 0) return points;

  // Échantillonner uniformément
  if (points.length <= numSamples) return points;

  const sampled = [];
  const step = points.length / numSamples;
  for (let j = 0; j < numSamples; j++) {
    const idx = Math.min(Math.floor(j * step), points.length - 1);
    sampled.push(points[idx]);
  }

  return sampled;
}

/**
 * Calcule la bounding box d'un ensemble de points
 */
function computeBoundingBox(points) {
  if (points.length === 0) return null;

  let minX = Infinity, minY = Infinity;
  let maxX = -Infinity, maxY = -Infinity;

  for (const p of points) {
    if (p.x < minX) minX = p.x;
    if (p.y < minY) minY = p.y;
    if (p.x > maxX) maxX = p.x;
    if (p.y > maxY) maxY = p.y;
  }

  return { minX, minY, maxX, maxY };
}

/**
 * Calcule la distance minimale entre deux bounding boxes
 */
function bboxDistance(bbox1, bbox2) {
  // Distance horizontale
  let dx = 0;
  if (bbox1.maxX < bbox2.minX) {
    dx = bbox2.minX - bbox1.maxX;
  } else if (bbox2.maxX < bbox1.minX) {
    dx = bbox1.minX - bbox2.maxX;
  }

  // Distance verticale
  let dy = 0;
  if (bbox1.maxY < bbox2.minY) {
    dy = bbox2.minY - bbox1.maxY;
  } else if (bbox2.maxY < bbox1.minY) {
    dy = bbox1.minY - bbox2.maxY;
  }

  return Math.sqrt(dx * dx + dy * dy);
}

/**
 * Calcule la distance minimale entre deux ensembles de points
 */
function minPointDistance(points1, points2) {
  let minDist = Infinity;

  for (const p1 of points1) {
    for (const p2 of points2) {
      const dx = p1.x - p2.x;
      const dy = p1.y - p2.y;
      const dist = Math.sqrt(dx * dx + dy * dy);
      if (dist < minDist) {
        minDist = dist;
      }
    }
  }

  return minDist;
}

/**
 * Extrait tous les paths du SVG avec leurs points échantillonnés
 */
function extractSectors(svgContent, numSamples) {
  const sectors = [];

  // Regex pour extraire les paths (supporte id avant ou après d)
  const pathPattern = /<path\s+[^>]*?>/g;
  let pathMatch;

  while ((pathMatch = pathPattern.exec(svgContent)) !== null) {
    const pathElement = pathMatch[0];

    // Extraire l'ID
    const idMatch = pathElement.match(/id="([^"]+)"/);
    if (!idMatch) continue;
    const id = idMatch[1];

    // Extraire le chemin d
    const dMatch = pathElement.match(/d="([^"]+)"/);
    if (!dMatch) continue;
    const d = dMatch[1];

    const sectorNumber = extractSectorNumber(id);
    if (sectorNumber === null) continue;

    const points = samplePointsFromPath(d, numSamples);
    const bbox = computeBoundingBox(points);

    if (bbox && points.length > 0) {
      sectors.push({
        id,
        number: sectorNumber,
        points,
        bbox
      });
    }
  }

  return sectors;
}

/**
 * Détecte les voisins pour chaque secteur
 */
function detectNeighbors(sectors, threshold) {
  const neighbors = {};

  // Initialiser les listes de voisins
  for (const sector of sectors) {
    neighbors[sector.number] = [];
  }

  // Comparer chaque paire de secteurs
  for (let i = 0; i < sectors.length; i++) {
    for (let j = i + 1; j < sectors.length; j++) {
      const s1 = sectors[i];
      const s2 = sectors[j];

      // Vérification rapide avec bounding box
      const bboxDist = bboxDistance(s1.bbox, s2.bbox);
      if (bboxDist > threshold * 2) continue;

      // Vérification précise avec les points
      const pointDist = minPointDistance(s1.points, s2.points);

      if (pointDist <= threshold) {
        neighbors[s1.number].push(s2.number);
        neighbors[s2.number].push(s1.number);
      }
    }
  }

  // Trier les voisins
  for (const key of Object.keys(neighbors)) {
    neighbors[key].sort((a, b) => a - b);
  }

  return neighbors;
}

/**
 * Génère la sortie JSON
 */
function generateOutput(neighbors, sectors) {
  const output = {
    generatedAt: new Date().toISOString(),
    totalSectors: sectors.length,
    threshold: DEFAULT_CONFIG.threshold,
    sectors: []
  };

  // Trier par numéro de secteur
  const sortedNumbers = Object.keys(neighbors)
    .map(n => parseInt(n, 10))
    .sort((a, b) => a - b);

  for (const num of sortedNumbers) {
    output.sectors.push({
      number: num,
      neighbors: neighbors[num]
    });
  }

  return output;
}

/**
 * Génère le format pour board.json (juste les neighbors)
 */
function generateBoardJsonFormat(neighbors) {
  const sortedNumbers = Object.keys(neighbors)
    .map(n => Number.parseInt(n, 10))
    .sort((a, b) => a - b);

  console.log('\n📋 Format pour board.json (copier-coller dans chaque secteur):');
  console.log('=' .repeat(60));

  for (const num of sortedNumbers) {
    console.log(`Secteur ${num}: "neighbors": [${neighbors[num].join(', ')}]`);
  }
}

/**
 * Met à jour un board.json existant avec les nouveaux neighbors
 */
function updateBoardJson(boardPath, neighbors) {
  let board;
  try {
    const content = readFileSync(boardPath, 'utf-8');
    board = JSON.parse(content);
  } catch (err) {
    console.error(`❌ Erreur lors de la lecture de ${boardPath}: ${err.message}`);
    return false;
  }

  let updated = 0;
  let notFound = [];

  for (const sector of board.sectors) {
    if (neighbors[sector.number]) {
      sector.neighbors = neighbors[sector.number];
      updated++;
    } else {
      notFound.push(sector.number);
    }
  }

  // Sauvegarder
  try {
    writeFileSync(boardPath, JSON.stringify(board, null, 2));
    console.log(`✅ ${updated} secteurs mis à jour dans ${boardPath}`);
    if (notFound.length > 0) {
      console.log(`⚠️  Secteurs non trouvés dans le SVG: [${notFound.join(', ')}]`);
    }
    return true;
  } catch (err) {
    console.error(`❌ Erreur lors de l'écriture: ${err.message}`);
    return false;
  }
}

/**
 * Génère un nouveau board.json avec tous les secteurs du SVG
 */
function generateNewBoardJson(boardPath, neighbors, sectors) {
  const sortedNumbers = Object.keys(neighbors)
    .map(n => Number.parseInt(n, 10))
    .sort((a, b) => a - b);

  // Noms par défaut pour les secteurs
  const sectorNames = [
    'QG Central', 'Place du Marché', 'Entrepôt Abandonné', 'Zone Industrielle',
    'Marché Noir', 'Dépôt d\'Armes', 'Terrain Vague', 'Bar Clandestin',
    'Raffinerie', 'Parc Abandonné', 'Station Service', 'Garage Mécanique',
    'Base Militaire', 'Quartier Résidentiel', 'Casino', 'Port de Contrebande',
    'Arsenal', 'Usine Chimique', 'Entrepôt Naval', 'Centre Commercial',
    'Bunker Souterrain', 'Tour de Contrôle', 'Prison Abandonnée', 'Aéroport',
    'Gare de Triage', 'Centrale Électrique', 'Silos à Grain', 'Dock de Pêche',
    'Mine de Charbon', 'Forêt Dense', 'Marais', 'Carrière', 'Champ de Tir',
    'Camp Militaire', 'Observatoire', 'Phare', 'Scierie', 'Ferme',
    'Château d\'Eau', 'Station Radio', 'Pont Suspendu', 'Tunnel', 'Dépôt de Bus',
    'Stade', 'Hôpital', 'Université', 'Musée', 'Cathédrale', 'Hôtel de Ville',
    'Commissariat', 'Caserne de Pompiers', 'Cimetière', 'Zoo', 'Aquarium',
    'Jardin Botanique', 'Théâtre', 'Opéra', 'Bibliothèque', 'Archives'
  ];

  const resources = ['or', 'marchandises', 'ferraille', 'acier', 'joyaux', 'munitions',
                     'alcool', 'cigares', 'essence', 'pièces', 'uranium', 'ivoire'];

  const board = {
    name: 'Carte Principale',
    mapImageUrl: '/assets/maps/main-map.jpg',
    svgOverlayUrl: '/assets/maps/main-map-overlay.svg',
    sectors: []
  };

  for (let i = 0; i < sortedNumbers.length; i++) {
    const num = sortedNumbers[i];

    const sectorData = {
      number: num,
      name: sectorNames[i] || `Secteur ${num}`,
      income: 1000 + Math.floor(Math.random() * 4000),
      army: [],
      neighbors: neighbors[num]
    };

    // Ajouter une ressource tous les 3 secteurs
    if (i % 3 === 0) {
      sectorData.resource = resources[i % resources.length];
    }

    board.sectors.push(sectorData);
  }

  // Pas besoin de nettoyer les undefined car on ne les ajoute plus

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

// === MAIN ===
async function main() {
  const config = parseArgs();

  console.log('🗺️  SVG Neighbor Detector');
  console.log('=' .repeat(40));
  console.log(`📁 Fichier SVG: ${config.svgPath}`);
  console.log(`📏 Seuil de distance: ${config.threshold}px`);
  console.log(`🎯 Points échantillonnés: ${config.samples}`);
  if (config.generate) {
    console.log(`🆕 Mode: Génération d'un nouveau board.json`);
  }
  console.log();

  // Lire le fichier SVG
  let svgContent;
  try {
    svgContent = readFileSync(config.svgPath, 'utf-8');
    console.log('✅ Fichier SVG chargé');
  } catch (err) {
    console.error(`❌ Erreur lors de la lecture du SVG: ${err.message}`);
    process.exit(1);
  }

  // Extraire les secteurs
  const sectors = extractSectors(svgContent, config.samples);
  console.log(`✅ ${sectors.length} secteurs trouvés`);

  if (sectors.length === 0) {
    console.error('❌ Aucun secteur trouvé. Vérifiez que les paths ont des IDs au format "pathX" ou "sector-X"');
    process.exit(1);
  }

  // Détecter les voisins
  const neighbors = detectNeighbors(sectors, config.threshold);
  console.log('✅ Détection des voisins terminée');

  // Générer la sortie JSON de base
  const output = generateOutput(neighbors, sectors);
  output.threshold = config.threshold;

  // Écrire le fichier JSON de sortie
  try {
    writeFileSync(config.outputPath, JSON.stringify(output, null, 2));
    console.log(`✅ Résultat écrit dans: ${config.outputPath}`);
  } catch (err) {
    console.error(`❌ Erreur lors de l'écriture: ${err.message}`);
  }

  // Afficher le résumé
  console.log('\n📊 Résumé:');
  console.log('-'.repeat(40));

  let totalNeighbors = 0;
  for (const sector of output.sectors) {
    totalNeighbors += sector.neighbors.length;
    console.log(`  Secteur ${sector.number.toString().padStart(2)}: ${sector.neighbors.length} voisins → [${sector.neighbors.join(', ')}]`);
  }

  console.log('-'.repeat(40));
  console.log(`Total: ${output.sectors.length} secteurs, ${totalNeighbors / 2} connexions`);

  // Générer ou mettre à jour board.json si demandé
  if (config.generate) {
    console.log('\n🔧 Génération du board.json...');
    generateNewBoardJson(config.boardPath, neighbors, sectors);
  } else {
    // Proposer la mise à jour
    console.log('\n💡 Pour mettre à jour board.json, relancez avec:');
    console.log(`   node svg-neighbor-detector.js --generate --board "${config.boardPath}"`);

    // Afficher quand même le format pour copier-coller
    generateBoardJsonFormat(neighbors);
  }
}

main().catch(console.error);