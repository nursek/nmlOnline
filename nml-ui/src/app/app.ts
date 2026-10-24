import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AuthService } from './services/auth.service';
import { PlayerStatusService } from './services/player-status.service';
import { ShopService } from './services/shop.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, MatIconModule, NavbarComponent],
  template: `
    <div class="app-container">
      <app-navbar></app-navbar>
      @if (playerStatus.eliminated()) {
        <div class="readonly-banner" role="status">
          <mat-icon>dangerous</mat-icon>
          <span>
            <strong>Joueur éliminé</strong> — consultation uniquement, toutes les actions sont
            désactivées.
          </span>
        </div>
      }
      <main class="main-content" [class.readonly-mode]="playerStatus.eliminated()">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [
    `
      .app-container {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
      }

      .main-content {
        flex: 1;
      }

      .readonly-banner {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 16px;
        background: color-mix(in srgb, var(--danger) 14%, var(--surface));
        border-bottom: 1.5px solid color-mix(in srgb, var(--danger) 45%, var(--line));
        color: var(--ink);
        font-size: 0.85rem;

        mat-icon {
          color: var(--danger);
        }
      }
    `,
  ],
})
export class App {
  private readonly auth = inject(AuthService);
  private readonly shop = inject(ShopService);
  readonly playerStatus = inject(PlayerStatusService);

  constructor() {
    void this.auth.initSession();
    void this.shop;
  }
}
