/**
 * Slugify : normalise un nom en identifiant de fichier stable.
 * minuscules → strip accents → non-alphanum → « - » → trim tirets.
 */
export function slugify(value: string): string {
  return value
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
}
