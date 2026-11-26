import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../../services/auth.service';
import { ApiService } from '../../services/api.service';

describe('RegisterComponent', () => {
  const auth = {
    loading: signal(false),
    error: signal<string | null>(null),
    register: jest.fn().mockResolvedValue(undefined),
  };
  const api = {
    getRegistrationMode: jest.fn(() => of({ enabled: true, inviteRequired: false })),
  };
  const invite = { value: null as string | null };

  beforeEach(() => {
    jest.clearAllMocks();
    invite.value = null;
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: ApiService, useValue: api },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: { get: () => invite.value } } },
        },
      ],
    });
  });

  function component(): RegisterComponent {
    return TestBed.runInInjectionContext(() => new RegisterComponent());
  }

  it('préremplit le code d’invitation depuis l’URL et charge le mode', async () => {
    invite.value = 'code-42';
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(page.inviteCode()).toBe('code-42');
    expect(page.mode()).toEqual({ enabled: true, inviteRequired: false });
  });

  it('valide pseudo, mot de passe, confirmation et code d’invitation', async () => {
    api.getRegistrationMode.mockReturnValueOnce(of({ enabled: true, inviteRequired: true }));
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));

    page.username.set('a');
    page.password.set('court');
    page.confirmPassword.set('autre');
    expect(page.usernameError()).not.toBeNull();
    expect(page.passwordError()).not.toBeNull();
    expect(page.confirmError()).not.toBeNull();
    expect(page.inviteError()).not.toBeNull();
  });

  it('enregistre uniquement un formulaire valide et une inscription ouverte', async () => {
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));
    page.username.set('lurio');
    page.password.set('motdepasse1');
    page.confirmPassword.set('motdepasse1');

    await page.onSubmit({ preventDefault: jest.fn() } as unknown as Event);

    expect(auth.register).toHaveBeenCalledWith({
      username: 'lurio',
      password: 'motdepasse1',
      inviteCode: undefined,
    });
  });

  it('bloque l’inscription quand elle est fermée', async () => {
    api.getRegistrationMode.mockReturnValueOnce(of({ enabled: false, inviteRequired: true }));
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));
    page.username.set('lurio');
    page.password.set('motdepasse1');
    page.confirmPassword.set('motdepasse1');
    page.inviteCode.set('code');

    await page.onSubmit({ preventDefault: jest.fn() } as unknown as Event);

    expect(auth.register).not.toHaveBeenCalled();
  });
});
