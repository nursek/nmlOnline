import { Component, inject, OnInit, signal, computed, ElementRef, ViewChild, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { HttpClient } from '@angular/common/http';
import { ApiService } from '../../services/api.service';
import { Board, Sector, Player } from '../../models';
import { forkJoin } from 'rxjs';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

interface SectorWithPlayer extends Sector {
  playerName?: string;
  playerColor?: string;
}

@Component({
  selector: 'app-carte',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  templateUrl: './carte.component.html',
  styleUrls: ['./carte.component.scss']
})
export class CarteComponent implements OnInit, AfterViewInit {
  private readonly apiService = inject(ApiService);
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);

  @ViewChild('svgContainer') svgContainer!: ElementRef<HTMLDivElement>;

  // State avec signals
  loading = signal(true);
  error = signal<string | null>(null);
  board = signal<Board | null>(null);
  players = signal<Player[]>([]);
  selectedSector = signal<SectorWithPlayer | null>(null);
  selectedPlayer = signal<Player | null>(null);
  hoveredSectorNumber = signal<number | null>(null);

  // SVG content
  svgContent = signal<SafeHtml | null>(null);
  svgLoaded = signal(false);

  // Couleurs des joueurs par ID
  private readonly playerColorMap = new Map<number, string>();
  private readonly colors = [
    '#6366f1', '#ef4444', '#10b981', '#f59e0b',
    '#8b5cf6', '#ec4899', '#f97316', '#06b6d4',
    '#84cc16', '#14b8a6', '#f43f5e', '#a855f7',
  ];

  // Computed values
  allSectors = computed(() => {
    const b = this.board();
    if (!b) return [];

    return Object.values(b.sectors).map(sector => ({
      ...sector,
      playerName: this.getPlayerName(sector.ownerId),
      playerColor: this.getPlayerColor(sector.ownerId),
    })) as SectorWithPlayer[];
  });

  // Map pour accès rapide aux secteurs par numéro
  sectorsMap = computed(() => {
    const map = new Map<number, SectorWithPlayer>();
    this.allSectors().forEach(s => {
      if (s.number !== null) {
        map.set(s.number, s);
      }
    });
    return map;
  });

  neutralSectorsCount = computed(() =>
    this.allSectors().filter(s => !s.ownerId).length
  );

  conqueredSectorsCount = computed(() =>
    this.allSectors().filter(s => s.ownerId).length
  );

  // URLs de la carte
  mapImageUrl = computed(() => this.board()?.mapImageUrl || null);
  svgOverlayUrl = computed(() => this.board()?.svgOverlayUrl || null);


  ngOnInit(): void {
    this.loadData();
  }

  ngAfterViewInit(): void {
    // Le SVG sera initialisé après le chargement des données
  }

  loadData(): void {
    forkJoin({
      boards: this.apiService.getAllBoards(),
      players: this.apiService.getAllPlayers()
    }).subscribe({
      next: ({ boards, players }) => {
        // Prendre la première board disponible
        const board = boards.length > 0 ? boards[0] : null;

        // Assigner les couleurs aux joueurs
        players.forEach((player, index) => {
          if (player.id) {
            this.playerColorMap.set(player.id, this.colors[index % this.colors.length]);
          }
        });

        this.board.set(board);
        this.players.set(players);
        this.loading.set(false);

        // Charger le SVG overlay si disponible
        if (board?.svgOverlayUrl) {
          this.loadSvgOverlay(board.svgOverlayUrl);
        }
      },
      error: (err) => {
        console.error('Erreur chargement carte:', err);
        this.error.set('Impossible de charger la carte. Vérifiez que le serveur est démarré.');
        this.loading.set(false);
      }
    });
  }

  private loadSvgOverlay(url: string): void {
    this.http.get(url, { responseType: 'text' }).subscribe({
      next: (svgText) => {
        // Injecter le SVG et configurer les handlers
        this.svgContent.set(this.sanitizer.bypassSecurityTrustHtml(svgText));
        this.svgLoaded.set(true);

        // Attendre que le DOM soit mis à jour puis attacher les événements
        setTimeout(() => this.initializeSvgInteractions(), 0);
      },
      error: (err) => {
        console.warn('Impossible de charger le SVG overlay, fallback vers grille:', err);
        this.svgLoaded.set(false);
      }
    });
  }

  private initializeSvgInteractions(): void {
    if (!this.svgContainer) return;

    const container = this.svgContainer.nativeElement;
    const paths = container.querySelectorAll('path[id^="path"], polygon[id^="path"]');

    paths.forEach((path) => {
      const id = path.getAttribute('id');
      if (!id) return;

      const sectorNumber = parseInt(id.replace('path', ''), 10);
      if (isNaN(sectorNumber)) return;

      // Configurer les styles de base
      this.updatePathStyle(path as SVGElement, sectorNumber);

      // Event listeners
      path.addEventListener('click', () => this.onSectorClick(sectorNumber));
      path.addEventListener('mouseenter', () => this.onSectorHover(sectorNumber, path as SVGElement));
      path.addEventListener('mouseleave', () => this.onSectorLeave(sectorNumber, path as SVGElement));
    });

    // Appliquer les couleurs initiales
    this.updateAllPathColors();
  }

  private updatePathStyle(path: SVGElement, sectorNumber: number): void {
    const sector = this.sectorsMap().get(sectorNumber);
    const color = sector ? this.getSectorColor(sector) : '#94a3b8';
    const isSelected = this.selectedSector()?.number === sectorNumber;
    const isHighlighted = this.isNeighbor(sectorNumber);
    const isDimmed = this.selectedPlayer() && sector?.ownerId !== this.selectedPlayer()?.id;

    // Styles de base
    path.style.fill = 'transparent';
    path.style.stroke = color;
    path.style.strokeWidth = isSelected ? '4' : '2';
    path.style.cursor = 'pointer';
    path.style.transition = 'all 0.2s ease';
    path.style.opacity = isDimmed ? '0.3' : '1';

    if (isSelected) {
      path.style.fill = color + '40'; // 25% opacity
      path.style.filter = 'drop-shadow(0 0 8px ' + color + ')';
    } else if (isHighlighted) {
      path.style.fill = '#f59e0b30';
      path.style.stroke = '#f59e0b';
    } else {
      path.style.filter = 'none';
    }
  }

  private updateAllPathColors(): void {
    if (!this.svgContainer) return;

    const container = this.svgContainer.nativeElement;
    const paths = container.querySelectorAll('path[id^="path"], polygon[id^="path"]');

    paths.forEach((path) => {
      const id = path.getAttribute('id');
      if (!id) return;

      const sectorNumber = parseInt(id.replace('path', ''), 10);
      if (!isNaN(sectorNumber)) {
        this.updatePathStyle(path as SVGElement, sectorNumber);
      }
    });
  }

  private onSectorClick(sectorNumber: number): void {
    const sector = this.sectorsMap().get(sectorNumber);
    if (sector) {
      this.selectSector(sector);
      this.updateAllPathColors();
    }
  }

  private onSectorHover(sectorNumber: number, path: SVGElement): void {
    this.hoveredSectorNumber.set(sectorNumber);
    const sector = this.sectorsMap().get(sectorNumber);
    const color = sector ? this.getSectorColor(sector) : '#94a3b8';

    if (this.selectedSector()?.number !== sectorNumber) {
      path.style.fill = color + '30'; // 20% opacity on hover
      path.style.strokeWidth = '3';
    }
  }

  private onSectorLeave(sectorNumber: number, path: SVGElement): void {
    this.hoveredSectorNumber.set(null);
    this.updatePathStyle(path, sectorNumber);
  }

  getPlayerColor(playerId: number | null): string {
    if (!playerId) return '#94a3b8';  // Gris pour neutre
    return this.playerColorMap.get(playerId) || '#94a3b8';
  }

  getPlayerName(playerId: number | null): string | undefined {
    if (!playerId) return undefined;
    return this.players().find(p => p.id === playerId)?.name;
  }

  getSectorColor(sector: Sector | SectorWithPlayer): string {
    return this.getPlayerColor(sector.ownerId);
  }

  selectSector(sector: SectorWithPlayer): void {
    this.selectedSector.set(sector);
    this.updateAllPathColors();
  }

  togglePlayerFilter(player: Player): void {
    if (this.selectedPlayer()?.id === player.id) {
      this.selectedPlayer.set(null);
    } else {
      this.selectedPlayer.set(player);
    }
    this.updateAllPathColors();
  }

  clearFilter(): void {
    this.selectedPlayer.set(null);
    this.updateAllPathColors();
  }

  isNeighbor(sectorNumber: number): boolean {
    const selected = this.selectedSector();
    if (!selected) return false;
    return selected.neighbors?.includes(sectorNumber) || false;
  }

  getSectorByNumber(number: number): SectorWithPlayer | undefined {
    return this.sectorsMap().get(number);
  }

  getInitials(name: string): string {
    return name.split(' ')
      .map(word => word[0])
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
