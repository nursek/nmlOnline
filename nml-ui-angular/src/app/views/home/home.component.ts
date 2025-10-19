import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="home-container">
      <div class="hero-section">
        <div class="hero-content">
          <h1 class="hero-title">
            <span class="gradient-text">Bienvenue dans NML Online</span>
          </h1>
          <p class="hero-subtitle">
            Un jeu de stratégie en ligne où vous contrôlez des territoires,
            gérez vos ressources et affrontez d'autres joueurs.
          </p>
          
          @if (!authService.isAuthenticated()) {
            <div class="hero-actions">
              <a routerLink="/login" class="btn btn-primary btn-large">
                <span>🎮</span>
                Commencer à jouer
              </a>
              <a routerLink="/regles" class="btn btn-secondary btn-large">
                <span>📖</span>
                Voir les règles
              </a>
            </div>
          } @else {
            <div class="hero-actions">
              <a routerLink="/carte" class="btn btn-primary btn-large">
                <span>🗺️</span>
                Aller à la carte
              </a>
              <a routerLink="/joueur" class="btn btn-secondary btn-large">
                <span>👤</span>
                Mon profil
              </a>
            </div>
          }
        </div>

        <div class="hero-illustration">
          <div class="floating-card card-1">
            <span class="card-icon">🗺️</span>
            <span class="card-text">Explorez</span>
          </div>
          <div class="floating-card card-2">
            <span class="card-icon">⚔️</span>
            <span class="card-text">Combattez</span>
          </div>
          <div class="floating-card card-3">
            <span class="card-icon">👑</span>
            <span class="card-text">Dominez</span>
          </div>
        </div>
      </div>

      <div class="features-section">
        <h2 class="section-title">Fonctionnalités principales</h2>
        <div class="features-grid">
          <div class="feature-card">
            <div class="feature-icon">🗺️</div>
            <h3>Carte Interactive</h3>
            <p>Explorez et conquérez des territoires sur une carte dynamique</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">⚔️</div>
            <h3>Combat Stratégique</h3>
            <p>Affrontez d'autres joueurs avec vos unités et équipements</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">🛒</div>
            <h3>Boutique</h3>
            <p>Achetez des équipements pour renforcer vos troupes</p>
          </div>
          <div class="feature-card">
            <div class="feature-icon">👥</div>
            <h3>Gestion d'Unités</h3>
            <p>Recrutez et gérez vos troupes pour étendre votre empire</p>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .home-container {
      min-height: calc(100vh - 80px);
    }

    .hero-section {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 4rem;
      padding: 4rem 2rem;
      max-width: 1400px;
      margin: 0 auto;
      align-items: center;
      min-height: 500px;
    }

    .hero-content {
      animation: fadeInLeft 0.8s ease-out;
    }

    .hero-title {
      font-size: 3.5rem;
      font-weight: 800;
      margin: 0 0 1.5rem 0;
      line-height: 1.2;
    }

    .gradient-text {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    .hero-subtitle {
      font-size: 1.25rem;
      color: #666;
      margin-bottom: 2rem;
      line-height: 1.6;
    }

    .hero-actions {
      display: flex;
      gap: 1rem;
      flex-wrap: wrap;
    }

    .hero-illustration {
      position: relative;
      height: 400px;
      animation: fadeInRight 0.8s ease-out;
    }

    .floating-card {
      position: absolute;
      background: white;
      border-radius: 16px;
      padding: 1.5rem;
      box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      animation: float 3s ease-in-out infinite;
    }

    .card-icon {
      font-size: 3rem;
    }

    .card-text {
      font-weight: 600;
      color: #333;
    }

    .card-1 {
      top: 50px;
      left: 50px;
      animation-delay: 0s;
    }

    .card-2 {
      top: 150px;
      right: 100px;
      animation-delay: 0.5s;
    }

    .card-3 {
      bottom: 80px;
      left: 100px;
      animation-delay: 1s;
    }

    .features-section {
      background: linear-gradient(to bottom, #f8f9fa, white);
      padding: 4rem 2rem;
    }

    .section-title {
      text-align: center;
      font-size: 2.5rem;
      font-weight: 700;
      margin-bottom: 3rem;
      color: #333;
    }

    .features-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
      gap: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .feature-card {
      background: white;
      padding: 2rem;
      border-radius: 16px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
      text-align: center;
      transition: transform 0.3s, box-shadow 0.3s;
    }

    .feature-card:hover {
      transform: translateY(-8px);
      box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
    }

    .feature-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .feature-card h3 {
      font-size: 1.5rem;
      margin-bottom: 0.75rem;
      color: #333;
    }

    .feature-card p {
      color: #666;
      line-height: 1.6;
    }

    .btn {
      padding: 1rem 2rem;
      border-radius: 12px;
      text-decoration: none;
      font-weight: 600;
      font-size: 1.1rem;
      display: inline-flex;
      align-items: center;
      gap: 0.75rem;
      transition: all 0.3s;
      border: none;
      cursor: pointer;
    }

    .btn-large {
      padding: 1.25rem 2.5rem;
      font-size: 1.2rem;
    }

    .btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
    }

    .btn-primary:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 30px rgba(102, 126, 234, 0.6);
    }

    .btn-secondary {
      background: white;
      color: #667eea;
      border: 2px solid #667eea;
    }

    .btn-secondary:hover {
      background: #667eea;
      color: white;
      transform: translateY(-3px);
    }

    @keyframes fadeInLeft {
      from {
        opacity: 0;
        transform: translateX(-30px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    @keyframes fadeInRight {
      from {
        opacity: 0;
        transform: translateX(30px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    @keyframes float {
      0%, 100% {
        transform: translateY(0);
      }
      50% {
        transform: translateY(-20px);
      }
    }

    @media (max-width: 968px) {
      .hero-section {
        grid-template-columns: 1fr;
        gap: 2rem;
        padding: 2rem 1rem;
      }

      .hero-title {
        font-size: 2.5rem;
      }

      .hero-illustration {
        display: none;
      }

      .features-grid {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class HomeComponent {
  constructor(public authService: AuthService) {}
}
