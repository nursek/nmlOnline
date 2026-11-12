import { readFileSync } from 'node:fs';

const [svgPath, boardPath] = process.argv.slice(2);
if (!svgPath || !boardPath) {
  console.error('usage: node tools/check-map.mjs <overlay.svg> <board.json>');
  process.exit(2);
}

const svg = readFileSync(svgPath, 'utf8');
const board = JSON.parse(readFileSync(boardPath, 'utf8'));
const errors = [];

const ids = [...svg.matchAll(/<(?:path|polygon)\b[^>]*\bid=["']path(\d+)["']/g)].map((match) => Number(match[1]));
const sectors = Array.isArray(board.sectors) ? board.sectors : [];
const byNumber = new Map();

if (!sectors.length) errors.push('board.json : "sectors" est vide ou absent');
for (const sector of sectors) {
  if (!Number.isInteger(sector.number) || sector.number < 1) {
    errors.push(`secteur sans numéro valide : ${JSON.stringify(sector)}`);
    continue;
  }
  if (byNumber.has(sector.number)) errors.push(`numéro ${sector.number} dupliqué dans board.json`);
  byNumber.set(sector.number, sector);
  if (typeof sector.name !== 'string' || !sector.name.trim()) {
    errors.push(`secteur ${sector.number} : "name" manquant`);
  }
  if (!Number.isFinite(sector.income)) errors.push(`secteur ${sector.number} : "income" manquant`);
  if (sector.neighbors !== undefined && !Array.isArray(sector.neighbors)) {
    errors.push(`secteur ${sector.number} : "neighbors" doit être une liste`);
  }
}

const duplicateIds = ids.filter((id, index) => ids.indexOf(id) !== index);
if (duplicateIds.length) errors.push(`id="pathN" dupliqué(s) dans le SVG : ${duplicateIds.join(', ')}`);
for (const id of ids) if (!byNumber.has(id)) errors.push(`SVG : path${id} sans secteur dans board.json`);
for (const number of byNumber.keys()) if (!ids.includes(number)) errors.push(`board.json : secteur ${number} sans path${number} dans le SVG`);

for (const sector of byNumber.values()) {
  const neighbors = sector.neighbors ?? [];
  if (new Set(neighbors).size !== neighbors.length) errors.push(`secteur ${sector.number} : voisin dupliqué`);
  for (const neighbor of neighbors) {
    if (neighbor === sector.number) errors.push(`secteur ${sector.number} : voisin de lui-même`);
    else if (!byNumber.has(neighbor)) errors.push(`secteur ${sector.number} : voisin ${neighbor} inexistant`);
    else if (!(byNumber.get(neighbor).neighbors ?? []).includes(sector.number)) {
      errors.push(`voisinage non réciproque : ${sector.number} → ${neighbor}`);
    }
  }
}

const start = byNumber.keys().next().value;
if (start !== undefined) {
  const reached = new Set([start]);
  const queue = [start];
  while (queue.length) {
    for (const neighbor of byNumber.get(queue.shift()).neighbors ?? []) {
      if (byNumber.has(neighbor) && !reached.has(neighbor)) {
        reached.add(neighbor);
        queue.push(neighbor);
      }
    }
  }
  if (reached.size !== byNumber.size) {
    errors.push(`graphe non connexe : ${reached.size}/${byNumber.size} secteurs atteints`);
  }
}

if (errors.length) {
  console.error(`✗ ${errors.length} problème(s) dans ${svgPath} / ${boardPath}`);
  for (const error of errors) console.error(`  - ${error}`);
  process.exit(1);
}
console.log(`✓ ${ids.length} secteurs, voisinage réciproque et graphe connexe (${sectors.length} entrées board.json)`);
