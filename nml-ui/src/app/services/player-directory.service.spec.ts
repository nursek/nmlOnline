import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlayerDirectoryService } from './player-directory.service';
import { AuthService } from './auth.service';
import { MAP_THEME } from '../pages/carte/carte.config';
import { PageResult, Player, User } from '../models';
import { environment } from '../../environments/environment';

function playerNamed(id: number, name: string): Player {
  return {
    id,
    name,
    stats: {} as Player['stats'],
    equipments: [],
    resources: [],
    sectors: [],
    character: null,
    buildings: [],
  };
}

function page(content: Player[], totalElements = content.length): PageResult<Player> {
  return { content, totalElements };
}

describe('PlayerDirectoryService (annuaire lié à la session)', () => {
  it('vide l’annuaire au logout et attribue des couleurs stables par id', async () => {
    const user = signal<User | null>(null);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { user: user.asReadonly() } },
      ],
    });
    const service = TestBed.inject(PlayerDirectoryService);
    const http = TestBed.inject(HttpTestingController);
    const appRef = TestBed.inject(ApplicationRef);

    user.set({ id: 1, username: 'lurio' });
    TestBed.tick();
    http
      .expectOne((req) => req.url === `${environment.apiBaseUrl}/players`)
      .flush(
        page(
          Array.from({ length: 50 }, (_, i) => playerNamed(i + 1, `p${i + 1}`)),
          60,
        ),
      );
    await appRef.whenStable();
    expect(service.players().length).toBe(50);
    expect(service.colorMap().get(1)).toBe(MAP_THEME.playerPalette[0]);
    expect(service.colorMap().get(2)).toBe(MAP_THEME.playerPalette[1]);
    expect(service.truncated()).toBe(true);

    user.set(null);
    TestBed.tick();
    expect(service.players()).toEqual([]);
    expect(service.colorMap().size).toBe(0);
    http.verify();
  });
});
