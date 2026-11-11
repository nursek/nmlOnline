import { ChangeDetectionStrategy, Component, effect, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { InstanceService } from '../../services/instance.service';
import { GameInstance } from '../../models';

@Component({
  selector: 'app-parties',
  imports: [MatCardModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="parties">
      <h1>Choisir une partie</h1>
      @if (instanceService.loading()) {
        <p>Chargement des parties…</p>
      } @else if (instanceService.instances().length === 0) {
        <p>Aucune partie ne vous est attribuée. Contactez l'organisateur.</p>
      } @else {
        <div class="grid">
          @for (instance of instanceService.instances(); track instance.id) {
            <mat-card class="card" [class.selected]="instance.id === instanceService.selectedId()">
              <mat-card-title>{{ instance.name }}</mat-card-title>
              <mat-card-content>
                <p>Tour {{ instance.currentTurn }} — {{ instance.playerCount }} joueurs</p>
                @if (instance.playerName) {
                  <p>Votre personnage : {{ instance.playerName }}</p>
                }
                @if (instance.eliminated) {
                  <p class="warn">Éliminé</p>
                }
              </mat-card-content>
              <mat-card-actions>
                <button mat-flat-button color="primary" (click)="join(instance)">
                  {{
                    instance.role === 'MJ' || instance.role === 'ADMIN' ? 'Administrer' : 'Jouer'
                  }}
                </button>
              </mat-card-actions>
              <span class="role">{{ instance.role }}</span>
            </mat-card>
          }
        </div>
      }
    </section>
  `,
  styles: `
    .parties {
      padding: 1.5rem;
    }
    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(18rem, 1fr));
      gap: 1rem;
    }
    .card {
      padding: 0.5rem;
    }
    .card.selected {
      outline: 2px solid var(--mat-sys-primary, #6750a4);
    }
    .role {
      margin-left: auto;
      font-size: 0.75rem;
      opacity: 0.7;
    }
    .warn {
      color: #b3261e;
    }
  `,
})
export class PartiesComponent {
  readonly instanceService = inject(InstanceService);
  private readonly router = inject(Router);
  private redirected = false;

  constructor() {
    // Une seule partie : elle se choisit toute seule, l'écran ne sert que s'il y a un choix.
    effect(() => {
      const sole = this.instanceService.soleInstance();
      if (sole === null || this.redirected) return;
      this.redirected = true;
      this.instanceService.select(sole);
      void this.router.navigate([targetOf(sole)]);
    });
  }

  join(instance: GameInstance): void {
    this.instanceService.select(instance);
    // Rechargement complet : tous les services racine (carte, joueurs, stats) sont liés à la partie.
    window.location.assign(targetOf(instance));
  }
}

function targetOf(instance: GameInstance): string {
  const admin = instance.role === 'ADMIN' || instance.role === 'MJ';
  return admin ? '/admin' : '/accueil';
}
