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

@Component({
  selector: 'app-sector-map-preview',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatIconModule],
  templateUrl: './sector-map-preview.component.html',
  styleUrls: ['./sector-map-preview.component.scss'],
})
export class SectorMapPreviewComponent {
  readonly sectorNumber = input.required<number>();

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
      const sectorNumber = this.sectorNumber();
      const sectors = this.activeBoard.sectors();
      const colors = this.playerDirectory.colorMap();
      const mapImageUrl = this.activeBoard.board()?.mapImageUrl ?? null;
      if (!text || !host) return;
      untracked(() => this.render(host, text, sectorNumber, sectors, colors, mapImageUrl));
    });
  }

  private render(
    host: HTMLDivElement,
    text: string,
    sectorNumber: number,
    sectors: readonly {
      number: number | null;
      ownerId: number | null;
      neighbors: number[];
    }[],
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

    if (mapImageUrl) {
      const viewBox = svg.viewBox.baseVal;
      const image = document.createElementNS(SVG_NS, 'image');
      image.setAttribute('x', String(viewBox?.x ?? 0));
      image.setAttribute('y', String(viewBox?.y ?? 0));
      image.setAttribute('width', String(viewBox?.width || svg.width.baseVal.value));
      image.setAttribute('height', String(viewBox?.height || svg.height.baseVal.value));
      image.setAttribute('preserveAspectRatio', 'none');
      image.setAttribute('href', mapImageUrl);
      image.style.pointerEvents = 'none';
      svg.insertBefore(image, svg.firstChild);
    }

    const byNumber = new Map<
      number,
      { number: number | null; ownerId: number | null; neighbors: number[] }
    >();
    for (const sector of sectors) {
      if (sector.number !== null) byNumber.set(sector.number, sector);
    }

    const paths = svg.querySelectorAll<SVGGraphicsElement>('path[id^="path"], polygon[id^="path"]');

    paths.forEach((path) => {
      const id = path.getAttribute('id');
      const number = id ? parseInt(id.replace('path', ''), 10) : Number.NaN;
      if (Number.isNaN(number)) return;

      const sector = byNumber.get(number);
      const color =
        sector?.ownerId != null ? (colors.get(sector.ownerId) ?? NEUTRAL_COLOR) : NEUTRAL_COLOR;
      const isSelected = number === sectorNumber;

      path.style.pointerEvents = 'none';
      path.style.fill = color;
      path.style.fillOpacity = isSelected ? '0.82' : '0.5';
      path.style.stroke = isSelected ? LABEL_COLOR : color;
      path.style.strokeOpacity = isSelected ? '1' : '0.8';
      path.style.strokeWidth = isSelected ? '3' : '1';
      path.style.vectorEffect = 'non-scaling-stroke';
      path.style.filter = isSelected
        ? `drop-shadow(0 0 4px ${color}) drop-shadow(0 0 10px ${color})`
        : 'none';
    });

    const selected = svg.querySelector<SVGGraphicsElement>(`#path${sectorNumber}`);
    if (!selected) {
      this.hasGeometry.set(false);
      return;
    }

    const box = selected.getBBox();
    if (!box.width && !box.height) {
      this.hasGeometry.set(false);
      return;
    }

    const pad = Math.max(box.width, box.height) * 0.3;
    const crop = {
      x: box.x - pad,
      y: box.y - pad,
      width: box.width + pad * 2,
      height: box.height + pad * 2,
    };
    svg.setAttribute('viewBox', `${crop.x} ${crop.y} ${crop.width} ${crop.height}`);

    const selectedFont = Math.max(16, Math.min(box.width, box.height) * 0.25);
    this.addLabel(
      svg,
      sectorNumber,
      box.x + box.width / 2,
      box.y + box.height / 2,
      selectedFont,
      '800',
    );

    const selectedSector = byNumber.get(sectorNumber);
    const neighborFont = Math.max(12, Math.min(crop.width, crop.height) * 0.06);
    const marginX = crop.width * 0.07;
    const marginY = crop.height * 0.07;
    for (const neighborNumber of selectedSector?.neighbors ?? []) {
      const neighbor = svg.querySelector<SVGGraphicsElement>(`#path${neighborNumber}`);
      if (!neighbor) continue;
      const neighborBox = neighbor.getBBox();
      if (!neighborBox.width && !neighborBox.height) continue;
      const centerX = neighborBox.x + neighborBox.width / 2;
      const centerY = neighborBox.y + neighborBox.height / 2;
      const x = Math.min(crop.x + crop.width - marginX, Math.max(crop.x + marginX, centerX));
      const y = Math.min(crop.y + crop.height - marginY, Math.max(crop.y + marginY, centerY));
      this.addLabel(svg, neighborNumber, x, y, neighborFont, '600');
    }

    this.hasGeometry.set(true);
  }

  private addLabel(
    svg: SVGSVGElement,
    value: number,
    x: number,
    y: number,
    fontSize: number,
    fontWeight: string,
  ): void {
    const label = document.createElementNS(SVG_NS, 'text');
    label.setAttribute('x', String(x));
    label.setAttribute('y', String(y));
    label.setAttribute('text-anchor', 'middle');
    label.setAttribute('dominant-baseline', 'central');
    label.setAttribute('font-size', String(fontSize));
    label.setAttribute('font-weight', fontWeight);
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
