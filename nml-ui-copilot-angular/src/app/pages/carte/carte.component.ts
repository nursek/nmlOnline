import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { PlayerActions } from '../../store/player/player.actions';
import { selectAllPlayers, selectPlayerLoading, selectPlayerError } from '../../store/player/player.selectors';

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
    } @else {
      <div class="container fade-in">
        <!-- Header -->
        <div class="page-header">
          <div class="avatar">
            <mat-icon>map</mat-icon>
          </div>
          <div>
            <h1 class="title">Carte du Monde</h1>
            <p class="subtitle">Vue d'ensemble des territoires conquis</p>
          </div>
        </div>

        <!-- Légende des joueurs -->
        <mat-card class="card">
          <mat-card-content>
            <div class="section-header">
              <mat-icon color="primary">person</mat-icon>
              <h2>Joueurs actifs</h2>
            </div>
            <p class="section-subtitle">Commandants en présence</p>

            <div class="players-grid">
              @for (player of players$ | async; track player.id; let i = $index) {
                <div class="player-item" [style.border-color]="getPlayerColor(i)">
                  <div class="player-color" [style.background-color]="getPlayerColor(i)"></div>
                  <div>
                    <div class="player-name">{{ player.name }}</div>
                    <div class="player-territories">
                      {{ player.sectors.length }} {{ player.sectors.length > 1 ? 'territoires' : 'territoire' }}
                    </div>
                  </div>
                </div>
              }
            </div>
          </mat-card-content>
        </mat-card>

        <!-- Carte des territoires -->
        <mat-card class="card">
          <mat-card-content>
            <div class="section-header">
              <mat-icon color="primary">place</mat-icon>
              <h2>Carte des territoires</h2>
            </div>
            <p class="section-subtitle">{{ allSectors.length }} secteur{{ allSectors.length > 1 ? 's' : '' }} contrôlé{{ allSectors.length > 1 ? 's' : '' }}</p>

            @if (allSectors.length === 0) {
              <div class="empty-state">
                <mat-icon>map</mat-icon>
                <h3>Aucun territoire n'a encore été conquis</h3>
                <p>La carte est vide pour le moment</p>
              </div>
            } @else {
              <div class="territories-grid">
                @for (sector of allSectors; track sector.number) {
                  <div class="territory-card" [style.border-color]="sector.playerColor">
                    <div class="territory-header">
                      <div class="territory-color" [style.background-color]="sector.playerColor"></div>
                      <mat-chip>#{{ sector.number ?? 'N/A' }}</mat-chip>
                    </div>
                    <h3 class="territory-name">{{ sector.name }}</h3>
                    <p class="territory-owner">{{ sector.playerName }}</p>
                    <mat-divider></mat-divider>
                    <div class="territory-stats">
                      <div class="stat-row">
                        <span>Revenus:</span>
                        <span class="stat-value warning">{{ sector.income ?? 0 }} ₡</span>
                      </div>
                      @if (sector.army && sector.army.length > 0) {
                        <div class="stat-row">
                          <span>Armée:</span>
                          <span class="stat-value">{{ sector.army.length }} unités</span>
                        </div>
                      }
                    </div>
                  </div>
                }
              </div>
            }
          </mat-card-content>
        </mat-card>

        <!-- Statistiques des joueurs -->
        <h2 class="section-title">Statistiques des joueurs</h2>
        <div class="stats-grid">
          @for (player of players$ | async; track player.id; let i = $index) {
            <mat-card class="stat-card hover-lift">
              <mat-card-content>
                <div class="player-header">
                  <div class="player-color" [style.background-color]="getPlayerColor(i)"></div>
                  <h3>{{ player.name }}</h3>
                </div>
                <p class="player-territories">{{ player.sectors.length }} territoire{{ player.sectors.length > 1 ? 's' : '' }}</p>
                <mat-divider></mat-divider>
                <div class="player-stats">
                  <div class="stat-row">
                    <span>Puissance globale:</span>
                    <span class="stat-value">{{ player.stats.globalPower | number:'1.0-0' }}</span>
                  </div>
                  <div class="stat-row">
                    <span>Argent:</span>
                    <span class="stat-value">{{ player.stats.money | number:'1.0-0' }} ₡</span>
                  </div>
                  <div class="stat-row">
                    <span>Revenus:</span>
                    <span class="stat-value">{{ player.stats.totalIncome | number:'1.0-0' }} ₡/tour</span>
                  </div>
                  <div class="stat-row">
                    <span>Territoires:</span>
                    <span class="stat-value">{{ player.sectors.length }}</span>
                  </div>
                </div>
              </mat-card-content>
            </mat-card>
          }
        </div>
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

    .section-subtitle {
      color: #64748b;
      margin: 0 0 24px;
      font-size: 0.875rem;
    }

    .section-title {
      font-size: 1.25rem;
      font-weight: 600;
      margin: 0 0 16px;
    }

    .players-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
      gap: 16px;
    }

    .player-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: #f8fafc;
      border: 2px solid;
      border-radius: 8px;
    }

    .player-color {
      width: 16px;
      height: 16px;
      border-radius: 50%;
      flex-shrink: 0;
    }

    .player-name {
      font-weight: 600;
    }

    .player-territories {
      font-size: 0.75rem;
      color: #64748b;
    }

    .territories-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
      gap: 16px;
    }

    .territory-card {
      padding: 16px;
      background: #f8fafc;
      border: 2px solid;
      border-radius: 8px;
      transition: transform 0.3s, box-shadow 0.3s;
      cursor: pointer;

      &:hover {
        transform: translateY(-4px);
        box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
      }
    }

    .territory-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
    }

    .territory-color {
      width: 16px;
      height: 16px;
      border-radius: 50%;
    }

    .territory-name {
      margin: 0 0 4px;
      font-weight: 600;
    }

    .territory-owner {
      font-size: 0.75rem;
      color: #64748b;
      margin: 0 0 12px;
    }

    .territory-stats, .player-stats {
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
    }

    .stat-value {
      font-weight: 600;

      &.warning {
        color: #f59e0b;
      }
    }

    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 24px;
    }

    .stat-card {
      border-radius: 12px;
    }

    .player-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;

      h3 {
        margin: 0;
        font-weight: 600;
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

      h3 {
        margin: 0 0 8px;
        color: #64748b;
      }

      p {
        margin: 0;
        color: #94a3b8;
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
export class CarteComponent implements OnInit {
  private store = inject(Store);

  players$ = this.store.select(selectAllPlayers);
  loading$ = this.store.select(selectPlayerLoading);
  error$ = this.store.select(selectPlayerError);

  playerColors = [
    '#6366f1', '#ef4444', '#10b981', '#f59e0b',
    '#8b5cf6', '#ec4899', '#f97316', '#06b6d4',
  ];

  allSectors: any[] = [];

  ngOnInit(): void {
    this.store.dispatch(PlayerActions.fetchAllPlayers());

    this.players$.subscribe(players => {
      this.allSectors = players.flatMap((player, idx) =>
        player.sectors.map(sector => ({
          ...sector,
          playerName: player.name,
          playerColor: this.getPlayerColor(idx),
        }))
      );
    });
  }

  getPlayerColor(index: number): string {
    return this.playerColors[index % this.playerColors.length];
  }
}
