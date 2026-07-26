import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  linkedSignal,
  signal,
  viewChild,
} from '@angular/core';
import { SlicePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Board, PageResult, Player, Sector } from '../../models';
import { environment } from '../../../environments/environment';

interface SectorWithPlayer extends Sector {
  playerName?: string | null;
  playerColor?: string;
}

// Accepte uniquement les chemins relatifs same-origin ("/assets/...", pas "//host" ni "https://...").
function isSameOriginAssetUrl(url: string): boolean {
  return url.startsWith('/') && !url.startsWith('//');
}

@Component({
  selector: 'app-carte',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    SlicePipe,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './carte.component.html',
  styleUrls: ['./carte.component.scss'],
})
export class CarteComponent {
  private readonly sanitizer = inject(DomSanitizer);
  private static readonly COLORS = [
    '#6366f1',
    '#ef4444',
    '#10b981',
    '#f59e0b',
    '#8b5cf6',
    '#ec4899',
    '#f97316',
    '#06b6d4',
    '#84cc16',
    '#14b8a6',
    '#f43f5e',
    '#a855f7',
  ];

  // Re-read-only catalogs via httpResource.
  private readonly boardsRef = httpResource<Board[]>(() => ({
    url: `${environment.apiBaseUrl}/boards`,
  }));
  private readonly playersRef = httpResource<PageResult<Player>>(() => ({
    url: `${environment.apiBaseUrl}/players`,
    params: { page: '0', size: '50' },
  }));

  readonly loading = computed(() => this.boardsRef.isLoading() || this.playersRef.isLoading());
  readonly error = computed(() => {
    const e = this.boardsRef.error() || this.playersRef.error();
    return e ? 'Impossible de charger la carte. Vérifiez que le serveur est démarré.' : null;
  });

  readonly board = computed(() => this.boardsRef.value()?.[0] ?? null);
  readonly players = computed(() => this.playersRef.value()?.content ?? []);

  private readonly playerColorMap = computed(() => {
    const map = new Map<number, string>();
    this.players().forEach((player, index) => {
      if (player.id != null) {
        map.set(player.id, CarteComponent.COLORS[index % CarteComponent.COLORS.length]);
      }
    });
    return map;
  });

  private readonly svgOverlayUrl = computed(() => this.board()?.svgOverlayUrl || null);
  private readonly svgTextRef = httpResource.text(() => this.svgOverlayUrl() ?? undefined);

  readonly svgContent = computed<SafeHtml | string | null>(() => {
    const text = this.svgTextRef.value();
    if (!text) return null;
    // ponytail: bypass direct — le sanitizer HTML d'Angular jette les balises SVG.
    // OK car c'est un asset statique embarqué (assets/maps), pas de l'input utilisateur.
    // Defense-in-depth: on ne bypass que si l'URL est same-origin (chemin relatif en "/").
    // Sinon on renvoie la string brute — Angular sanitize lui-même au binding [innerHTML].
    const url = this.svgOverlayUrl();
    if (!url || !isSameOriginAssetUrl(url)) {
      return text;
    }
    return this.sanitizer.bypassSecurityTrustHtml(text);
  });
  readonly svgLoaded = computed(() => this.svgContent() !== null);

  readonly mapImageUrl = computed(() => this.board()?.mapImageUrl || null);

  // Interactive state.
  readonly selectedSector = signal<SectorWithPlayer | null>(null);
  // Default to all players selected; linked to the players signal so a refresh
  // of the players list reinitializes the selection set.
  readonly selectedPlayerIds = linkedSignal<Player[], Set<number>>({
    source: this.players,
    computation: (players) =>
      new Set(players.map((p) => p.id).filter((id): id is number => id != null)),
  });
  readonly showNeutral = signal(true);
  readonly hoveredSectorNumber = signal<number | null>(null);

  private readonly svgContainer = viewChild<ElementRef<HTMLDivElement>>('svgContainer');

  // Derived sector lookups.
  private readonly playerMap = computed(() => {
    const map = new Map<number, Player>();
    this.players().forEach((p) => {
      if (p.id != null) map.set(p.id, p);
    });
    return map;
  });

  readonly allSectors = computed(() => {
    const b = this.board();
    if (!b) return [];
    const playersById = this.playerMap();
    return Object.values(b.sectors).map((sector) => ({
      ...sector,
      playerName: sector.ownerId != null ? (playersById.get(sector.ownerId)?.name ?? null) : null,
      playerColor: this.getPlayerColor(sector.ownerId),
    })) as SectorWithPlayer[];
  });

  readonly sectorsMap = computed(() => {
    const b = this.board();
    const playersById = this.playerMap();
    const map = new Map<number, SectorWithPlayer>();
    if (!b) return map;
    Object.values(b.sectors).forEach((sector) => {
      if (sector.number !== null) {
        map.set(sector.number, {
          ...sector,
          playerName:
            sector.ownerId != null ? (playersById.get(sector.ownerId)?.name ?? null) : null,
          playerColor: this.getPlayerColor(sector.ownerId),
        });
      }
    });
    return map;
  });

  readonly neutralSectorsCount = computed(
    () => Object.values(this.board()?.sectors ?? {}).filter((s) => !s.ownerId).length,
  );

  readonly conqueredSectorsCount = computed(
    () => Object.values(this.board()?.sectors ?? {}).filter((s) => s.ownerId).length,
  );

  private readonly eventCleanupFns: (() => void)[] = [];

  constructor() {
    // Attach SVG listeners once whenever a fresh SVG is rendered. Cleanup drops
    // them before re-attaching on the next SVG.
    afterRenderEffect((onCleanup) => {
      if (!this.svgLoaded()) return;
      const container = this.svgContainer()?.nativeElement;
      if (!container) return;
      this.ensureNeutralPattern(container);
      this.attachSectorListeners(container);
      onCleanup(() => {
        this.eventCleanupFns.forEach((fn) => fn());
        this.eventCleanupFns.length = 0;
      });
    });

    // Repaint path styles whenever the SVG or any interactive signal changes.
    afterRenderEffect(() => {
      if (!this.svgLoaded()) return;
      const container = this.svgContainer()?.nativeElement;
      if (!container) return;
      // Tracking these signals re-runs this effect on each filter change.
      void this.selectedSector();
      void this.selectedPlayerIds();
      void this.showNeutral();
      this.updateAllPathColors(container);
    });
  }

  private attachSectorListeners(container: HTMLElement): void {
    const paths = container.querySelectorAll('path[id^="path"], polygon[id^="path"]');
    paths.forEach((path) => {
      const id = path.getAttribute('id');
      if (!id) return;
      const sectorNumber = parseInt(id.replace('path', ''), 10);
      if (Number.isNaN(sectorNumber)) return;

      const clickHandler = (event: Event) => {
        event.stopPropagation();
        this.onSectorClick(sectorNumber);
      };
      const enterHandler = () => this.onSectorHover(sectorNumber, path as SVGElement);
      const leaveHandler = () => this.onSectorLeave(sectorNumber, path as SVGElement);

      path.addEventListener('click', clickHandler);
      path.addEventListener('mouseenter', enterHandler);
      path.addEventListener('mouseleave', leaveHandler);

      this.eventCleanupFns.push(() => {
        path.removeEventListener('click', clickHandler);
        path.removeEventListener('mouseenter', enterHandler);
        path.removeEventListener('mouseleave', leaveHandler);
      });
    });
  }

  private ensureNeutralPattern(container: HTMLElement): void {
    const svg = container.querySelector('svg');
    if (!svg || svg.querySelector('#neutral-stripes')) return;

    const svgNs = 'http://www.w3.org/2000/svg';
    const defs = document.createElementNS(svgNs, 'defs');
    const pattern = document.createElementNS(svgNs, 'pattern');
    pattern.setAttribute('id', 'neutral-stripes');
    pattern.setAttribute('patternUnits', 'userSpaceOnUse');
    pattern.setAttribute('width', '8');
    pattern.setAttribute('height', '8');
    pattern.setAttribute('patternTransform', 'rotate(45)');

    const bg = document.createElementNS(svgNs, 'rect');
    bg.setAttribute('width', '8');
    bg.setAttribute('height', '8');
    bg.setAttribute('fill', '#ffffff');

    const stripe = document.createElementNS(svgNs, 'rect');
    stripe.setAttribute('width', '3');
    stripe.setAttribute('height', '8');
    stripe.setAttribute('fill', '#94a3b8');

    pattern.appendChild(bg);
    pattern.appendChild(stripe);
    defs.appendChild(pattern);
    svg.appendChild(defs);
  }

  private updatePathStyle(path: SVGElement, sectorNumber: number): void {
    const sector = this.sectorsMap().get(sectorNumber);
    const color = sector ? this.getSectorColor(sector) : '#94a3b8';
    const isNeutral = sector?.ownerId == null;
    const isSelected = this.selectedSector()?.number === sectorNumber;
    const isDimmed =
      (sector?.ownerId != null && !this.selectedPlayerIds().has(sector.ownerId)) ||
      (sector?.ownerId == null && !this.showNeutral());

    path.style.fill = isNeutral ? 'url(#neutral-stripes)' : color + '66';
    path.style.stroke = color;
    path.style.strokeWidth = '2';
    path.style.cursor = 'pointer';
    path.style.transition = 'all 0.2s ease';
    path.style.opacity = isDimmed ? '0.25' : '1';

    if (isSelected) {
      path.style.fill = isNeutral ? '#94a3b8' : color;
      path.style.strokeWidth = '5';
      path.style.filter = 'drop-shadow(0 0 12px ' + color + ')';
    } else {
      path.style.filter = 'none';
    }
  }

  private updateAllPathColors(container: HTMLElement): void {
    const paths = container.querySelectorAll('path[id^="path"], polygon[id^="path"]');
    paths.forEach((path) => {
      const id = path.getAttribute('id');
      if (!id) return;
      const sectorNumber = parseInt(id.replace('path', ''), 10);
      if (!Number.isNaN(sectorNumber)) {
        this.updatePathStyle(path as SVGElement, sectorNumber);
      }
    });
  }

  private onSectorClick(sectorNumber: number): void {
    const sector = this.sectorsMap().get(sectorNumber);
    if (sector) {
      this.selectSector(sector);
    }
  }

  private onSectorHover(sectorNumber: number, path: SVGElement): void {
    this.hoveredSectorNumber.set(sectorNumber);
    const sector = this.sectorsMap().get(sectorNumber);
    const color = sector ? this.getSectorColor(sector) : '#94a3b8';
    if (this.selectedSector()?.number !== sectorNumber) {
      path.style.fill = color + 'B3';
      path.style.strokeWidth = '3';
    }
  }

  private onSectorLeave(sectorNumber: number, path: SVGElement): void {
    this.hoveredSectorNumber.set(null);
    this.updatePathStyle(path, sectorNumber);
  }

  getPlayerColor(playerId: number | null): string {
    if (!playerId) return '#94a3b8';
    return this.playerColorMap().get(playerId) || '#94a3b8';
  }

  getSectorColor(sector: Sector | SectorWithPlayer): string {
    return this.getPlayerColor(sector.ownerId);
  }

  /** Readable text color (white or dark) for a given hex background. */
  getContrastColor(hexColor: string): string {
    const hex = hexColor.replace('#', '');
    const r = Number.parseInt(hex.substring(0, 2), 16);
    const g = Number.parseInt(hex.substring(2, 4), 16);
    const b = Number.parseInt(hex.substring(4, 6), 16);
    const yiq = (r * 299 + g * 587 + b * 114) / 1000;
    return yiq >= 128 ? '#1e293b' : '#ffffff';
  }

  selectSector(sector: SectorWithPlayer): void {
    this.selectedSector.set(this.selectedSector()?.number === sector.number ? null : sector);
  }

  onMapBackgroundClick(event: MouseEvent): void {
    const target = event.target as HTMLElement;
    if (!target.closest('path, polygon')) {
      this.selectedSector.set(null);
    }
  }

  togglePlayerFilter(player: Player): void {
    if (player.id == null) return;
    const next = new Set(this.selectedPlayerIds());
    if (next.has(player.id)) next.delete(player.id);
    else next.add(player.id);
    this.selectedPlayerIds.set(next);
  }

  toggleNeutralFilter(): void {
    this.showNeutral.update((v) => !v);
  }

  clearFilter(): void {
    this.selectedPlayerIds.set(
      new Set(
        this.players()
          .map((p) => p.id)
          .filter((id): id is number => id != null),
      ),
    );
    this.showNeutral.set(true);
  }

  getSectorByNumber(number: number): SectorWithPlayer | undefined {
    return this.sectorsMap().get(number);
  }

  getInitials(name: string): string {
    return name
      .split(' ')
      .map((word) => word[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }

  getHoveredSectorName(): string {
    const num = this.hoveredSectorNumber();
    if (num === null) return '';
    const sector = this.sectorsMap().get(num);
    return sector ? `${sector.name} - ${sector.playerName || 'Neutre'}` : '';
  }
}
