import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { NotificationsComponent } from './notifications.component';
import { NotificationsService } from '../../services/notifications.service';
import type { GameNotification } from '../../models';

describe('NotificationsComponent', () => {
  const read: GameNotification = {
    id: 1,
    type: 'TURN_ENDED',
    title: 'Tour terminé',
    message: 'Tour 2',
    link: null,
    createdAt: '2026-11-25T12:00:00Z',
    readAt: '2026-11-25T12:05:00Z',
  };
  const unread: GameNotification = {
    ...read,
    id: 2,
    type: 'ALLIANCE_PROPOSAL',
    title: 'Alliance',
    readAt: null,
  };
  const service = {
    items: signal([read, unread]),
    loading: signal(false),
    hasLoaded: signal(true),
    error: signal<string | null>(null),
    unreadCount: signal(1),
    markAllRead: jest.fn(),
    reload: jest.fn(),
  };

  beforeEach(() => {
    jest.clearAllMocks();
    TestBed.configureTestingModule({
      providers: [{ provide: NotificationsService, useValue: service }],
    });
  });

  function component(): NotificationsComponent {
    return TestBed.runInInjectionContext(() => new NotificationsComponent());
  }

  it('filtre toutes les notifications puis les non lues', () => {
    const page = component();

    expect(page.filter()).toBe('all');
    expect(page.visible()).toHaveLength(2);
    page.filter.set('unread');
    expect(page.visible().map((item) => item.id)).toEqual([2]);
    expect(page.unreadCount()).toBe(1);
  });

  it('expose les libellés d’icône, de tonalité et de temps', () => {
    const page = component();

    expect(page.icon('ALLIANCE_PROPOSAL')).toBe('handshake');
    expect(page.tone('ALLIANCE_PROPOSAL')).toBe('accent');
    expect(page.time(unread.createdAt)).toBeTruthy();
  });
});
