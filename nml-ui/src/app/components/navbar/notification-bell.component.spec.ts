import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ElementRef } from '@angular/core';
import { NotificationBellComponent } from './notification-bell.component';
import { NotificationsService } from '../../services/notifications.service';
import type { GameNotification } from '../../models';

describe('NotificationBellComponent', () => {
  const notification = {
    id: 1,
    type: 'TURN_ENDED',
    title: 'Tour terminé',
    message: 'Tour 2',
    link: null,
    createdAt: '2026-11-25T12:00:00Z',
    readAt: null,
  } as GameNotification;
  const services = {
    unreadCount: signal(2),
    unreadLabel: signal('2'),
    items: signal([notification]),
    preview: signal([notification]),
    toasts: signal([]),
    reload: jest.fn().mockResolvedValue(undefined),
    open: jest.fn(),
    markAllRead: jest.fn(),
    dismiss: jest.fn(),
  };
  const host = document.createElement('div');

  beforeEach(() => {
    jest.clearAllMocks();
    services.unreadCount.set(2);
    TestBed.configureTestingModule({
      providers: [
        { provide: NotificationsService, useValue: services },
        { provide: ElementRef, useValue: { nativeElement: host } },
      ],
    });
  });

  function component(): NotificationBellComponent {
    return TestBed.runInInjectionContext(() => new NotificationBellComponent());
  }

  it('annonce le nombre de non-lues dans le libellé accessible', () => {
    const page = component();
    expect(page.bellLabel()).toBe('Notifications, 2 non lues');

    services.unreadCount.set(0);
    expect(component().bellLabel()).toBe('Notifications');
  });

  it('ouvre le panneau et recharge le fil', () => {
    const page = component();

    page.toggle();

    expect(page.open()).toBe(true);
    expect(services.reload).toHaveBeenCalled();
    page.toggle();
    expect(page.open()).toBe(false);
  });

  it('ferme au clic extérieur mais pas au clic interne', () => {
    const page = component();
    page.toggle();

    const inside = document.createElement('button');
    host.appendChild(inside);
    page.onDocumentClick({ target: inside } as unknown as MouseEvent);
    expect(page.open()).toBe(true);

    page.onDocumentClick({ target: document.body } as unknown as MouseEvent);
    expect(page.open()).toBe(false);
  });

  it('ouvre une notification en fermant le panneau', () => {
    const page = component();
    page.toggle();

    page.openNotification(notification);

    expect(page.open()).toBe(false);
    expect(services.open).toHaveBeenCalledWith(notification);
  });

  it('expose les helpers de ton, icône et temps', () => {
    const page = component();
    expect(page.icon('TURN_ENDED')).toBe('event_repeat');
    expect(page.tone('ALLIANCE_BROKEN')).toBe('danger');
    expect(page.time(notification.createdAt)).toBeTruthy();
  });
});
