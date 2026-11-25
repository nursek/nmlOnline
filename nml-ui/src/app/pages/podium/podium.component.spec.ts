import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PodiumComponent } from './podium.component';
import { AuthService } from '../../services/auth.service';
import { InstanceService } from '../../services/instance.service';
import { PlayerDirectoryService } from '../../services/player-directory.service';
import { PODIUM, POSTHUMOUS } from './podium.fixture';

describe('PodiumComponent', () => {
  const isAdmin = signal(false);
  const selectedRole = signal<string | null>('PLAYER');
  let http: HttpTestingController;

  beforeEach(async () => {
    isAdmin.set(false);
    selectedRole.set('PLAYER');
    await TestBed.configureTestingModule({
      imports: [PodiumComponent],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { canAdmin: isAdmin } },
        { provide: InstanceService, useValue: { selectedRole } },
        { provide: PlayerDirectoryService, useValue: { colorMap: signal(new Map()) } },
        { provide: MatSnackBar, useValue: { open: jest.fn() } },
      ],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  it('affiche le classement complet et les causes lore', async () => {
    const fixture = TestBed.createComponent(PodiumComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/podium').flush(PODIUM);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.champion-name')?.textContent).toContain('nursek');
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(3);
    expect(fixture.nativeElement.textContent).toContain('Mort définitive');
    expect(fixture.nativeElement.textContent).toContain('QG conquis — retour possible');
    expect(fixture.nativeElement.querySelector('.admin-card')).toBeNull();
  });

  it('affiche le bloc MJ et désigne un champion posthume', async () => {
    isAdmin.set(true);
    const fixture = TestBed.createComponent(PodiumComponent);
    fixture.detectChanges();
    http.expectOne('/api/game/podium').flush(POSTHUMOUS);
    await fixture.whenStable();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.admin-card')).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Vainqueur posthume');

    fixture.componentInstance.selectedWinnerId.set(3);
    const pending = fixture.componentInstance.adjustWinner();
    const put = http.expectOne('/api/admin/game/winner');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ playerId: 3 });
    put.flush({});
    await pending;
    TestBed.tick();
    http.expectOne('/api/game/podium').flush(POSTHUMOUS);
    await fixture.whenStable();
  });
});
