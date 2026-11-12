import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { InstanceService } from '../../services/instance.service';
import { AuthService } from '../../services/auth.service';
import { Board, GameInstance } from '../../models';
import { CreateGameDialogComponent } from './create-game-dialog.component';

@Component({
  selector: 'app-parties',
  imports: [MatCardModule, MatButtonModule, MatDialogModule, MatIconModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="parties">
      <h1>Choisir une partie</h1>
      @if (auth.isAdmin()) {
        <button mat-flat-button color="primary" (click)="createGame()">
          <mat-icon>add</mat-icon>
          Nouvelle partie
        </button>
      }
      @if (instanceService.loading() && instanceService.instances().length === 0) {
        <p>Chargement des parties…</p>
      } @else if (instanceService.error(); as error) {
        <p class="warn">{{ error }}</p>
        <button mat-stroked-button (click)="retry()">Réessayer</button>
      } @else if (instanceService.instances().length === 0) {
        <p>Aucune partie ne vous est attribuée. Contactez l'organisateur.</p>
        <button mat-stroked-button (click)="retry()">Actualiser</button>
      } @else {
        <div class="grid">
          @for (instance of instanceService.instances(); track instance.id) {
            <mat-card class="card" [class.selected]="instance.id === instanceService.selectedId()">
              <mat-card-title>{{ instance.name }}</mat-card-title>
              <mat-card-content>
                <p>
                  @if (instance.currentTurn === 0) {
                    Préparation
                  } @else {
                    Tour {{ instance.currentTurn }}
                  }
                  — {{ instance.playerCount }} joueurs
                </p>
                @if (instance.playerName) {
                  <p>Votre personnage : {{ instance.playerName }}</p>
                }
                @if (instance.eliminated) {
                  <p class="warn">Éliminé</p>
                }
              </mat-card-content>
              <mat-card-actions>
                <button mat-flat-button color="primary" (click)="join(instance)">
                  {{ actionLabel(instance) }}
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
    :host {
      display: block;
    }
    .parties {
      box-sizing: border-box;
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      justify-content: safe center;
      gap: 2.25rem;
      padding: 3rem 1.5rem;
    }
    h1 {
      margin: 0;
      text-align: center;
      font-size: clamp(1.7rem, 1.35rem + 1.5vw, 2.4rem);
      letter-spacing: 0.08em;
    }
    h1::after {
      content: '';
      display: block;
      width: 4.5rem;
      height: 3px;
      margin: 0.9rem auto 0;
      background: var(--accent);
    }
    .grid {
      width: 100%;
      max-width: 62rem;
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(17rem, 21rem));
      justify-content: center;
      gap: 1.25rem;
    }
    .card {
      display: flex;
      flex-direction: column;
      height: 100%;
      padding: 1rem 1.15rem 0.9rem;
      text-align: left;
      transition:
        transform 0.18s ease,
        box-shadow 0.18s ease;
    }
    .card:hover {
      transform: translateY(-3px);
      box-shadow: var(--shadow-pop);
    }
    .card.selected {
      outline: 2px solid var(--accent);
      outline-offset: 3px;
    }
    .card mat-card-title {
      margin-bottom: 0.4rem;
    }
    .card mat-card-content p {
      font-family: var(--font-mono);
      font-size: 0.78rem;
      color: var(--muted);
    }
    .card mat-card-content p + p {
      margin-top: 0.3rem;
    }
    .card mat-card-actions {
      margin-top: auto;
      padding: 0.85rem 0 0;
    }
    .role {
      align-self: flex-end;
      font-size: 0.7rem;
      letter-spacing: 0.12em;
      text-transform: uppercase;
      opacity: 0.7;
    }
    .card mat-card-content p.warn {
      color: var(--danger);
    }
    .warn {
      color: var(--danger);
      text-align: center;
    }
  `,
})
export class PartiesComponent {
  readonly instanceService = inject(InstanceService);
  readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);

  constructor() {
    void this.instanceService.refresh().catch(() => undefined);
  }

  retry(): void {
    void this.instanceService.refresh().catch(() => undefined);
  }

  join(instance: GameInstance): void {
    this.instanceService.enter(instance);
  }

  actionLabel(instance: GameInstance): string {
    if (instance.currentTurn === 0) {
      if (instance.role === 'ADMIN') return 'Préparer';
      if (instance.role === 'MJ') return 'Administrer';
      return 'Entrer en préparation';
    }
    return instance.role === 'MJ' || instance.role === 'ADMIN' ? 'Administrer' : 'Jouer';
  }

  createGame(): void {
    this.dialog
      .open(CreateGameDialogComponent, { width: '520px', maxWidth: '95vw' })
      .afterClosed()
      .subscribe((board: Board | undefined) => {
        if (!board) return;
        void this.instanceService.refresh().then((instances) => {
          const created = instances.find((candidate) => candidate.id === board.id);
          if (created) this.instanceService.enter(created);
        });
      });
  }
}
