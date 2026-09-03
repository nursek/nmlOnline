import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
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
import { MAP_THEME } from './carte.config';

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

  // Couleur neutre exposée au template (légende) et au SCSS.
  readonly neutralColor = MAP_THEME.neutralColor;
  readonly labelFontPx = MAP_THEME.label.fontPx;
  readonly labelWeight = MAP_THEME.label.weight;
  readonly labelStrokeColor = MAP_THEME.label.strokeColor;
  readonly labelStrokeWidth = MAP_THEME.label.strokeWidthPx;
  // Décalage (px CSS) de l'overlay par rapport à l'image de fond, pour rattraper
  // un mismatch de calage entre le SVG et le PNG. Valeur négative tire l'overlay
  // vers le haut (corrige un overlay « trop bas »), positive vers le bas.
  readonly overlayOffsetY = MAP_THEME.overlay.offsetY;
  readonly overlayOffsetX = MAP_THEME.overlay.offsetX;

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
    // Indexation par id trié : la couleur d'un joueur ne dépend plus de
    // l'ordre d'insertion renvoyé par l'API, donc reste stable au refresh.
    const sorted = [...this.players()]
      .filter((p) => p.id != null)
      .sort((a, b) => (a.id as number) - (b.id as number));
    const palette = MAP_THEME.playerPalette;
    sorted.forEach((player, index) => {
      map.set(player.id as number, palette[index % palette.length]);
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
      this.ensureDefs(container);
      this.renderSectorLabels(container);
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
      this.updateSectorLabelColors(container);
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

  private ensureDefs(container: HTMLElement): void {
    const svg = container.querySelector('svg');
    if (!svg) return;
    const svgNs = 'http://www.w3.org/2000/svg';

    svg.setAttribute('preserveAspectRatio', 'none');
    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.style.display = 'block';

    let defs = svg.querySelector<SVGDefsElement>('defs');
    if (!defs) {
      defs = document.createElementNS(svgNs, 'defs');
      svg.appendChild(defs);
    }

    // Motif de hachures neutres.
    if (!svg.querySelector('#neutral-stripes')) {
      const np = MAP_THEME.neutralPattern;
      const pattern = document.createElementNS(svgNs, 'pattern');
      pattern.setAttribute('id', 'neutral-stripes');
      pattern.setAttribute('patternUnits', 'userSpaceOnUse');
      pattern.setAttribute('width', String(np.width));
      pattern.setAttribute('height', String(np.height));
      pattern.setAttribute('patternTransform', `rotate(${np.rotateDeg})`);

      const bg = document.createElementNS(svgNs, 'rect');
      bg.setAttribute('width', String(np.width));
      bg.setAttribute('height', String(np.height));
      bg.setAttribute('fill', np.background);

      const stripe = document.createElementNS(svgNs, 'rect');
      stripe.setAttribute('width', String(np.stripeWidth));
      stripe.setAttribute('height', String(np.height));
      stripe.setAttribute('fill', MAP_THEME.neutralColor);

      pattern.appendChild(bg);
      pattern.appendChild(stripe);
      defs.appendChild(pattern);
    }

    // Filtre de débordement de contour à la sélection (feMorphology dilate).
    // Identique pour tous les secteurs : conserve la couleur du SourceGraphic,
    // puis remet l'original au-dessus via feMerge.
    if (!svg.querySelector('#selection-overflow')) {
      const filter = document.createElementNS(svgNs, 'filter');
      filter.setAttribute('id', 'selection-overflow');
      // Laisser de la marge autour pour que la zone dilatée ne soit pas rognée.
      filter.setAttribute('x', '-20%');
      filter.setAttribute('y', '-20%');
      filter.setAttribute('width', '160%');
      filter.setAttribute('height', '160%');

      const morph = document.createElementNS(svgNs, 'feMorphology');
      morph.setAttribute('in', 'SourceGraphic');
      morph.setAttribute('operator', 'dilate');
      morph.setAttribute('radius', String(MAP_THEME.selection.overflowRadius));
      morph.setAttribute('result', 'dilated');

      const merge = document.createElementNS(svgNs, 'feMerge');
      const node1 = document.createElementNS(svgNs, 'feMergeNode');
      node1.setAttribute('in', 'dilated');
      const node2 = document.createElementNS(svgNs, 'feMergeNode');
      node2.setAttribute('in', 'SourceGraphic');
      merge.appendChild(node1);
      merge.appendChild(node2);

      filter.appendChild(morph);
      filter.appendChild(merge);
      defs.appendChild(filter);
    }
  }

  /**
   * Positionne le label au « pôle d'inaccessibilité » : le point intérieur
   * le plus éloigné de la bordure. Approximé par échantillonnage d'une grille
   * dans la bbox (filtrée via isPointInFill) puis max de la distance min aux
   * points du contour. Contrairement au centroïde, l'étiquette reste loin des
   * bords même sur les secteurs en L ou étroits (28, 2, 25, 42, 8, 16, 12, 6).
   * Retombe sur le centre de la bbox si la géométrie n'est pas mesurable.
   * ponytail: coût O(samples² × boundary) par secteur — OK pour ~50 secteurs
   * au chargement ; revoir si la carte devient très dense.
   */
  private renderSectorLabels(container: HTMLElement): void {
    const svg = container.querySelector('svg');
    if (!svg) return;
    const svgNs = 'http://www.w3.org/2000/svg';

    let labelsGroup = svg.querySelector<SVGGElement>('#sector-labels');
    if (!labelsGroup) {
      labelsGroup = document.createElementNS(svgNs, 'g');
      labelsGroup.setAttribute('id', 'sector-labels');
      labelsGroup.setAttribute('pointer-events', 'none');
      svg.appendChild(labelsGroup);
    }
    // Recrée les <text> à chaque cycle : simple et idempotent.
    labelsGroup.replaceChildren();

    const paths = svg.querySelectorAll('path[id^="path"], polygon[id^="path"]');
    paths.forEach((path) => {
      const id = path.getAttribute('id');
      if (!id) return;
      const sectorNumber = parseInt(id.replace('path', ''), 10);
      if (Number.isNaN(sectorNumber)) return;

      const center = this.computeVisualCenter(path as SVGGeometryElement);
      const text = document.createElementNS(svgNs, 'text');
      text.setAttribute('x', String(center.x));
      text.setAttribute('y', String(center.y));
      // Centre le glyphe sur le pôle d'inaccessibilité : sans ça, l'ancrage
      // par défaut (start/alphabetic) fait déborder le numéro vers la droite
      // et le haut, surtout sur les secteurs étroits/allongés.
      text.setAttribute('text-anchor', 'middle');
      text.setAttribute('dominant-baseline', 'central');
      text.setAttribute('class', 'sector-label');
      text.setAttribute('data-sector', String(sectorNumber));
      text.textContent = String(sectorNumber);
      labelsGroup.appendChild(text);
    });
  }

  /**
   * Pôle d'inaccessibilité approximé. 1) échantillonne le contour, 2) égrène
   * une grille dans la bbox en ne gardant que les points intérieurs
   * (isPointInFill), 3) retourne celui dont la distance min au contour est max.
   */
  private computeVisualCenter(el: SVGGeometryElement): { x: number; y: number } {
    const bbox = el.getBBox();
    const fallback = { x: bbox.x + bbox.width / 2, y: bbox.y + bbox.height / 2 };

    // Échantillonne le contour. getTotalLength/getPointAtLength n'existent que
    // sur SVGPathElement ; pour les <polygon> on décode les points.
    const boundary: { x: number; y: number }[] = [];
    const pathEl = el.tagName.toLowerCase() === 'path' ? (el as SVGPathElement) : null;
    try {
      if (pathEl) {
        const total = pathEl.getTotalLength();
        if (total <= 0) return fallback;
        const BOUNDARY_SAMPLES = Math.min(128, Math.max(32, Math.ceil(total / 6)));
        for (let i = 0; i < BOUNDARY_SAMPLES; i++) {
          const pt = pathEl.getPointAtLength((i * total) / (BOUNDARY_SAMPLES - 1));
          boundary.push({ x: pt.x, y: pt.y });
        }
      } else {
        const ptsStr = el.getAttribute('points') ?? '';
        for (const pair of ptsStr
          .trim()
          .split(/[\s,]+/)
          .reduce<string[][]>((acc, v, i) => {
            if (i % 2 === 0) acc.push([v]);
            else acc[acc.length - 1].push(v);
            return acc;
          }, [])) {
          const x = Number(pair[0]);
          const y = Number(pair[1]);
          if (Number.isFinite(x) && Number.isFinite(y)) boundary.push({ x, y });
        }
        if (boundary.length === 0) return fallback;
      }
    } catch {
      return fallback;
    }

    // isPointInFill requiert une géométrie « fillable » ; on retombe au besoin.
    const canFillTest = typeof el.isPointInFill === 'function';
    // Pas fin pour résoudre les secteurs étroits/allongés (ex. 2, 28, 8, 5,
    // 42, 43, 14, 25). Coût acceptable au chargement.
    const step = 4;
    // Marge préférentielle : on privilégie les candidats dont la distance min
    // au contour dépasse le demi-glyphe (~12 unités), pour garder le numéro
    // à l'intérieur même après centrage du texte.
    const MARGIN = 12;
    const candidates: { x: number; y: number }[] = [];
    for (let gy = bbox.y; gy <= bbox.y + bbox.height; gy += step) {
      for (let gx = bbox.x; gx <= bbox.x + bbox.width; gx += step) {
        if (canFillTest) {
          try {
            if (!el.isPointInFill({ x: gx, y: gy })) continue;
          } catch {
            continue;
          }
        }
        candidates.push({ x: gx, y: gy });
      }
    }
    if (candidates.length === 0) return fallback;

    let best = candidates[0];
    let bestDist = -Infinity;
    let bestAboveMargin = false;
    for (const c of candidates) {
      let minDist = Infinity;
      for (const b of boundary) {
        const dx = c.x - b.x;
        const dy = c.y - b.y;
        const d = dx * dx + dy * dy;
        if (d < minDist) minDist = d;
      }
      const above = minDist >= MARGIN * MARGIN;
      if (bestAboveMargin) {
        // Ne garder que les candidats sous marge, et le max parmi eux.
        if (!above || minDist <= bestDist) continue;
      } else if (above) {
        // Premier candidat sous marge : il devient le nouveau best.
        bestAboveMargin = true;
      } else if (minDist <= bestDist) {
        continue;
      }
      bestDist = minDist;
      best = c;
    }
    return best;
  }

  /**
   * Applique la couleur du joueur propriétaire à chaque <text> de label,
   * recalculée à chaque changement de filtre/sélection (en phase avec
   * updateAllPathColors). Le halo noir par défaut est porté par le CSS. Sur
   * sélection, le fond devient opaque : on recolore le chiffre via
   * getContrastColor pour garantir la lisibilité (configuré par
   * MAP_THEME.label.contrastOnSelect) et on inverse le halo — blanc si le
   * texte est gris foncé, gris si le texte est blanc (cf carte.config.ts).
   */
  private updateSectorLabelColors(container: HTMLElement): void {
    const labels = container.querySelectorAll('text.sector-label');
    const selectedNum = this.selectedSector()?.number ?? null;
    const lbl = MAP_THEME.label;
    labels.forEach((label) => {
      const num = parseInt(label.getAttribute('data-sector') ?? '', 10);
      if (Number.isNaN(num)) return;
      const sector = this.sectorsMap().get(num);
      const color = sector ? this.getSectorColor(sector) : MAP_THEME.neutralColor;
      const txt = label as SVGTextElement;
      if (lbl.contrastOnSelect && selectedNum === num) {
        txt.style.fill = this.getContrastColor();
        txt.style.stroke = lbl.selectedStroke;
      } else {
        txt.style.fill = color;
        // Rend la main au halo par défaut porté par la CSS var.
        txt.style.stroke = '';
      }
    });
  }

  private updatePathStyle(path: SVGElement, sectorNumber: number): void {
    const sector = this.sectorsMap().get(sectorNumber);
    const color = sector ? this.getSectorColor(sector) : MAP_THEME.neutralColor;
    const isNeutral = sector?.ownerId == null;
    const selected = this.selectedSector();
    const isSelected = selected?.number === sectorNumber;

    const isDimmed =
      (sector?.ownerId != null && !this.selectedPlayerIds().has(sector.ownerId)) ||
      (sector?.ownerId == null && !this.showNeutral());

    path.style.fill = isNeutral ? 'url(#neutral-stripes)' : color + MAP_THEME.fill.normalAlpha;
    path.style.stroke = color;
    path.style.strokeWidth = String(MAP_THEME.stroke.normal);
    path.style.cursor = 'pointer';
    path.style.transition = 'all 0.2s ease';
    path.style.opacity = isDimmed ? String(MAP_THEME.fill.dimmedOpacity) : '1';
    path.style.filter = 'none';

    if (isSelected) {
      path.style.fill = isNeutral ? MAP_THEME.neutralColor : color;
      path.style.strokeWidth = String(MAP_THEME.stroke.selected);
      const filters: string[] = ['url(#selection-overflow)'];
      if (MAP_THEME.selection.glowRadius > 0) {
        filters.push(`drop-shadow(0 0 ${MAP_THEME.selection.glowRadius}px ${color})`);
      }
      path.style.filter = filters.join(' ');
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
    const color = sector ? this.getSectorColor(sector) : MAP_THEME.neutralColor;
    if (this.selectedSector()?.number !== sectorNumber) {
      path.style.fill = color + MAP_THEME.fill.hoverAlpha;
      path.style.strokeWidth = String(MAP_THEME.stroke.hover);
    }
  }

  private onSectorLeave(sectorNumber: number, path: SVGElement): void {
    this.hoveredSectorNumber.set(null);
    this.updatePathStyle(path, sectorNumber);
  }

  getPlayerColor(playerId: number | null): string {
    if (!playerId) return MAP_THEME.neutralColor;
    return this.playerColorMap().get(playerId) || MAP_THEME.neutralColor;
  }

  getSectorColor(sector: Sector | SectorWithPlayer): string {
    return this.getPlayerColor(sector.ownerId);
  }

  /** Readable text color (white or dark) for a given hex background. */
  getContrastColor(): string {
    return '#1e293b';
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
