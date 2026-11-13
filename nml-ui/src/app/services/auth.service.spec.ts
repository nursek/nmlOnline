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

function setup() {
  const router = { navigate: jest.fn().mockResolvedValue(true) };
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

    const login = auth.login({ username: 'lurio', password: 'secret' });
    http.expectOne(`${environment.apiBaseUrl}/login`).flush(response());
    await new Promise((resolve) => setTimeout(resolve, 0));
    http
      .expectOne(`${environment.apiBaseUrl}/instances`)
      .flush([instance(9, false, 'ADMIN'), instance(7, true, 'MJ')]);
    await login;

    expect(enter).toHaveBeenCalledWith(expect.objectContaining({ id: 7, role: 'MJ' }));
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('passe par le sélecteur quand le compte joue plusieurs parties', async () => {
    const { auth, http, router } = setup();

    const login = auth.login({ username: 'lurio', password: 'secret' });
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

    const login = auth.login({ username: 'lurio', password: 'secret' });
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

describe('AuthService — blocage temporaire du login', () => {
  beforeEach(() => sessionStorage.clear());
  afterEach(() => {
    sessionStorage.clear();
    jest.useRealTimers();
  });

  it('décompte les secondes restantes du Retry-After', async () => {
    jest.useFakeTimers();
    const { auth, http } = setup();

    const login = auth.login({ username: 'lurio', password: 'mauvais' });
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
