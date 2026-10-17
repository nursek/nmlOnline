import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActiveBoardService } from './active-board.service';
import { AuthService } from './auth.service';
import { Board, User } from '../models';
import { environment } from '../../environments/environment';

function boardNamed(name: string): Board {
  return { id: 1, name, mapImageUrl: null, svgOverlayUrl: null, sectors: {} };
}

describe('ActiveBoardService (carte liée à la session)', () => {
  it('vide la carte au logout et recharge celle du compte suivant', async () => {
    const user = signal<User | null>(null);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { user: user.asReadonly() } },
      ],
    });
    const service = TestBed.inject(ActiveBoardService);
    const http = TestBed.inject(HttpTestingController);
    const appRef = TestBed.inject(ApplicationRef);

    user.set({ id: 1, username: 'lurio' });
    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/boards`).flush([boardNamed('lurio')]);
    await appRef.whenStable();
    expect(service.board()?.name).toBe('lurio');

    user.set(null);
    TestBed.tick();
    expect(service.board()).toBeNull();

    user.set({ id: 2, username: 'nursek' });
    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/boards`).flush([boardNamed('nursek')]);
    await appRef.whenStable();
    expect(service.board()?.name).toBe('nursek');
    http.verify();
  });
});
