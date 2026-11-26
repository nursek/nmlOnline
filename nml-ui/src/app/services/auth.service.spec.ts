import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { InstanceService } from './instance.service';
import { AuthResponse, GameInstance } from '../models';
import { environment } from '../../environments/environment';

function instance(id: number, participant: boolean, role: GameInstance['role']): GameInstance {
  return {
    id,
    name: `Partie ${id}`,
    currentTurn: 1,
    playerCount: 2,
    playerId: participant ? id * 10 : null,
    playerName: participant ? `Joueur ${id}` : null,
    eliminated: false,
    role,
    participant,
  };
}

function response(): AuthResponse {
  return { token: 'token', id: 1, name: 'lurio', role: 'USER' };
}

function setup(returnUrl?: string, stored = false) {
  if (stored) {
    sessionStorage.setItem('accessToken', 'vieux');
    sessionStorage.setItem(
      'user',
      JSON.stringify({ id: 1, username: 'lurio', role: 'USER', avatarUrl: null }),
    );
  }
  const router = {
    navigate: jest.fn().mockResolvedValue(true),
    routerState: { snapshot: { root: { queryParamMap: new Map([['returnUrl', returnUrl]]) } } },
  };
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
      { provide: Router, useValue: router },
    ],
  });
  return {
    auth: TestBed.inject(AuthService),
    instance: TestBed.inject(InstanceService),
    http: TestBed.inject(HttpTestingController),
    router,
  };
}

describe('AuthService — route après login', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => sessionStorage.clear());

  it("atterrit sur l'unique partie jouée, même si d'autres parties sont accessibles", async () => {
    const { auth, instance: service, http, router } = setup();
    const enter = jest.spyOn(service, 'enter').mockImplementation(() => undefined);

    const login = auth.login({ username: 'lurio', password: 'secret', rememberMe: false });
    http.expectOne(`${environment.apiBaseUrl}/login`).flush(response());
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne(`${environment.apiBaseUrl}/instances`)
      .flush([instance(9, false, 'ADMIN'), instance(7, true, 'MJ')]);
    await login;

    expect(enter).toHaveBeenCalledWith(expect.objectContaining({ id: 7, role: 'MJ' }));
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('revient à la page demandée avant la connexion, jamais vers un autre site', async () => {
    for (const [returnUrl, expected] of [
      ['/journal/3', ['/journal/3']],
      ['//pirate.example', []],
    ] as const) {
      TestBed.resetTestingModule();
      const { auth, instance: service, http } = setup(returnUrl);
      const enter = jest.spyOn(service, 'enter').mockImplementation(() => undefined);

      const login = auth.login({ username: 'lurio', password: 'secret', rememberMe: false });
      http.expectOne(`${environment.apiBaseUrl}/login`).flush(response());
      await new Promise((resolve) => setTimeout(resolve, 0));
      http.expectOne(`${environment.apiBaseUrl}/instances`).flush([instance(7, true, 'PLAYER')]);
      await login;

      expect(enter).toHaveBeenCalledWith(expect.objectContaining({ id: 7 }), ...expected);
    }
  });

  it('passe par le sélecteur quand le compte joue plusieurs parties', async () => {
    const { auth, http, router } = setup();

    const login = auth.login({ username: 'lurio', password: 'secret', rememberMe: false });
    http.expectOne(`${environment.apiBaseUrl}/login`).flush(response());
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne(`${environment.apiBaseUrl}/instances`)
      .flush([instance(7, true, 'PLAYER'), instance(8, true, 'PLAYER')]);
    await login;

    expect(router.navigate).toHaveBeenCalledWith(['/parties']);
  });

  it('ne transforme pas une liste indisponible en échec de connexion', async () => {
    const { auth, instance: service, http, router } = setup();

    const login = auth.login({ username: 'lurio', password: 'secret', rememberMe: false });
    http.expectOne(`${environment.apiBaseUrl}/login`).flush(response());
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne(`${environment.apiBaseUrl}/instances`)
      .flush('boom', { status: 500, statusText: 'Server Error' });
    await expect(login).resolves.toBeUndefined();

    expect(service.selectedId()).toBeNull();
    expect(service.error()).not.toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/parties']);
  });
});

describe('AuthService — messages de connexion', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => {
    sessionStorage.clear();
    jest.restoreAllMocks();
  });

  const failedLogin = async (status: number, body: unknown) => {
    const { auth, http } = setup();
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

    const login = auth.login({ username: 'lurio', password: 'mauvais', rememberMe: false });
    http.expectOne(`${environment.apiBaseUrl}/login`).flush(body, { status, statusText: 'Error' });
    await expect(login).rejects.toThrow();

    return { auth, warn };
  };

  it('traduit des identifiants refusés en message joueur et garde le détail en console', async () => {
    const { auth, warn } = await failedLogin(401, {
      title: 'Unauthorized',
      status: 401,
      detail: 'Identifiants invalides',
    });

    expect(auth.error()).toBe("Nom d'utilisateur ou mot de passe incorrect.");
    expect(warn).toHaveBeenCalled();
  });

  it('ne montre pas la panne serveur brute au joueur', async () => {
    const { auth } = await failedLogin(503, { title: 'Service Unavailable', status: 503 });

    expect(auth.error()).toBe(
      'Le serveur est momentanément indisponible. Réessayez dans quelques instants.',
    );
  });
});

describe('AuthService — messages d’inscription', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => {
    sessionStorage.clear();
    jest.restoreAllMocks();
  });

  const failedRegister = async (status: number, body: unknown) => {
    const { auth, http } = setup();
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => undefined);

    const register = auth.register({
      username: 'lurio',
      password: 'motdepasse',
      inviteCode: 'abc',
    });
    http
      .expectOne(`${environment.apiBaseUrl}/register`)
      .flush(body, { status, statusText: 'Error' });
    await expect(register).rejects.toThrow();

    return { auth, warn };
  };

  it('garde le message d’invitation du backend, déjà destiné au joueur', async () => {
    const { auth, warn } = await failedRegister(403, { detail: "Code d'invitation invalide" });

    expect(auth.error()).toBe("Code d'invitation invalide");
    expect(warn).toHaveBeenCalled();
  });

  it('traduit les autres échecs en messages joueur', async () => {
    const conflict = await failedRegister(409, { detail: 'Utilisateur déjà existant' });
    expect(conflict.auth.error()).toBe('Ce nom d’utilisateur est déjà pris.');

    TestBed.resetTestingModule();
    const outage = await failedRegister(503, {});
    expect(outage.auth.error()).toBe(
      'Le serveur est momentanément indisponible. Réessayez dans quelques instants.',
    );
  });
});

describe('AuthService — blocage temporaire du login', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => {
    sessionStorage.clear();
    jest.useRealTimers();
  });

  it('décompte les secondes restantes du Retry-After', async () => {
    jest.useFakeTimers();
    const { auth, http } = setup();

    const login = auth.login({ username: 'lurio', password: 'mauvais', rememberMe: false });
    http.expectOne(`${environment.apiBaseUrl}/login`).flush(
      { detail: 'Trop de tentatives, réessayez plus tard' },
      { status: 429, statusText: 'Too Many Requests', headers: { 'Retry-After': '90' } },
    );
    await expect(login).rejects.toThrow('Trop de tentatives');

    expect(auth.retryInSeconds()).toBe(90);
    expect(auth.error()).toBeNull();

    jest.advanceTimersByTime(30_000);
    expect(auth.retryInSeconds()).toBe(60);

    jest.advanceTimersByTime(60_000);
    expect(auth.retryInSeconds()).toBe(0);
  });
});

describe('AuthService — dérivés de session', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => sessionStorage.clear());

  function serviceFor(userRole: string | null, instanceRole: string | null) {
    sessionStorage.clear();
    if (userRole) {
      sessionStorage.setItem(
        'user',
        JSON.stringify({ id: 1, username: 'lurio', role: userRole, avatarUrl: null }),
      );
    }
    if (instanceRole) sessionStorage.setItem('instanceRole', instanceRole);
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: { navigate: jest.fn().mockResolvedValue(true) } },
      ],
    });
    return TestBed.inject(AuthService);
  }

  it('canAdmin suit le rôle de la partie : joueur non, MJ oui, sans toucher au jeton', () => {
    expect(serviceFor('USER', 'PLAYER').canAdmin()).toBe(false);
    expect(serviceFor('USER', 'MJ').canAdmin()).toBe(true);
    expect(serviceFor('ADMIN', null).canAdmin()).toBe(true);
    expect(serviceFor('USER', 'ADMIN').canAdmin()).toBe(true);
    expect(serviceFor(null, 'MJ').isAuthenticated()).toBe(false);
  });

  it('isAdmin ne confond pas le rôle global et le rôle MJ de la partie', () => {
    expect(serviceFor('ADMIN', 'MJ').isAdmin()).toBe(true);
    expect(serviceFor('USER', 'MJ').isAdmin()).toBe(false);
  });

  it('initSession sans jeton ni mémorisation marque la session initialisée sans appel réseau', async () => {
    const auth = serviceFor(null, null);
    const http = TestBed.inject(HttpTestingController);

    await auth.initSession();

    expect(auth.initialized()).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
    http.verify();
  });

  it('logout nettoie la session locale même si le serveur échoue', async () => {
    const auth = serviceFor('USER', 'PLAYER');
    const http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);

    const logout = auth.logout();
    http
      .expectOne(`${environment.apiBaseUrl}/auth/logout`)
      .flush(null, { status: 500, statusText: 'Server Error' });
    await logout;

    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('user')).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});

describe('AuthService — restauration de session au démarrage', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => {
    sessionStorage.clear();
    localStorage.clear();
  });

  it('un refresh valide restaure l’utilisateur et le jeton', async () => {
    const { auth, http } = setup(undefined, true);

    const pending = auth.initSession();
    http.expectOne(`${environment.apiBaseUrl}/auth/refresh`).flush({
      valid: true,
      token: 'neuf',
      id: 1,
      name: 'lurio',
      role: 'USER',
    });
    await pending;

    expect(auth.initialized()).toBe(true);
    expect(auth.isAuthenticated()).toBe(true);
    expect(sessionStorage.getItem('accessToken')).toBe('neuf');
  });

  it('une panne 5xx garde le jeton pour retenter plus tard', async () => {
    const { auth, http } = setup(undefined, true);

    const pending = auth.initSession();
    http
      .expectOne(`${environment.apiBaseUrl}/auth/refresh`)
      .flush(null, { status: 500, statusText: 'Erreur' });
    await pending;

    expect(auth.initialized()).toBe(true);
    expect(auth.isAuthenticated()).toBe(false);
    expect(sessionStorage.getItem('accessToken')).toBe('vieux');
  });

  it('un 401 purge la session stockée', async () => {
    const { auth, http } = setup(undefined, true);

    const pending = auth.initSession();
    http
      .expectOne(`${environment.apiBaseUrl}/auth/refresh`)
      .flush(null, { status: 401, statusText: 'Non autorisé' });
    await pending;

    expect(auth.initialized()).toBe(true);
    expect(sessionStorage.getItem('accessToken')).toBeNull();
    expect(sessionStorage.getItem('user')).toBeNull();
  });
});
