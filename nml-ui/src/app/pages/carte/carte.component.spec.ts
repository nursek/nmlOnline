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

type PathHost = {
  overlays: () => ReadonlySet<'front' | 'resources'>;
  selectedSector: () => { number: number } | null;
  sectorsMap: () => Map<
    number,
    { number: number; ownerId: number | null; resource: string | null }
  >;
  playerColorMap: () => ReadonlyMap<number, string>;
  isAllyOwner: (playerId: number | null) => boolean;
  hoveredSectorNumber: { set: (value: number | null) => void };
  updatePathStyle: (path: SVGElement, sectorNumber: number) => void;
  onSectorHover: (sectorNumber: number, path: SVGElement) => void;
};

function styleHost(overlay: 'resources' | null, selected: number | null): PathHost {
  const component = Object.create(CarteComponent.prototype) as unknown as PathHost;
  component.overlays = () => new Set<'front' | 'resources'>(overlay ? [overlay] : []);
  component.selectedSector = () => (selected === null ? null : { number: selected });
  component.sectorsMap = () =>
    new Map([
      [1, { number: 1, ownerId: 1, resource: 'Prométhéum' }],
      [2, { number: 2, ownerId: 2, resource: null }],
    ]);
  component.playerColorMap = () =>
    new Map([
      [1, '#ff0000'],
      [2, '#00ff00'],
    ]);
  component.isAllyOwner = () => false;
  component.hoveredSectorNumber = { set: () => undefined };
  return component;
}

function svgPath(): SVGElement {
  return document.createElementNS('http://www.w3.org/2000/svg', 'path');
}

describe('updatePathStyle — calque ressources', () => {
  it('aplatit le secteur sans trait ni halo qui débordent', () => {
    const component = styleHost('resources', 1);
    const selected = svgPath();
    const normal = svgPath();

    component.updatePathStyle(selected, 1);
    component.updatePathStyle(normal, 2);

    expect(selected.style.fill).toBe('#6e788580');
    expect(normal.style.fill).toBe('#ffffff80');
    for (const path of [selected, normal]) {
      expect(path.style.stroke).toBe('none');
      expect(path.style.strokeWidth).toBe('0');
      expect(path.style.filter).toBe('none');
    }
  });

  it('grise la case au survol', () => {
    const component = styleHost('resources', null);
    const path = svgPath();

    component.updatePathStyle(path, 1);
    expect(path.style.fill).toBe('#ffffff80');

    component.onSectorHover(1, path);

    expect(path.style.fill).toBe('#6e788540');
    expect(path.style.strokeWidth).toBe('0');
  });

  it('restaure la couleur du propriétaire et le contour de sélection hors calque', () => {
    const component = styleHost(null, 1);
    const selected = svgPath();

    component.updatePathStyle(selected, 1);

    expect(selected.style.stroke).toBe('#ff0000');
    expect(selected.style.strokeWidth).toBe('3');
    expect(selected.style.filter).toContain('url(#selection-overflow)');
  });
});
