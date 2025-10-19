import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Zone {
  id: string;
  name: string;
  owner: string;
  troops: number;
  color: string;
  path: string;
}

@Component({
  selector: 'app-carte',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="carte-container">
      <div class="page-header">
        <h1>🗺️ Carte du Monde</h1>
        <p>Explorez et conquérez les territoires</p>
      </div>

      <div class="carte-layout">
        <!-- Carte SVG Interactive -->
        <div class="map-section">
          <div class="map-controls">
            <button class="control-btn" (click)="zoomIn()">
              <span>🔍+</span>
            </button>
            <button class="control-btn" (click)="zoomOut()">
              <span>🔍-</span>
            </button>
            <button class="control-btn" (click)="resetView()">
              <span>🎯</span>
            </button>
          </div>

          <svg 
            xmlns="http://www.w3.org/2000/svg" 
            viewBox="0 0 1000 600" 
            class="interactive-map"
            [style.transform]="'scale(' + zoom() + ')'"
          >
            <defs>
              <filter id="shadow">
                <feDropShadow dx="2" dy="2" stdDeviation="3" flood-opacity="0.3"/>
              </filter>
            </defs>

            @for (zone of zones; track zone.id) {
              <path
                [attr.d]="zone.path"
                [attr.fill]="zone.color"
                [attr.stroke]="selectedZone()?.id === zone.id ? '#FFD700' : '#333'"
                [attr.stroke-width]="selectedZone()?.id === zone.id ? '4' : '2'"
                [attr.filter]="selectedZone()?.id === zone.id ? 'url(#shadow)' : ''"
                class="zone-path"
                [class.selected]="selectedZone()?.id === zone.id"
                (click)="selectZone(zone)"
                (mouseenter)="hoveredZone.set(zone)"
                (mouseleave)="hoveredZone.set(null)"
              />
            }

            @for (zone of zones; track zone.id) {
              <text
                [attr.x]="getZoneCenter(zone).x"
                [attr.y]="getZoneCenter(zone).y"
                class="zone-label"
                text-anchor="middle"
                [class.selected]="selectedZone()?.id === zone.id"
              >
                {{ zone.name }}
              </text>
            }
          </svg>

          @if (hoveredZone() && hoveredZone() !== selectedZone()) {
            <div class="hover-tooltip" [style.left.px]="tooltipPosition.x" [style.top.px]="tooltipPosition.y">
              <strong>{{ hoveredZone()?.name }}</strong>
              <div>{{ hoveredZone()?.owner }}</div>
            </div>
          }
        </div>

        <!-- Panneau d'informations -->
        <div class="info-panel">
          @if (selectedZone()) {
            <div class="zone-info">
              <div class="zone-header">
                <h2>{{ selectedZone()?.name }}</h2>
                <span class="zone-badge" [style.background-color]="selectedZone()?.color">
                  Niveau {{ getZoneLevel(selectedZone()!) }}
                </span>
              </div>

              <div class="info-section">
                <div class="info-item">
                  <span class="info-label">👑 Contrôlée par</span>
                  <span class="info-value">{{ selectedZone()?.owner }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">⚔️ Troupes</span>
                  <span class="info-value">{{ selectedZone()?.troops }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">💰 Revenus</span>
                  <span class="info-value">{{ getZoneIncome(selectedZone()!) }} or/tour</span>
                </div>
                <div class="info-item">
                  <span class="info-label">🛡️ Défense</span>
                  <span class="info-value">{{ getZoneDefense(selectedZone()!) }}%</span>
                </div>
              </div>

              <div class="info-section">
                <h3>Ressources</h3>
                <div class="resources-grid">
                  <div class="resource-card">
                    <span class="resource-icon">🌾</span>
                    <div class="resource-info">
                      <span class="resource-name">Nourriture</span>
                      <span class="resource-amount">{{ getRandomResource(100, 500) }}</span>
                    </div>
                  </div>
                  <div class="resource-card">
                    <span class="resource-icon">⚒️</span>
                    <div class="resource-info">
                      <span class="resource-name">Fer</span>
                      <span class="resource-amount">{{ getRandomResource(50, 300) }}</span>
                    </div>
                  </div>
                  <div class="resource-card">
                    <span class="resource-icon">💎</span>
                    <div class="resource-info">
                      <span class="resource-name">Gemmes</span>
                      <span class="resource-amount">{{ getRandomResource(10, 100) }}</span>
                    </div>
                  </div>
                </div>
              </div>

              <div class="action-buttons">
                @if (isOwnedByPlayer(selectedZone()!)) {
                  <button class="btn btn-primary">
                    <span>🏗️</span>
                    Construire
                  </button>
                  <button class="btn btn-secondary">
                    <span>👥</span>
                    Recruter
                  </button>
                } @else {
                  <button class="btn btn-danger">
                    <span>⚔️</span>
                    Attaquer
                  </button>
                  <button class="btn btn-secondary">
                    <span>🕵️</span>
                    Espionner
                  </button>
                }
              </div>
            </div>
          } @else {
            <div class="empty-selection">
              <div class="empty-icon">🗺️</div>
              <h3>Sélectionnez un territoire</h3>
              <p>Cliquez sur une zone de la carte pour voir ses détails</p>
            </div>
          }
        </div>
      </div>

      <!-- Légende -->
      <div class="map-legend">
        <h3>Légende</h3>
        <div class="legend-items">
          <div class="legend-item">
            <span class="legend-color" style="background: #4CAF50;"></span>
            <span>Vos territoires</span>
          </div>
          <div class="legend-item">
            <span class="legend-color" style="background: #F44336;"></span>
            <span>Territoires ennemis</span>
          </div>
          <div class="legend-item">
            <span class="legend-color" style="background: #9E9E9E;"></span>
            <span>Territoires neutres</span>
          </div>
          <div class="legend-item">
            <span class="legend-color" style="background: #2196F3;"></span>
            <span>Territoires alliés</span>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .carte-container {
      max-width: 1600px;
      margin: 0 auto;
      padding: 2rem;
      animation: fadeIn 0.5s ease-out;
    }

    .page-header {
      margin-bottom: 2rem;
    }

    .page-header h1 {
      font-size: 2.5rem;
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .page-header p {
      color: #666;
      font-size: 1.1rem;
      margin: 0;
    }

    .carte-layout {
      display: grid;
      grid-template-columns: 1fr 400px;
      gap: 2rem;
      margin-bottom: 2rem;
    }

    .map-section {
      background: white;
      border-radius: 16px;
      padding: 1.5rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      position: relative;
    }

    .map-controls {
      position: absolute;
      top: 1.5rem;
      right: 1.5rem;
      display: flex;
      gap: 0.5rem;
      z-index: 10;
    }

    .control-btn {
      width: 40px;
      height: 40px;
      border: none;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.2rem;
      transition: all 0.3s;
    }

    .control-btn:hover {
      transform: scale(1.1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .interactive-map {
      width: 100%;
      height: auto;
      transition: transform 0.3s ease-out;
      transform-origin: center;
    }

    .zone-path {
      cursor: pointer;
      transition: all 0.2s;
    }

    .zone-path:hover {
      opacity: 0.8;
      stroke-width: 3;
    }

    .zone-path.selected {
      filter: brightness(1.1);
    }

    .zone-label {
      fill: white;
      font-weight: 600;
      font-size: 18px;
      pointer-events: none;
      text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.5);
    }

    .zone-label.selected {
      font-size: 22px;
      fill: #FFD700;
    }

    .hover-tooltip {
      position: absolute;
      background: rgba(0, 0, 0, 0.9);
      color: white;
      padding: 0.75rem 1rem;
      border-radius: 8px;
      pointer-events: none;
      z-index: 100;
      font-size: 0.9rem;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    }

    .info-panel {
      background: white;
      border-radius: 16px;
      padding: 1.5rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      max-height: 600px;
      overflow-y: auto;
    }

    .zone-info {
      animation: slideIn 0.3s ease-out;
    }

    .zone-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
      padding-bottom: 1rem;
      border-bottom: 2px solid #f0f0f0;
    }

    .zone-header h2 {
      margin: 0;
      font-size: 1.75rem;
      color: #333;
    }

    .zone-badge {
      padding: 0.5rem 1rem;
      border-radius: 20px;
      color: white;
      font-weight: 600;
      font-size: 0.9rem;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }

    .info-section {
      margin-bottom: 1.5rem;
    }

    .info-section h3 {
      font-size: 1.1rem;
      margin: 0 0 1rem 0;
      color: #333;
      font-weight: 600;
    }

    .info-item {
      display: flex;
      justify-content: space-between;
      padding: 0.75rem;
      background: #f8f9fa;
      border-radius: 8px;
      margin-bottom: 0.5rem;
    }

    .info-label {
      color: #666;
      font-weight: 500;
    }

    .info-value {
      color: #333;
      font-weight: 600;
    }

    .resources-grid {
      display: grid;
      gap: 0.75rem;
    }

    .resource-card {
      display: flex;
      align-items: center;
      gap: 1rem;
      padding: 1rem;
      background: #f8f9fa;
      border-radius: 10px;
      transition: all 0.3s;
    }

    .resource-card:hover {
      background: #e9ecef;
      transform: translateX(5px);
    }

    .resource-icon {
      font-size: 2rem;
    }

    .resource-info {
      display: flex;
      flex-direction: column;
      flex: 1;
    }

    .resource-name {
      font-size: 0.85rem;
      color: #666;
    }

    .resource-amount {
      font-size: 1.2rem;
      font-weight: 700;
      color: #333;
    }

    .action-buttons {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 0.75rem;
      margin-top: 1.5rem;
    }

    .btn {
      padding: 0.85rem 1rem;
      border: none;
      border-radius: 10px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
    }

    .btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }

    .btn-secondary {
      background: #e9ecef;
      color: #333;
    }

    .btn-secondary:hover {
      background: #dee2e6;
    }

    .btn-danger {
      background: linear-gradient(135deg, #f44336 0%, #e91e63 100%);
      color: white;
    }

    .btn-danger:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(244, 67, 54, 0.4);
    }

    .empty-selection {
      text-align: center;
      padding: 3rem 2rem;
      color: #999;
    }

    .empty-icon {
      font-size: 4rem;
      margin-bottom: 1rem;
    }

    .empty-selection h3 {
      color: #666;
      margin: 0 0 0.5rem 0;
    }

    .map-legend {
      background: white;
      border-radius: 16px;
      padding: 1.5rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    }

    .map-legend h3 {
      margin: 0 0 1rem 0;
      font-size: 1.2rem;
      color: #333;
    }

    .legend-items {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .legend-color {
      width: 24px;
      height: 24px;
      border-radius: 6px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateX(20px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    @media (max-width: 1200px) {
      .carte-layout {
        grid-template-columns: 1fr;
      }

      .info-panel {
        max-height: none;
      }
    }

    @media (max-width: 768px) {
      .carte-container {
        padding: 1rem;
      }

      .page-header h1 {
        font-size: 2rem;
      }

      .action-buttons {
        grid-template-columns: 1fr;
      }

      .legend-items {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class CarteComponent {
  selectedZone = signal<Zone | null>(null);
  hoveredZone = signal<Zone | null>(null);
  zoom = signal(1);
  tooltipPosition = { x: 0, y: 0 };

  zones: Zone[] = [
    {
      id: 'zone1',
      name: 'Plaines du Nord',
      owner: 'Vous',
      troops: 150,
      color: '#4CAF50',
      path: 'M100,100 L350,80 L380,250 L120,280 Z'
    },
    {
      id: 'zone2',
      name: 'Forêt d\'Émeraude',
      owner: 'Joueur 2',
      troops: 85,
      color: '#F44336',
      path: 'M380,80 L620,90 L640,270 L390,260 Z'
    },
    {
      id: 'zone3',
      name: 'Montagnes de Fer',
      owner: 'Neutre',
      troops: 50,
      color: '#9E9E9E',
      path: 'M650,90 L900,100 L880,280 L650,270 Z'
    },
    {
      id: 'zone4',
      name: 'Désert du Sud',
      owner: 'Alliance',
      troops: 120,
      color: '#2196F3',
      path: 'M100,300 L380,280 L360,500 L110,480 Z'
    },
    {
      id: 'zone5',
      name: 'Vallée Mystique',
      owner: 'Vous',
      troops: 200,
      color: '#4CAF50',
      path: 'M390,290 L640,285 L620,490 L370,510 Z'
    },
    {
      id: 'zone6',
      name: 'Côte des Tempêtes',
      owner: 'Joueur 3',
      troops: 95,
      color: '#F44336',
      path: 'M650,290 L900,295 L880,500 L630,500 Z'
    }
  ];

  selectZone(zone: Zone): void {
    this.selectedZone.set(zone);
  }

  getZoneCenter(zone: Zone): { x: number, y: number } {
    // Calculer approximativement le centre de chaque zone
    const centerMap: { [key: string]: { x: number, y: number } } = {
      'zone1': { x: 225, y: 180 },
      'zone2': { x: 500, y: 175 },
      'zone3': { x: 775, y: 185 },
      'zone4': { x: 240, y: 390 },
      'zone5': { x: 505, y: 390 },
      'zone6': { x: 775, y: 395 }
    };
    return centerMap[zone.id] || { x: 0, y: 0 };
  }

  getZoneLevel(zone: Zone): number {
    return Math.floor(zone.troops / 50) + 1;
  }

  getZoneIncome(zone: Zone): number {
    return this.getZoneLevel(zone) * 50;
  }

  getZoneDefense(zone: Zone): number {
    return Math.min(this.getZoneLevel(zone) * 15, 90);
  }

  getRandomResource(min: number, max: number): number {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  isOwnedByPlayer(zone: Zone): boolean {
    return zone.owner === 'Vous';
  }

  zoomIn(): void {
    this.zoom.update(z => Math.min(z + 0.2, 2));
  }

  zoomOut(): void {
    this.zoom.update(z => Math.max(z - 0.2, 0.5));
  }

  resetView(): void {
    this.zoom.set(1);
  }
}
