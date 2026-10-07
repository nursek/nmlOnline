export interface OptimizedImage {
  src: string;
  srcset: string;
}

const RASTER_EXTENSION = /\.(png|jpe?g|webp)$/i;

/** Mappe une URL d'original `assets/...` vers ses dérivés WebP générés par scripts/optimize-images.mjs. */
export function optimizedImage(originalUrl: string): OptimizedImage {
  if (!originalUrl) {
    return { src: '', srcset: '' };
  }
  const base = originalUrl.replace(/^\/?assets\//, '/assets/_opt/').replace(RASTER_EXTENSION, '');
  const small = `${base}.320.webp`;
  return { src: small, srcset: `${small} 1x, ${base}.640.webp 2x` };
}
