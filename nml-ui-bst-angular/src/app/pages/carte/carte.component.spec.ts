import { isSameOriginAssetUrl } from './carte.component';

describe('isSameOriginAssetUrl', () => {
  it('accepte les chemins same-origin et rejette les hôtes déguisés', () => {
    expect(isSameOriginAssetUrl('/boards/overlay.svg')).toBe(true);
    expect(isSameOriginAssetUrl('/assets/maps/main-map-overlay.svg')).toBe(true);
    expect(isSameOriginAssetUrl('/\\evil.example/x.svg')).toBe(false);
    expect(isSameOriginAssetUrl('//evil.example/x.svg')).toBe(false);
    expect(isSameOriginAssetUrl('https://evil.example/x.svg')).toBe(false);
  });
});
