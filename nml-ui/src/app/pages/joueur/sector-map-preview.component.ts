import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  effect,
  inject,
  input,
  signal,
  untracked,
  viewChild,
} from '@angular/core';
import { httpResource } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import type { Sector } from '../../models';
import { ActiveBoardService } from '../../services/active-board.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { sanitizeSvg } from '../../core/svg-sanitize';
import { contrastColor, haloColor, MAP_THEME } from '../carte/carte.config';
import { frontBorders, resourceIconSrc } from '../carte/carte.helpers';
import { computeVisualCenter } from '../carte/sector-label.helpers';

const SVG_NS = 'http://www.w3.org/2000/svg';
const NEUTRAL_COLOR = '#6e7885';
const LABEL_COLOR = '#e6decb';
const START_COLOR = '#e8bb45';
const TARGET_COLOR = '#ff2d9d';
const TRANSIT_COLOR = '#4fc3c9';

type RouteRole = 'start' | 'target' | 'transit';

@Component({
  selector: 'app-sector-map-preview',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  templateUrl: './sector-map-preview.component.html',
  styleUrls: ['./sector-map-preview.component.scss'],
})
export class SectorMapPreviewComponent {
  readonly startSector = input.required<number>();
  readonly route = input<readonly number[]>([]);
  readonly playerId = input<number | null>(null);
  readonly allyPlayerIds = input<ReadonlySet<number>>(new Set());
  readonly showFront = input(false);
  readonly showResources = input(false);

  private readonly activeBoard = inject(ActiveBoardService);
  private readonly playerDirectory = inject(PlayerDirectoryService);
  private readonly mapHost = viewChild<ElementRef<HTMLDivElement>>('map');
  private readonly svgUrl = computed(() => this.activeBoard.board()?.svgOverlayUrl ?? null);
  private readonly svgText = httpResource.text(() => this.svgUrl() ?? undefined);

  readonly hasGeometry = signal(false);
  private labelCentersKey = '';
  private readonly labelCenters = new Map<number, { x: number; y: number }>();
  private maskSequence = 0;

  constructor() {
    effect(() => {
      const text = this.svgText.hasValue() ? this.svgText.value() : null;
      const host = this.mapHost()?.nativeElement;
      const startSector = this.startSector();
      const route = this.route();
      const playerId = this.playerId();
      const allyPlayerIds = this.allyPlayerIds();
      const showFront = this.showFront();
      const showResources = this.showResources();
      const sectors = this.activeBoard.sectors();
      const colors = this.playerDirectory.colorMap();
      const mapImageUrl = this.activeBoard.board()?.mapImageUrl ?? null;
      if (!text || !host) return;
      untracked(() =>
        this.render(
          host,
          text,
          startSector,
          route,
          sectors,
          colors,
          mapImageUrl,
          playerId,
          allyPlayerIds,
          showFront,
          showResources,
        ),
      );
    });
  }

  private render(
    host: HTMLDivElement,
    text: string,
    startSector: number,
    route: readonly number[],
    sectors: readonly Sector[],
    colors: ReadonlyMap<number, string>,
    mapImageUrl: string | null,
    playerId: number | null,
    allyPlayerIds: ReadonlySet<number>,
    showFront: boolean,
    showResources: boolean,
  ): void {
    const sanitized = sanitizeSvg(text);
    if (!sanitized) {
      this.hasGeometry.set(false);
      return;
    }

    host.innerHTML = sanitized;
    const svg = host.querySelector<SVGSVGElement>('svg');
    if (!svg) {
      this.hasGeometry.set(false);
      return;
    }

    svg.setAttribute('width', '100%');
    svg.setAttribute('height', '100%');
    svg.setAttribute('preserveAspectRatio', 'xMidYMid meet');

    const viewBox = svg.viewBox.baseVal;
    const mapX = viewBox?.x ?? 0;
    const mapY = viewBox?.y ?? 0;
    const mapWidth = viewBox?.width || svg.width.baseVal.value;
    const mapHeight = viewBox?.height || svg.height.baseVal.value;
    if (mapWidth > 0 && mapHeight > 0) {
      svg.setAttribute('viewBox', `${mapX} ${mapY} ${mapWidth} ${mapHeight}`);
    }
    this.ensureNeutralPattern(svg);

    if (mapImageUrl) {
      const image = document.createElementNS(SVG_NS, 'image');
      image.setAttribute('x', String(mapX));
      image.setAttribute('y', String(mapY));
      image.setAttribute('width', String(mapWidth));
      image.setAttribute('height', String(mapHeight));
      image.setAttribute('preserveAspectRatio', 'none');
      image.setAttribute('href', mapImageUrl);
      image.style.pointerEvents = 'none';
      svg.insertBefore(image, svg.firstChild);
    }

    const byNumber = new Map<number, Sector>();
    for (const sector of sectors) {
      if (sector.number !== null) byNumber.set(sector.number, sector);
    }

    const targetSector = route.length > 1 ? route[route.length - 1] : null;
    const transitSectors = new Set(route.length > 2 ? route.slice(1, -1) : []);
    const roleColors: Record<RouteRole, string> = {
      start: START_COLOR,
      target: TARGET_COLOR,
      transit: TRANSIT_COLOR,
    };

    const paths = svg.querySelectorAll<SVGGraphicsElement>('path[id^="path"], polygon[id^="path"]');
    paths.forEach((path) => {
      const id = path.getAttribute('id');
      const number = id ? parseInt(id.replace('path', ''), 10) : Number.NaN;
      if (Number.isNaN(number)) return;

      const sector = byNumber.get(number);
      const ownerColor =
        sector?.ownerId != null ? (colors.get(sector.ownerId) ?? NEUTRAL_COLOR) : NEUTRAL_COLOR;
      const role: RouteRole | null =
        number === startSector
          ? 'start'
          : targetSector === number
            ? 'target'
            : transitSectors.has(number)
              ? 'transit'
              : null;
      const color = role ? roleColors[role] : ownerColor;
      const isNeutral = sector?.ownerId == null;
      const isAlly = sector?.ownerId != null && allyPlayerIds.has(sector.ownerId);

      path.style.pointerEvents = 'none';
      path.style.fill = role
        ? color
        : showResources
          ? MAP_THEME.resourceFill + MAP_THEME.fill.normalAlpha
          : isNeutral
            ? 'url(#neutral-stripes)'
            : color + MAP_THEME.fill.normalAlpha;
      path.style.fillOpacity = role ? '0.9' : '1';
      path.style.stroke = role ? LABEL_COLOR : showResources ? 'none' : color;
      path.style.strokeOpacity = role ? '1' : '0.8';
      path.style.strokeWidth = role ? '6' : showResources ? '0' : '1';
      path.style.strokeDasharray = !showResources && !role && isAlly ? '7 4' : '';
      path.style.vectorEffect = 'non-scaling-stroke';
      path.style.filter = role
        ? `drop-shadow(0 0 4px ${color}) drop-shadow(0 0 10px ${color})`
        : 'none';
    });

    if (!paths.length) {
      this.hasGeometry.set(false);
      return;
    }

    if (showFront) {
      this.renderFrontOverlay(svg, paths, sectors, playerId, allyPlayerIds);
    }

    const cacheKey = this.svgUrl() ?? '';
    if (cacheKey !== this.labelCentersKey) {
      this.labelCenters.clear();
      this.labelCentersKey = cacheKey;
    }

    const labelFont = MAP_THEME.label.fontPx;
    paths.forEach((path) => {
      const id = path.getAttribute('id');
      const number = id ? parseInt(id.replace('path', ''), 10) : Number.NaN;
      if (Number.isNaN(number)) return;

      let center = this.labelCenters.get(number);
      if (!center) {
        center = computeVisualCenter(path as SVGGeometryElement);
        this.labelCenters.set(number, center);
      }

      const sector = byNumber.get(number);
      const ownerColor =
        sector?.ownerId != null ? (colors.get(sector.ownerId) ?? NEUTRAL_COLOR) : NEUTRAL_COLOR;
      const role: RouteRole | null =
        number === startSector
          ? 'start'
          : targetSector === number
            ? 'target'
            : transitSectors.has(number)
              ? 'transit'
              : null;
      const roleColor = role ? roleColors[role] : null;
      if (showResources) {
        if (sector?.resource) {
          this.addResourceIcon(svg, sector.resource, center.x, center.y);
        }
      } else {
        this.addLabel(
          svg,
          number,
          center.x,
          center.y,
          labelFont,
          roleColor ? contrastColor(roleColor) : ownerColor,
          roleColor ? haloColor(roleColor) : MAP_THEME.label.strokeColor,
        );
      }
    });

    this.hasGeometry.set(true);
  }

  private ensureNeutralPattern(svg: SVGSVGElement): void {
    let defs = svg.querySelector<SVGDefsElement>('defs');
    if (!defs) {
      defs = document.createElementNS(SVG_NS, 'defs');
      svg.insertBefore(defs, svg.firstChild);
    }
    if (svg.querySelector('#neutral-stripes')) return;

    const np = MAP_THEME.neutralPattern;
    const pattern = document.createElementNS(SVG_NS, 'pattern');
    pattern.setAttribute('id', 'neutral-stripes');
    pattern.setAttribute('patternUnits', 'userSpaceOnUse');
    pattern.setAttribute('width', String(np.width));
    pattern.setAttribute('height', String(np.height));
    pattern.setAttribute('patternTransform', `rotate(${np.rotateDeg})`);

    const background = document.createElementNS(SVG_NS, 'rect');
    background.setAttribute('width', String(np.width));
    background.setAttribute('height', String(np.height));
    background.setAttribute('fill', np.background);

    const stripe = document.createElementNS(SVG_NS, 'rect');
    stripe.setAttribute('width', String(np.stripeWidth));
    stripe.setAttribute('height', String(np.height));
    stripe.setAttribute('fill', MAP_THEME.neutralColor);

    pattern.appendChild(background);
    pattern.appendChild(stripe);
    defs.appendChild(pattern);
  }

  private renderFrontOverlay(
    svg: SVGSVGElement,
    paths: NodeListOf<SVGGraphicsElement>,
    sectors: readonly Sector[],
    playerId: number | null,
    allyPlayerIds: ReadonlySet<number>,
  ): void {
    const { mine, enemies } = frontBorders(sectors, playerId, allyPlayerIds);
    const defs = svg.querySelector('defs');
    if (!mine.length || !enemies.length || !defs) return;

    const byNumber = new Map<number, SVGGraphicsElement>();
    paths.forEach((path) => {
      const id = path.getAttribute('id');
      const number = id ? parseInt(id.replace('path', ''), 10) : Number.NaN;
      if (!Number.isNaN(number)) byNumber.set(number, path);
    });

    const maskId = `preview-front-mask-${++this.maskSequence}`;
    const mask = document.createElementNS(SVG_NS, 'mask');
    mask.setAttribute('id', maskId);
    mask.setAttribute('maskUnits', 'userSpaceOnUse');
    for (const number of mine) {
      const source = byNumber.get(number);
      if (!source) continue;
      const area = this.clonePath(source);
      area.style.fill = '#fff';
      area.style.stroke = '#fff';
      area.style.strokeWidth = String(MAP_THEME.overlays.frontMaskWidth);
      area.style.vectorEffect = 'non-scaling-stroke';
      mask.appendChild(area);
    }
    defs.appendChild(mask);

    const group = document.createElementNS(SVG_NS, 'g');
    group.setAttribute('pointer-events', 'none');
    for (const number of enemies) {
      const source = byNumber.get(number);
      if (!source) continue;
      const clone = this.clonePath(source);
      clone.style.fill = 'none';
      clone.style.stroke = MAP_THEME.overlays.frontStroke;
      clone.style.strokeWidth = String(MAP_THEME.overlays.frontWidth);
      clone.setAttribute('mask', `url(#${maskId})`);
      group.appendChild(clone);
    }
    svg.appendChild(group);
  }

  private clonePath(path: SVGGraphicsElement): SVGElement {
    const clone = path.cloneNode(true) as SVGElement;
    clone.removeAttribute('id');
    clone.style.filter = 'none';
    clone.style.opacity = '1';
    clone.style.strokeDasharray = '';
    clone.style.pointerEvents = 'none';
    return clone;
  }

  private addResourceIcon(svg: SVGSVGElement, resource: string, x: number, y: number): void {
    const size = MAP_THEME.resourceIcon.size;
    const icon = document.createElementNS(SVG_NS, 'image');
    icon.setAttribute('href', resourceIconSrc(resource));
    icon.setAttribute('x', String(x - size / 2));
    icon.setAttribute('y', String(y - size / 2));
    icon.setAttribute('width', String(size));
    icon.setAttribute('height', String(size));
    icon.setAttribute('pointer-events', 'none');
    svg.appendChild(icon);
  }

  private addLabel(
    svg: SVGSVGElement,
    value: number,
    x: number,
    y: number,
    fontSize: number,
    fill: string,
    stroke: string,
  ): void {
    const label = document.createElementNS(SVG_NS, 'text');
    label.setAttribute('x', String(x));
    label.setAttribute('y', String(y));
    label.setAttribute('text-anchor', 'middle');
    label.setAttribute('dominant-baseline', 'central');
    label.setAttribute('font-size', String(fontSize));
    label.setAttribute('font-weight', String(MAP_THEME.label.weight));
    label.setAttribute('font-family', 'system-ui, sans-serif');
    label.setAttribute('fill', fill);
    label.setAttribute('stroke', stroke);
    label.setAttribute('stroke-width', String(MAP_THEME.label.strokeWidthPx));
    label.setAttribute('stroke-linejoin', 'round');
    label.setAttribute('paint-order', 'stroke');
    label.setAttribute('pointer-events', 'none');
    label.setAttribute('data-sector', String(value));
    label.textContent = String(value);
    svg.appendChild(label);
  }
}
