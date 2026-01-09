import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { LoginRequest } from '../../models/auth.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent {
  // Login form
  username = '';
  password = '';
  rememberMe = false;

  // Register form
  registerUsername = '';
  registerPassword = '';
  showRegister = false;

  // State
  loading = signal(false);
  errorMessage = signal('');
  registerError = signal('');
  registerSuccess = signal(false);

  private returnUrl: string = '/';

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/';
  }

  handleLogin(event: Event): void {
    event.preventDefault();

    if (!this.username || !this.password) {
      this.errorMessage.set('Veuillez remplir tous les champs');
      return;
    }

    this.loading.set(true);
    this.errorMessage.set('');

    const loginRequest: LoginRequest = {
      username: this.username,
      password: this.password,
      rememberMe: this.rememberMe
    };

    this.authService.login(loginRequest).subscribe({
      next: () => {
        this.loading.set(false);
        this.router.navigate([this.returnUrl]);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          error.status === 401
            ? 'Identifiants incorrects'
            : 'Erreur de connexion. Veuillez réessayer.'
        );
      }
    });
  }

  handleRegister(event: Event): void {
    event.preventDefault();

    if (!this.registerUsername || !this.registerPassword) {
      this.registerError.set('Veuillez remplir tous les champs');
      return;
    }

    this.loading.set(true);
    this.registerError.set('');
    this.registerSuccess.set(false);

    const registerRequest = {
      username: this.registerUsername,
      password: this.registerPassword
    };

    this.authService.register(registerRequest).subscribe({
      next: () => {
        this.loading.set(false);
        this.registerSuccess.set(true);
        this.registerUsername = '';
        this.registerPassword = '';

        // Auto-close modal after 2 seconds
        setTimeout(() => {
          this.showRegister = false;
          this.registerSuccess.set(false);
        }, 2000);
      },
      error: (error) => {
        this.loading.set(false);
        this.registerError.set(
          error.status === 409
            ? 'Ce nom d\'utilisateur existe déjà'
            : 'Erreur lors de la création du compte'
        );
      }
    });
  }
}

