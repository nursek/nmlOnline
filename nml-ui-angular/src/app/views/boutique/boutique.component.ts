import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { EquipmentService } from '../../services/equipment.service';
import { Equipment } from '../../models/equipment.model';

@Component({
  selector: 'app-boutique',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './boutique.component.html',
  styleUrl: './boutique.component.css'
})
export class BoutiqueComponent implements OnInit {
  equipment = signal<Equipment[]>([]);
  loading = signal(false);
  error = signal('');
  purchasing = signal(false);

  searchQuery = '';
  selectedCategory = 'all';
  playerGold = 5000; // Simulation - à remplacer par les vraies données du joueur

  filteredEquipment = computed(() => {
    let items = this.equipment();

    // Filter by category
    if (this.selectedCategory !== 'all') {
      items = items.filter(item => this.getCategoryType(item.category) === this.selectedCategory);
    }

    // Filter by search query
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      items = items.filter(item =>
        item.name.toLowerCase().includes(query) ||
        item.category?.toLowerCase().includes(query)
      );
    }

    return items;
  });

  constructor(private equipmentService: EquipmentService) {}

  ngOnInit(): void {
    this.loadEquipment();
  }

  loadEquipment(): void {
    this.loading.set(true);
    this.error.set('');

    this.equipmentService.getAll().subscribe({
      next: (data) => {
        this.equipment.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Erreur lors du chargement des équipements:', err);
        this.error.set('Impossible de charger l\'arsenal. Veuillez réessayer.');
        this.loading.set(false);

        // Fallback avec des données mock
        this.loadMockData();
      }
    });
  }

  loadMockData(): void {
    const mockEquipment: Equipment[] = [
      { name: 'M4A1', category: 'Arme à feu', cost: 1500, pdfBonus: 25, pdcBonus: 0, armBonus: 0, evasionBonus: 0 },
      { name: 'Sniper Rifle', category: 'Arme à feu', cost: 3000, pdfBonus: 50, pdcBonus: 0, armBonus: 0, evasionBonus: 0 },
      { name: 'Body Armor', category: 'Armure', cost: 2000, pdfBonus: 0, pdcBonus: 0, armBonus: 30, evasionBonus: 0 },
      { name: 'Tactical Helmet', category: 'Armure', cost: 1200, pdfBonus: 0, pdcBonus: 0, armBonus: 15, evasionBonus: 0 },
      { name: 'Couteau de combat', category: 'Arme de mêlée', cost: 400, pdfBonus: 0, pdcBonus: 15, armBonus: 0, evasionBonus: 0 },
      { name: 'Tenue légère', category: 'Armure', cost: 800, pdfBonus: 0, pdcBonus: 0, armBonus: 5, evasionBonus: 10 }
    ];
    this.equipment.set(mockEquipment);
  }

  filterEquipment(): void {
    // La logique de filtrage est gérée par le computed signal
  }

  filterByCategory(category: string): void {
    this.selectedCategory = category;
  }

  /**
   * Détermine le type de catégorie pour le filtrage
   */
  getCategoryType(category: string): string {
    const cat = category?.toLowerCase() || '';
    if (cat.includes('arme') || cat.includes('pistolet') || cat.includes('fusil')) return 'weapon';
    if (cat.includes('armure') || cat.includes('gilet') || cat.includes('tenue') || cat.includes('bouclier')) return 'armor';
    return 'other';
  }

  canAfford(item: Equipment): boolean {
    return this.playerGold >= item.cost;
  }

  getEquipmentEmoji(item: Equipment): string {
    const type = this.getCategoryType(item.category);
    if (type === 'weapon') return '⚔️';
    if (type === 'armor') return '🛡️';
    return '📦';
  }

  purchaseEquipment(item: Equipment): void {
    if (!this.canAfford(item)) {
      alert('Crédits insuffisants !');
      return;
    }

    this.purchasing.set(true);

    // Simulation d'achat
    setTimeout(() => {
      this.playerGold -= item.cost;
      this.purchasing.set(false);
      alert(`${item.name} acheté avec succès !`);
    }, 500);
  }
}

