import { optimizedImage } from './optimized-image';

describe('optimizedImage', () => {
  it('mappe un original vers ses dérivés 320/640 sous _opt', () => {
    expect(optimizedImage('assets/shop/equipment/necrons/gauss-blaster.png')).toEqual({
      src: '/assets/_opt/shop/equipment/necrons/gauss-blaster.320.webp',
      srcset:
        '/assets/_opt/shop/equipment/necrons/gauss-blaster.320.webp 1x, /assets/_opt/shop/equipment/necrons/gauss-blaster.640.webp 2x',
    });
  });

  it("accepte un chemin absolu et les extensions jpg/jpeg/webp", () => {
    expect(optimizedImage('/assets/foo/bar.JPEG').src).toBe('/assets/_opt/foo/bar.320.webp');
    expect(optimizedImage('assets/foo/bar.webp').src).toBe('/assets/_opt/foo/bar.320.webp');
  });

  it('renvoie des chaînes vides pour une URL absente', () => {
    expect(optimizedImage('')).toEqual({ src: '', srcset: '' });
  });
});
