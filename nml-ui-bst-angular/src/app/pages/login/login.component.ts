import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    MatIconModule,
  ],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
})
export class LoginComponent {
  private readonly auth = inject(AuthService);

  readonly loading = this.auth.loading;
  readonly error = this.auth.error;

  readonly username = signal('');
  readonly password = signal('');
  readonly rememberMe = signal(false);

  /** Becomes true after the first submit attempt so errors only show afterwards. */
  readonly submitted = signal(false);

  readonly usernameError = computed(() =>
    this.username().trim().length === 0 ? "Le nom d'utilisateur est requis" : null,
  );
  readonly passwordError = computed(() =>
    this.password().length === 0 ? 'Le mot de passe est requis' : null,
  );
  readonly invalid = computed(() => this.usernameError() !== null || this.passwordError() !== null);

  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    this.submitted.set(true);
    if (this.invalid()) return;
    try {
      await this.auth.login({
        username: this.username().trim(),
        password: this.password(),
        rememberMe: this.rememberMe(),
      });
    } catch {
      // AuthService.login already set the error signal; swallow here.
    }
  }
}
