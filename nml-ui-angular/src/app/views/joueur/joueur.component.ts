import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { PlayerService } from '../../services/player.service';
import { Player } from '../../models/player.model';

interface Unit {
  id: number;
  name: string;
  troops: string[];
  equipment: string[];
  health: number;
  maxHealth: number;
}

@Component({
  selector: 'app-joueur',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="joueur-container">
      <div class="page-header">
        <div class="header-content">
          <h1>👤 Mon Profil</h1>
          <p>Bienvenue, <strong>{{ authService.currentUser()?.username }}</strong></p>
        </div>
        <div class="stats-summary">
          <div class="stat-card">
            <div class="stat-value">{{ units().length }}</div>
            <div class="stat-label">Unités</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ getTotalTroops() }}</div>
            <div class="stat-label">Troupes</div>
          </div>
          <div class="stat-card">
            <div class="stat-value">{{ getTotalEquipment() }}</div>
            <div class="stat-label">Équipements</div>
          </div>
        </div>
      </div>

      @if (loading()) {
        <div class="loading-container">
          <div class="spinner-large"></div>
          <p>Chargement des unités...</p>
        </div>
      } @else if (error()) {
        <div class="error-container">
          <div class="error-icon">⚠️</div>
          <h3>Erreur de chargement</h3>
          <p>{{ error() }}</p>
          <button class="btn btn-primary" (click)="loadUnits()">
            Réessayer
          </button>
        </div>
      } @else if (units().length === 0) {
        <div class="empty-state">
          <div class="empty-icon">📦</div>
          <h3>Aucune unité</h3>
          <p>Vous n'avez pas encore d'unités. Commencez par recruter des troupes !</p>
          <button class="btn btn-primary" (click)="createDemoUnit()">
            <span>➕</span>
            Créer une unité de démonstration
          </button>
        </div>
      } @else {
        <div class="units-grid">
          @for (unit of units(); track unit.id) {
            <div class="unit-card">
              <div class="unit-header">
                <h3>{{ unit.name }}</h3>
                <div class="health-badge" [class.low-health]="getHealthPercentage(unit) < 30">
                  {{ unit.health }}/{{ unit.maxHealth }} HP
                </div>
              </div>
              
              <div class="health-bar">
                <div 
                  class="health-fill" 
                  [style.width.%]="getHealthPercentage(unit)"
                  [class.low]="getHealthPercentage(unit) < 30"
                  [class.medium]="getHealthPercentage(unit) >= 30 && getHealthPercentage(unit) < 70"
                ></div>
              </div>

              <div class="unit-section">
                <div class="section-header">
                  <span class="section-icon">⚔️</span>
                  <span class="section-title">Troupes</span>
                  <span class="section-count">{{ unit.troops.length }}</span>
                </div>
                <div class="tags-container">
                  @for (troop of unit.troops; track troop) {
                    <span class="tag tag-troop">{{ troop }}</span>
                  }
                  @if (unit.troops.length === 0) {
                    <span class="text-muted">Aucune troupe</span>
                  }
                </div>
              </div>

              <div class="unit-section">
                <div class="section-header">
                  <span class="section-icon">🛡️</span>
                  <span class="section-title">Équipements</span>
                  <span class="section-count">{{ unit.equipment.length }}</span>
                </div>
                <div class="tags-container">
                  @for (equip of unit.equipment; track equip) {
                    <span class="tag tag-equipment">{{ equip }}</span>
                  }
                  @if (unit.equipment.length === 0) {
                    <span class="text-muted">Aucun équipement</span>
                  }
                </div>
              </div>

              <div class="unit-actions">
                <button class="btn btn-sm btn-secondary">
                  <span>✏️</span>
                  Modifier
                </button>
                <button class="btn btn-sm btn-danger" (click)="deleteUnit(unit.id)">
                  <span>🗑️</span>
                  Supprimer
                </button>
              </div>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .joueur-container {
      max-width: 1400px;
      margin: 0 auto;
      padding: 2rem;
      animation: fadeIn 0.5s ease-out;
    }

    .page-header {
      margin-bottom: 2rem;
    }

    .header-content h1 {
      font-size: 2.5rem;
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .header-content p {
      color: #666;
      font-size: 1.1rem;
      margin: 0;
    }

    .header-content strong {
      color: #667eea;
    }

    .stats-summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
      gap: 1rem;
      margin-top: 1.5rem;
    }

    .stat-card {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 1.5rem;
      border-radius: 16px;
      text-align: center;
      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
    }

    .stat-value {
      font-size: 2.5rem;
      font-weight: 700;
      margin-bottom: 0.25rem;
    }

    .stat-label {
      font-size: 0.95rem;
      opacity: 0.9;
    }

    .loading-container,
    .error-container,
    .empty-state {
      text-align: center;
      padding: 4rem 2rem;
    }

    .spinner-large {
      width: 60px;
      height: 60px;
      border: 4px solid #f3f3f3;
      border-top-color: #667eea;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin: 0 auto 1rem;
    }

    .error-icon,
    .empty-icon {
      font-size: 4rem;
      margin-bottom: 1rem;
    }

    .error-container h3,
    .empty-state h3 {
      font-size: 1.5rem;
      color: #333;
      margin: 0 0 0.5rem 0;
    }

    .error-container p,
    .empty-state p {
      color: #666;
      margin-bottom: 1.5rem;
    }

    .units-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
      gap: 1.5rem;
    }

    .unit-card {
      background: white;
      border-radius: 16px;
      padding: 1.5rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      transition: all 0.3s;
      border: 2px solid transparent;
    }

    .unit-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
      border-color: #667eea;
    }

    .unit-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1rem;
    }

    .unit-header h3 {
      margin: 0;
      font-size: 1.5rem;
      color: #333;
    }

    .health-badge {
      background: #4caf50;
      color: white;
      padding: 0.35rem 0.75rem;
      border-radius: 20px;
      font-size: 0.85rem;
      font-weight: 600;
    }

    .health-badge.low-health {
      background: #f44336;
      animation: pulse 1.5s ease-in-out infinite;
    }

    .health-bar {
      height: 8px;
      background: #e0e0e0;
      border-radius: 10px;
      overflow: hidden;
      margin-bottom: 1.5rem;
    }

    .health-fill {
      height: 100%;
      background: #4caf50;
      transition: width 0.3s, background-color 0.3s;
    }

    .health-fill.medium {
      background: #ff9800;
    }

    .health-fill.low {
      background: #f44336;
    }

    .unit-section {
      margin-bottom: 1.5rem;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.75rem;
      padding-bottom: 0.5rem;
      border-bottom: 2px solid #f0f0f0;
    }

    .section-icon {
      font-size: 1.2rem;
    }

    .section-title {
      font-weight: 600;
      color: #333;
      flex: 1;
    }

    .section-count {
      background: #667eea;
      color: white;
      padding: 0.25rem 0.6rem;
      border-radius: 12px;
      font-size: 0.8rem;
      font-weight: 600;
    }

    .tags-container {
      display: flex;
      flex-wrap: wrap;
      gap: 0.5rem;
    }

    .tag {
      padding: 0.4rem 0.8rem;
      border-radius: 8px;
      font-size: 0.85rem;
      font-weight: 500;
    }

    .tag-troop {
      background: #e3f2fd;
      color: #1976d2;
    }

    .tag-equipment {
      background: #f3e5f5;
      color: #7b1fa2;
    }

    .text-muted {
      color: #999;
      font-style: italic;
      font-size: 0.9rem;
    }

    .unit-actions {
      display: flex;
      gap: 0.5rem;
      margin-top: 1rem;
      padding-top: 1rem;
      border-top: 1px solid #f0f0f0;
    }

    .btn {
      padding: 0.65rem 1.25rem;
      border: none;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.95rem;
    }

    .btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
    }

    .btn-secondary {
      background: #f0f0f0;
      color: #333;
    }

    .btn-secondary:hover {
      background: #e0e0e0;
    }

    .btn-danger {
      background: #ffebee;
      color: #f44336;
    }

    .btn-danger:hover {
      background: #f44336;
      color: white;
    }

    .btn-sm {
      padding: 0.5rem 1rem;
      font-size: 0.85rem;
      flex: 1;
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
        transform: translateY(20px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    @keyframes pulse {
      0%, 100% {
        opacity: 1;
      }
      50% {
        opacity: 0.7;
      }
    }

    @media (max-width: 768px) {
      .joueur-container {
        padding: 1rem;
      }

      .header-content h1 {
        font-size: 2rem;
      }

      .units-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class JoueurComponent implements OnInit {
  units = signal<Unit[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(
    public authService: AuthService,
    private playerService: PlayerService
  ) {}

  ngOnInit(): void {
    this.loadUnits();
  }

  loadUnits(): void {
    this.loading.set(true);
    this.error.set(null);

    // TODO: Remplacer par l'appel API réel une fois que le backend est prêt
    // Pour l'instant, on simule avec des données de démo
    setTimeout(() => {
      this.units.set([
        {
          id: 1,
          name: 'Garde Royale',
          troops: ['Chevalier', 'Archer', 'Lancier'],
          equipment: ['Épée Longue', 'Bouclier Lourd', 'Armure de Plates'],
          health: 85,
          maxHealth: 100
        },
        {
          id: 2,
          name: 'Éclaireurs',
          troops: ['Éclaireur', 'Rôdeur'],
          equipment: ['Arc Court', 'Cape d\'Invisibilité'],
          health: 42,
          maxHealth: 60
        },
        {
          id: 3,
          name: 'Mages de Guerre',
          troops: ['Mage', 'Apprenti'],
          equipment: ['Bâton Magique', 'Robe Enchantée', 'Grimoire'],
          health: 55,
          maxHealth: 80
        }
      ]);
      this.loading.set(false);
    }, 800);

    /* Code pour l'API réelle :
    const userId = this.authService.currentUser()?.id;
    if (userId) {
      this.playerService.getById(userId).subscribe({
        next: (data) => {
          this.units.set(data.units || []);
          this.loading.set(false);
        },
        error: (err) => {
          this.error.set('Impossible de charger les unités');
          this.loading.set(false);
        }
      });
    }
    */
  }

  getTotalTroops(): number {
    return this.units().reduce((sum, unit) => sum + unit.troops.length, 0);
  }

  getTotalEquipment(): number {
    return this.units().reduce((sum, unit) => sum + unit.equipment.length, 0);
  }

  getHealthPercentage(unit: Unit): number {
    return (unit.health / unit.maxHealth) * 100;
  }

  createDemoUnit(): void {
    const newUnit: Unit = {
      id: Date.now(),
      name: 'Nouvelle Unité',
      troops: ['Soldat'],
      equipment: ['Épée'],
      health: 100,
      maxHealth: 100
    };
    this.units.set([...this.units(), newUnit]);
  }

  deleteUnit(id: number): void {
    if (confirm('Êtes-vous sûr de vouloir supprimer cette unité ?')) {
      this.units.set(this.units().filter(u => u.id !== id));
    }
  }
}
