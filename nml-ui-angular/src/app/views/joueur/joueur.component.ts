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
  sectors = signal<Sector[]>([]);
  units = signal<Unit[]>([]);
  loading = signal(false);
  error = signal('');

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
    if (!currentUser?.id) {
      this.error.set('Utilisateur non connecté');
      this.loading.set(false);
      return;
    }

    this.playerService.getById(currentUser.id).subscribe({
      next: (player) => {
        this.player.set(player);
        this.sectors.set(player.sectors || []);

        // Extraction de toutes les unités de tous les secteurs
        const allUnits = player.sectors?.flatMap(sector => sector.army || []) || [];
        this.units.set(allUnits);

        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des données du joueur:', err);
        this.error.set('Impossible de charger les données du joueur. Veuillez réessayer.');
        this.loading.set(false);
      }
    });
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

