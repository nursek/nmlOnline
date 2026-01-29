import { Component, inject, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ApiService } from '../../services/api.service';
import { Board, Sector, Player } from '../../models';
import { forkJoin } from 'rxjs';

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
  template: `
    @if (loading()) {
      <div class="loading-container">
        <mat-spinner diameter="60"></mat-spinner>
        <p>Chargement de la carte...</p>
      </div>
    } @else if (error()) {
      <div class="container">
        <div class="error-alert">
          <mat-icon>error</mat-icon>
          {{ error() }}
        </div>
      </div>
    } @else {
      <div class="container fade-in">
        <!-- Header -->
        <div class="page-header">
          <div class="avatar">
            <mat-icon>map</mat-icon>
          </div>
          <div>
            <h1 class="title">Carte du Monde</h1>
            <p class="subtitle">Cliquez sur un secteur pour voir les détails</p>
          </div>
        </div>

        <div class="map-layout">
          <!-- Légende des joueurs -->
          <mat-card class="legend-card">
            <mat-card-content>
              <div class="section-header">
                <mat-icon color="primary">people</mat-icon>
                <h2>Joueurs</h2>
              </div>
              @for (player of players(); track player.id) {
                <div class="legend-item"
                     [class.active]="!selectedPlayer() || selectedPlayer()?.id === player.id"
                     (click)="togglePlayerFilter(player)">
                  <div class="legend-color" [style.background-color]="getPlayerColor(player.id)"></div>
                  <div class="legend-info">
                    <span class="legend-name">{{ player.name }}</span>
                    <span class="legend-count">{{ player.sectors.length }} secteur(s)</span>
                  </div>
                </div>
              }
              <div class="legend-item neutral">
                <div class="legend-color" style="background-color: #94a3b8"></div>
                <div class="legend-info">
                  <span class="legend-name">Neutre</span>
                  <span class="legend-count">{{ neutralSectorsCount() }} secteur(s)</span>
                </div>
              </div>
              @if (selectedPlayer()) {
                <button mat-stroked-button color="primary" class="clear-filter" (click)="clearFilter()">
                  <mat-icon>clear</mat-icon>
                  Effacer le filtre
                </button>
              }
            </mat-card-content>
          </mat-card>

          <!-- Carte interactive -->
          <div class="map-container">
            <div class="sector-grid">
              @for (sector of filteredSectors(); track sector.number) {
                <div class="sector-wrapper"
                     [style.grid-row]="(sector.y ?? 0) + 1"
                     [style.grid-column]="(sector.x ?? 0) + 1">
                  <div class="sector-tile"
                       [class.selected]="selectedSector()?.number === sector.number"
                       [class.highlighted]="isNeighbor(sector.number!)"
                       [class.dimmed]="selectedPlayer() && sector.ownerId !== selectedPlayer()?.id"
                       [style.--sector-color]="getSectorColor(sector)"
                       (click)="selectSector(sector)"
                       [matTooltip]="sector.name + ' - ' + (sector.playerName || 'Neutre')">
                    <div class="sector-content">
                      <span class="sector-number">#{{ sector.number }}</span>
                      <span class="sector-name">{{ sector.name }}</span>
                      @if (sector.resource) {
                        <span class="sector-resource">{{ sector.resource }}</span>
                      }
                      @if (sector.playerName) {
                        <span class="owner-badge" [style.background-color]="getSectorColor(sector)">
                          {{ getInitials(sector.playerName) }}
                        </span>
                      }
                    </div>
                  </div>
                </div>
              }
            </div>
          </div>

          <!-- Détails du secteur sélectionné -->
          @if (selectedSector()) {
            <mat-card class="details-card">
              <mat-card-content>
                <div class="sector-header" [style.border-color]="getSectorColor(selectedSector()!)">
                  <div class="sector-color" [style.background-color]="getSectorColor(selectedSector()!)"></div>
                  <div>
                    <h2>{{ selectedSector()!.name }}</h2>
                    <span class="sector-id">Secteur #{{ selectedSector()!.number }}</span>
                  </div>
                </div>

                <mat-divider></mat-divider>

                <div class="detail-section">
                  <h3><mat-icon>person</mat-icon> Propriétaire</h3>
                  <p class="owner-name">{{ selectedSector()!.playerName || 'Territoire neutre' }}</p>
                </div>

                <div class="detail-section">
                  <h3><mat-icon>attach_money</mat-icon> Économie</h3>
                  <div class="stat-row">
                    <span>Revenus</span>
                    <span class="value gold">{{ selectedSector()!.income || 0 }} ₡/tour</span>
                  </div>
                  @if (selectedSector()!.resource) {
                    <div class="stat-row">
                      <span>Ressource</span>
                      <span class="value">{{ selectedSector()!.resource }}</span>
                    </div>
                  }
                </div>

                @if (selectedSector()!.army && selectedSector()!.army!.length > 0) {
                  <div class="detail-section">
                    <h3><mat-icon>military_tech</mat-icon> Armée</h3>
                    <div class="stat-row">
                      <span>Unités</span>
                      <span class="value">{{ selectedSector()!.army!.length }}</span>
                    </div>
                  </div>
                }

                @if (selectedSector()!.stats) {
                  <div class="detail-section">
                    <h3><mat-icon>shield</mat-icon> Statistiques</h3>
                    <div class="stats-grid">
                      <div class="mini-stat">
                        <span class="label">ATK</span>
                        <span class="value red">{{ selectedSector()!.stats!.totalAtk || 0 }}</span>
                      </div>
                      <div class="mini-stat">
                        <span class="label">DEF</span>
                        <span class="value green">{{ selectedSector()!.stats!.totalDef || 0 }}</span>
                      </div>
                      <div class="mini-stat">
                        <span class="label">PDF</span>
                        <span class="value blue">{{ selectedSector()!.stats!.totalPdf || 0 }}</span>
                      </div>
                      <div class="mini-stat">
                        <span class="label">ARM</span>
                        <span class="value purple">{{ selectedSector()!.stats!.totalArmor || 0 }}</span>
                      </div>
                    </div>
                  </div>
                }

                <div class="detail-section">
                  <h3><mat-icon>share</mat-icon> Voisins ({{ selectedSector()!.neighbors.length }})</h3>
                  <div class="neighbors-list">
                    @for (neighborNum of selectedSector()!.neighbors; track neighborNum) {
                      @if (getSectorByNumber(neighborNum); as neighbor) {
                        <mat-chip (click)="selectSector(neighbor)" class="neighbor-chip">
                          #{{ neighborNum }} - {{ neighbor.name | slice:0:12 }}
                        </mat-chip>
                      }
                    }
                  </div>
                </div>

                <button mat-stroked-button color="warn" class="close-btn" (click)="selectedSector.set(null)">
                  <mat-icon>close</mat-icon>
                  Fermer
                </button>
              </mat-card-content>
            </mat-card>
          }
        </div>

        <!-- Statistiques globales -->
        <mat-card class="stats-card">
          <mat-card-content>
            <div class="section-header">
              <mat-icon color="primary">analytics</mat-icon>
              <h2>Statistiques globales</h2>
            </div>
            <div class="global-stats">
              <div class="global-stat">
                <mat-icon>place</mat-icon>
                <div>
                  <span class="stat-value">{{ allSectors().length }}</span>
                  <span class="stat-label">Secteurs</span>
                </div>
              </div>
              <div class="global-stat">
                <mat-icon>people</mat-icon>
                <div>
                  <span class="stat-value">{{ players().length }}</span>
                  <span class="stat-label">Joueurs</span>
                </div>
              </div>
              <div class="global-stat">
                <mat-icon>flag</mat-icon>
                <div>
                  <span class="stat-value">{{ conqueredSectorsCount() }}</span>
                  <span class="stat-label">Conquis</span>
                </div>
              </div>
              <div class="global-stat">
                <mat-icon>landscape</mat-icon>
                <div>
                  <span class="stat-value">{{ neutralSectorsCount() }}</span>
                  <span class="stat-label">Neutres</span>
                </div>
              </div>
            </div>
          </mat-card-content>
        </mat-card>
      </div>
    }
  `,
  styles: [`
    .loading-container {
      display: flex;
      flex-direction: column;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
      gap: 16px;
      color: #64748b;
    }

    .container {
      max-width: 1600px;
      margin: 0 auto;
      padding: 32px 16px;
    }

    .page-header {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 32px;
    }

    .avatar {
      width: 64px;
      height: 64px;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      mat-icon {
        font-size: 40px;
        width: 40px;
        height: 40px;
        color: white;
      }
    }

    .title {
      font-size: 1.75rem;
      font-weight: 700;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin: 0;
    }

    .subtitle {
      color: #64748b;
      margin: 4px 0 0;
    }

    .map-layout {
      display: grid;
      grid-template-columns: 250px 1fr 300px;
      gap: 24px;
      margin-bottom: 24px;

      @media (max-width: 1200px) {
        grid-template-columns: 1fr;
      }
    }

    .legend-card, .details-card, .stats-card {
      border-radius: 12px;
      height: fit-content;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;

      h2 {
        margin: 0;
        font-size: 1.1rem;
        font-weight: 600;
      }
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      border-radius: 8px;
      cursor: pointer;
      transition: all 0.2s;
      margin-bottom: 8px;

      &:hover {
        background: #f1f5f9;
      }

      &.active {
        background: #ede9fe;
      }

      &.neutral {
        cursor: default;
        opacity: 0.7;
      }
    }

    .legend-color {
      width: 24px;
      height: 24px;
      border-radius: 6px;
      flex-shrink: 0;
    }

    .legend-info {
      display: flex;
      flex-direction: column;
    }

    .legend-name {
      font-weight: 600;
      font-size: 0.9rem;
    }

    .legend-count {
      font-size: 0.75rem;
      color: #64748b;
    }

    .clear-filter {
      width: 100%;
      margin-top: 8px;
    }

    /* Grille de secteurs */
    .map-container {
      background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
      border-radius: 16px;
      padding: 24px;
      overflow: auto;
      min-height: 500px;
    }

    .sector-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      grid-template-rows: repeat(4, 1fr);
      gap: 12px;
      max-width: 700px;
      margin: 0 auto;
    }

    .sector-wrapper {
      aspect-ratio: 1;
    }

    .sector-tile {
      width: 100%;
      height: 100%;
      background: linear-gradient(145deg, #ffffff 0%, #f1f5f9 100%);
      border: 3px solid var(--sector-color, #94a3b8);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s ease;
      position: relative;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 6px;
        background: var(--sector-color, #94a3b8);
        border-radius: 9px 9px 0 0;
      }

      &:hover {
        transform: translateY(-4px) scale(1.02);
        box-shadow: 0 12px 24px rgba(0, 0, 0, 0.2);
        z-index: 10;
      }

      &.selected {
        transform: translateY(-4px) scale(1.05);
        box-shadow: 0 0 0 4px #6366f1, 0 12px 24px rgba(99, 102, 241, 0.3);
        z-index: 20;
        background: linear-gradient(145deg, #ede9fe 0%, #ddd6fe 100%);
      }

      &.highlighted {
        background: linear-gradient(145deg, #fef3c7 0%, #fde68a 100%);
        box-shadow: 0 0 0 2px #f59e0b;
      }

      &.dimmed {
        opacity: 0.35;
        filter: grayscale(0.5);
      }
    }

    .sector-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: 12px 8px;
      gap: 4px;
      width: 100%;
    }

    .sector-number {
      font-size: 0.7rem;
      color: #64748b;
      font-weight: 700;
      background: #e2e8f0;
      padding: 2px 8px;
      border-radius: 10px;
    }

    .sector-name {
      font-size: 0.85rem;
      font-weight: 700;
      color: #1e293b;
      line-height: 1.2;
      max-width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .sector-resource {
      font-size: 0.65rem;
      color: #64748b;
      font-style: italic;
    }

    .owner-badge {
      font-size: 0.75rem;
      font-weight: 700;
      color: white;
      border-radius: 50%;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-top: 4px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    /* Détails secteur */
    .details-card {
      position: sticky;
      top: 100px;
    }

    .sector-header {
      display: flex;
      align-items: center;
      gap: 12px;
      padding-bottom: 16px;
      border-left: 4px solid;
      padding-left: 12px;

      h2 {
        margin: 0;
        font-size: 1.2rem;
        font-weight: 700;
      }

      .sector-id {
        font-size: 0.8rem;
        color: #64748b;
      }
    }

    .sector-color {
      width: 32px;
      height: 32px;
      border-radius: 8px;
    }

    .detail-section {
      padding: 16px 0;
      border-bottom: 1px solid #e2e8f0;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0 0 12px;
        font-size: 0.9rem;
        font-weight: 600;
        color: #64748b;

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }
    }

    .owner-name {
      font-size: 1.1rem;
      font-weight: 600;
      margin: 0;
    }

    .stat-row {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 0.9rem;

      .value {
        font-weight: 600;

        &.gold { color: #f59e0b; }
        &.red { color: #ef4444; }
        &.green { color: #10b981; }
        &.blue { color: #3b82f6; }
        &.purple { color: #8b5cf6; }
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
    }

    .mini-stat {
      background: #f8fafc;
      padding: 8px;
      border-radius: 6px;
      text-align: center;

      .label {
        display: block;
        font-size: 0.7rem;
        color: #64748b;
        margin-bottom: 2px;
      }

      .value {
        font-size: 1rem;
        font-weight: 700;
      }
    }

    .neighbors-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .neighbor-chip {
      cursor: pointer;
      font-size: 0.75rem !important;

      &:hover {
        background: #6366f1 !important;
        color: white !important;
      }
    }

    .close-btn {
      width: 100%;
      margin-top: 16px;
    }

    /* Stats globales */
    .global-stats {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
      gap: 16px;
    }

    .global-stat {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #f8fafc;
      border-radius: 8px;

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #6366f1;
      }

      .stat-value {
        display: block;
        font-size: 1.5rem;
        font-weight: 700;
        color: #1e293b;
      }

      .stat-label {
        font-size: 0.8rem;
        color: #64748b;
      }
    }

    .error-alert {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #fef2f2;
      color: #dc2626;
      padding: 16px;
      border-radius: 8px;
      border: 1px solid #fecaca;
    }

    .fade-in {
      animation: fadeIn 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class CarteComponent implements OnInit {
  private apiService = inject(ApiService);

  // State avec signals
  loading = signal(true);
  error = signal<string | null>(null);
  board = signal<Board | null>(null);
  players = signal<Player[]>([]);
  selectedSector = signal<SectorWithPlayer | null>(null);
  selectedPlayer = signal<Player | null>(null);

  // Couleurs des joueurs par ID
  private playerColorMap = new Map<number, string>();
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

  filteredSectors = computed(() => {
    const selected = this.selectedPlayer();
    if (!selected) return this.allSectors();
    return this.allSectors();  // On ne filtre pas vraiment, on dim les autres
  });

  neutralSectorsCount = computed(() =>
    this.allSectors().filter(s => !s.ownerId).length
  );

  conqueredSectorsCount = computed(() =>
    this.allSectors().filter(s => s.ownerId).length
  );

  ngOnInit(): void {
    this.loadData();
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
      },
      error: (err) => {
        console.error('Erreur chargement carte:', err);
        this.error.set('Impossible de charger la carte. Vérifiez que le serveur est démarré.');
        this.loading.set(false);
      }
    });
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
    return sector.color || this.getPlayerColor(sector.ownerId);
  }

  selectSector(sector: SectorWithPlayer): void {
    this.selectedSector.set(sector);
  }

  togglePlayerFilter(player: Player): void {
    if (this.selectedPlayer()?.id === player.id) {
      this.selectedPlayer.set(null);
    } else {
      this.selectedPlayer.set(player);
    }
  }

  clearFilter(): void {
    this.selectedPlayer.set(null);
  }

  isNeighbor(sectorNumber: number): boolean {
    const selected = this.selectedSector();
    if (!selected) return false;
    return selected.neighbors?.includes(sectorNumber) || false;
  }

  getSectorByNumber(number: number): SectorWithPlayer | undefined {
    return this.allSectors().find(s => s.number === number);
  }

  getInitials(name: string): string {
    return name.split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase()
      .substring(0, 2);
  }
}
