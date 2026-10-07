import { test } from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { copyFile, mkdir, mkdtemp, readdir, rm, stat, writeFile } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join } from 'node:path';
import { isUnchanged, outputsFor, removeOrphans } from './optimize-images.mjs';

const TINY_PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64',
);

function runScript(scriptPath, args = []) {
  return new Promise((resolve, reject) => {
    const child = spawn(process.execPath, [scriptPath, ...args], {
      stdio: ['ignore', 'pipe', 'ignore'],
    });
    let stdout = '';
    child.stdout.on('data', (chunk) => (stdout += chunk));
    child.on('error', reject);
    child.on('close', (code) => resolve({ code, stdout }));
  });
}

test("outputsFor remplace l'extension par les deux largeurs WebP", () => {
  assert.deepEqual(outputsFor('shop/equipment/foo.PNG'), [
    'shop/equipment/foo.320.webp',
    'shop/equipment/foo.640.webp',
  ]);
});

test('isUnchanged ne saute que si sha1, outputs, force et encodeur concordent', () => {
  const entry = { sha1: 'a', outputs: ['x.320.webp', 'x.640.webp'] };
  const outputs = ['x.320.webp', 'x.640.webp'];
  const stable = { force: false, encoderChanged: false };

  assert.equal(isUnchanged(entry, 'a', outputs, stable), true);
  assert.equal(isUnchanged(entry, 'b', outputs, stable), false);
  assert.equal(isUnchanged(entry, 'a', ['x.320.webp'], stable), false);
  assert.equal(isUnchanged(entry, 'a', outputs, { force: true, encoderChanged: false }), false);
  assert.equal(isUnchanged(entry, 'a', outputs, { force: false, encoderChanged: true }), false);
  assert.equal(isUnchanged(undefined, 'a', outputs, stable), false);
});

test('removeOrphans supprime les webp et tmp non attendus, garde le reste', async () => {
  const dir = await mkdtemp(join(tmpdir(), 'nml-images-'));
  await mkdir(join(dir, 'sub'), { recursive: true });
  await writeFile(join(dir, 'sub', 'keep.320.webp'), '');
  await writeFile(join(dir, 'sub', 'gone.320.webp'), '');
  await writeFile(join(dir, 'sub', 'keep.320.webp.tmp'), '');
  await writeFile(join(dir, 'sub', 'notes.txt'), '');

  const removed = await removeOrphans(dir, new Set(['sub/keep.320.webp']));

  assert.deepEqual(removed.sort(), ['sub/gone.320.webp', 'sub/keep.320.webp.tmp']);
  assert.deepEqual((await readdir(join(dir, 'sub'))).sort(), ['keep.320.webp', 'notes.txt']);
});

test('main() génère, saute, régénère un dérivé manquant et supprime les orphelins', async () => {
  const cacheDir = join(import.meta.dirname, '..', 'node_modules', '.cache');
  await mkdir(cacheDir, { recursive: true });
  const root = await mkdtemp(join(cacheDir, 'img-pipeline-'));
  try {
    await mkdir(join(root, 'scripts'), { recursive: true });
    await mkdir(join(root, 'src', 'assets'), { recursive: true });
    await copyFile(
      join(import.meta.dirname, 'optimize-images.mjs'),
      join(root, 'scripts', 'optimize-images.mjs'),
    );
    await writeFile(join(root, 'src', 'assets', 'tiny.png'), TINY_PNG);
    const script = join(root, 'scripts', 'optimize-images.mjs');
    const outputDir = join(root, 'src', 'assets', '_opt');

    assert.equal((await runScript(script)).code, 0);
    assert.deepEqual((await readdir(outputDir)).sort(), ['tiny.320.webp', 'tiny.640.webp']);
    assert.ok((await stat(join(root, '.image-pipeline.json'))).size > 0);

    await rm(join(outputDir, 'tiny.320.webp'));
    const dryRun = await runScript(script, ['--dry-run']);
    assert.equal(dryRun.code, 0);
    assert.match(dryRun.stdout, /1 à générer/);
    assert.equal((await runScript(script)).code, 0);
    await stat(join(outputDir, 'tiny.320.webp'));

    await writeFile(join(outputDir, 'orphan.320.webp'), 'x');
    assert.equal((await runScript(script)).code, 0);
    await assert.rejects(stat(join(outputDir, 'orphan.320.webp')));
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});
