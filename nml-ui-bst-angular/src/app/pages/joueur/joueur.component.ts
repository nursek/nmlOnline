import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Store } from '@ngrx/store';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { selectUser, selectCurrentPlayer, selectPlayerLoading, selectPlayerError, PlayerActions } from '../../store';
import { filter, take } from 'rxjs/operators';
import { Player } from '../../models';

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
  ],
  templateUrl: './joueur.component.html',
  styleUrls: ['./joueur.component.scss']
})
export class JoueurComponent implements OnInit {
  private readonly store = inject(Store);

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
