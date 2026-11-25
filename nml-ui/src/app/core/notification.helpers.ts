import { NotificationTone, NotificationType } from '../models';

export function notificationIcon(type: NotificationType): string {
  switch (type) {
    case 'EXCHANGE_RECEIVED':
    case 'EXCHANGE_ACCEPTED':
    case 'EXCHANGE_DECLINED':
    case 'EXCHANGE_CANCELLED':
      return 'swap_horiz';
    case 'ALLIANCE_PROPOSAL':
    case 'ALLIANCE_ACCEPTED':
    case 'ALLIANCE_DECLINED':
      return 'handshake';
    case 'ALLIANCE_BROKEN':
      return 'heart_broken';
    case 'TURN_ENDED':
      return 'event_repeat';
    case 'GAME_STARTED':
      return 'flag';
    case 'GAME_FINISHED':
      return 'emoji_events';
    case 'PLAYER_ELIMINATED':
      return 'dangerous';
    case 'GAZETTE_PUBLISHED':
      return 'newspaper';
    default:
      return 'notifications';
  }
}

export function notificationTone(type: NotificationType): NotificationTone {
  switch (type) {
    case 'EXCHANGE_RECEIVED':
      return 'cobalt';
    case 'ALLIANCE_PROPOSAL':
      return 'accent';
    case 'ALLIANCE_BROKEN':
    case 'PLAYER_ELIMINATED':
      return 'danger';
    case 'EXCHANGE_ACCEPTED':
    case 'ALLIANCE_ACCEPTED':
    case 'GAME_FINISHED':
      return 'teal';
    default:
      return 'accent';
  }
}

export function notificationTime(createdAt: string, now = Date.now()): string {
  const formatter = new Intl.RelativeTimeFormat('fr', { numeric: 'auto' });
  const minutes = Math.min(0, Math.round((new Date(createdAt).getTime() - now) / 60_000));
  if (Math.abs(minutes) < 60) {
    return formatter.format(minutes, 'minute');
  }
  const hours = Math.round(minutes / 60);
  if (Math.abs(hours) < 24) {
    return formatter.format(hours, 'hour');
  }
  const days = Math.round(hours / 24);
  if (Math.abs(days) < 7) {
    return formatter.format(days, 'day');
  }
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short' }).format(
    new Date(createdAt),
  );
}

export function safeNotificationLink(link: string | null): string | null {
  return link && link.startsWith('/') && !link.startsWith('//') ? link : null;
}
