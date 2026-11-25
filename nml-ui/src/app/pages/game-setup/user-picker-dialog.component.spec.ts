import { TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { UserPickerDialogComponent } from './user-picker-dialog.component';
import { ApiService } from '../../services/api.service';
import type { AdminUser } from '../../models';

describe('UserPickerDialogComponent', () => {
  const api = {
    adminSearchUsers: jest.fn(() =>
      of({
        content: [
          { id: 1, username: 'lurio', role: 'USER' },
          { id: 2, username: 'testadmin', role: 'ADMIN' },
          { id: 3, username: 'nursek', role: 'USER' },
        ] as AdminUser[],
      }),
    ),
  };
  const dialogRef = { close: jest.fn() };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [
        { provide: ApiService, useValue: api },
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            title: 'Relier',
            actionLabel: 'Relier',
            excludeUserIds: [1],
            excludeRoles: ['ADMIN'],
          },
        },
      ],
    });
  });

  function component(): UserPickerDialogComponent {
    return TestBed.runInInjectionContext(() => new UserPickerDialogComponent());
  }

  it('recherche et filtre les comptes exclus', async () => {
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(page.results().map((user) => user.username)).toEqual(['nursek']);
    expect(page.loading()).toBe(false);
  });

  it('recherche avec la saisie nettoyée', async () => {
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));
    page.query.set('  nur  ');

    await page.search();

    expect(api.adminSearchUsers).toHaveBeenLastCalledWith('nur');
  });

  it('expose l’erreur de recherche', async () => {
    api.adminSearchUsers.mockReturnValueOnce(
      throwError(() => ({ error: { detail: 'Recherche refusée' } })),
    );
    const page = component();
    await new Promise((resolve) => setTimeout(resolve, 0));

    expect(page.error()).toBe('Recherche refusée');
  });

  it('sélectionne un compte ou annule', () => {
    const page = component();
    const user = { id: 3, username: 'nursek', role: 'USER' } as AdminUser;

    page.pick(user);
    expect(dialogRef.close).toHaveBeenCalledWith(user);

    page.cancel();
    expect(dialogRef.close).toHaveBeenLastCalledWith();
  });
});
