import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';
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

type InteractionHost = {
  selectedSector: ReturnType<typeof signal<{ number: number } | null>>;
  overlays: ReturnType<typeof signal<Set<'front' | 'resources'>>>;
  selectSector: (sector: { number: number }) => void;
  toggleOverlay: (overlay: 'front' | 'resources') => void;
  onMapBackgroundClick: (event: MouseEvent) => void;
};

function interactionHost(): InteractionHost {
  const component = Object.create(CarteComponent.prototype) as unknown as InteractionHost;
  component.selectedSector = signal<{ number: number } | null>(null);
  component.overlays = signal(new Set<'front' | 'resources'>());
  return component;
}

describe('sélection et calques de carte', () => {
  it('bascule la sélection d’un secteur', () => {
    const component = interactionHost();

    component.selectSector({ number: 5 });
    expect(component.selectedSector()).toEqual({ number: 5 });
    component.selectSector({ number: 5 });
    expect(component.selectedSector()).toBeNull();
  });

  it('active et désactive un calque sans toucher aux autres', () => {
    const component = interactionHost();

    component.toggleOverlay('front');
    component.toggleOverlay('resources');
    expect([...component.overlays()].sort()).toEqual(['front', 'resources']);
    component.toggleOverlay('front');
    expect([...component.overlays()]).toEqual(['resources']);
  });

  it('efface la sélection au clic sur le fond mais pas sur un secteur', () => {
    const component = interactionHost();
    component.selectedSector.set({ number: 3 });

    component.onMapBackgroundClick({ target: document.createElement('div') } as unknown as MouseEvent);
    expect(component.selectedSector()).toBeNull();

    component.selectedSector.set({ number: 3 });
    const path = document.createElementNS('http://www.w3.org/2000/svg', 'path');
    component.onMapBackgroundClick({ target: path } as unknown as MouseEvent);
    expect(component.selectedSector()).toEqual({ number: 3 });
  });
});

type NoteHost = {
  board: ReturnType<typeof signal<{ id: number } | null>>;
  selectedSector: ReturnType<typeof signal<{ number: number } | null>>;
  noteDraft: ReturnType<typeof signal<string>>;
  savingNote: ReturnType<typeof signal<boolean>>;
  api: { saveSectorNote: jest.Mock };
  sectorNotesRef: { reload: jest.Mock };
  snackBar: { open: jest.Mock };
  saveSectorNote: () => Promise<void>;
};

function noteHost(sector: { number: number } | null): NoteHost {
  const component = Object.create(CarteComponent.prototype) as unknown as NoteHost;
  component.board = signal({ id: 3 });
  component.selectedSector = signal(sector);
  component.noteDraft = signal('note du secteur');
  component.savingNote = signal(false);
  component.api = { saveSectorNote: jest.fn(() => of(null)) };
  component.sectorNotesRef = { reload: jest.fn() };
  component.snackBar = { open: jest.fn() };
  return component;
}

describe('note de secteur', () => {
  it('enregistre la note du secteur sélectionné et recharge les notes', async () => {
    const component = noteHost({ number: 5 });

    await component.saveSectorNote();

    expect(component.api.saveSectorNote).toHaveBeenCalledWith(3, 5, 'note du secteur');
    expect(component.sectorNotesRef.reload).toHaveBeenCalled();
    expect(component.snackBar.open).toHaveBeenCalledWith('Note enregistrée.', 'OK', {
      duration: 2500,
    });
  });

  it('ignore l’enregistrement sans secteur sélectionné', async () => {
    const component = noteHost(null);

    await component.saveSectorNote();

    expect(component.api.saveSectorNote).not.toHaveBeenCalled();
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

type ActionHost = {
  board: { (): { id: number; sectors: Record<number, unknown> } | null };
  myPlayer: { (): { id: number } | null };
  sectorsMap: { (): Map<number, { ownerId: number | null }> };
  placing: ReturnType<typeof signal<{ kind: 'unit' | 'vehicle'; id: number; label: string } | null>>;
  snackBar: { open: jest.Mock };
  playerService: {
    placeUnit: jest.Mock;
    placeVehicle: jest.Mock;
    error: () => string | null;
  };
  activeBoard: { reload: jest.Mock };
  playerActions: {
    loadActions: jest.Mock;
    actions: () => { type: string; fromSectorNumber: number | null }[];
    harvest: jest.Mock;
    error: () => string | null;
  };
  harvesting: ReturnType<typeof signal<boolean>>;
  harvestSectors: { (): number[] };
  dialog: { open: jest.Mock };
  importingBoard: ReturnType<typeof signal<boolean>>;
  selectedSector: ReturnType<typeof signal<unknown>>;
  instances: { refresh: jest.Mock };
  api: { adminUploadBoardAssets: jest.Mock; adminImportBoard: jest.Mock };
  deployReserve: (sectorNumber: number) => Promise<void>;
  harvestResources: () => Promise<void>;
  triggerBoardImport: () => void;
};

function actionHost(): ActionHost {
  const component = Object.create(CarteComponent.prototype) as unknown as ActionHost & {
    [key: string]: unknown;
  };
  component.board = () => ({ id: 3, sectors: {} });
  component.myPlayer = () => ({ id: 1 });
  component.sectorsMap = () => new Map([[5, { ownerId: 1 }], [6, { ownerId: 2 }]]);
  component.placing = signal(null);
  component.snackBar = { open: jest.fn() };
  component.playerService = {
    placeUnit: jest.fn().mockResolvedValue({ id: 10 }),
    placeVehicle: jest.fn().mockResolvedValue({ id: 11 }),
    error: () => null as string | null,
  };
  component.activeBoard = { reload: jest.fn() };
  component.playerActions = {
    loadActions: jest.fn().mockResolvedValue(undefined),
    actions: () => [],
    harvest: jest.fn().mockResolvedValue(true),
    error: () => null as string | null,
  };
  component.harvesting = signal(false);
  component.harvestSectors = () => [];
  component.dialog = { open: jest.fn() };
  component.importingBoard = signal(false);
  component.selectedSector = signal(null);
  component.instances = { refresh: jest.fn().mockResolvedValue([]) };
  component.api = {
    adminUploadBoardAssets: jest.fn(),
    adminImportBoard: jest.fn(),
  };
  return component;
}

describe('déploiement et récolte depuis la carte', () => {
  it('refuse un secteur qui n’appartient pas au joueur, puis déploie sur le bon', async () => {
    const component = actionHost();
    component.placing.set({ kind: 'unit', id: 10, label: 'LARBIN n°1' });

    await component.deployReserve(6);
    expect(component.playerService.placeUnit).not.toHaveBeenCalled();
    expect(component.snackBar.open).toHaveBeenCalledWith(
      'Choisissez un secteur que vous possédez.',
      'OK',
      { duration: 3000 },
    );

    await component.deployReserve(5);
    expect(component.playerService.placeUnit).toHaveBeenCalledWith(10, 3, 5);
    expect(component.snackBar.open).toHaveBeenCalledWith(
      'LARBIN n°1 déployé en secteur 5.',
      'OK',
      { duration: 3000 },
    );
    expect(component.placing()).toBeNull();
    expect(component.activeBoard.reload).toHaveBeenCalled();
  });

  it('déploie un véhicule et signale un échec', async () => {
    const component = actionHost();
    component.placing.set({ kind: 'vehicle', id: 20, label: 'VTT' });
    component.playerService.placeVehicle.mockResolvedValueOnce(null);
    component.playerService.error = () => 'Placement refusé';

    await component.deployReserve(5);

    expect(component.playerService.placeVehicle).toHaveBeenCalledWith(20, 3, 5);
    expect(component.snackBar.open).toHaveBeenCalledWith('Placement refusé', 'OK', {
      duration: 4000,
    });
    expect(component.placing()).not.toBeNull();
  });

  it('ne récolte que les secteurs non déjà récoltés', async () => {
    const component = actionHost();
    component.harvestSectors = () => [5, 6, 7];
    component.playerActions.actions = () => [
      { type: 'HARVEST_MONEY', fromSectorNumber: 5 },
      { type: 'SELL_RESOURCE', fromSectorNumber: null },
    ];

    await component.harvestResources();

    expect(component.playerActions.harvest).toHaveBeenCalledWith('HARVEST_RESOURCE', [6, 7]);
    expect(component.snackBar.open).toHaveBeenCalledWith(
      '2 secteur(s) récolté(s) en ressources.',
      'OK',
      { duration: 4000 },
    );
    expect(component.activeBoard.reload).toHaveBeenCalled();
  });

  it('signale une récolte déjà complète ou refusée', async () => {
    const complete = actionHost();
    complete.harvestSectors = () => [5];
    complete.playerActions.actions = () => [{ type: 'HARVEST_MONEY', fromSectorNumber: 5 }];
    await complete.harvestResources();
    expect(complete.snackBar.open).toHaveBeenCalledWith(
      'Tous vos secteurs sont déjà récoltés ce tour.',
      'OK',
      { duration: 3500 },
    );
    expect(complete.playerActions.harvest).not.toHaveBeenCalled();

    const refused = actionHost();
    refused.harvestSectors = () => [6];
    refused.playerActions.harvest.mockResolvedValueOnce(false);
    refused.playerActions.error = () => 'Récolte impossible';
    await refused.harvestResources();
    expect(refused.snackBar.open).toHaveBeenCalledWith('Récolte impossible', 'OK', {
      duration: 4000,
    });
    expect(refused.activeBoard.reload).not.toHaveBeenCalled();
  });
});

describe('import de board depuis la carte', () => {
  it('téléverse les assets puis importe le board', async () => {
    const component = actionHost();
    const result = {
      file: new File(['{}'], 'board.json', { type: 'application/json' }),
      mapImage: new File(['img'], 'map.png', { type: 'image/png' }),
      svgOverlay: new File(['svg'], 'overlay.svg', { type: 'image/svg+xml' }),
    };
    component.dialog.open.mockReturnValueOnce({ afterClosed: () => of(result) });
    component.api.adminUploadBoardAssets.mockReturnValueOnce(
      of({ mapImageUrl: '/boards/map.png', svgOverlayUrl: '/boards/overlay.svg' }),
    );
    component.api.adminImportBoard.mockReturnValueOnce(
      of({ name: 'Nouvelle carte', sectors: { 1: {}, 2: {} } }),
    );

    component.triggerBoardImport();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(component.api.adminUploadBoardAssets).toHaveBeenCalledWith(
      result.mapImage,
      result.svgOverlay,
    );
    expect(component.api.adminImportBoard).toHaveBeenCalledWith(
      result.file,
      '/boards/map.png',
      '/boards/overlay.svg',
    );
    expect(component.snackBar.open).toHaveBeenCalledWith(
      'Board "Nouvelle carte" importé (2 secteurs)',
      'OK',
      { duration: 5000 },
    );
    expect(component.selectedSector()).toBeNull();
    expect(component.activeBoard.reload).toHaveBeenCalled();
    expect(component.importingBoard()).toBe(false);
  });

  it('importe sans assets et signale une erreur', async () => {
    const component = actionHost();
    const result = { file: new File(['{}'], 'board.json', { type: 'application/json' }) };
    component.dialog.open.mockReturnValueOnce({ afterClosed: () => of(result) });
    component.api.adminImportBoard.mockReturnValueOnce(of({ name: 'Carte', sectors: {} }));

    component.triggerBoardImport();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(component.api.adminUploadBoardAssets).not.toHaveBeenCalled();
    expect(component.api.adminImportBoard).toHaveBeenCalledWith(result.file, undefined, undefined);

    const failed = actionHost();
    failed.dialog.open.mockReturnValueOnce({ afterClosed: () => of(result) });
    failed.api.adminImportBoard.mockReturnValueOnce(
      throwError(() => ({ error: { detail: 'Board invalide' } })),
    );
    failed.triggerBoardImport();
    await new Promise((resolve) => setTimeout(resolve, 0));
    expect(failed.snackBar.open).toHaveBeenCalledWith('Board invalide', 'Fermer', {
      duration: 6000,
    });
  });
});

type RenderHost = {
  selectedSector: ReturnType<typeof signal<{ number: number } | null>>;
  placing: ReturnType<typeof signal<unknown>>;
  hoveredSectorNumber: ReturnType<typeof signal<number | null>>;
  sectorsMap: () => Map<number, { number: number; ownerId: number | null }>;
  playerColorMap: () => ReadonlyMap<number, string>;
  eventCleanupFns: (() => void)[];
  isOverlay: (overlay: string) => boolean;
  updatePathStyle: jest.Mock;
  updateOverlays: jest.Mock;
  renderSectorLabels: jest.Mock;
  ensureDefs: (container: HTMLElement) => void;
  attachSectorListeners: (container: HTMLElement) => void;
  selectedSectorColor: () => string;
  getPlayerColor: (playerId: number | null) => string;
  getContrastColor: () => string;
  getHaloColor: () => string;
};

function renderHost(): RenderHost {
  const component = Object.create(CarteComponent.prototype) as unknown as RenderHost;
  component.selectedSector = signal(null);
  component.placing = signal(null);
  component.hoveredSectorNumber = signal(null);
  component.sectorsMap = () => new Map([
    [1, { number: 1, ownerId: 1 }],
    [2, { number: 2, ownerId: 2 }],
  ]);
  component.playerColorMap = () => new Map([[1, '#ff0000'], [2, '#00ff00']]);
  component.eventCleanupFns = [];
  component.isOverlay = () => false;
  component.updatePathStyle = jest.fn();
  return component;
}

function svgContainer(): HTMLElement {
  const container = document.createElement('div');
  container.innerHTML = `
    <svg>
      <defs></defs>
      <path id="path1" d="M0,0 L10,0 L10,10 Z" />
      <path id="path2" d="M20,0 L30,0 L30,10 Z" />
    </svg>`;
  return container;
}

describe('rendu SVG et interactions secteur', () => {
  it('prépare defs, motif neutre et filtre de sélection une seule fois', () => {
    const component = renderHost();
    const container = svgContainer();

    component.ensureDefs(container);
    component.ensureDefs(container);

    const svg = container.querySelector('svg')!;
    expect(svg.getAttribute('preserveAspectRatio')).toBe('none');
    expect(container.querySelectorAll('#neutral-stripes')).toHaveLength(1);
    expect(container.querySelectorAll('#selection-overflow')).toHaveLength(1);
    expect(container.querySelector('#neutral-stripes')?.getAttribute('patternUnits')).toBe(
      'userSpaceOnUse',
    );
  });

  it('relie clavier et souris aux secteurs et nettoie les écouteurs', () => {
    const component = renderHost();
    const container = svgContainer();

    component.attachSectorListeners(container);
    const first = container.querySelector<SVGPathElement>('#path1')!;
    const second = container.querySelector<SVGPathElement>('#path2')!;
    expect(first.getAttribute('tabindex')).toBe('0');
    expect(first.getAttribute('role')).toBe('button');
    expect(first.getAttribute('aria-label')).toBe('Secteur 1');
    expect(component.eventCleanupFns).toHaveLength(2);

    first.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(component.selectedSector()).toEqual({ number: 1, ownerId: 1 });
    second.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(component.selectedSector()).toEqual({ number: 2, ownerId: 2 });

    second.dispatchEvent(new MouseEvent('mouseenter'));
    expect(component.hoveredSectorNumber()).toBe(2);
    second.dispatchEvent(new MouseEvent('mouseleave'));
    expect(component.hoveredSectorNumber()).toBeNull();
    expect(component.updatePathStyle).toHaveBeenCalledWith(second, 2);

    component.eventCleanupFns.forEach((cleanup) => cleanup());
    first.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    expect(component.selectedSector()).toEqual({ number: 2, ownerId: 2 });
  });

  it('calcule couleurs de joueur, contraste et halo', () => {
    const component = renderHost();
    component.selectedSector.set({ number: 1, ownerId: 1 });

    expect(component.getPlayerColor(1)).toBe('#ff0000');
    expect(component.getPlayerColor(null)).toBeTruthy();
    expect(component.getPlayerColor(9)).toBeTruthy();
    expect(component.getContrastColor()).toBeTruthy();
    expect(component.getHaloColor()).toBeTruthy();
  });
});
