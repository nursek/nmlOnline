import { Component, ChangeDetectionStrategy } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-not-found',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule],
  template: `
    <div class="not-found-container">
      <mat-card class="not-found-card">
        <mat-card-content>
          <mat-icon class="not-found-icon">travel_explore</mat-icon>
          <h1>404</h1>
          <p>La page demandée est introuvable.</p>
          <button mat-raised-button color="primary" routerLink="/carte">
            <mat-icon>map</mat-icon>
            Retour à la carte
          </button>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [
    `
      .not-found-container {
        min-height: 100vh;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 24px;
      }
      .not-found-card {
        text-align: center;
        max-width: 420px;
        width: 100%;
        padding: 32px 24px;
      }
      .not-found-icon {
        font-size: 64px;
        width: 64px;
        height: 64px;
        color: #6366f1;
        margin-bottom: 16px;
      }
      h1 {
        font-size: 4rem;
        margin: 0 0 8px;
        font-weight: 700;
        color: #1e293b;
      }
      p {
        color: #64748b;
        margin-bottom: 24px;
      }
    `,
  ],
})
export class NotFoundComponent {}
