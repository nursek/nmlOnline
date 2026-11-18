import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { firstValueFrom } from 'rxjs';
import { httpErrorMessage } from '../../core/http-error.interceptor';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';

const COTE_PHOTO = 256;
const TAILLE_MAX_FICHIER = 8 * 1024 * 1024;
const FORMATS_PHOTO = ['image/png', 'image/jpeg', 'image/webp'];

@Component({
  selector: 'app-profil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './profil.component.html',
  styleUrls: ['./profil.component.scss'],
})
export class ProfilComponent {
  private readonly auth = inject(AuthService);
  private readonly api = inject(ApiService);
  private readonly snack = inject(MatSnackBar);

  readonly user = this.auth.user;
  readonly avatarUrl = computed(() => this.user()?.avatarUrl ?? null);

  readonly pseudo = signal(this.user()?.username ?? '');
  readonly motDePasseActuel = signal('');
  readonly nouveauMotDePasse = signal('');
  readonly confirmationMotDePasse = signal('');

  readonly enregistrementPseudo = signal(false);
  readonly enregistrementPhoto = signal(false);
  readonly enregistrementMotDePasse = signal(false);

  readonly erreurPseudo = signal<string | null>(null);
  readonly erreurPhoto = signal<string | null>(null);
  readonly erreurMotDePasse = signal<string | null>(null);

  readonly pseudoModifie = computed(() => {
    const pseudo = this.pseudo().trim();
    return pseudo.length >= 2 && pseudo !== this.user()?.username;
  });

  readonly motDePasseValide = computed(
    () =>
      this.motDePasseActuel().length > 0 &&
      this.nouveauMotDePasse().length >= 8 &&
      this.nouveauMotDePasse() === this.confirmationMotDePasse(),
  );

  async enregistrerPseudo(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.pseudoModifie() || this.enregistrementPseudo()) return;
    this.enregistrementPseudo.set(true);
    this.erreurPseudo.set(null);
    try {
      const profile = await firstValueFrom(this.api.updateProfileUsername(this.pseudo().trim()));
      this.auth.updateUser({ username: profile.username });
      this.pseudo.set(profile.username);
      this.notifier('Pseudo mis à jour');
    } catch (error) {
      this.erreurPseudo.set(httpErrorMessage(error, 'Impossible de changer le pseudo'));
    } finally {
      this.enregistrementPseudo.set(false);
    }
  }

  async choisirPhoto(event: Event): Promise<void> {
    const input = event.target as HTMLInputElement;
    const fichier = input.files?.[0];
    input.value = '';
    if (!fichier) return;
    if (!FORMATS_PHOTO.includes(fichier.type)) {
      this.erreurPhoto.set('Choisissez une image PNG, JPEG ou WebP.');
      return;
    }
    if (fichier.size > TAILLE_MAX_FICHIER) {
      this.erreurPhoto.set('Image trop lourde (8 Mo maximum).');
      return;
    }
    this.enregistrementPhoto.set(true);
    this.erreurPhoto.set(null);
    try {
      const avatarUrl = await this.recadrerPhoto(fichier);
      const profile = await firstValueFrom(this.api.updateProfileAvatar(avatarUrl));
      this.auth.updateUser({ avatarUrl: profile.avatarUrl ?? null });
      this.notifier('Photo de profil mise à jour');
    } catch (error) {
      this.erreurPhoto.set(httpErrorMessage(error, 'Impossible de changer la photo'));
    } finally {
      this.enregistrementPhoto.set(false);
    }
  }

  async retirerPhoto(): Promise<void> {
    if (this.enregistrementPhoto()) return;
    this.enregistrementPhoto.set(true);
    this.erreurPhoto.set(null);
    try {
      await firstValueFrom(this.api.updateProfileAvatar(null));
      this.auth.updateUser({ avatarUrl: null });
      this.notifier('Photo de profil retirée');
    } catch (error) {
      this.erreurPhoto.set(httpErrorMessage(error, 'Impossible de retirer la photo'));
    } finally {
      this.enregistrementPhoto.set(false);
    }
  }

  async changerMotDePasse(event: Event): Promise<void> {
    event.preventDefault();
    if (!this.motDePasseValide() || this.enregistrementMotDePasse()) return;
    this.enregistrementMotDePasse.set(true);
    this.erreurMotDePasse.set(null);
    try {
      await firstValueFrom(
        this.api.updateProfilePassword(this.motDePasseActuel(), this.nouveauMotDePasse()),
      );
      this.motDePasseActuel.set('');
      this.nouveauMotDePasse.set('');
      this.confirmationMotDePasse.set('');
      this.notifier('Mot de passe mis à jour');
    } catch (error) {
      this.erreurMotDePasse.set(httpErrorMessage(error, 'Impossible de changer le mot de passe'));
    } finally {
      this.enregistrementMotDePasse.set(false);
    }
  }

  private async recadrerPhoto(fichier: File): Promise<string> {
    const bitmap = await createImageBitmap(fichier);
    const cote = Math.min(bitmap.width, bitmap.height);
    const canvas = document.createElement('canvas');
    canvas.width = COTE_PHOTO;
    canvas.height = COTE_PHOTO;
    const contexte = canvas.getContext('2d');
    if (!contexte) {
      bitmap.close?.();
      throw new Error('Impossible de préparer la photo');
    }
    contexte.fillStyle = '#12181b';
    contexte.fillRect(0, 0, COTE_PHOTO, COTE_PHOTO);
    contexte.drawImage(
      bitmap,
      (bitmap.width - cote) / 2,
      (bitmap.height - cote) / 2,
      cote,
      cote,
      0,
      0,
      COTE_PHOTO,
      COTE_PHOTO,
    );
    bitmap.close?.();
    return canvas.toDataURL('image/jpeg', 0.85);
  }

  private notifier(message: string): void {
    this.snack.open(message, 'OK', { duration: 3000 });
  }
}
