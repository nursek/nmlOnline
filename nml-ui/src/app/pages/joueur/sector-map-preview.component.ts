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
import { ActiveBoardService } from '../../services/active-board.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { sanitizeSvg } from '../../core/svg-sanitize';

const SVG_NS = 'http://www.w3.org/2000/svg';
const NEUTRAL_COLOR = '#6e7885';
const LABEL_COLOR = '#e6decb';
const LABEL_HALO = '#060809';
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

  private readonly activeBoard = inject(ActiveBoardService);
  private readonly playerDirectory = inject(PlayerDirectoryService);
  private readonly mapHost = viewChild<ElementRef<HTMLDivElement>>('map');
  private readonly svgUrl = computed(() => this.activeBoard.board()?.svgOverlayUrl ?? null);
  private readonly svgText = httpResource.text(() => this.svgUrl() ?? undefined);

  readonly hasGeometry = signal(false);

  constructor() {
    effect(() => {
      const text = this.svgText.hasValue() ? this.svgText.value() : null;
      const host = this.mapHost()?.nativeElement;
      const startSector = this.startSector();
      const route = this.route();
      const sectors = this.activeBoard.sectors();
      const colors = this.playerDirectory.colorMap();
      const mapImageUrl = this.activeBoard.board()?.mapImageUrl ?? null;
      if (!text || !host) return;
      untracked(() => this.render(host, text, startSector, route, sectors, colors, mapImageUrl));
    });
  }

  private render(
    host: HTMLDivElement,
    text: string,
    startSector: number,
    route: readonly number[],
    sectors: readonly { number: number | null; ownerId: number | null }[],
    colors: ReadonlyMap<number, string>,
    mapImageUrl: string | null,
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

    const byNumber = new Map<number, { number: number | null; ownerId: number | null }>();
    for (const sector of sectors) {
      if (sector.number !== null) byNumber.set(sector.number, sector);
    }

    const targetSector = route.length > 1 ? route[route.length - 1] : null;
    const transitSectors = new Set(route.length > 2 ? route.slice(1, -1) : []);
    const roleColor: Record<RouteRole, string> = {
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
      const color = role ? roleColor[role] : ownerColor;

      path.style.pointerEvents = 'none';
      path.style.fill = color;
      path.style.fillOpacity = role ? '0.9' : '0.5';
      path.style.stroke = role ? LABEL_COLOR : color;
      path.style.strokeOpacity = role ? '1' : '0.8';
      path.style.strokeWidth = role ? '6' : '1';
      path.style.vectorEffect = 'non-scaling-stroke';
      path.style.filter = role
        ? `drop-shadow(0 0 4px ${color}) drop-shadow(0 0 10px ${color})`
        : 'none';
    });

    if (!paths.length) {
      this.hasGeometry.set(false);
      return;
    }

    const labelFont = Math.max(10, Math.min(mapWidth, mapHeight) * 0.022);
    const marginX = Math.max(mapWidth * 0.01, labelFont * 1.2);
    const marginY = Math.max(mapHeight * 0.01, labelFont * 1.2);
    const candidates: {
      number: number;
      x: number;
      y: number;
      area: number;
      priority: number;
    }[] = [];
    const seen = new Set<number>();
    paths.forEach((path) => {
      const id = path.getAttribute('id');
      const number = id ? parseInt(id.replace('path', ''), 10) : Number.NaN;
      if (Number.isNaN(number) || seen.has(number)) return;
      const box = path.getBBox();
      if (!box.width && !box.height) return;

      const priority =
        number === startSector
          ? 3
          : targetSector === number
            ? 2
            : transitSectors.has(number)
              ? 1
              : 0;
      candidates.push({
        number,
        x: Math.min(mapX + mapWidth - marginX, Math.max(mapX + marginX, box.x + box.width / 2)),
        y: Math.min(mapY + mapHeight - marginY, Math.max(mapY + marginY, box.y + box.height / 2)),
        area: box.width * box.height,
        priority,
      });
      seen.add(number);
    });

    candidates.sort((a, b) => b.priority - a.priority || b.area - a.area);
    const placed: { x: number; y: number }[] = [];
    const minDistance = labelFont * 1.35;
    for (const candidate of candidates) {
      const offsets = [0, minDistance, -minDistance, minDistance * 2, -minDistance * 2];
      let chosen: { x: number; y: number } | null = null;
      for (const dx of offsets) {
        for (const dy of offsets) {
          const x = candidate.x + dx;
          const y = candidate.y + dy;
          if (
            x < mapX + marginX ||
            x > mapX + mapWidth - marginX ||
            y < mapY + marginY ||
            y > mapY + mapHeight - marginY
          ) {
            continue;
          }
          if (placed.every((point) => Math.hypot(point.x - x, point.y - y) >= minDistance)) {
            chosen = { x, y };
            break;
          }
        }
        if (chosen) break;
      }
      const label = chosen ?? { x: candidate.x, y: candidate.y };
      this.addLabel(svg, candidate.number, label.x, label.y, labelFont);
      placed.push(label);
    }

    this.hasGeometry.set(true);
  }

  private addLabel(
    svg: SVGSVGElement,
    value: number,
    x: number,
    y: number,
    fontSize: number,
  ): void {
    const label = document.createElementNS(SVG_NS, 'text');
    label.setAttribute('x', String(x));
    label.setAttribute('y', String(y));
    label.setAttribute('text-anchor', 'middle');
    label.setAttribute('dominant-baseline', 'central');
    label.setAttribute('font-size', String(fontSize));
    label.setAttribute('font-weight', '700');
    label.setAttribute('font-family', 'system-ui, sans-serif');
    label.setAttribute('fill', LABEL_COLOR);
    label.setAttribute('stroke', LABEL_HALO);
    label.setAttribute('stroke-width', String(Math.max(2, fontSize * 0.16)));
    label.setAttribute('paint-order', 'stroke');
    label.setAttribute('pointer-events', 'none');
    label.textContent = String(value);
    svg.appendChild(label);
  }
}
