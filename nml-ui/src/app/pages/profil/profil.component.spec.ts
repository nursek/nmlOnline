import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';
import type { User } from '../../models';
import { ApiService } from '../../services/api.service';
import { AuthService } from '../../services/auth.service';
import { ProfilComponent } from './profil.component';

describe('ProfilComponent', () => {
  const utilisateur: User = { id: 1, username: 'lurio', role: 'USER', avatarUrl: null };
  let api: {
    updateProfileAvatar: jest.Mock;
    updateProfileUsername: jest.Mock;
    updateProfilePassword: jest.Mock;
  };
  let auth: { user: ReturnType<typeof signal<User>>; updateUser: jest.Mock; logout: jest.Mock };
  let snack: { open: jest.Mock };

  beforeEach(async () => {
    api = {
      updateProfileAvatar: jest.fn(),
      updateProfileUsername: jest.fn(),
      updateProfilePassword: jest.fn(() => of(void 0)),
    };
    auth = {
      user: signal(utilisateur),
      updateUser: jest.fn(),
      logout: jest.fn().mockResolvedValue(undefined),
    };
    snack = { open: jest.fn() };
    await TestBed.configureTestingModule({
      imports: [ProfilComponent],
      providers: [
        { provide: ApiService, useValue: api },
        { provide: AuthService, useValue: auth },
        { provide: MatSnackBar, useValue: snack },
      ],
    }).compileComponents();
  });

  afterEach(() => {
    jest.restoreAllMocks();
    delete (globalThis as { createImageBitmap?: unknown }).createImageBitmap;
  });

  it('active les enregistrements seulement quand pseudo ou mot de passe sont valides', () => {
    const composant = TestBed.createComponent(ProfilComponent).componentInstance;

    expect(composant.pseudoModifie()).toBe(false);
    composant.pseudo.set('l');
    expect(composant.pseudoModifie()).toBe(false);
    composant.pseudo.set('  lurio2  ');
    expect(composant.pseudoModifie()).toBe(true);

    composant.motDePasseActuel.set('Actuel123');
    composant.nouveauMotDePasse.set('motdepasse1');
    composant.confirmationMotDePasse.set('motdepasse2');
    expect(composant.motDePasseValide()).toBe(false);
    composant.confirmationMotDePasse.set('motdepasse1');
    expect(composant.motDePasseValide()).toBe(true);
  });

  it('enregistre le pseudo nettoyé et met à jour la session', async () => {
    api.updateProfileUsername.mockReturnValue(of({ ...utilisateur, username: 'lurio2' }));
    const composant = TestBed.createComponent(ProfilComponent).componentInstance;
    composant.pseudo.set('  lurio2  ');

    await composant.enregistrerPseudo({ preventDefault: jest.fn() } as unknown as Event);

    expect(api.updateProfileUsername).toHaveBeenCalledWith('lurio2');
    expect(auth.updateUser).toHaveBeenCalledWith({ username: 'lurio2' });
    expect(composant.pseudo()).toBe('lurio2');
    expect(composant.enregistrementPseudo()).toBe(false);
  });

  it('refuse une photo de mauvais format ou trop lourde sans appel API', async () => {
    const composant = TestBed.createComponent(ProfilComponent).componentInstance;
    await composant.choisirPhoto({
      target: { files: [new File(['x'], 'photo.gif', { type: 'image/gif' })], value: '' },
    } as unknown as Event);
    expect(composant.erreurPhoto()).toContain('PNG, JPEG ou WebP');

    const lourde = new File([new Uint8Array(8 * 1024 * 1024 + 1)], 'photo.png', {
      type: 'image/png',
    });
    await composant.choisirPhoto({
      target: { files: [lourde], value: '' },
    } as unknown as Event);
    expect(composant.erreurPhoto()).toContain('8 Mo');
    expect(api.updateProfileAvatar).not.toHaveBeenCalled();
  });

  it('retire la photo de profil', async () => {
    api.updateProfileAvatar.mockReturnValue(of({ ...utilisateur, avatarUrl: null }));
    const composant = TestBed.createComponent(ProfilComponent).componentInstance;

    await composant.retirerPhoto();

    expect(api.updateProfileAvatar).toHaveBeenCalledWith(null);
    expect(auth.updateUser).toHaveBeenCalledWith({ avatarUrl: null });
  });

  it('recadre la photo en carré 256 et envoie le data URL', async () => {
    const drawImage = jest.fn();
    const bitmap = { width: 3000, height: 1000, close: jest.fn() };
    Object.defineProperty(globalThis, 'createImageBitmap', {
      configurable: true,
      value: jest.fn().mockResolvedValue(bitmap),
    });
    jest.spyOn(HTMLCanvasElement.prototype, 'getContext').mockReturnValue({
      fillStyle: '',
      fillRect: jest.fn(),
      drawImage,
    } as unknown as CanvasRenderingContext2D);
    jest
      .spyOn(HTMLCanvasElement.prototype, 'toDataURL')
      .mockReturnValue('data:image/jpeg;base64,photo');
    api.updateProfileAvatar.mockReturnValue(
      of({ ...utilisateur, avatarUrl: 'data:image/jpeg;base64,photo' }),
    );

    const composant = TestBed.createComponent(ProfilComponent).componentInstance;
    const fichier = new File(['image'], 'photo.png', { type: 'image/png' });
    await composant.choisirPhoto({
      target: { files: [fichier], value: 'photo.png' },
    } as unknown as Event);

    expect(drawImage).toHaveBeenCalledWith(bitmap, 1000, 0, 1000, 1000, 0, 0, 256, 256);
    expect(api.updateProfileAvatar).toHaveBeenCalledWith('data:image/jpeg;base64,photo');
    expect(auth.updateUser).toHaveBeenCalledWith({ avatarUrl: 'data:image/jpeg;base64,photo' });
  });

  it('déconnecte après un changement de mot de passe réussi', async () => {
    const composant = TestBed.createComponent(ProfilComponent).componentInstance;
    composant.motDePasseActuel.set('Actuel123');
    composant.nouveauMotDePasse.set('motdepasse1');
    composant.confirmationMotDePasse.set('motdepasse1');

    await composant.changerMotDePasse({ preventDefault: jest.fn() } as unknown as Event);

    expect(api.updateProfilePassword).toHaveBeenCalledWith('Actuel123', 'motdepasse1');
    expect(auth.logout).toHaveBeenCalled();
  });
});
