import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { MockStore, provideMockStore } from '@ngrx/store/testing';
import { authGuard, noAuthGuard } from './auth.guard';
import { selectIsAuthenticated, selectAuthLoading } from '../store';
import { firstValueFrom } from 'rxjs';

describe('Auth Guards', () => {
  let store: MockStore;
  let router: jest.Mocked<Pick<Router, 'createUrlTree'>>;

  beforeEach(() => {
    router = {
      createUrlTree: jest.fn((commands: string[], extras?: any) => {
        return { toString: () => commands.join('/'), queryParams: extras?.queryParams } as unknown as UrlTree;
      }),
    };

    TestBed.configureTestingModule({
      providers: [
        provideMockStore({
          selectors: [
            { selector: selectAuthLoading, value: false },
            { selector: selectIsAuthenticated, value: false },
          ],
        }),
        { provide: Router, useValue: router },
      ],
    });

    store = TestBed.inject(MockStore);
  });

  afterEach(() => {
    store.resetSelectors();
  });

  describe('authGuard', () => {
    it('should allow access when authenticated', async () => {
      store.overrideSelector(selectIsAuthenticated, true);
      store.refreshState();

      const result = await firstValueFrom(
        TestBed.runInInjectionContext(() =>
          authGuard({} as any, { url: '/carte' } as any)
        ) as any
      );

      expect(result).toBe(true);
    });

    it('should redirect to /login when not authenticated', async () => {
      store.overrideSelector(selectIsAuthenticated, false);
      store.refreshState();

      const result = await firstValueFrom(
        TestBed.runInInjectionContext(() =>
          authGuard({} as any, { url: '/carte' } as any)
        ) as any
      );

      expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
        queryParams: { returnUrl: '/carte' },
      });
      expect(result).not.toBe(true);
    });

    it('should wait for loading to complete before deciding', async () => {
      store.overrideSelector(selectAuthLoading, true);
      store.overrideSelector(selectIsAuthenticated, false);
      store.refreshState();

      let resolved = false;
      const promise = firstValueFrom(
        TestBed.runInInjectionContext(() =>
          authGuard({} as any, { url: '/carte' } as any)
        ) as any
      ).then((r: any) => { resolved = true; return r; });

      // Should not resolve while loading
      await new Promise(resolve => setTimeout(resolve, 50));
      expect(resolved).toBe(false);

      // Stop loading, set authenticated
      store.overrideSelector(selectAuthLoading, false);
      store.overrideSelector(selectIsAuthenticated, true);
      store.refreshState();

      const result = await promise;
      expect(result).toBe(true);
    });
  });

  describe('noAuthGuard', () => {
    it('should allow access when not authenticated', async () => {
      store.overrideSelector(selectIsAuthenticated, false);
      store.refreshState();

      const result = await firstValueFrom(
        TestBed.runInInjectionContext(() =>
          noAuthGuard({} as any, {} as any)
        ) as any
      );

      expect(result).toBe(true);
    });

    it('should redirect to /carte when authenticated', async () => {
      store.overrideSelector(selectIsAuthenticated, true);
      store.refreshState();

      const result = await firstValueFrom(
        TestBed.runInInjectionContext(() =>
          noAuthGuard({} as any, {} as any)
        ) as any
      );

      expect(router.createUrlTree).toHaveBeenCalledWith(['/carte']);
      expect(result).not.toBe(true);
    });
  });
});
