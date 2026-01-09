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
      items = items.filter(item => item.type === this.selectedCategory);
    }

    // Filter by search query
    if (this.searchQuery.trim()) {
      const query = this.searchQuery.toLowerCase();
      items = items.filter(item =>
        item.name.toLowerCase().includes(query) ||
        item.description?.toLowerCase().includes(query)
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
      { id: 1, name: 'M4A1', type: 'weapon', description: 'Fusil d\'assaut standard', price: 1500, attack: 25, defense: 0 },
      { id: 2, name: 'Sniper Rifle', type: 'weapon', description: 'Fusil de précision longue portée', price: 3000, attack: 50, defense: 0 },
      { id: 3, name: 'Body Armor', type: 'armor', description: 'Gilet pare-balles renforcé', price: 2000, attack: 0, defense: 30 },
      { id: 4, name: 'Tactical Helmet', type: 'armor', description: 'Casque tactique avec vision nocturne', price: 1200, attack: 0, defense: 15 },
      { id: 5, name: 'LMG', type: 'weapon', description: 'Mitrailleuse légère de suppression', price: 4000, attack: 40, defense: 0 },
      { id: 6, name: 'Combat Vest', type: 'armor', description: 'Veste tactique avec poches', price: 800, attack: 0, defense: 10 }
    ];
    this.equipment.set(mockEquipment);
  }

  filterEquipment(): void {
    // La logique de filtrage est gérée par le computed signal
  }

  filterByCategory(category: string): void {
    this.selectedCategory = category;
  }

  canAfford(item: Equipment): boolean {
    return this.playerGold >= item.price;
  }

  getEquipmentEmoji(item: Equipment): string {
    if (item.type === 'weapon') return '⚔️';
    if (item.type === 'armor') return '🛡️';
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
      this.playerGold -= item.price;
      this.purchasing.set(false);
      alert(`${item.name} acheté avec succès !`);
    }, 500);
  }
}

