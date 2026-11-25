import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { NotificationsService } from './notifications.service';
import { AuthService } from './auth.service';
import { InstanceService } from './instance.service';
import { GameNotification, NotificationFeed } from '../models';
import { environment } from '../../environments/environment';

function notification(id: number, readAt: string | null = null): GameNotification {
  return {
    id,
    type: 'EXCHANGE_RECEIVED',
    title: `Notification ${id}`,
    message: 'Message',
    link: '/joueur',
    createdAt: new Date(2026, 0, id).toISOString(),
    readAt,
  };
}

function feed(items: GameNotification[]): NotificationFeed {
  return { items, unreadCount: items.filter((item) => !item.readAt).length };
}

describe('NotificationsService', () => {
  const initialized = signal(true);
  const user = signal<{ id: number; username: string; role: string } | null>({
    id: 1,
    username: 'lurio',
    role: 'PLAYER',
  });

  beforeEach(() => {
    initialized.set(true);
    user.set({ id: 1, username: 'lurio', role: 'PLAYER' });
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        {
          provide: AuthService,
          useValue: { initialized: initialized.asReadonly(), user: user.asReadonly() },
        },
        { provide: InstanceService, useValue: { selectedId: signal(1) } },
      ],
    });
  });

  afterEach(() => {
    user.set(null);
    TestBed.tick();
  });

  it("n'alerte pas au premier chargement et pop les seules nouveautés", async () => {
    const service = TestBed.inject(NotificationsService);
    const http = TestBed.inject(HttpTestingController);
    const first = notification(1);

    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([first]));
    await TestBed.inject(ApplicationRef).whenStable();

    expect(service.items()).toHaveLength(1);
    expect(service.unreadCount()).toBe(1);
    expect(service.toasts()).toHaveLength(0);
    expect(service.hasLoaded()).toBe(true);

    const second = notification(2);
    const reload = service.reload();
    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([second, first]));
    await reload;
    await TestBed.inject(ApplicationRef).whenStable();

    expect(service.toasts().map((toast) => toast.id)).toEqual([second.id]);
    expect(service.unreadCount()).toBe(2);

    const markRead = service.markRead(second.id);
    http.expectOne(`${environment.apiBaseUrl}/notifications/2/read`).flush(null);
    await markRead;

    expect(service.unreadCount()).toBe(1);
    expect(service.items().find((item) => item.id === second.id)?.readAt).toBeTruthy();

    const markAll = service.markAllRead();
    http.expectOne(`${environment.apiBaseUrl}/notifications/read-all`).flush(null);
    await markAll;

    expect(service.unreadCount()).toBe(0);
    expect(service.items().every((item) => item.readAt)).toBe(true);
  });


  it('open marque lu, ferme le toast et navigue avec l’identifiant de notification', async () => {
    const service = TestBed.inject(NotificationsService);
    const http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    const navigate = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const item = notification(5);

    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([item]));
    await TestBed.inject(ApplicationRef).whenStable();

    service.open(item);
    http.expectOne(`${environment.apiBaseUrl}/notifications/5/read`).flush(null);
    await Promise.resolve();

    expect(navigate).toHaveBeenCalledWith('/joueur?notif=5');
    expect(service.unreadCount()).toBe(0);
    expect(service.toasts()).toHaveLength(0);
  });

  it('open n’ouvre pas un lien externe ou javascript', async () => {
    const service = TestBed.inject(NotificationsService);
    const http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    const navigate = jest.spyOn(router, 'navigateByUrl').mockResolvedValue(true);
    const unsafe = { ...notification(6), link: 'javascript:alert(1)' };

    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([unsafe]));
    await TestBed.inject(ApplicationRef).whenStable();

    service.open(unsafe);
    http.expectOne(`${environment.apiBaseUrl}/notifications/6/read`).flush(null);
    await Promise.resolve();

    expect(navigate).not.toHaveBeenCalled();
  });

  it('une lecture en échec recharge le fil', async () => {
    const service = TestBed.inject(NotificationsService);
    const http = TestBed.inject(HttpTestingController);
    const item = notification(7);

    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([item]));
    await TestBed.inject(ApplicationRef).whenStable();

    const pending = service.markRead(7);
    http
      .expectOne(`${environment.apiBaseUrl}/notifications/7/read`)
      .flush(null, { status: 500, statusText: 'Erreur' });
    await pending;
    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([item]));
  });

  it('ignore un poll antérieur à une lecture locale', async () => {
    const service = TestBed.inject(NotificationsService);
    const http = TestBed.inject(HttpTestingController);
    const first = notification(1);
    const second = notification(2);

    TestBed.tick();
    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([second, first]));
    await TestBed.inject(ApplicationRef).whenStable();
    expect(service.unreadCount()).toBe(2);

    const pendingReload = service.reload();
    const pendingMark = service.markRead(second.id);
    http.expectOne(`${environment.apiBaseUrl}/notifications/2/read`).flush(null);
    await pendingMark;

    http.expectOne(`${environment.apiBaseUrl}/notifications`).flush(feed([second, first]));
    await pendingReload;

    expect(service.unreadCount()).toBe(1);
    expect(service.items().find((item) => item.id === second.id)?.readAt).toBeTruthy();
  });
});
