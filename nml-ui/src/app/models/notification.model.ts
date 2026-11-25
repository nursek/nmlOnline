export type NotificationType =
  | 'EXCHANGE_RECEIVED'
  | 'EXCHANGE_ACCEPTED'
  | 'EXCHANGE_DECLINED'
  | 'EXCHANGE_CANCELLED'
  | 'ALLIANCE_PROPOSAL'
  | 'ALLIANCE_ACCEPTED'
  | 'ALLIANCE_DECLINED'
  | 'ALLIANCE_BROKEN'
  | 'TURN_ENDED'
  | 'GAME_STARTED'
  | 'GAME_FINISHED'
  | 'BATTLE_REPORTS_AVAILABLE'
  | 'GAZETTE_PUBLISHED';

export type NotificationTone = 'accent' | 'danger' | 'teal' | 'cobalt';

export interface GameNotification {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  link: string | null;
  createdAt: string;
  readAt: string | null;
}

export interface NotificationFeed {
  items: GameNotification[];
  unreadCount: number;
}
