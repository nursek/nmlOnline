import { ApplicationRef, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
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
});
