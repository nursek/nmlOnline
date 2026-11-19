import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { InstanceService } from './instance.service';
import { GameNotification, NotificationFeed } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';
import { safeNotificationLink } from '../core/notification.helpers';

const POLL_MS = 30_000;
const TOAST_MS = 8_000;
const MAX_TOASTS = 3;

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly instance = inject(InstanceService);
  private readonly router = inject(Router);

  private readonly _items = signal<GameNotification[]>([]);
  private readonly _unreadCount = signal(0);
  private readonly _toasts = signal<GameNotification[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly items = this._items.asReadonly();
  readonly unreadCount = this._unreadCount.asReadonly();
  readonly toasts = this._toasts.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();
  readonly preview = computed(() => this._items().slice(0, 5));
  readonly unreadLabel = computed(() => {
    const count = this._unreadCount();
    return count > 99 ? '99+' : String(count);
  });

  private readonly knownIds = new Set<number>();
  private readonly toastTimers = new Map<number, number>();
  private primed = false;
  private inFlight = false;
  private generation = 0;

  constructor() {
    effect((onCleanup) => {
      if (!this.auth.initialized() || !this.auth.user() || this.instance.selectedId() === null) {
        this.reset();
        return;
      }
      void this.reload();
      const timer = window.setInterval(() => void this.reload(), POLL_MS);
      onCleanup(() => window.clearInterval(timer));
    });
  }

  async reload(): Promise<void> {
    if (this.inFlight || !this.auth.user() || this.instance.selectedId() === null) {
      return;
    }
    const generation = this.generation;
    this.inFlight = true;
    this._loading.set(true);
    try {
      const feed = await firstValueFrom(this.api.getNotifications());
      if (generation === this.generation) {
        this.apply(feed);
        this._error.set(null);
      }
    } catch (error) {
      if (generation === this.generation) {
        this._error.set(httpErrorMessage(error, 'Impossible de charger les notifications.'));
      }
    } finally {
      this.inFlight = false;
      this._loading.set(false);
    }
  }

  async markRead(notificationId: number): Promise<void> {
    const item = this._items().find((candidate) => candidate.id === notificationId);
    if (!item || item.readAt) {
      return;
    }
    this.markLocally([notificationId]);
    try {
      await firstValueFrom(this.api.markNotificationRead(notificationId));
    } catch {
      void this.reload();
    }
  }

  async markAllRead(): Promise<void> {
    if (this._unreadCount() === 0) {
      return;
    }
    const now = new Date().toISOString();
    this._items.update((items) =>
      items.map((item) => (item.readAt ? item : { ...item, readAt: now })),
    );
    this._unreadCount.set(0);
    try {
      await firstValueFrom(this.api.markAllNotificationsRead());
    } catch {
      void this.reload();
    }
  }

  open(notification: GameNotification): void {
    void this.markRead(notification.id);
    this.dismiss(notification.id);
    const link = safeNotificationLink(notification.link);
    if (link) {
      void this.router.navigateByUrl(link);
    }
  }

  dismiss(notificationId: number): void {
    const timer = this.toastTimers.get(notificationId);
    if (timer !== undefined) {
      window.clearTimeout(timer);
      this.toastTimers.delete(notificationId);
    }
    this._toasts.update((toasts) => toasts.filter((toast) => toast.id !== notificationId));
  }

  private apply(feed: NotificationFeed): void {
    const fresh = this.primed
      ? feed.items.filter((item) => !item.readAt && !this.knownIds.has(item.id))
      : [];
    feed.items.forEach((item) => this.knownIds.add(item.id));
    this.primed = true;
    this._items.set(feed.items);
    this._unreadCount.set(feed.unreadCount);
    if (fresh.length) {
      this.pushToasts(fresh);
    }
  }

  private pushToasts(items: GameNotification[]): void {
    const fresh = items.slice(0, MAX_TOASTS);
    this._toasts.update((toasts) => [...fresh, ...toasts].slice(0, MAX_TOASTS));
    for (const item of fresh) {
      const timer = window.setTimeout(() => this.dismiss(item.id), TOAST_MS);
      this.toastTimers.set(item.id, timer);
    }
  }

  private markLocally(notificationIds: number[]): void {
    const now = new Date().toISOString();
    const ids = new Set(notificationIds);
    let marked = 0;
    this._items.update((items) =>
      items.map((item) => {
        if (!ids.has(item.id) || item.readAt) {
          return item;
        }
        marked++;
        return { ...item, readAt: now };
      }),
    );
    this._unreadCount.update((count) => Math.max(0, count - marked));
  }

  private reset(): void {
    this.generation++;
    this.inFlight = false;
    this.toastTimers.forEach((timer) => window.clearTimeout(timer));
    this.toastTimers.clear();
    this.knownIds.clear();
    this.primed = false;
    this._items.set([]);
    this._unreadCount.set(0);
    this._toasts.set([]);
    this._error.set(null);
    this._loading.set(false);
  }
}
