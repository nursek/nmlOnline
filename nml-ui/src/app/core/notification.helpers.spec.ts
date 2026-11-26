import {
  notificationIcon,
  notificationTime,
  notificationTone,
  safeNotificationLink,
} from './notification.helpers';

describe('notification.helpers', () => {
  it('safeNotificationLink accepte les routes SPA internes seulement', () => {
    expect(safeNotificationLink('/joueur?alliance=1')).toBe('/joueur?alliance=1');
    expect(safeNotificationLink('/podium')).toBe('/podium');
    expect(safeNotificationLink(null)).toBeNull();
    expect(safeNotificationLink('')).toBeNull();
    expect(safeNotificationLink('//evil.example/phish')).toBeNull();
    expect(safeNotificationLink('https://evil.example/phish')).toBeNull();
    expect(safeNotificationLink('javascript:alert(1)')).toBeNull();
  });

  it('mappe les types sur une icône et une tonalité', () => {
    expect(notificationIcon('EXCHANGE_RECEIVED')).toBe('swap_horiz');
    expect(notificationIcon('ALLIANCE_BROKEN')).toBe('heart_broken');
    expect(notificationIcon('GAME_FINISHED')).toBe('emoji_events');
    expect(notificationIcon('UNKNOWN' as never)).toBe('notifications');
    expect(notificationTone('PLAYER_ELIMINATED')).toBe('danger');
    expect(notificationTone('GAME_FINISHED')).toBe('teal');
  });

  it('formate le temps relatif en minutes, heures, jours puis date', () => {
    const now = new Date('2026-11-25T12:00:00Z').getTime();
    expect(notificationTime('2026-11-25T11:55:00Z', now)).toContain('5 minutes');
    expect(notificationTime('2026-11-25T09:00:00Z', now)).toContain('3 heures');
    expect(notificationTime('2026-11-22T12:00:00Z', now)).toContain('3 jours');
    expect(notificationTime('2026-11-01T12:00:00Z', now)).toMatch(/nov/);
  });
});
