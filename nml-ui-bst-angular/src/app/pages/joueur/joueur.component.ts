import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { PlayerActions, selectCurrentPlayer, selectPlayerLoading, selectPlayerError, selectUser} from '../../store';
import { filter, take } from 'rxjs/operators';
import { Player, PlayerResource } from '../../models';
import { ResourceService } from '../../services/resource.service';

@Component({
  selector: 'app-joueur',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatButtonModule,
    MatSnackBarModule,
  ],
  template: `
    @if (loading$ | async) {
      <div class="loading-container">
        <mat-spinner diameter="60"></mat-spinner>
      </div>
    } @else if (error$ | async; as error) {
      <div class="container">
        <div class="error-alert">
          <mat-icon>error</mat-icon>
          {{ error }}
        </div>
      </div>
    } @else if (player$ | async; as player) {
      <div class="container fade-in">
        <!-- Header -->
        <div class="page-header">
          <div class="avatar">
            <mat-icon>person</mat-icon>
          </div>
          <div>
            <h1 class="title">{{ player.name }}</h1>
            <p class="subtitle">Commandant en chef</p>
          </div>
        </div>

        <!-- Stats Cards -->
        <div class="stats-row">
          @for (stat of getMainStats(player); track stat.label) {
            <mat-card class="stat-card hover-lift">
              <mat-card-content>
                <div class="stat-content">
                  <div>
                    <p class="stat-label">{{ stat.label }}</p>
                    <h3 class="stat-value">{{ stat.value }}</h3>
                  </div>
                  <div class="stat-icon" [style.background-color]="stat.color + '20'">
                    <mat-icon [style.color]="stat.color">{{ stat.icon }}</mat-icon>
                  </div>
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>

        <!-- Statistiques détaillées -->
        <mat-card class="card">
          <mat-card-content>
            <h2 class="section-title">Statistiques détaillées</h2>
            <p class="section-subtitle">Analyse complète de votre puissance</p>

            <div class="details-grid">
              <div class="detail-item">
                <span class="detail-label">Puissance offensive</span>
                <span class="detail-value error">{{ player.stats.totalOffensivePower | number:'1.0-0' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Puissance défensive</span>
                <span class="detail-value success">{{ player.stats.totalDefensivePower | number:'1.0-0' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Puissance économique</span>
                <span class="detail-value warning">{{ player.stats.totalEconomyPower | number:'1.0-0' }}</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Valeur des véhicules</span>
                <span class="detail-value">{{ player.stats.totalVehiclesValue | number:'1.0-0' }} ₡</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Valeur des équipements</span>
                <span class="detail-value">{{ player.stats.totalEquipmentValue | number:'1.0-0' }} ₡</span>
              </div>
              <div class="detail-item">
                <span class="detail-label">Armure totale</span>
                <span class="detail-value">{{ player.stats.totalArmor | number:'1.0-0' }}</span>
              </div>
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Équipements -->
        <mat-card class="card">
          <mat-card-content>
            <div class="section-header">
              <mat-icon color="primary">inventory_2</mat-icon>
              <h2>Équipements possédés</h2>
            </div>
            <p class="section-subtitle">Votre arsenal actuel</p>

            @if (player.equipments.length === 0) {
              <div class="empty-state">
                <mat-icon>inventory_2</mat-icon>
                <p>Aucun équipement pour le moment. Visitez la boutique !</p>
              </div>
            } @else {
              <div class="equipment-grid">
                @for (stack of player.equipments; track stack.equipment.name) {
                  <div class="equipment-card">
                    <div class="equipment-info">
                      <h4>{{ stack.equipment.name }}</h4>
                      <span class="category">{{ stack.equipment.category }}</span>
                      <div class="chips">
                        @if (stack.equipment.pdfBonus > 0) {
                          <mat-chip class="bonus-chip pdf">+{{ stack.equipment.pdfBonus }} PDF</mat-chip>
                        }
                        @if (stack.equipment.pdcBonus > 0) {
                          <mat-chip class="bonus-chip pdc">+{{ stack.equipment.pdcBonus }} PDC</mat-chip>
                        }
                        @if (stack.equipment.armBonus > 0) {
                          <mat-chip class="bonus-chip arm">+{{ stack.equipment.armBonus }} ARM</mat-chip>
                        }
                        @if (stack.equipment.evasionBonus > 0) {
                          <mat-chip class="bonus-chip esq">+{{ stack.equipment.evasionBonus }} ESQ</mat-chip>
                        }
                      </div>
                    </div>
                    <div class="quantity">×{{ stack.quantity }}</div>
                  </div>
                }
              </div>
            }
          </mat-card-content>
        </mat-card>

        <!-- Ressources -->
        <mat-card class="card">
          <mat-card-content>
            <div class="section-header">
              <mat-icon color="primary">diamond</mat-icon>
              <h2>Ressources possédées</h2>
            </div>
            <p class="section-subtitle">Votre inventaire de ressources précieuses</p>

            @if (!player.resources || player.resources.length === 0) {
              <div class="empty-state">
                <mat-icon>diamond</mat-icon>
                <p>Aucune ressource pour le moment. Collectez des ressources dans vos territoires !</p>
              </div>
            } @else {
              <div class="resource-grid">
                @for (resource of player.resources; track resource.name) {
                  <div class="resource-card">
                    <div class="resource-info">
                      <mat-icon class="resource-icon">diamond</mat-icon>
                      <div>
                        <h4>{{ resource.name }}</h4>
                        <span class="quantity-label">Quantité: {{ resource.quantity }}</span>
                        @if (resource.baseValue) {
                          <span class="value-label">Valeur unitaire: {{ resource.baseValue }} ₡</span>
                        }
                      </div>
                    </div>
                    <button
                      mat-raised-button
                      color="accent"
                      class="sell-button"
                      (click)="sellResource(resource, 1)">
                      <mat-icon>sell</mat-icon>
                      Vendre (×1)
                    </button>
                  </div>
                }
              </div>
            }
          </mat-card-content>
        </mat-card>

        <!-- Territoires -->
        <mat-card class="card">
          <mat-card-content>
            <div class="section-header">
              <mat-icon color="primary">place</mat-icon>
              <h2>Territoires contrôlés</h2>
            </div>
            <p class="section-subtitle">Les secteurs sous votre commandement</p>

            @if (player.sectors.length === 0) {
              <div class="empty-state">
                <mat-icon>place</mat-icon>
                <p>Aucun territoire contrôlé. Partez à la conquête !</p>
              </div>
            } @else {
              <div class="territories-grid">
                @for (sector of player.sectors; track $index) {
                  <div class="territory-card">
                    <h4>{{ sector.name }}</h4>
                    <span class="sector-number">Secteur #{{ sector.number ?? 'N/A' }}</span>
                    <mat-divider></mat-divider>
                    <div class="territory-stats">
                      <div class="stat-row">
                        <span>Revenus:</span>
                        <span class="warning">{{ sector.income ?? 0 }} ₡/tour</span>
                      </div>
                      @if (sector.army && sector.army.length > 0) {
                        <div class="stat-row">
                          <span>Unités:</span>
                          <span>{{ sector.army.length }}</span>
                        </div>
                      }
                      @if (sector.stats) {
                        <div class="stat-row">
                          <span>Défense:</span>
                          <span>+{{ sector.stats.defenseBonus }}</span>
                        </div>
                        <div class="stat-row">
                          <span>Production:</span>
                          <span>{{ sector.stats.resourceProduction }}</span>
                        </div>
                      }
                    </div>
                  </div>
                }
              </div>
            }
          </mat-card-content>
        </mat-card>
      </div>
    }
  `,
  styles: [`
    .loading-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 80vh;
    }

    .container {
      max-width: 1400px;
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
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
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

    .stats-row {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 24px;
      margin-bottom: 24px;
    }

    .stat-card {
      border-radius: 12px;
    }

    .stat-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .stat-label {
      font-size: 0.875rem;
      color: #64748b;
      margin: 0 0 4px;
    }

    .stat-value {
      font-size: 1.5rem;
      font-weight: 700;
      margin: 0;
    }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
      }
    }

    .card {
      margin-bottom: 24px;
      border-radius: 12px;
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      h2 {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 600;
      }
    }

    .section-title {
      font-size: 1.25rem;
      font-weight: 600;
      margin: 0 0 8px;
    }

    .section-subtitle {
      color: #64748b;
      margin: 0 0 24px;
      font-size: 0.875rem;
    }

    .details-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 16px;
    }

    .detail-item {
      padding: 16px;
      background: #f8fafc;
      border-radius: 8px;
    }

    .detail-label {
      display: block;
      font-size: 0.875rem;
      color: #64748b;
      margin-bottom: 4px;
    }

    .detail-value {
      font-size: 1.25rem;
      font-weight: 700;

      &.error { color: #ef4444; }
      &.success { color: #10b981; }
      &.warning { color: #f59e0b; }
    }

    .equipment-grid, .territories-grid, .resource-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 16px;
    }

    .equipment-card {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        border-color: #6366f1;
        transform: translateY(-4px);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
      }
    }

    .equipment-info {
      h4 {
        margin: 0 0 4px;
        font-weight: 600;
      }

      .category {
        font-size: 0.75rem;
        color: #64748b;
      }
    }

    .chips {
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
      margin-top: 8px;
    }

    .bonus-chip {
      font-size: 0.7rem !important;
      min-height: 24px !important;
      padding: 0 8px !important;

      &.pdf { background: #dc2626 !important; color: white !important; }
      &.pdc { background: #0891b2 !important; color: white !important; }
      &.arm { background: #059669 !important; color: white !important; }
      &.esq { background: #d97706 !important; color: white !important; }
    }

    .quantity {
      font-size: 1.5rem;
      font-weight: 700;
      color: #6366f1;
    }

    .resource-card {
      display: flex;
      flex-direction: column;
      gap: 12px;
      padding: 16px;
      background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
      border: 2px solid #f59e0b;
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 25px rgba(245, 158, 11, 0.3);
      }
    }

    .resource-info {
      display: flex;
      align-items: center;
      gap: 12px;

      .resource-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #f59e0b;
      }

      h4 {
        margin: 0 0 4px;
        font-weight: 600;
        color: #78350f;
      }

      .quantity-label, .value-label {
        display: block;
        font-size: 0.75rem;
        color: #92400e;
        margin-top: 2px;
      }
    }

    .sell-button {
      width: 100%;

      mat-icon {
        margin-right: 4px;
      }
    }

    .territory-card {
      padding: 16px;
      background: #f8fafc;
      border: 2px solid #6366f1;
      border-radius: 8px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
      }

      h4 {
        margin: 0 0 4px;
        font-weight: 600;
      }

      .sector-number {
        font-size: 0.75rem;
        color: #64748b;
        display: block;
        margin-bottom: 12px;
      }
    }

    .territory-stats {
      margin-top: 12px;
    }

    .stat-row {
      display: flex;
      justify-content: space-between;
      font-size: 0.875rem;
      margin-bottom: 4px;

      span:first-child {
        color: #64748b;
      }

      span:last-child {
        font-weight: 600;
      }

      .warning {
        color: #f59e0b;
      }
    }

    .empty-state {
      text-align: center;
      padding: 64px 16px;

      mat-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #94a3b8;
        margin-bottom: 16px;
      }

      p {
        margin: 0;
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

    .hover-lift {
      transition: transform 0.3s, box-shadow 0.3s;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
      }
    }
  `]
})
export class JoueurComponent implements OnInit {
  private readonly store = inject(Store);
  private readonly resourceService = inject(ResourceService);
  private readonly snackBar = inject(MatSnackBar);

  player$ = this.store.select(selectCurrentPlayer);
  loading$ = this.store.select(selectPlayerLoading);
  error$ = this.store.select(selectPlayerError);

  ngOnInit(): void {
    // Attendre que l'utilisateur soit authentifié et charger le joueur
    this.store.select(selectUser).pipe(
      filter((user): user is NonNullable<typeof user> => !!user && !!user.username),
      take(1)
    ).subscribe(user => {
      console.log('Loading player for user:', user.username);
      this.store.dispatch(PlayerActions.fetchCurrentPlayer({ username: user.username }));
    });
  }

  /**
   * Vend une ressource du joueur
   */
  sellResource(resource: PlayerResource, quantity: number): void {
    if (!resource.id) {
      this.snackBar.open('Erreur: ressource invalide', 'Fermer', { duration: 3000 });
      return;
    }

    this.resourceService.sellResource(resource.id, quantity).subscribe({
      next: (response) => {
        this.snackBar.open(
          `✓ ${response.quantitySold}x ${response.resourceName} vendu(s) pour ${response.saleValue.toFixed(2)}$`,
          'Fermer',
          { duration: 4000, panelClass: ['success-snackbar'] }
        );

        // Recharger le joueur pour mettre à jour les données
        this.store.select(selectUser).pipe(take(1)).subscribe(user => {
          if (user?.username) {
            this.store.dispatch(PlayerActions.fetchCurrentPlayer({ username: user.username }));
          }
        });
      },
      error: (error) => {
        console.error('Erreur lors de la vente:', error);
        const message = error.error?.message || error.message || 'Erreur lors de la vente';
        this.snackBar.open(`❌ ${message}`, 'Fermer', {
          duration: 4000,
          panelClass: ['error-snackbar']
        });
      }
    });
  }

  getMainStats(player: Player) {
    return [
      {
        label: 'Argent',
        value: `${player.stats.money.toFixed(0)} ₡`,
        icon: 'attach_money',
        color: '#f59e0b',
      },
      {
        label: 'Revenus',
        value: `${player.stats.totalIncome.toFixed(0)} ₡/tour`,
        icon: 'trending_up',
        color: '#10b981',
      },
      {
        label: 'Puissance globale',
        value: player.stats.globalPower.toFixed(0),
        icon: 'shield',
        color: '#6366f1',
      },
      {
        label: 'Territoires',
        value: player.sectors.length,
        icon: 'place',
        color: '#8b5cf6',
      },
    ];
  }
}
