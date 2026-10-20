import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { SlicePipe, DecimalPipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { toSignal } from '@angular/core/rxjs-interop';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Player, Sector } from '../../models';
import { sanitizeSvg } from '../../core/svg-sanitize';
import { characterStats } from '../../core/stats';
import { ActiveBoardService } from '../../services/active-board.service';
import { AllianceStateService } from '../../services/alliance-state.service';
import { AuthService } from '../../services/auth.service';
import { PlayerActionsService } from '../../services/player-actions.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { PlayerService } from '../../services/player.service';
import { contrastColor, haloColor, MAP_THEME } from './carte.config';
import { frontBorders } from './carte.helpers';

const SVG_NS = 'http://www.w3.org/2000/svg';

type OverlayKey = 'front' | 'resources';

interface Placement {
  kind: 'unit' | 'vehicle';
  id: number;
  label: string;
}

interface SectorWithPlayer extends Sector {
  playerName?: string | null;
  playerColor?: string;
}

// Same-origin uniquement : `/\evil.example/x` et `//evil.example/x` se résolvent hors origine.
export function isSameOriginAssetUrl(url: string): boolean {
  if (!url.startsWith('/')) return false;
  try {
    return new URL(url, window.location.origin).origin === window.location.origin;
  } catch {
    return false;
  }
}

@Component({
  selector: 'app-carte',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    SlicePipe,
    DecimalPipe,
    RouterLink,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  templateUrl: './carte.component.html',
  styleUrls: ['./carte.component.scss'],
})
export class CarteComponent {
  private readonly sanitizer = inject(DomSanitizer);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly snackBar = inject(MatSnackBar);

  // Les libellés vivent dans le viewBox SVG : sans grossissement, ils deviennent
  // illisibles quand la carte est réduite à la largeur d'un téléphone.
  private readonly narrowViewport = toSignal(
    this.breakpointObserver.observe([Breakpoints.XSmall]).pipe(map((result) => result.matches)),
    { initialValue: false },
  );

  readonly neutralColor = MAP_THEME.neutralColor;
  readonly labelFontPx = computed(() =>
    this.narrowViewport() ? MAP_THEME.label.fontPx * 2 : MAP_THEME.label.fontPx,
  );
  readonly labelWeight = MAP_THEME.label.weight;
  readonly labelStrokeColor = MAP_THEME.label.strokeColor;
  readonly labelStrokeWidth = MAP_THEME.label.strokeWidthPx;
  // Décalage (px CSS) de l'overlay par rapport à l'image de fond, pour rattraper
  // un mismatch de calage entre le SVG et le PNG. Valeur négative tire l'overlay
  // vers le haut (corrige un overlay « trop bas »), positive vers le bas.
  readonly overlayOffsetY = MAP_THEME.overlay.offsetY;
  readonly overlayOffsetX = MAP_THEME.overlay.offsetX;

  private readonly activeBoard = inject(ActiveBoardService);
  private readonly playerDirectory = inject(PlayerDirectoryService);
  private readonly allianceState = inject(AllianceStateService);
  private readonly auth = inject(AuthService);
  private readonly playerActions = inject(PlayerActionsService);
  private readonly playerService = inject(PlayerService);

  readonly isAdmin = this.auth.isAdmin;
  readonly reserveUnits = this.playerService.reserveUnits;
  readonly undeployedVehicles = this.playerService.undeployedVehicles;
  readonly overlays = signal<ReadonlySet<OverlayKey>>(new Set());
  readonly placing = signal<Placement | null>(null);
  readonly resourceLegend = Object.entries(MAP_THEME.resourceColors).map(([name, color]) => ({
    name,
    color,
  }));

  readonly alliedPlayerIds = this.allianceState.alliedPlayerIds;
  readonly characterStats = characterStats;

  readonly loading = computed(() => this.activeBoard.loading() || this.playerDirectory.loading());
  readonly error = computed(() => {
    const e = this.activeBoard.error() || this.playerDirectory.error() || this.svgTextRef.error();
    return e ? 'Impossible de charger la carte. Vérifiez que le serveur est démarré.' : null;
  });

  readonly board = this.activeBoard.board;
  readonly players = this.playerDirectory.players;
  readonly playerColorMap = this.playerDirectory.colorMap;
  readonly myPlayer = computed(() => {
    const username = this.auth.user()?.username;
    return username ? (this.players().find((player) => player.name === username) ?? null) : null;
  });

  private readonly svgOverlayUrl = computed(() => this.board()?.svgOverlayUrl || null);
  private readonly svgTextRef = httpResource.text(() => this.svgOverlayUrl() ?? undefined);

  readonly svgContent = computed<SafeHtml | string | null>(() => {
    const text = this.svgTextRef.value();
    if (!text) return null;
    // Defense-in-depth: on ne bypass que si l'URL est same-origin (chemin relatif en "/").
    // Sinon on renvoie la string brute — Angular sanitize lui-même au binding [innerHTML].
    const url = this.svgOverlayUrl();
    if (!url || !isSameOriginAssetUrl(url)) {
      return text;
    }
    // ponytail: bypass direct — le sanitizer HTML d'Angular jette les balises SVG ;
    // on neutralise donc les éléments/attributs actifs (upload admin) avant injection.
    const sanitized = sanitizeSvg(text);
    return sanitized ? this.sanitizer.bypassSecurityTrustHtml(sanitized) : null;
  });
  // Sans overlay configuré, la carte est utilisable : on n'attend pas un SVG qui n'existe pas.
  readonly svgLoaded = computed(() => this.svgOverlayUrl() === null || this.svgContent() !== null);

  readonly mapImageUrl = computed(() => this.board()?.mapImageUrl || null);

  readonly selectedSector = signal<SectorWithPlayer | null>(null);
  readonly hoveredSectorNumber = signal<number | null>(null);

  private readonly svgContainer = viewChild<ElementRef<HTMLDivElement>>('svgContainer');

  private readonly playerMap = computed(() => {
    const map = new Map<number, Player>();
    this.players().forEach((p) => {
      if (p.id != null) map.set(p.id, p);
    });
    return map;
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

  readonly worldSectorCount = computed(() => Object.values(this.board()?.sectors ?? {}).length);

  readonly neutralSectorsCount = computed(
    () => Object.values(this.board()?.sectors ?? {}).filter((s) => !s.ownerId).length,
  );

  readonly harvesting = signal(false);
  readonly harvestSectors = computed(() => {
    const me = this.myPlayer();
    if (!me?.id) return [];
    return Object.values(this.board()?.sectors ?? {})
      .filter((sector) => sector.ownerId === me.id && sector.resource && sector.number !== null)
      .map((sector) => sector.number as number);
  });

  /** Bordures à dessiner : mes secteurs et les secteurs ennemis (jamais neutres) qu'ils touchent. */
  readonly frontSectors = computed(() =>
    frontBorders(
      Object.values(this.board()?.sectors ?? {}),
      this.myPlayer()?.id ?? null,
      this.alliedPlayerIds(),
    ),
  );

  private readonly eventCleanupFns: (() => void)[] = [];

  constructor() {
    void this.allianceState.loadMe();
    if (!this.isAdmin()) {
      void this.playerService.loadReserveUnits();
      void this.playerService.loadVehicles();
    }
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

    // Repeindre les tracés quand le SVG, la sélection, les alliances ou un calque changent.
    afterRenderEffect(() => {
      if (!this.svgLoaded()) return;
      const container = this.svgContainer()?.nativeElement;
      if (!container) return;
      void this.selectedSector();
      void this.alliedPlayerIds();
      void this.overlays();
      void this.frontSectors();
      this.updateAllPathColors(container);
      this.updateSectorLabelColors(container);
      this.updateOverlays(container);
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
        if (!above || minDist <= bestDist) continue;
      } else if (above) {
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
   * getContrastColor pour garantir la lisibilité (MAP_THEME.label.contrastOnSelect)
   * et on pose le halo opposé via getHaloColor.
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
        txt.style.stroke = this.getHaloColor();
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
    const showResources = this.isOverlay('resources');
    const isNeutral = showResources ? !sector?.resource : sector?.ownerId == null;
    const selected = this.selectedSector();
    const isSelected = selected?.number === sectorNumber;

    path.style.fill = isNeutral ? 'url(#neutral-stripes)' : color + MAP_THEME.fill.normalAlpha;
    path.style.stroke = color;
    path.style.strokeWidth = String(MAP_THEME.stroke.normal);
    path.style.strokeDasharray =
      !showResources && this.isAllyOwner(sector?.ownerId ?? null) ? '7 4' : '';
    path.style.cursor = 'pointer';
    path.style.transition = 'all 0.2s ease';
    path.style.opacity = '1';
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

  /** Calques DOM au-dessus des tracés (sous les labels) : frontières ennemies. */
  private updateOverlays(container: HTMLElement): void {
    const svg = container.querySelector('svg');
    if (!svg) return;
    svg.querySelector('#front-borders-mask')?.remove();
    let group = svg.querySelector<SVGGElement>('#map-overlays');
    if (!group) {
      group = document.createElementNS(SVG_NS, 'g');
      group.setAttribute('id', 'map-overlays');
      group.setAttribute('pointer-events', 'none');
      svg.insertBefore(group, svg.querySelector('#sector-labels'));
    }
    group.replaceChildren();

    const overlays = this.overlays();
    if (!overlays.size) return;

    const paths = new Map<number, SVGElement>();
    svg.querySelectorAll<SVGElement>('path[id^="path"], polygon[id^="path"]').forEach((path) => {
      const number = parseInt((path.getAttribute('id') ?? '').replace('path', ''), 10);
      if (!Number.isNaN(number)) paths.set(number, path);
    });
    const cloneOf = (number: number): SVGElement | null => {
      const source = paths.get(number);
      if (!source) return null;
      const clone = source.cloneNode() as SVGElement;
      clone.removeAttribute('id');
      clone.style.filter = 'none';
      clone.style.opacity = '1';
      clone.style.strokeDasharray = '';
      // Le CSS des tracés force pointer-events: all ; un calque ne doit jamais capter le clic.
      clone.style.pointerEvents = 'none';
      return clone;
    };

    if (overlays.has('front')) {
      const { mine, enemies } = this.frontSectors();
      const defs = svg.querySelector('defs');
      if (defs && mine.length && enemies.length) {
        // Masque élargi (contour blanc) : les polygones ne se touchent pas, un simple clip laissait le trait vide.
        const mask = document.createElementNS(SVG_NS, 'mask');
        mask.setAttribute('id', 'front-borders-mask');
        mask.setAttribute('maskUnits', 'userSpaceOnUse');
        mine.forEach((number) => {
          const source = paths.get(number);
          if (!source) return;
          const area = document.createElementNS(SVG_NS, 'path');
          area.setAttribute('d', source.getAttribute('d') ?? '');
          area.style.fill = '#fff';
          area.style.stroke = '#fff';
          area.style.strokeWidth = String(MAP_THEME.overlays.frontMaskWidth);
          area.style.vectorEffect = 'non-scaling-stroke';
          mask.appendChild(area);
        });
        defs.appendChild(mask);
        enemies.forEach((number) => {
          const clone = cloneOf(number);
          if (!clone) return;
          clone.style.fill = 'none';
          clone.style.stroke = MAP_THEME.overlays.frontStroke;
          clone.style.strokeWidth = String(MAP_THEME.overlays.frontWidth);
          clone.setAttribute('mask', 'url(#front-borders-mask)');
          group.appendChild(clone);
        });
      }
    }
  }

  private onSectorClick(sectorNumber: number): void {
    if (this.placing()) {
      void this.deployReserve(sectorNumber);
      return;
    }
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
    if (this.isOverlay('resources')) {
      return (sector.resource && MAP_THEME.resourceColors[sector.resource]) || MAP_THEME.neutralColor;
    }
    return this.getPlayerColor(sector.ownerId);
  }

  isAllyOwner(playerId: number | null): boolean {
    return playerId != null && this.alliedPlayerIds().has(playerId);
  }

  getContrastColor(): string {
    return contrastColor(this.selectedSectorColor());
  }

  getHaloColor(): string {
    return haloColor(this.selectedSectorColor());
  }

  private selectedSectorColor(): string {
    const sector = this.selectedSector();
    return sector ? this.getSectorColor(sector) : MAP_THEME.neutralColor;
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

  isOverlay(overlay: OverlayKey): boolean {
    return this.overlays().has(overlay);
  }

  toggleOverlay(overlay: OverlayKey): void {
    this.overlays.update((current) => {
      const next = new Set(current);
      if (next.has(overlay)) next.delete(overlay);
      else next.add(overlay);
      return next;
    });
  }

  startPlacement(kind: 'unit' | 'vehicle', id: number | null, label: string): void {
    if (id == null) return;
    this.placing.set({ kind, id, label });
  }

  cancelPlacement(): void {
    this.placing.set(null);
  }

  async deployReserve(sectorNumber: number): Promise<void> {
    const placement = this.placing();
    const board = this.board();
    const me = this.myPlayer();
    if (!placement || !board?.id || !me?.id) return;
    if (this.sectorsMap().get(sectorNumber)?.ownerId !== me.id) {
      this.snackBar.open('Choisissez un secteur que vous possédez.', 'OK', { duration: 3000 });
      return;
    }
    const placed =
      placement.kind === 'unit'
        ? await this.playerService.placeUnit(placement.id, board.id, sectorNumber)
        : await this.playerService.placeVehicle(placement.id, board.id, sectorNumber);
    if (!placed) {
      this.snackBar.open(this.playerService.error() ?? 'Déploiement impossible', 'OK', {
        duration: 4000,
      });
      return;
    }
    this.snackBar.open(`${placement.label} déployé en secteur ${sectorNumber}.`, 'OK', {
      duration: 3000,
    });
    this.placing.set(null);
    this.activeBoard.reload();
  }

  async harvestResources(): Promise<void> {
    if (this.harvesting() || !this.harvestSectors().length) return;
    this.harvesting.set(true);
    try {
      // Le backend refuse un secteur déjà récolté : on lit le journal pour ne renvoyer que le reste.
      await this.playerActions.loadActions();
      const harvested = new Set(
        this.playerActions
          .actions()
          .filter((action) => action.type === 'HARVEST_MONEY' || action.type === 'HARVEST_RESOURCE')
          .map((action) => action.fromSectorNumber)
          .filter((number): number is number => number != null),
      );
      const sectors = this.harvestSectors().filter((number) => !harvested.has(number));
      if (!sectors.length) {
        this.snackBar.open('Tous vos secteurs sont déjà récoltés ce tour.', 'OK', {
          duration: 3500,
        });
        return;
      }
      const ok = await this.playerActions.harvest('HARVEST_RESOURCE', sectors);
      this.snackBar.open(
        ok
          ? `${sectors.length} secteur(s) récolté(s) en ressources.`
          : (this.playerActions.error() ?? 'Récolte impossible'),
        'OK',
        { duration: 4000 },
      );
      if (ok) {
        this.activeBoard.reload();
      }
    } finally {
      this.harvesting.set(false);
    }
  }

  getSectorByNumber(number: number): SectorWithPlayer | undefined {
    return this.sectorsMap().get(number);
  }

  getHoveredSectorName(): string {
    const num = this.hoveredSectorNumber();
    if (num === null) return '';
    const sector = this.sectorsMap().get(num);
    return sector ? `${sector.name} - ${sector.playerName || 'Neutre'}` : '';
  }
}
