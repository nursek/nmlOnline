import { Component, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <nav class="navbar">
      <div class="navbar-container">
        <a routerLink="/" class="navbar-brand">
          <span class="brand-icon">🎮</span>
          <span class="brand-text">NML Online</span>
        </a>
        
        <div class="navbar-menu">
          <a routerLink="/carte" routerLinkActive="active" class="nav-link">
            <span class="nav-icon">🗺️</span>
            <span>Carte</span>
          </a>
          <a routerLink="/joueur" routerLinkActive="active" class="nav-link">
            <span class="nav-icon">👤</span>
            <span>Joueur</span>
          </a>
          <a routerLink="/boutique" routerLinkActive="active" class="nav-link">
            <span class="nav-icon">🛒</span>
            <span>Boutique</span>
          </a>
          <a routerLink="/regles" routerLinkActive="active" class="nav-link">
            <span class="nav-icon">📖</span>
            <span>Règles</span>
          </a>
        </div>

        <div class="navbar-user">
          @if (authService.isAuthenticated()) {
            <div class="user-info">
              <span class="user-avatar">{{ getUserInitial() }}</span>
              <span class="user-name">{{ authService.currentUser()?.username }}</span>
            </div>
            <button class="btn btn-outline" (click)="logout()">
              Déconnexion
            </button>
          } @else {
            <button class="btn btn-primary" (click)="goToLogin()">
              Connexion
            </button>
          }
        </div>
      </div>
    </nav>
  `,
  styles: [`
    .navbar {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
      padding: 0;
      position: sticky;
      top: 0;
      z-index: 1000;
    }

    .navbar-container {
      max-width: 1400px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 1rem 2rem;
      gap: 2rem;
    }

    .navbar-brand {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      text-decoration: none;
      color: white;
      font-size: 1.5rem;
      font-weight: bold;
      transition: transform 0.2s;
    }

    .navbar-brand:hover {
      transform: scale(1.05);
    }

    .brand-icon {
      font-size: 2rem;
      filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
    }

    .brand-text {
      letter-spacing: 0.5px;
    }

    .navbar-menu {
      display: flex;
      gap: 0.5rem;
      flex: 1;
      justify-content: center;
    }

    .nav-link {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.75rem 1.25rem;
      color: rgba(255, 255, 255, 0.9);
      text-decoration: none;
      border-radius: 8px;
      transition: all 0.3s;
      font-weight: 500;
      position: relative;
    }

    .nav-link:hover {
      background: rgba(255, 255, 255, 0.15);
      color: white;
      transform: translateY(-2px);
    }

    .nav-link.active {
      background: rgba(255, 255, 255, 0.25);
      color: white;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
    }

    .nav-link.active::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 50%;
      transform: translateX(-50%);
      width: 60%;
      height: 3px;
      background: white;
      border-radius: 2px 2px 0 0;
    }

    .nav-icon {
      font-size: 1.2rem;
    }

    .navbar-user {
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .user-info {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.5rem 1rem;
      background: rgba(255, 255, 255, 0.15);
      border-radius: 25px;
      backdrop-filter: blur(10px);
    }

    .user-avatar {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: bold;
      font-size: 1rem;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
    }

    .user-name {
      color: white;
      font-weight: 500;
    }

    .btn {
      padding: 0.65rem 1.5rem;
      border: none;
      border-radius: 8px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
      font-size: 0.95rem;
    }

    .btn-primary {
      background: white;
      color: #667eea;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }

    .btn-primary:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.25);
    }

    .btn-outline {
      background: transparent;
      color: white;
      border: 2px solid rgba(255, 255, 255, 0.6);
    }

    .btn-outline:hover {
      background: rgba(255, 255, 255, 0.15);
      border-color: white;
    }

    @media (max-width: 768px) {
      .navbar-container {
        flex-wrap: wrap;
        padding: 1rem;
      }

      .navbar-menu {
        order: 3;
        width: 100%;
        justify-content: space-around;
        margin-top: 1rem;
        padding-top: 1rem;
        border-top: 1px solid rgba(255, 255, 255, 0.2);
      }

      .nav-link {
        flex-direction: column;
        padding: 0.5rem;
        font-size: 0.85rem;
      }

      .user-name {
        display: none;
      }
    }
  `]
})
export class NavbarComponent {
  constructor(
    public authService: AuthService,
    private router: Router
  ) {}

  getUserInitial(): string {
    const username = this.authService.currentUser()?.username;
    return username ? username.charAt(0).toUpperCase() : '';
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.router.navigate(['/']);
      },
      error: (err) => {
        console.error('Erreur lors de la déconnexion:', err);
      }
    });
  }
}
