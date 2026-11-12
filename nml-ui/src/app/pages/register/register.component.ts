import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';
import { RegistrationMode } from '../../models';
import { SigilComponent } from '../../shared/sigil/sigil.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle/theme-toggle.component';

@Component({
  selector: 'app-register',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule,
    RouterLink,
    SigilComponent,
    ThemeToggleComponent,
  ],
  templateUrl: './register.component.html',
  styleUrls: ['../login/login.component.scss'],
  styles: [
    `
      .alt-link {
        display: block;
        margin-top: 18px;
        text-align: center;
        font-size: 0.82rem;
        color: var(--muted);
      }
      .alt-link a {
        color: var(--accent);
      }
      .closed {
        color: var(--muted);
        font-size: 0.9rem;
        line-height: 1.5;
        text-align: center;
      }
    `,
  ],
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  readonly loading = this.auth.loading;
  readonly error = this.auth.error;

  readonly mode = signal<RegistrationMode | null>(null);
  readonly username = signal('');
  readonly password = signal('');
  readonly confirmPassword = signal('');
  readonly inviteCode = signal('');
  readonly submitted = signal(false);

  readonly usernameError = computed(() =>
    this.username().trim().length < 2
      ? "Le nom d'utilisateur doit faire au moins 2 caractères"
      : null,
  );
  readonly passwordError = computed(() =>
    this.password().length < 8 ? 'Le mot de passe doit faire au moins 8 caractères' : null,
  );
  readonly confirmError = computed(() =>
    this.confirmPassword() !== this.password() ? 'Les mots de passe ne correspondent pas' : null,
  );
  readonly inviteError = computed(() =>
    this.mode()?.inviteRequired && this.inviteCode().trim().length === 0
      ? "Le code d'invitation est requis"
      : null,
  );
  readonly invalid = computed(
    () =>
      this.usernameError() !== null ||
      this.passwordError() !== null ||
      this.confirmError() !== null ||
      this.inviteError() !== null,
  );

  constructor() {
    const invite = this.route.snapshot.queryParamMap.get('invite');
    if (invite) this.inviteCode.set(invite);
    void this.loadMode();
  }

  private async loadMode(): Promise<void> {
    try {
      this.mode.set(await firstValueFrom(this.api.getRegistrationMode()));
    } catch {
      this.mode.set({ enabled: false, inviteRequired: true });
    }
  }

  async onSubmit(event: Event): Promise<void> {
    event.preventDefault();
    this.submitted.set(true);
    if (this.invalid() || !this.mode()?.enabled) return;
    try {
      await this.auth.register({
        username: this.username().trim(),
        password: this.password(),
        inviteCode: this.mode()?.inviteRequired ? this.inviteCode().trim() : undefined,
      });
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      if (returnUrl && returnUrl.startsWith('/')) {
        await this.router.navigateByUrl(returnUrl);
      }
    } catch {
      // AuthService a déjà posé le message d'erreur.
    }
  }
}
