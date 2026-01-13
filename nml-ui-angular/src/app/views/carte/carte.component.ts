import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { PlayerService } from '../../services/player.service';
import { Player, Sector, Unit } from '../../models/player.model';

// Interface pour les positions visuelles des secteurs sur la carte
interface SectorVisual {
  sector: Sector;
  path: string;
  centerX: number;
  centerY: number;
  color: string;
}

@Component({
  selector: 'app-carte',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './carte.component.html',
  styleUrl: './carte.component.css'
})
export class CarteComponent implements OnInit {
  player = signal<Player | null>(null);
  sectors = signal<Sector[]>([]);
  loading = signal(false);
  error = signal('');

  zoom = signal(1);
  selectedSector = signal<Sector | null>(null);
  hoveredSector = signal<Sector | null>(null);

  // Positions prédéfinies pour les secteurs (disposition hexagonale améliorée)
  // Les hexagones sont disposés en nid d'abeille pour une meilleure visualisation stratégique
  private sectorPositions: { path: string; centerX: number; centerY: number }[] = [
    // Rangée 1 (centre haut)
    { path: 'M 325,50 L 425,50 L 475,136 L 425,222 L 325,222 L 275,136 Z', centerX: 375, centerY: 136 },
    // Rangée 2
    { path: 'M 175,180 L 275,180 L 325,266 L 275,352 L 175,352 L 125,266 Z', centerX: 225, centerY: 266 },
    { path: 'M 475,180 L 575,180 L 625,266 L 575,352 L 475,352 L 425,266 Z', centerX: 525, centerY: 266 },
    // Rangée 3 (centre)
    { path: 'M 325,310 L 425,310 L 475,396 L 425,482 L 325,482 L 275,396 Z', centerX: 375, centerY: 396 },
    // Rangée 4
    { path: 'M 175,440 L 275,440 L 325,526 L 275,612 L 175,612 L 125,526 Z', centerX: 225, centerY: 526 },
    { path: 'M 475,440 L 575,440 L 625,526 L 575,612 L 475,612 L 425,526 Z', centerX: 525, centerY: 526 },
    // Rangée 5 (bas)
    { path: 'M 325,570 L 425,570 L 475,656 L 425,742 L 325,742 L 275,656 Z', centerX: 375, centerY: 656 },
    // Extensions possibles
    { path: 'M 25,310 L 125,310 L 175,396 L 125,482 L 25,482 L -25,396 Z', centerX: 75, centerY: 396 },
    { path: 'M 625,310 L 725,310 L 775,396 L 725,482 L 625,482 L 575,396 Z', centerX: 675, centerY: 396 },
  ];

  // Computed signal pour les secteurs avec leurs positions visuelles
  sectorVisuals = computed<SectorVisual[]>(() => {
    return this.sectors().map((sector, index) => ({
      sector,
      ...this.sectorPositions[index % this.sectorPositions.length],
      color: this.getSectorColor(sector)
    }));
  });

  players = signal<Player[]>([]);
  showPlayerSelector = signal(false);

  constructor(
    public authService: AuthService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    this.loadPlayerData();
  }

  loadPlayerData(): void {
    this.loading.set(true);
    this.error.set('');

    const currentUser = this.authService.currentUser();

    // Si l'utilisateur est connecté, essayer de charger son profil
    if (currentUser?.username) {
      this.playerService.getByName(currentUser.username).subscribe({
        next: (player) => {
          this.setPlayerData(player);
        },
        error: () => {
          // Si le joueur n'existe pas avec ce nom, charger la liste des joueurs disponibles
          this.loadAvailablePlayers();
        }
      });
    } else {
      // Si pas connecté, charger la liste des joueurs disponibles
      this.loadAvailablePlayers();
    }
  }

  /**
   * Charge la liste des joueurs disponibles pour permettre la sélection
   */
  loadAvailablePlayers(): void {
    this.playerService.getAll().subscribe({
      next: (players) => {
        this.players.set(players);
        if (players.length > 0) {
          // Charger automatiquement le premier joueur
          this.setPlayerData(players[0]);
          this.showPlayerSelector.set(true);
        } else {
          // Aucun joueur disponible, charger des données mock
          this.loadMockData();
        }
      },
      error: (err) => {
        console.error('Erreur lors du chargement des joueurs:', err);
        // Charger des données mock pour permettre de visualiser la carte
        this.loadMockData();
      }
    });
  }

  /**
   * Charge des données mock pour le développement / démo
   */
  private loadMockData(): void {
    const mockPlayer: Player = {
      id: 1,
      name: 'Général Suprême (Démo)',
      stats: {
        money: 15000,
        totalIncome: 4500,
        totalVehiclesValue: 0,
        totalEquipmentValue: 8000,
        totalOffensivePower: 250,
        totalDefensivePower: 180,
        globalPower: 215,
        totalEconomyPower: 4500,
        totalAtk: 80,
        totalPdf: 100,
        totalPdc: 70,
        totalDef: 120,
        totalArmor: 60
      },
      equipments: [],
      sectors: [
        {
          number: 1,
          name: 'Quartier Nord',
          income: 2000,
          army: [
            {
              id: 1,
              name: 'Escouade Alpha',
              experience: 8.5,
              type: { name: 'BRUTE' },
              classes: [{ name: 'TIREUR' }, { name: 'MASTODONTE' }],
              isInjured: false,
              equipments: [],
              attack: 25,
              defense: 20,
              pdf: 30,
              pdc: 15,
              armor: 10,
              evasion: 5
            },
            {
              id: 2,
              name: 'Escouade Bravo',
              experience: 6,
              type: { name: 'MALFRAT' },
              classes: [{ name: 'TIREUR' }],
              isInjured: false,
              equipments: [],
              attack: 20,
              defense: 15,
              pdf: 25,
              pdc: 10,
              armor: 8,
              evasion: 8
            }
          ],
          stats: {
            totalAtk: 45,
            totalPdf: 55,
            totalPdc: 25,
            totalDef: 35,
            totalArmor: 18,
            totalOffensive: 125,
            totalDefensive: 53,
            globalStats: 89
          }
        },
        {
          number: 2,
          name: 'Quartier Sud',
          income: 2500,
          army: [
            {
              id: 3,
              name: 'Escouade Charlie',
              experience: 7,
              type: { name: 'VOYOU' },
              classes: [{ name: 'LEGER' }],
              isInjured: true,
              equipments: [],
              attack: 18,
              defense: 12,
              pdf: 20,
              pdc: 25,
              armor: 5,
              evasion: 15
            }
          ],
          stats: {
            totalAtk: 18,
            totalPdf: 20,
            totalPdc: 25,
            totalDef: 12,
            totalArmor: 5,
            totalOffensive: 63,
            totalDefensive: 17,
            globalStats: 40
          }
        },
        {
          number: 3,
          name: 'Zone Industrielle',
          income: 3000,
          army: [
            {
              id: 4,
              name: 'Escouade Delta',
              experience: 9,
              type: { name: 'BRUTE' },
              classes: [{ name: 'MASTODONTE' }, { name: 'PILOTE_DESTRUCTEUR' }],
              isInjured: false,
              equipments: [],
              attack: 30,
              defense: 25,
              pdf: 35,
              pdc: 20,
              armor: 15,
              evasion: 3
            },
            {
              id: 5,
              name: 'Escouade Echo',
              experience: 5,
              type: { name: 'LARBIN' },
              classes: [],
              isInjured: false,
              equipments: [],
              attack: 10,
              defense: 8,
              pdf: 12,
              pdc: 8,
              armor: 4,
              evasion: 10
            }
          ],
          stats: {
            totalAtk: 40,
            totalPdf: 47,
            totalPdc: 28,
            totalDef: 33,
            totalArmor: 19,
            totalOffensive: 115,
            totalDefensive: 52,
            globalStats: 83.5
          }
        }
      ]
    };

    this.players.set([mockPlayer]);
    this.setPlayerData(mockPlayer);
    this.showPlayerSelector.set(true);
  }

  /**
   * Sélectionne un joueur dans la liste
   */
  selectPlayer(player: Player): void {
    this.setPlayerData(player);
  }

  /**
   * Gère le changement de joueur via le sélecteur
   */
  onPlayerChange(event: Event): void {
    const select = event.target as HTMLSelectElement;
    const playerName = select.value;
    const selectedPlayer = this.players().find(p => p.name === playerName);
    if (selectedPlayer) {
      this.setPlayerData(selectedPlayer);
    }
  }

  /**
   * Configure les données du joueur sélectionné
   */
  private setPlayerData(player: Player): void {
    this.player.set(player);
    this.sectors.set(player.sectors || []);
    // Sélectionner le premier secteur par défaut
    if (player.sectors?.length > 0) {
      this.selectedSector.set(player.sectors[0]);
    }
    this.loading.set(false);
  }

  selectSector(sector: Sector): void {
    this.selectedSector.set(sector);
  }

  getSectorColor(sector: Sector): string {
    // Couleur basée sur la puissance militaire du secteur
    const armySize = sector.army?.length || 0;
    if (armySize >= 5) return '#3d5a3c'; // Vert militaire - bien défendu
    if (armySize >= 3) return '#5a6a3c'; // Vert-jaune - défense moyenne
    if (armySize >= 1) return '#6a5a3c'; // Jaune-marron - défense faible
    return '#4a5568'; // Gris - secteur vide
  }

  getArmySize(sector: Sector): number {
    return sector.army?.length || 0;
  }

  getTotalOffensivePower(sector: Sector): number {
    if (!sector.stats) return 0;
    // Utiliser totalOffensive du backend si disponible, sinon calculer
    if (sector.stats.totalOffensive !== undefined) {
      return Math.round(sector.stats.totalOffensive);
    }
    return Math.round((sector.stats.totalAtk || 0) + (sector.stats.totalPdf || 0) + (sector.stats.totalPdc || 0));
  }

  getTotalDefensivePower(sector: Sector): number {
    if (!sector.stats) return 0;
    // Utiliser totalDefensive du backend si disponible, sinon calculer
    if (sector.stats.totalDefensive !== undefined) {
      return Math.round(sector.stats.totalDefensive);
    }
    return Math.round((sector.stats.totalDef || 0) + (sector.stats.totalArmor || 0));
  }

  getGlobalPower(sector: Sector): number {
    // Utiliser globalStats du backend si disponible
    if (sector.stats?.globalStats !== undefined) {
      return Math.round(sector.stats.globalStats);
    }
    return Math.round((this.getTotalOffensivePower(sector) + this.getTotalDefensivePower(sector)) / 2);
  }

  getTotalIncome(): number {
    return this.sectors().reduce((sum, s) => sum + (s.income || 0), 0);
  }

  getTotalUnits(): number {
    return this.sectors().reduce((sum, s) => sum + (s.army?.length || 0), 0);
  }

  getDefenseLevel(sector: Sector): string {
    const power = this.getGlobalPower(sector);
    if (power >= 100) return 'Excellente';
    if (power >= 50) return 'Bonne';
    if (power >= 20) return 'Moyenne';
    if (power > 0) return 'Faible';
    return 'Aucune';
  }

  getUnitTypeIcon(unit: Unit): string {
    const typeName = unit.type?.name?.toUpperCase() || '';
    switch (typeName) {
      case 'BRUTE': return '💪';
      case 'MALFRAT': return '🔫';
      case 'VOYOU': return '🗡️';
      case 'LARBIN': return '👤';
      default: return '⚔️';
    }
  }

  zoomIn(): void {
    this.zoom.update(z => Math.min(2, z + 0.2));
  }

  zoomOut(): void {
    this.zoom.update(z => Math.max(0.5, z - 0.2));
  }

  resetView(): void {
    this.zoom.set(1);
  }
}

