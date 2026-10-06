import { createHash } from 'node:crypto';
import { mkdir, readFile, readdir, rename, stat, unlink, writeFile } from 'node:fs/promises';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';
import sharp from 'sharp';

// Incrémenter pour régénérer tous les dérivés (qualité, largeur ou encodeur modifiés).
const PIPELINE_VERSION = 1;
const WIDTHS = [320, 640];
const WEBP_QUALITY = 80;

const scriptDir = dirname(fileURLToPath(import.meta.url));
const sourceDir = join(scriptDir, '..', 'src', 'assets');
const outputDir = join(sourceDir, '_opt');
const manifestPath = join(scriptDir, '..', '.image-pipeline.json');

// maps/ exclu : carte affichée en pleine largeur, le WebP pleine résolution est plus lourd que le PNG source.
const EXCLUDED_DIRS = new Set(['_opt', 'maps']);
const SOURCE_EXTENSIONS = /\.(png|jpe?g|webp)$/i;

async function walk(dir) {
  const found = [];
  for (const entry of await readdir(dir, { withFileTypes: true })) {
    const path = join(dir, entry.name);
    if (entry.isDirectory()) {
      if (!EXCLUDED_DIRS.has(entry.name)) found.push(...(await walk(path)));
    } else if (SOURCE_EXTENSIONS.test(entry.name)) {
      found.push(path);
    }
  }
  return found;
}

export function outputsFor(relativePath) {
  const base = relativePath.replace(SOURCE_EXTENSIONS, '');
  return WIDTHS.map((width) => `${base}.${width}.webp`);
}

export function isUnchanged(previousEntry, sha1, outputs, { force, encoderChanged }) {
  return (
    !force &&
    !encoderChanged &&
    previousEntry?.sha1 === sha1 &&
    JSON.stringify(previousEntry?.outputs) === JSON.stringify(outputs)
  );
}

async function exists(path) {
  try {
    await stat(path);
    return true;
  } catch {
    return false;
  }
}

async function readManifest() {
  try {
    return JSON.parse(await readFile(manifestPath, 'utf8'));
  } catch {
    return { entries: {} };
  }
}

export async function removeOrphans(dir, expected) {
  const removed = [];
  async function scan(current) {
    for (const entry of await readdir(current, { withFileTypes: true })) {
      const path = join(current, entry.name);
      if (entry.isDirectory()) {
        await scan(path);
      } else if (entry.name.endsWith('.tmp') || /\.webp$/i.test(entry.name)) {
        const relativePath = relative(dir, path).split('\\').join('/');
        if (!expected.has(relativePath)) {
          await unlink(path);
          removed.push(relativePath);
        }
      }
    }
  }
  if (await exists(dir)) await scan(dir);
  return removed;
}

export async function main() {
  const args = new Set(process.argv.slice(2));
  const dryRun = args.has('--dry-run');
  const force = args.has('--force');
  const verbose = args.has('--verbose');

  const previous = await readManifest();
  const encoderChanged =
    previous.pipeline !== PIPELINE_VERSION ||
    previous.sharp !== sharp.versions.sharp ||
    previous.vips !== sharp.versions.vips;
  if (encoderChanged && !dryRun) {
    console.log(
      `optimize-images: encodeur/pipeline modifié (v${previous.pipeline ?? '?'} → v${PIPELINE_VERSION}), régénération complète`,
    );
  }

  const sources = await walk(sourceDir);
  const entries = {};
  const expected = new Set();
  let generated = 0;
  let upToDate = 0;
  const failures = [];
  const started = Date.now();

  for (const source of sources) {
    const relativePath = relative(sourceDir, source).split('\\').join('/');
    try {
      const bytes = await readFile(source);
      const sha1 = createHash('sha1').update(bytes).digest('hex');
      const outputs = outputsFor(relativePath);
      for (const output of outputs) expected.add(output);

      const previousEntry = previous.entries?.[relativePath];
      if (isUnchanged(previousEntry, sha1, outputs, { force, encoderChanged })) {
        const present =
          dryRun ||
          (await Promise.all(outputs.map((output) => exists(join(outputDir, output))))).every(
            Boolean,
          );
        if (present) {
          entries[relativePath] = previousEntry;
          upToDate++;
          continue;
        }
      }

      if (dryRun) {
        generated++;
        if (verbose) console.log(`  à générer ${relativePath} → ${outputs.join(', ')}`);
        entries[relativePath] = { sha1, outputs };
        continue;
      }

      for (const [index, width] of WIDTHS.entries()) {
        const buffer = await sharp(bytes)
          .rotate()
          .resize({ width, withoutEnlargement: true })
          .webp({ quality: WEBP_QUALITY, effort: 4 })
          .toBuffer();
        const target = join(outputDir, outputs[index]);
        await mkdir(dirname(target), { recursive: true });
        // Écriture atomique : une interruption ne doit pas laisser un dérivé tronqué considéré à jour.
        const tmp = `${target}.tmp`;
        await writeFile(tmp, buffer);
        await rename(tmp, target);
        if (verbose) console.log(`  généré ${outputs[index]}`);
      }
      entries[relativePath] = { sha1, outputs };
      generated++;
    } catch (error) {
      failures.push({ relativePath, message: error.message });
    }
  }

  let removed = [];
  if (!dryRun) {
    removed = await removeOrphans(outputDir, expected);
    const manifest = {
      pipeline: PIPELINE_VERSION,
      sharp: sharp.versions.sharp,
      vips: sharp.versions.vips,
      entries,
    };
    await mkdir(outputDir, { recursive: true });
    await writeFile(manifestPath, JSON.stringify(manifest, null, 2));
  }

  const seconds = ((Date.now() - started) / 1000).toFixed(1);
  const label = dryRun ? 'à générer' : 'généré';
  console.log(
    `optimize-images: ${sources.length} source(s) | ${generated} ${label}(s) | ${upToDate} à jour | ${removed.length} orphelin(s) supprimé(s) | ${seconds}s`,
  );

  for (const failure of failures) {
    console.error(`  ÉCHEC ${failure.relativePath}: ${failure.message}`);
  }
  if (failures.length > 0) {
    process.exitCode = 1;
  }
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  await main();
}
