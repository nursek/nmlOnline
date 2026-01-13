import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { PlayerService } from '../../services/player.service';
import { Player, Sector, Unit } from '../../models/player.model';

@Component({
  selector: 'app-joueur',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './joueur.component.html',
  styleUrl: './joueur.component.css'
})
export class JoueurComponent implements OnInit {
  player = signal<Player | null>(null);
  players = signal<Player[]>([]);
  sectors = signal<Sector[]>([]);
  units = signal<Unit[]>([]);
  loading = signal(false);
  error = signal('');
  showPlayerSelector = signal(false);

  constructor(
    public authService: AuthService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    this.loadUnits();
  }

  loadUnits(): void {
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
          // Si le joueur n'existe pas, charger la liste des joueurs disponibles
          this.loadAvailablePlayers();
        }
      });
    } else {
      // Si pas connecté, charger la liste des joueurs disponibles
      this.loadAvailablePlayers();
    }
  }

  /**
   * Charge la liste des joueurs disponibles
   */
  loadAvailablePlayers(): void {
    this.playerService.getAll().subscribe({
      next: (players) => {
        this.players.set(players);
        if (players.length > 0) {
          this.setPlayerData(players[0]);
          this.showPlayerSelector.set(true);
        } else {
          // Aucun joueur disponible, charger des données mock
          this.loadMockData();
        }
      },
      error: (err) => {
        console.error('Erreur lors du chargement des joueurs:', err);
        // Charger des données mock pour permettre de visualiser
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
              number: 5,
              experience: 8.5,
              type: { name: 'BRUTE' },
              classes: [{ name: 'TIREUR' }, { name: 'MASTODONTE' }],
              isInjured: false,
              equipments: [{ name: 'Mini machine gun', cost: 500, pdfBonus: 15, pdcBonus: 0, armBonus: 0, evasionBonus: 0, category: 'Arme à feu' }],
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
              number: 4,
              experience: 6,
              type: { name: 'MALFRAT' },
              classes: [{ name: 'TIREUR' }],
              isInjured: false,
              equipments: [{ name: 'Gilet pare-balles léger', cost: 300, pdfBonus: 0, pdcBonus: 0, armBonus: 8, evasionBonus: 0, category: 'Armure' }],
              attack: 20,
              defense: 15,
              pdf: 25,
              pdc: 10,
              armor: 8,
              evasion: 8
            }
          ],
          stats: { totalAtk: 45, totalPdf: 55, totalPdc: 25, totalDef: 35, totalArmor: 18, totalOffensive: 125, totalDefensive: 53, globalStats: 89 }
        },
        {
          number: 2,
          name: 'Quartier Sud',
          income: 2500,
          army: [
            {
              id: 3,
              name: 'Escouade Charlie',
              number: 3,
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
          stats: { totalAtk: 18, totalPdf: 20, totalPdc: 25, totalDef: 12, totalArmor: 5, totalOffensive: 63, totalDefensive: 17, globalStats: 40 }
        }
      ]
    };

    this.players.set([mockPlayer]);
    this.setPlayerData(mockPlayer);
    this.showPlayerSelector.set(true);
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

    // Extraction de toutes les unités de tous les secteurs
    const allUnits = player.sectors?.flatMap(sector => sector.army || []) || [];
    this.units.set(allUnits);

    this.loading.set(false);
  }

  getTotalTroops(): number {
    return this.units().reduce((sum, unit) => sum + (unit.number || 0), 0);
  }

  getTotalEquipment(): number {
    return this.units().reduce((sum, unit) => sum + (unit.equipments?.length || 0), 0);
  }

  getHealthPercentage(unit: Unit): number {
    // Si l'unité est blessée, elle a 50% de santé, sinon 100%
    return unit.isInjured ? 50 : 100;
  }

  getUnitClasses(unit: Unit): string {
    return unit.classes?.map(c => c.name).join(', ') || 'Aucune classe';
  }

  getSectorName(unitId: number): string {
    const sector = this.sectors().find(s =>
      s.army?.some(u => u.id === unitId)
    );
    return sector ? `${sector.name} (Secteur ${sector.number})` : 'Secteur inconnu';
  }
}

