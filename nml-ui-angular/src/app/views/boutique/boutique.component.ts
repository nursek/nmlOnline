import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../../services/equipment.service';
import { Equipment } from '../../models/equipment.model';

@Component({
  selector: 'app-boutique',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="boutique-container">
      <div class="page-header">
        <h1>🛒 Boutique</h1>
        <p>Équipez vos unités pour devenir plus puissant</p>
        
        <div class="header-actions">
          <div class="search-bar">
            <span class="search-icon">🔍</span>
            <input 
              type="text" 
              placeholder="Rechercher un équipement..."
              [(ngModel)]="searchQuery"
              (input)="filterEquipment()"
            />
          </div>
          
          <div class="filter-buttons">
            <button 
              class="filter-btn"
              [class.active]="selectedCategory === 'all'"
              (click)="filterByCategory('all')"
            >
              Tous
            </button>
            <button 
              class="filter-btn"
              [class.active]="selectedCategory === 'weapon'"
              (click)="filterByCategory('weapon')"
            >
              ⚔️ Armes
            </button>
            <button 
              class="filter-btn"
              [class.active]="selectedCategory === 'armor'"
              (click)="filterByCategory('armor')"
            >
              🛡️ Armures
            </button>
            <button 
              class="filter-btn"
              [class.active]="selectedCategory === 'magic'"
              (click)="filterByCategory('magic')"
            >
              ✨ Magie
            </button>
          </div>
        </div>
      </div>

      <div class="player-gold">
        <span class="gold-icon">💰</span>
        <span class="gold-amount">{{ playerGold }}</span>
        <span class="gold-label">Or</span>
      </div>

      @if (loading()) {
        <div class="loading-container">
          <div class="spinner-large"></div>
          <p>Chargement de la boutique...</p>
        </div>
      } @else if (error()) {
        <div class="error-container">
          <div class="error-icon">⚠️</div>
          <h3>Erreur de chargement</h3>
          <p>{{ error() }}</p>
          <button class="btn btn-primary" (click)="loadEquipment()">
            Réessayer
          </button>
        </div>
      } @else if (filteredEquipment().length === 0) {
        <div class="empty-state">
          <div class="empty-icon">📦</div>
          <h3>Aucun équipement trouvé</h3>
          <p>Essayez de modifier vos filtres de recherche</p>
        </div>
      } @else {
        <div class="equipment-grid">
          @for (item of filteredEquipment(); track item.id) {
            <div class="equipment-card" [class.affordable]="canAfford(item)">
              <div class="equipment-image">
                <span class="equipment-emoji">{{ getEquipmentEmoji(item) }}</span>
                @if (!canAfford(item)) {
                  <div class="locked-overlay">
                    <span>🔒</span>
                  </div>
                }
              </div>
              
              <div class="equipment-content">
                <h3 class="equipment-name">{{ item.name }}</h3>
                <p class="equipment-description">{{ item.description || 'Équipement de qualité' }}</p>
                
                <div class="equipment-stats">
                  <div class="stat">
                    <span class="stat-label">Type:</span>
                    <span class="stat-value">{{ getEquipmentType(item) }}</span>
                  </div>
                  <div class="stat">
                    <span class="stat-label">Rareté:</span>
                    <span class="stat-value rarity" [class]="getEquipmentRarity(item)">
                      {{ getEquipmentRarity(item) }}
                    </span>
                  </div>
                </div>
                
                <div class="equipment-footer">
                  <div class="price">
                    <span class="price-icon">💰</span>
                    <span class="price-amount">{{ item.price || 100 }}</span>
                  </div>
                  
                  <button 
                    class="btn btn-primary btn-sm"
                    [disabled]="!canAfford(item) || purchasing() === item.id"
                    (click)="purchaseEquipment(item)"
                  >
                    @if (purchasing() === item.id) {
                      <span class="spinner-small"></span>
                    } @else if (!canAfford(item)) {
                      <span>Insuffisant</span>
                    } @else {
                      <span>Acheter</span>
                    }
                  </button>
                </div>
              </div>
            </div>
          }
        </div>
      }
    </div>

    @if (purchaseSuccess()) {
      <div class="toast toast-success" [@slideIn]>
        <span class="toast-icon">✅</span>
        <span>Équipement acheté avec succès !</span>
      </div>
    }

    @if (purchaseError()) {
      <div class="toast toast-error" [@slideIn]>
        <span class="toast-icon">❌</span>
        <span>{{ purchaseError() }}</span>
      </div>
    }
  `,
  styles: [`
    .boutique-container {
      max-width: 1400px;
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
      margin: 0 0 1.5rem 0;
    }

    .header-actions {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
      align-items: center;
    }

    .search-bar {
      flex: 1;
      min-width: 250px;
      position: relative;
    }

    .search-icon {
      position: absolute;
      left: 1rem;
      top: 50%;
      transform: translateY(-50%);
      font-size: 1.2rem;
    }

    .search-bar input {
      width: 100%;
      padding: 0.85rem 1rem 0.85rem 3rem;
      border: 2px solid #e0e0e0;
      border-radius: 12px;
      font-size: 1rem;
      transition: all 0.3s;
    }

    .search-bar input:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
    }

    .filter-buttons {
      display: flex;
      gap: 0.5rem;
      flex-wrap: wrap;
    }

    .filter-btn {
      padding: 0.75rem 1.25rem;
      border: 2px solid #e0e0e0;
      background: white;
      border-radius: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
      color: #666;
    }

    .filter-btn:hover {
      border-color: #667eea;
      color: #667eea;
    }

    .filter-btn.active {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border-color: transparent;
    }

    .player-gold {
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
      background: linear-gradient(135deg, #ffd700 0%, #ffed4e 100%);
      padding: 1rem 1.5rem;
      border-radius: 16px;
      box-shadow: 0 4px 15px rgba(255, 215, 0, 0.3);
      margin-bottom: 2rem;
    }

    .gold-icon {
      font-size: 2rem;
    }

    .gold-amount {
      font-size: 1.5rem;
      font-weight: 700;
      color: #333;
    }

    .gold-label {
      font-weight: 600;
      color: #666;
    }

    .equipment-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 1.5rem;
    }

    .equipment-card {
      background: white;
      border-radius: 16px;
      overflow: hidden;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      transition: all 0.3s;
      opacity: 0.7;
    }

    .equipment-card.affordable {
      opacity: 1;
    }

    .equipment-card:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
    }

    .equipment-image {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      height: 150px;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }

    .equipment-emoji {
      font-size: 4rem;
      filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2));
    }

    .locked-overlay {
      position: absolute;
      inset: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 3rem;
    }

    .equipment-content {
      padding: 1.25rem;
    }

    .equipment-name {
      font-size: 1.25rem;
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .equipment-description {
      color: #666;
      font-size: 0.9rem;
      margin: 0 0 1rem 0;
      line-height: 1.5;
    }

    .equipment-stats {
      display: flex;
      gap: 1rem;
      margin-bottom: 1rem;
      padding: 0.75rem;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .stat {
      flex: 1;
    }

    .stat-label {
      display: block;
      font-size: 0.75rem;
      color: #999;
      margin-bottom: 0.25rem;
    }

    .stat-value {
      font-weight: 600;
      color: #333;
    }

    .stat-value.rarity {
      padding: 0.25rem 0.5rem;
      border-radius: 6px;
      font-size: 0.8rem;
      display: inline-block;
    }

    .stat-value.common {
      background: #e0e0e0;
      color: #666;
    }

    .stat-value.rare {
      background: #e3f2fd;
      color: #1976d2;
    }

    .stat-value.epic {
      background: #f3e5f5;
      color: #7b1fa2;
    }

    .stat-value.legendary {
      background: #fff3e0;
      color: #f57c00;
    }

    .equipment-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 1rem;
      border-top: 1px solid #f0f0f0;
    }

    .price {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .price-icon {
      font-size: 1.5rem;
    }

    .price-amount {
      font-size: 1.5rem;
      font-weight: 700;
      color: #333;
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
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }

    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-sm {
      padding: 0.5rem 1rem;
      font-size: 0.85rem;
    }

    .spinner-small {
      width: 14px;
      height: 14px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
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

    .toast {
      position: fixed;
      bottom: 2rem;
      right: 2rem;
      padding: 1rem 1.5rem;
      border-radius: 12px;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
      z-index: 1000;
      animation: slideIn 0.3s ease-out;
    }

    .toast-success {
      background: #4caf50;
      color: white;
    }

    .toast-error {
      background: #f44336;
      color: white;
    }

    .toast-icon {
      font-size: 1.5rem;
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

    @keyframes slideIn {
      from {
        transform: translateX(100%);
        opacity: 0;
      }
      to {
        transform: translateX(0);
        opacity: 1;
      }
    }

    @media (max-width: 768px) {
      .boutique-container {
        padding: 1rem;
      }

      .page-header h1 {
        font-size: 2rem;
      }

      .equipment-grid {
        grid-template-columns: 1fr;
      }

      .header-actions {
        flex-direction: column;
      }

      .search-bar {
        width: 100%;
      }
    }
  `]
})
export class BoutiqueComponent implements OnInit {
  equipment = signal<Equipment[]>([]);
  filteredEquipment = signal<Equipment[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);
  purchasing = signal<number | null>(null);
  purchaseSuccess = signal(false);
  purchaseError = signal<string | null>(null);
  
  searchQuery = '';
  selectedCategory = 'all';
  playerGold = 5000; // TODO: Récupérer depuis le profil du joueur

  constructor(private equipmentService: EquipmentService) {}

  ngOnInit(): void {
    this.loadEquipment();
  }

  loadEquipment(): void {
    this.loading.set(true);
    this.error.set(null);

    // Données de démonstration
    setTimeout(() => {
      const demoEquipment: Equipment[] = [
        { id: 1, name: 'Épée Longue', description: 'Une épée de qualité supérieure', price: 500 },
        { id: 2, name: 'Bouclier Lourd', description: 'Protection maximale', price: 800 },
        { id: 3, name: 'Arc Court', description: 'Attaque à distance rapide', price: 350 },
        { id: 4, name: 'Bâton Magique', description: 'Augmente les pouvoirs magiques', price: 1200 },
        { id: 5, name: 'Armure de Plates', description: 'Armure lourde très résistante', price: 1500 },
        { id: 6, name: 'Cape d\'Invisibilité', description: 'Permet de se cacher des ennemis', price: 2000 },
        { id: 7, name: 'Robe Enchantée', description: 'Protection magique', price: 900 },
        { id: 8, name: 'Grimoire Ancien', description: 'Contient des sorts puissants', price: 3000 }
      ];
      this.equipment.set(demoEquipment);
      this.filteredEquipment.set(demoEquipment);
      this.loading.set(false);
    }, 600);

    /* Code pour l'API réelle :
    this.equipmentService.getAll().subscribe({
      next: (data) => {
        this.equipment.set(data);
        this.filteredEquipment.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Impossible de charger les équipements');
        this.loading.set(false);
      }
    });
    */
  }

  filterEquipment(): void {
    let filtered = this.equipment();
    
    if (this.searchQuery) {
      filtered = filtered.filter(e => 
        e.name.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        e.description?.toLowerCase().includes(this.searchQuery.toLowerCase())
      );
    }
    
    if (this.selectedCategory !== 'all') {
      filtered = filtered.filter(e => 
        this.getEquipmentType(e).toLowerCase() === this.selectedCategory
      );
    }
    
    this.filteredEquipment.set(filtered);
  }

  filterByCategory(category: string): void {
    this.selectedCategory = category;
    this.filterEquipment();
  }

  canAfford(item: Equipment): boolean {
    return this.playerGold >= (item.price || 0);
  }

  purchaseEquipment(item: Equipment): void {
    this.purchasing.set(item.id || 0);
    this.purchaseError.set(null);
    
    setTimeout(() => {
      if (this.canAfford(item)) {
        this.playerGold -= (item.price || 0);
        this.purchasing.set(null);
        this.purchaseSuccess.set(true);
        
        setTimeout(() => {
          this.purchaseSuccess.set(false);
        }, 3000);
      } else {
        this.purchasing.set(null);
        this.purchaseError.set('Or insuffisant');
        
        setTimeout(() => {
          this.purchaseError.set(null);
        }, 3000);
      }
    }, 500);
  }

  getEquipmentEmoji(item: Equipment): string {
    const name = item.name.toLowerCase();
    if (name.includes('épée') || name.includes('arc')) return '⚔️';
    if (name.includes('bouclier') || name.includes('armure')) return '🛡️';
    if (name.includes('bâton') || name.includes('magie') || name.includes('grimoire') || name.includes('robe')) return '✨';
    if (name.includes('cape')) return '🎭';
    return '⚔️';
  }

  getEquipmentType(item: Equipment): string {
    const name = item.name.toLowerCase();
    if (name.includes('épée') || name.includes('arc')) return 'Arme';
    if (name.includes('bouclier') || name.includes('armure')) return 'Armure';
    if (name.includes('bâton') || name.includes('magie') || name.includes('grimoire') || name.includes('robe') || name.includes('cape')) return 'Magie';
    return 'Autre';
  }

  getEquipmentRarity(item: Equipment): string {
    const price = item.price || 0;
    if (price >= 2000) return 'legendary';
    if (price >= 1000) return 'epic';
    if (price >= 500) return 'rare';
    return 'common';
  }
}
