import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  const auth = {
    loading: signal(false),
    error: signal<string | null>(null),
    retryInSeconds: signal(0),
    login: jest.fn().mockResolvedValue(undefined),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: auth }],
    });
  });

  function component(): LoginComponent {
    return TestBed.runInInjectionContext(() => new LoginComponent());
  }

  it('valide le formulaire et n’appelle pas l’API si vide', async () => {
    const page = component();

    await page.onSubmit({ preventDefault: jest.fn() } as unknown as Event);

    expect(page.invalid()).toBe(true);
    expect(page.submitted()).toBe(true);
    expect(auth.login).not.toHaveBeenCalled();
  });

  it('connecte avec le pseudo nettoyé et l’option mémorisation', async () => {
    const page = component();
    page.username.set('  lurio  ');
    page.password.set('secret123');
    page.rememberMe.set(true);

    await page.onSubmit({ preventDefault: jest.fn() } as unknown as Event);

    expect(auth.login).toHaveBeenCalledWith({
      username: 'lurio',
      password: 'secret123',
      rememberMe: true,
    });
  });

  it('formate le décompte de blocage', () => {
    auth.retryInSeconds.set(90);

    expect(component().retryLabel()).toBe('1:30');
  });
});
