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
  templateUrl: './carte.component.html',
  styleUrls: ['./carte.component.scss']
})
export class CarteComponent implements OnInit {
  private readonly apiService = inject(ApiService);

  // State avec signals
  loading = signal(true);
  error = signal<string | null>(null);
  board = signal<Board | null>(null);
  players = signal<Player[]>([]);
  selectedSector = signal<SectorWithPlayer | null>(null);
  selectedPlayer = signal<Player | null>(null);

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

  // filteredSectors est identique à allSectors (le filtre se fait via CSS avec .dimmed)
  filteredSectors = this.allSectors;

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
