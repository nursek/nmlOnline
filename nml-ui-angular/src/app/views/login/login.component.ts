import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <h1>🎮</h1>
          <h2>Connexion</h2>
          <p>Connectez-vous pour accéder au jeu</p>
        </div>

        <form (ngSubmit)="handleLogin($event)" class="login-form">
          <div class="form-group">
            <label for="username">Nom d'utilisateur</label>
            <div class="input-wrapper">
              <span class="input-icon">👤</span>
              <input
                type="text"
                id="username"
                [(ngModel)]="username"
                name="username"
                placeholder="Entrez votre nom d'utilisateur"
                [disabled]="loading()"
                required
              />
            </div>
          </div>

          <div class="form-group">
            <label for="password">Mot de passe</label>
            <div class="input-wrapper">
              <span class="input-icon">🔒</span>
              <input
                type="password"
                id="password"
                [(ngModel)]="password"
                name="password"
                placeholder="Entrez votre mot de passe"
                [disabled]="loading()"
                required
              />
            </div>
          </div>

          <div class="form-check">
            <input
              type="checkbox"
              id="rememberMe"
              [(ngModel)]="rememberMe"
              name="rememberMe"
            />
            <label for="rememberMe">Se souvenir de moi</label>
          </div>

          @if (errorMessage()) {
            <div class="alert alert-error">
              <span class="alert-icon">⚠️</span>
              <span>{{ errorMessage() }}</span>
            </div>
          }

          <button 
            type="submit" 
            class="btn btn-primary btn-block"
            [disabled]="loading()"
          >
            @if (loading()) {
              <span class="spinner"></span>
              <span>Connexion en cours...</span>
            } @else {
              <span>Se connecter</span>
            }
          </button>
        </form>

        <div class="login-footer">
          <p>Pas encore de compte ?</p>
          <button 
            type="button" 
            class="btn btn-link"
            (click)="showRegister = true"
            [disabled]="loading()"
          >
            Créer un compte
          </button>
        </div>
      </div>

      @if (showRegister) {
        <div class="register-modal" (click)="showRegister = false">
          <div class="register-card" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h3>Créer un compte</h3>
              <button class="btn-close" (click)="showRegister = false">×</button>
            </div>
            
            <form (ngSubmit)="handleRegister($event)" class="register-form">
              <div class="form-group">
                <label for="reg-username">Nom d'utilisateur</label>
                <input
                  type="text"
                  id="reg-username"
                  [(ngModel)]="registerUsername"
                  name="registerUsername"
                  placeholder="Choisissez un nom d'utilisateur"
                  required
                />
              </div>

              <div class="form-group">
                <label for="reg-password">Mot de passe</label>
                <input
                  type="password"
                  id="reg-password"
                  [(ngModel)]="registerPassword"
                  name="registerPassword"
                  placeholder="Choisissez un mot de passe"
                  required
                />
              </div>

              @if (registerError()) {
                <div class="alert alert-error">
                  <span class="alert-icon">⚠️</span>
                  <span>{{ registerError() }}</span>
                </div>
              }

              @if (registerSuccess()) {
                <div class="alert alert-success">
                  <span class="alert-icon">✅</span>
                  <span>Compte créé avec succès ! Vous pouvez maintenant vous connecter.</span>
                </div>
              }

              <div class="modal-actions">
                <button type="button" class="btn btn-secondary" (click)="showRegister = false">
                  Annuler
                </button>
                <button type="submit" class="btn btn-primary" [disabled]="loading()">
                  @if (loading()) {
                    <span class="spinner"></span>
                  }
                  <span>Créer</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .login-container {
      min-height: calc(100vh - 80px);
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 2rem;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    .login-card {
      background: white;
      border-radius: 24px;
      padding: 3rem;
      width: 100%;
      max-width: 450px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      animation: slideIn 0.5s ease-out;
    }

    .login-header {
      text-align: center;
      margin-bottom: 2rem;
    }

    .login-header h1 {
      font-size: 4rem;
      margin: 0;
    }

    .login-header h2 {
      font-size: 2rem;
      margin: 1rem 0 0.5rem 0;
      color: #333;
    }

    .login-header p {
      color: #666;
      margin: 0;
    }

    .login-form {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .form-group label {
      font-weight: 600;
      color: #333;
      font-size: 0.95rem;
    }

    .input-wrapper {
      position: relative;
      display: flex;
      align-items: center;
    }

    .input-icon {
      position: absolute;
      left: 1rem;
      font-size: 1.2rem;
      pointer-events: none;
    }

    input[type="text"],
    input[type="password"] {
      width: 100%;
      padding: 1rem 1rem 1rem 3rem;
      border: 2px solid #e0e0e0;
      border-radius: 12px;
      font-size: 1rem;
      transition: all 0.3s;
      background: #f8f9fa;
    }

    input[type="text"]:focus,
    input[type="password"]:focus {
      outline: none;
      border-color: #667eea;
      background: white;
      box-shadow: 0 0 0 4px rgba(102, 126, 234, 0.1);
    }

    input:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .form-check {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    .form-check input[type="checkbox"] {
      width: 18px;
      height: 18px;
      cursor: pointer;
    }

    .form-check label {
      cursor: pointer;
      user-select: none;
      color: #666;
    }

    .alert {
      padding: 1rem;
      border-radius: 12px;
      display: flex;
      align-items: center;
      gap: 0.75rem;
      animation: slideDown 0.3s ease-out;
    }

    .alert-error {
      background: #fee;
      color: #c33;
      border: 1px solid #fcc;
    }

    .alert-success {
      background: #efe;
      color: #3a3;
      border: 1px solid #cfc;
    }

    .alert-icon {
      font-size: 1.2rem;
    }

    .btn {
      padding: 1rem 2rem;
      border: none;
      border-radius: 12px;
      font-weight: 600;
      font-size: 1rem;
      cursor: pointer;
      transition: all 0.3s;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
    }

    .btn-primary {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
    }

    .btn-primary:hover:not(:disabled) {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
    }

    .btn-primary:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }

    .btn-block {
      width: 100%;
    }

    .btn-link {
      background: none;
      color: #667eea;
      padding: 0.5rem;
    }

    .btn-link:hover:not(:disabled) {
      text-decoration: underline;
    }

    .btn-secondary {
      background: #e0e0e0;
      color: #333;
    }

    .btn-secondary:hover {
      background: #d0d0d0;
    }

    .spinner {
      width: 16px;
      height: 16px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.6s linear infinite;
    }

    .login-footer {
      text-align: center;
      margin-top: 1.5rem;
      padding-top: 1.5rem;
      border-top: 1px solid #e0e0e0;
    }

    .login-footer p {
      color: #666;
      margin: 0 0 0.5rem 0;
    }

    .register-modal {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.7);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.3s ease-out;
    }

    .register-card {
      background: white;
      border-radius: 20px;
      padding: 2rem;
      width: 90%;
      max-width: 400px;
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      animation: scaleIn 0.3s ease-out;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 1.5rem;
    }

    .modal-header h3 {
      margin: 0;
      color: #333;
    }

    .btn-close {
      background: none;
      border: none;
      font-size: 2rem;
      cursor: pointer;
      color: #999;
      padding: 0;
      width: 32px;
      height: 32px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      transition: all 0.2s;
    }

    .btn-close:hover {
      background: #f0f0f0;
      color: #333;
    }

    .register-form {
      display: flex;
      flex-direction: column;
      gap: 1.5rem;
    }

    .register-form input {
      padding-left: 1rem;
    }

    .modal-actions {
      display: flex;
      gap: 1rem;
      justify-content: flex-end;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateY(30px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    @keyframes fadeIn {
      from {
        opacity: 0;
      }
      to {
        opacity: 1;
      }
    }

    @keyframes scaleIn {
      from {
        transform: scale(0.9);
        opacity: 0;
      }
      to {
        transform: scale(1);
        opacity: 1;
      }
    }

    @keyframes spin {
      to {
        transform: rotate(360deg);
      }
    }

    @media (max-width: 576px) {
      .login-card {
        padding: 2rem 1.5rem;
      }

      .login-header h2 {
        font-size: 1.5rem;
      }
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  rememberMe = false;
  
  registerUsername = '';
  registerPassword = '';
  showRegister = false;
  
  loading = signal(false);
  errorMessage = signal<string | null>(null);
  registerError = signal<string | null>(null);
  registerSuccess = signal(false);

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  handleLogin(event: Event): void {
    event.preventDefault();
    
    if (!this.username || !this.password) {
      this.errorMessage.set('Veuillez remplir tous les champs');
      return;
    }

    const credentials: LoginRequest = {
      username: this.username,
      password: this.password,
      rememberMe: this.rememberMe
    };

    this.loading.set(true);
    this.errorMessage.set(null);

    this.authService.login(credentials).subscribe({
      next: () => {
        this.loading.set(false);
        const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/joueur';
        this.router.navigate([returnUrl]);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401) {
          this.errorMessage.set('Identifiants incorrects');
        } else if (err.status === 429) {
          this.errorMessage.set('Trop de tentatives. Veuillez réessayer plus tard.');
        } else {
          this.errorMessage.set('Erreur de connexion. Veuillez réessayer.');
        }
      }
    });
  }

  handleRegister(event: Event): void {
    event.preventDefault();
    
    if (!this.registerUsername || !this.registerPassword) {
      this.registerError.set('Veuillez remplir tous les champs');
      return;
    }

    if (this.registerPassword.length < 4) {
      this.registerError.set('Le mot de passe doit contenir au moins 4 caractères');
      return;
    }

    this.loading.set(true);
    this.registerError.set(null);
    this.registerSuccess.set(false);

    this.authService.register({
      username: this.registerUsername,
      password: this.registerPassword
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.registerSuccess.set(true);
        this.registerUsername = '';
        this.registerPassword = '';
        
        // Fermer la modal après 2 secondes
        setTimeout(() => {
          this.showRegister = false;
          this.registerSuccess.set(false);
        }, 2000);
      },
      error: (err) => {
        this.loading.set(false);
        if (err.status === 409) {
          this.registerError.set('Ce nom d\'utilisateur existe déjà');
        } else {
          this.registerError.set('Erreur lors de la création du compte');
        }
      }
    });
  }
}
