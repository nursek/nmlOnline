import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-regles',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatListModule,
    MatDividerModule,
  ],
  template: `
    <div class="container fade-in">
      <!-- Header -->
      <div class="page-header">
        <div class="avatar">
          <mat-icon>menu_book</mat-icon>
        </div>
        <div>
          <h1 class="title">Règles du Jeu</h1>
          <p class="subtitle">Guide complet de NML Online</p>
        </div>
      </div>

      <!-- But du jeu -->
      <mat-card class="card highlight">
        <mat-card-content>
          <div class="section-header">
            <mat-icon color="primary">my_location</mat-icon>
            <h2>But du jeu</h2>
          </div>
          <p class="text">
            Contrôler des territoires et gérer les ressources pour devenir le joueur le plus puissant.
            Votre objectif est de conquérir et de maintenir le contrôle du plus grand nombre de territoires
            possible avant la fin du temps imparti.
          </p>
        </mat-card-content>
      </mat-card>

      <!-- Déroulement -->
      <mat-card class="card">
        <mat-card-content>
          <div class="section-header">
            <mat-icon color="primary">people</mat-icon>
            <h2>Déroulement</h2>
          </div>
          <p class="section-subtitle">Comment se déroule une partie</p>

          <mat-list>
            <mat-list-item class="step-item">
              <div class="step-number">1</div>
              <div class="step-content">
                <h3>Recrutement de troupes</h3>
                <p>Les joueurs recrutent des troupes pour renforcer leur armée. Chaque unité
                possède des caractéristiques spécifiques comme des points de vie et des points
                de mouvement.</p>
              </div>
            </mat-list-item>

            <mat-list-item class="step-item">
              <div class="step-number">2</div>
              <div class="step-content">
                <h3>Achat d'équipements</h3>
                <p>Visitez la boutique pour acheter des équipements qui amélioreront les capacités
                de vos unités. Les équipements offrent des bonus comme la force de frappe (PDF),
                la défense (PDC), l'armure (ARM) et l'évasion (ESQ).</p>
              </div>
            </mat-list-item>

            <mat-list-item class="step-item">
              <div class="step-number">3</div>
              <div class="step-content">
                <h3>Capture de territoires</h3>
                <p>Utilisez vos troupes pour capturer des territoires ennemis ou neutres.
                Chaque territoire contrôlé augmente votre influence et peut fournir des
                ressources précieuses.</p>
              </div>
            </mat-list-item>
          </mat-list>

          <div class="combat-box">
            <div class="combat-header">
              <mat-icon color="primary">sports_esports</mat-icon>
              <h3>Système de combat</h3>
            </div>
            <p>Chaque unité possède des points de vie et de mouvement. Les combats se font en
            comparant la force des troupes opposées, en tenant compte des équipements et
            des bonus de territoire. La stratégie et le positionnement sont essentiels
            pour remporter la victoire !</p>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Conditions de victoire -->
      <mat-card class="card victory">
        <mat-card-content>
          <div class="section-header">
            <mat-icon class="gold">emoji_events</mat-icon>
            <h2>Conditions de victoire</h2>
          </div>
          <p class="text">
            Le joueur ayant le <strong class="primary">plus de territoires</strong> à
            la fin du temps imparti remporte la partie.
          </p>

          <div class="podium">
            <div class="podium-item gold-bg">
              <span class="emoji">🥇</span>
              <h4>1ère Place</h4>
              <p>Le commandant avec le plus de territoires</p>
            </div>
            <div class="podium-item silver-bg">
              <span class="emoji">🥈</span>
              <h4>2ème Place</h4>
              <p>Le deuxième plus grand conquérant</p>
            </div>
            <div class="podium-item bronze-bg">
              <span class="emoji">🥉</span>
              <h4>3ème Place</h4>
              <p>Le troisième commandant</p>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <!-- Conseils stratégiques -->
      <mat-card class="card">
        <mat-card-content>
          <div class="section-header">
            <mat-icon color="primary">tips_and_updates</mat-icon>
            <h2>Conseils stratégiques</h2>
          </div>
          <p class="section-subtitle">Pour devenir un grand conquérant</p>

          <div class="tips-grid">
            <div class="tip-item">
              <div class="tip-header">
                <mat-icon color="primary">shopping_bag</mat-icon>
                <h4>Équipez intelligemment</h4>
              </div>
              <p>Investissez dans des équipements adaptés à vos unités. Un bon équipement peut
              faire la différence dans les batailles critiques.</p>
            </div>

            <div class="tip-item">
              <div class="tip-header">
                <mat-icon color="primary">place</mat-icon>
                <h4>Contrôlez les ressources</h4>
              </div>
              <p>Les territoires génèrent des revenus. Plus vous en contrôlez, plus vous
              pourrez investir dans votre armée.</p>
            </div>

            <div class="tip-item">
              <div class="tip-header">
                <mat-icon color="primary">security</mat-icon>
                <h4>Défendez vos positions</h4>
              </div>
              <p>Ne vous concentrez pas uniquement sur l'attaque. Assurez-vous que vos
              territoires sont bien défendus contre les invasions.</p>
            </div>

            <div class="tip-item">
              <div class="tip-header">
                <mat-icon color="primary">people</mat-icon>
                <h4>Gérez vos ressources</h4>
              </div>
              <p>Ne dépensez pas tout votre argent d'un coup. Gardez une réserve pour
              réagir aux opportunités et menaces.</p>
            </div>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .container {
      max-width: 1000px;
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

      &.highlight {
        border: 2px solid #6366f1;
      }

      &.victory {
        border: 2px solid #f59e0b;
        background: linear-gradient(135deg, rgba(245, 158, 11, 0.05) 0%, rgba(245, 158, 11, 0.02) 100%);
      }
    }

    .section-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 16px;

      h2 {
        margin: 0;
        font-size: 1.25rem;
        font-weight: 600;
      }

      .gold {
        color: #f59e0b;
      }
    }

    .section-subtitle {
      color: #64748b;
      margin: 0 0 24px;
      font-size: 0.875rem;
    }

    .text {
      line-height: 1.8;
      margin: 0;

      .primary {
        color: #6366f1;
      }
    }

    .step-item {
      height: auto !important;
      padding: 16px 0 !important;
      align-items: flex-start !important;
    }

    .step-number {
      width: 40px;
      height: 40px;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: 700;
      flex-shrink: 0;
      margin-right: 16px;
    }

    .step-content {
      h3 {
        margin: 0 0 8px;
        font-weight: 600;
      }

      p {
        margin: 0;
        color: #64748b;
        font-size: 0.875rem;
      }
    }

    .combat-box {
      padding: 24px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      border-radius: 8px;
      margin-top: 24px;

      .combat-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 12px;

        h3 {
          margin: 0;
          font-weight: 600;
        }
      }

      p {
        margin: 0;
        color: #64748b;
        font-size: 0.875rem;
      }
    }

    .podium {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 16px;
      margin-top: 24px;
    }

    .podium-item {
      padding: 24px;
      border-radius: 8px;
      text-align: center;
      border: 1px solid;

      &.gold-bg {
        background: rgba(245, 158, 11, 0.1);
        border-color: #f59e0b;
      }

      &.silver-bg {
        background: rgba(156, 163, 175, 0.1);
        border-color: #9ca3af;
      }

      &.bronze-bg {
        background: rgba(249, 115, 22, 0.1);
        border-color: #f97316;
      }

      .emoji {
        font-size: 2rem;
        display: block;
        margin-bottom: 8px;
      }

      h4 {
        margin: 0 0 8px;
        font-weight: 600;
      }

      p {
        margin: 0;
        font-size: 0.875rem;
        color: #64748b;
      }
    }

    .tips-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 16px;
    }

    .tip-item {
      padding: 16px;
      background: #f8fafc;
      border-radius: 8px;

      .tip-header {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 8px;

        h4 {
          margin: 0;
          font-weight: 600;
        }
      }

      p {
        margin: 0;
        font-size: 0.875rem;
        color: #64748b;
      }
    }

    .fade-in {
      animation: fadeIn 0.3s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }
  `]
})
export class ReglesComponent {}
