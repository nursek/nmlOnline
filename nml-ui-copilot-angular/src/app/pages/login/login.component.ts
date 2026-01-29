import { Component, inject, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Store } from '@ngrx/store';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthActions } from '../../store/auth/auth.actions';
import { selectAuthLoading, selectAuthError, selectIsAuthenticated } from '../../store/auth/auth.selectors';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  template: `
    <div class="login-page">
      <div class="login-container">
        <mat-card class="login-card">
          <mat-card-content>
            <!-- Logo et titre -->
            <div class="header">
              <div class="avatar">
                <mat-icon>shield</mat-icon>
              </div>
              <h1 class="title">NML Online</h1>
              <p class="subtitle">Connectez-vous pour conquérir des territoires</p>
            </div>

            <!-- Formulaire -->
            <form [formGroup]="loginForm" (ngSubmit)="onSubmit()">
              <!-- Erreur -->
              @if (error$ | async; as error) {
                <div class="error-alert">
                  <mat-icon>error</mat-icon>
                  {{ error }}
                </div>
              }

              <!-- Nom d'utilisateur -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Nom d'utilisateur</mat-label>
                <input matInput formControlName="username" autocomplete="username">
                @if (loginForm.get('username')?.hasError('required')) {
                  <mat-error>Le nom d'utilisateur est requis</mat-error>
                }
              </mat-form-field>

              <!-- Mot de passe -->
              <mat-form-field appearance="outline" class="full-width">
                <mat-label>Mot de passe</mat-label>
                <input matInput type="password" formControlName="password" autocomplete="current-password">
                @if (loginForm.get('password')?.hasError('required')) {
                  <mat-error>Le mot de passe est requis</mat-error>
                }
              </mat-form-field>

              <!-- Se souvenir de moi -->
              <mat-checkbox formControlName="rememberMe" color="primary">
                Se souvenir de moi (30 jours)
              </mat-checkbox>

              <!-- Bouton de connexion -->
              <button mat-raised-button
                      color="primary"
                      type="submit"
                      class="submit-btn"
                      [disabled]="loginForm.invalid || (loading$ | async)">
                @if (loading$ | async) {
                  <mat-spinner diameter="20"></mat-spinner>
                  Connexion en cours...
                } @else {
                  Se connecter
                }
              </button>
            </form>
          </mat-card-content>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #1e293b 0%, #334155 50%, #1e293b 100%);
      padding: 16px;
    }

    .login-container {
      width: 100%;
      max-width: 450px;
    }

    .login-card {
      padding: 32px;
      border-radius: 16px;
      box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
    }

    .header {
      text-align: center;
      margin-bottom: 32px;
    }

    .avatar {
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 16px;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        color: white;
      }
    }

    .title {
      font-size: 2rem;
      font-weight: 700;
      background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      margin: 0 0 8px;
    }

    .subtitle {
      color: #64748b;
      margin: 0;
    }

    .full-width {
      width: 100%;
      margin-bottom: 16px;
    }

    .error-alert {
      display: flex;
      align-items: center;
      gap: 8px;
      background: #fef2f2;
      color: #dc2626;
      padding: 12px 16px;
      border-radius: 8px;
      margin-bottom: 16px;
      border: 1px solid #fecaca;

      mat-icon {
        color: #dc2626;
      }
    }

    mat-checkbox {
      margin-bottom: 24px;
      display: block;
    }

    .submit-btn {
      width: 100%;
      padding: 12px;
      font-size: 1rem;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
  `]
})
export class LoginComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private store = inject(Store);
  private router = inject(Router);
  private destroy$ = new Subject<void>();

  loading$ = this.store.select(selectAuthLoading);
  error$ = this.store.select(selectAuthError);

  loginForm: FormGroup = this.fb.group({
    username: ['', Validators.required],
    password: ['', Validators.required],
    rememberMe: [false],
  });

  ngOnInit(): void {
    this.store.select(selectIsAuthenticated)
      .pipe(takeUntil(this.destroy$))
      .subscribe(isAuth => {
        if (isAuth) {
          this.router.navigate(['/carte']);
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.store.dispatch(AuthActions.clearError());
  }

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.store.dispatch(AuthActions.login({
        credentials: this.loginForm.value
      }));
    }
  }
}
