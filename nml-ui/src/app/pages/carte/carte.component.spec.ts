import { CarteComponent, isSameOriginAssetUrl } from './carte.component';

describe('isSameOriginAssetUrl', () => {
  it('accepte les chemins same-origin et rejette les hôtes déguisés', () => {
    expect(isSameOriginAssetUrl('/boards/overlay.svg')).toBe(true);
    expect(isSameOriginAssetUrl('/assets/maps/main-map-overlay.svg')).toBe(true);
    expect(isSameOriginAssetUrl('/\\evil.example/x.svg')).toBe(false);
    expect(isSameOriginAssetUrl('//evil.example/x.svg')).toBe(false);
    expect(isSameOriginAssetUrl('https://evil.example/x.svg')).toBe(false);
  });
});

type OverlayHost = {
  overlays: () => ReadonlySet<'front' | 'resources'>;
  frontSectors: () => { mine: number[]; enemies: number[] };
  updateOverlays: (container: HTMLElement) => void;
};

function renderOverlays(mine: number[], enemies: number[]): SVGElement {
  const container = document.createElement('div');
  container.innerHTML = `
    <svg>
      <defs></defs>
      <polygon id="path1" points="0,0 20,0 20,20 0,20" />
      <path id="path2" d="M30,0 L50,0 L50,20 L30,20 Z" />
      <g id="sector-labels"></g>
    </svg>`;
  const component = Object.create(CarteComponent.prototype) as unknown as OverlayHost;
  component.overlays = () => new Set<'front' | 'resources'>(['front']);
  component.frontSectors = () => ({ mine, enemies });
  component.updateOverlays(container);
  return container.querySelector('svg')!;
}

describe('updateOverlays — frontières', () => {
  it('conserve la géométrie d’un secteur <polygon> dans le masque', () => {
    const svg = renderOverlays([1], [2]);
    const maskShape = svg.querySelector('#front-borders-mask')?.firstElementChild;
    expect(maskShape?.tagName.toLowerCase()).toBe('polygon');
    expect(maskShape?.getAttribute('points')).toBe('0,0 20,0 20,20 0,20');
  });

  it('masque le calque du secteur ennemi derrière mes secteurs', () => {
    const svg = renderOverlays([1], [2]);
    const front = svg.querySelector('#map-overlays')?.firstElementChild;
    expect(front?.tagName.toLowerCase()).toBe('path');
    expect(front?.getAttribute('mask')).toBe('url(#front-borders-mask)');
    expect(front?.getAttribute('id')).toBeNull();
  });
});
