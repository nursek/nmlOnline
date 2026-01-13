import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../services/auth.service';
import { PlayerService } from '../../services/player.service';
import { EquipmentService } from '../../services/equipment.service';
import { LoginRequest, Player, Equipment } from '../../models';

/**
 * Exemple de composant démontrant l'utilisation des services API
 */
@Component({
  selector: 'app-api-example',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="api-example">
      <h2>Exemple d'utilisation de l'API</h2>

      <!-- Section Authentification -->
      <section class="auth-section">
        <h3>Authentification</h3>
        @if (!authService.isAuthenticated()) {
          <div class="login-form">
            <input
              type="text"
              [(ngModel)]="username"
              placeholder="Nom d'utilisateur"
            />
            <input
              type="password"
              [(ngModel)]="password"
              placeholder="Mot de passe"
            />
            <label>
              <input type="checkbox" [(ngModel)]="rememberMe" />
              Se souvenir de moi
            </label>
            <button (click)="login()">Se connecter</button>
            <button (click)="register()">S'inscrire</button>
          </div>
        } @else {
          <div class="user-info">
            <p>Connecté en tant que: {{ authService.currentUser()?.username }}</p>
            <button (click)="logout()">Se déconnecter</button>
          </div>
        }
      </section>

      <!-- Section Joueurs -->
      @if (authService.isAuthenticated()) {
        <section class="players-section">
          <h3>Joueurs</h3>
          <button (click)="loadPlayers()">Charger les joueurs</button>
          @if (loading()) {
            <p>Chargement...</p>
          } @else if (players().length > 0) {
            <ul>
              @for (player of players(); track player.id) {
                <li>{{ player.name }} (ID: {{ player.id }})</li>
              }
            </ul>
          }
        </section>

        <!-- Section Équipements -->
        <section class="equipment-section">
          <h3>Équipements</h3>
          <button (click)="loadEquipment()">Charger les équipements</button>
          @if (loading()) {
            <p>Chargement...</p>
          } @else if (equipment().length > 0) {
            <ul>
              @for (item of equipment(); track item.name) {
                <li>{{ item.name }} ({{ item.category }} - {{ item.cost }}$)</li>
              }
            </ul>
          }
        </section>
      }

      <!-- Messages d'erreur -->
      @if (error()) {
        <div class="error-message">
          {{ error() }}
        </div>
      }
    </div>
  `,
  styles: [`
    .api-example {
      padding: 20px;
      max-width: 800px;
      margin: 0 auto;
    }

    section {
      margin: 20px 0;
      padding: 15px;
      border: 1px solid #ddd;
      border-radius: 5px;
    }

    h2, h3 {
      color: #333;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 10px;
      max-width: 300px;
    }

    input[type="text"],
    input[type="password"] {
      padding: 8px;
      border: 1px solid #ccc;
      border-radius: 4px;
    }

    button {
      padding: 10px 15px;
      background-color: #007bff;
      color: white;
      border: none;
      border-radius: 4px;
      cursor: pointer;
    }

    button:hover {
      background-color: #0056b3;
    }

    .error-message {
      padding: 10px;
      background-color: #f8d7da;
      color: #721c24;
      border: 1px solid #f5c6cb;
      border-radius: 4px;
      margin-top: 10px;
    }

    .user-info {
      padding: 10px;
      background-color: #d4edda;
      border: 1px solid #c3e6cb;
      border-radius: 4px;
    }

    ul {
      list-style: none;
      padding: 0;
    }

    li {
      padding: 8px;
      margin: 5px 0;
      background-color: #f8f9fa;
      border-radius: 4px;
    }
  `]
})
export class ApiExampleComponent implements OnInit {
  // Credentials pour la connexion
  username = '';
  password = '';
  rememberMe = false;

  // Signals pour gérer l'état
  players = signal<Player[]>([]);
  equipment = signal<Equipment[]>([]);
  loading = signal(false);
  error = signal<string | null>(null);

  constructor(
    public authService: AuthService,
    private playerService: PlayerService,
    private equipmentService: EquipmentService
  ) {}

  ngOnInit(): void {
    // Charger les données si l'utilisateur est déjà connecté
    if (this.authService.isAuthenticated()) {
      this.loadPlayers();
      this.loadEquipment();
    }
  }

  /**
   * Connexion
   */
  login(): void {
    if (!this.username || !this.password) {
      this.error.set('Veuillez remplir tous les champs');
      return;
    }

    const credentials: LoginRequest = {
      username: this.username,
      password: this.password,
      rememberMe: this.rememberMe
    };

    this.loading.set(true);
    this.error.set(null);

    this.authService.login(credentials).subscribe({
      next: () => {
        this.loading.set(false);
        this.loadPlayers();
        this.loadEquipment();
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('Erreur de connexion: ' + (err.error?.message || err.message));
      }
    });
  }

  /**
   * Inscription
   */
  register(): void {
    if (!this.username || !this.password) {
      this.error.set('Veuillez remplir tous les champs');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.register({ username: this.username, password: this.password }).subscribe({
      next: () => {
        this.loading.set(false);
        this.error.set(null);
        alert('Inscription réussie! Vous pouvez maintenant vous connecter.');
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set('Erreur d\'inscription: ' + (err.error?.message || err.message));
      }
    });
  }

  /**
   * Déconnexion
   */
  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.players.set([]);
        this.equipment.set([]);
        this.username = '';
        this.password = '';
        this.rememberMe = false;
      },
      error: (err) => {
        this.error.set('Erreur de déconnexion: ' + err.message);
      }
    });
  }

  /**
   * Charge la liste des joueurs
   */
  loadPlayers(): void {
    this.loading.set(true);
    this.error.set(null);

    this.playerService.getAll().subscribe({
      next: (data) => {
        this.players.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des joueurs: ' + err.message);
        this.loading.set(false);
      }
    });
  }

  /**
   * Charge la liste des équipements
   */
  loadEquipment(): void {
    this.loading.set(true);
    this.error.set(null);

    this.equipmentService.getAll().subscribe({
      next: (data) => {
        this.equipment.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Erreur lors du chargement des équipements: ' + err.message);
        this.loading.set(false);
      }
    });
  }
}
