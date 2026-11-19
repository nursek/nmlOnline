import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  computed,
  inject,
  signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { NotificationsService } from '../../services/notifications.service';
import { GameNotification } from '../../models';
import {
  notificationIcon,
  notificationTime,
  notificationTone,
} from '../../core/notification.helpers';

@Component({
  selector: 'app-notification-bell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  host: {
    '(document:click)': 'onDocumentClick($event)',
    '(document:keydown.escape)': 'close()',
  },
  template: `
    <button
      mat-icon-button
      type="button"
      class="bell"
      [attr.aria-label]="bellLabel()"
      [attr.aria-expanded]="open()"
      aria-haspopup="dialog"
      aria-controls="notification-panel"
      (click)="toggle()"
    >
      <mat-icon>notifications</mat-icon>
      @if (notifications.unreadCount() > 0) {
        <span class="badge" aria-hidden="true">{{ notifications.unreadLabel() }}</span>
      }
    </button>

    @if (open()) {
      <section
        id="notification-panel"
        class="panel"
        role="dialog"
        aria-label="Centre de notifications"
      >
        <header class="panel-head">
          <div>
            <strong>Notifications</strong>
            <span class="panel-subtitle"> {{ notifications.unreadCount() }} non lue(s) </span>
          </div>
          <button
            mat-button
            type="button"
            class="panel-action"
            aria-label="Marquer toutes les notifications comme lues"
            [disabled]="notifications.unreadCount() === 0"
            (click)="notifications.markAllRead()"
          >
            Tout lire
          </button>
        </header>

        @if (!notifications.items().length) {
          <div class="panel-empty">
            <mat-icon>notifications_none</mat-icon>
            <p>Aucune notification pour cette partie.</p>
          </div>
        } @else {
          <ul class="panel-list">
            @for (item of notifications.preview(); track item.id) {
              <li>
                <button
                  type="button"
                  class="panel-item"
                  [class.unread]="!item.readAt"
                  (click)="openNotification(item)"
                >
                  <span class="item-icon" [class]="'tone-' + tone(item.type)">
                    <mat-icon>{{ icon(item.type) }}</mat-icon>
                  </span>
                  <span class="item-content">
                    <span class="item-head">
                      <strong>{{ item.title }}</strong>
                      <time [attr.datetime]="item.createdAt">{{ time(item.createdAt) }}</time>
                    </span>
                    <span class="item-message">{{ item.message }}</span>
                  </span>
                  @if (!item.readAt) {
                    <span class="unread-dot" aria-hidden="true"></span>
                  }
                </button>
              </li>
            }
          </ul>
        }
        <footer class="panel-foot">
          <a mat-button routerLink="/notifications" (click)="close()">
            Voir toutes les notifications
          </a>
        </footer>
      </section>
    }

    <div class="toast-stack" aria-live="polite">
      @for (toast of notifications.toasts(); track toast.id) {
        <article class="toast" [class]="'toast tone-' + tone(toast.type)">
          <span class="toast-icon">
            <mat-icon>{{ icon(toast.type) }}</mat-icon>
          </span>
          <button type="button" class="toast-content" (click)="openNotification(toast)">
            <strong>{{ toast.title }}</strong>
            <span>{{ toast.message }}</span>
          </button>
          <button
            type="button"
            class="toast-close"
            aria-label="Fermer la notification"
            (click)="notifications.dismiss(toast.id)"
          >
            <mat-icon>close</mat-icon>
          </button>
        </article>
      }
    </div>
  `,
  styles: [
    `
      @use '../../../styles/shared' as shared;

      :host {
        display: inline-flex;
      }

      .bell {
        position: relative;
        color: var(--ink-2);

        &:hover {
          color: var(--accent);
        }
      }

      .badge {
        position: absolute;
        top: 1px;
        right: -1px;
        display: grid;
        place-items: center;
        min-width: 18px;
        height: 18px;
        padding: 0 4px;
        border: 1.5px solid var(--paper);
        border-radius: var(--r-pill);
        background: var(--danger);
        color: var(--void);
        font-family: var(--font-mono);
        font-size: 0.6rem;
        font-weight: 700;
        line-height: 1;
      }

      .panel {
        position: fixed;
        top: calc(var(--nav-h) + 10px);
        right: max(16px, calc((100vw - 1400px) / 2 + 16px));
        z-index: 1002;
        display: flex;
        flex-direction: column;
        width: min(390px, calc(100vw - 24px));
        max-height: min(560px, calc(100dvh - var(--nav-h) - 24px));
        overflow: hidden;
        border: 1.5px solid var(--line-strong);
        border-radius: var(--r-md);
        background:
          linear-gradient(180deg, color-mix(in srgb, var(--ink) 5%, transparent), transparent 45%),
          linear-gradient(180deg, var(--surface-2), var(--surface));
        box-shadow: var(--shadow-pop);
        white-space: normal;
        animation: panelIn 0.18s ease;
      }

      .panel::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 3px;
        background: linear-gradient(90deg, var(--accent), transparent 70%);
      }

      .panel-head {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 14px 14px 10px;
        border-bottom: 1px solid var(--line);

        strong {
          display: block;
          font-family: var(--font-display);
          font-size: 1rem;
          letter-spacing: 0.06em;
          text-transform: uppercase;
        }
      }

      .panel-subtitle {
        display: block;
        color: var(--muted);
        font-family: var(--font-mono);
        font-size: 0.68rem;
        letter-spacing: 0.04em;
      }

      .panel-action {
        flex: 0 0 auto;
        font-family: var(--font-mono);
        font-size: 0.68rem;
        letter-spacing: 0.05em;
        text-transform: uppercase;
      }

      .panel-list {
        @include shared.custom-scrollbar;
        flex: 1 1 auto;
        min-height: 0;
        margin: 0;
        padding: 0;
        overflow-y: auto;
        list-style: none;
      }

      .panel-item {
        display: grid;
        grid-template-columns: 32px minmax(0, 1fr) 8px;
        align-items: start;
        gap: 10px;
        width: 100%;
        padding: 11px 14px;
        border: 0;
        border-bottom: 1px solid var(--line);
        background: transparent;
        color: var(--ink);
        text-align: left;
        cursor: pointer;
        transition: background-color 0.15s ease;

        &:hover {
          background: color-mix(in srgb, var(--accent) 8%, transparent);
        }

        &:focus-visible {
          outline: 2px solid var(--accent);
          outline-offset: -2px;
        }

        &.unread {
          background: color-mix(in srgb, var(--accent) 6%, transparent);
        }
      }

      .item-icon,
      .toast-icon {
        display: grid;
        place-items: center;
        width: 32px;
        height: 32px;
        border: 1px solid var(--line-2);
        border-radius: var(--r-sm);
        background: var(--surface-3);

        mat-icon {
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .tone-accent {
        color: var(--accent);
        border-color: var(--accent-line);
        background: var(--accent-tint);
      }

      .tone-danger {
        color: var(--danger);
        border-color: var(--danger-line);
        background: var(--danger-tint);
      }

      .tone-teal {
        color: var(--teal);
        border-color: var(--teal-line);
        background: var(--teal-tint);
      }

      .tone-cobalt {
        color: var(--cobalt);
        border-color: color-mix(in srgb, var(--cobalt) 40%, var(--line-2));
        background: var(--cobalt-tint);
      }

      .item-content {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
      }

      .item-head {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 8px;

        strong {
          overflow: hidden;
          font-family: var(--font-sans);
          font-size: 0.83rem;
          font-weight: 600;
          text-overflow: ellipsis;
          white-space: nowrap;
        }

        time {
          flex: 0 0 auto;
          color: var(--muted);
          font-family: var(--font-mono);
          font-size: 0.62rem;
        }
      }

      .item-message {
        display: -webkit-box;
        overflow: hidden;
        color: var(--ink-2);
        font-size: 0.76rem;
        line-height: 1.35;
        -webkit-box-orient: vertical;
        -webkit-line-clamp: 2;
      }

      .unread-dot {
        width: 7px;
        height: 7px;
        margin-top: 6px;
        border-radius: 50%;
        background: var(--danger);
        box-shadow: 0 0 0 2px color-mix(in srgb, var(--danger) 20%, transparent);
      }

      .panel-empty {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 6px;
        padding: 26px 18px;
        color: var(--muted);

        mat-icon {
          color: var(--line-2);
          font-size: 30px;
          width: 30px;
          height: 30px;
        }

        p {
          margin: 0;
          font-size: 0.8rem;
        }
      }

      .panel-foot {
        display: flex;
        justify-content: flex-end;
        padding: 6px 8px;
        border-top: 1px solid var(--line);

        a {
          font-family: var(--font-mono);
          font-size: 0.68rem;
          letter-spacing: 0.04em;
          text-transform: uppercase;
        }
      }

      .toast-stack {
        position: fixed;
        top: calc(var(--nav-h) + 12px);
        right: max(16px, calc((100vw - 1400px) / 2 + 16px));
        z-index: 1005;
        display: flex;
        flex-direction: column;
        gap: 8px;
        width: min(390px, calc(100vw - 24px));
        pointer-events: none;
      }

      .toast {
        display: grid;
        grid-template-columns: 32px minmax(0, 1fr) 26px;
        align-items: center;
        gap: 9px;
        padding: 10px;
        border: 1.5px solid var(--line-strong);
        border-left: 3px solid var(--accent);
        border-radius: var(--r-sm);
        background:
          linear-gradient(180deg, color-mix(in srgb, var(--ink) 6%, transparent), transparent 55%),
          var(--surface-2);
        box-shadow: var(--shadow-pop);
        pointer-events: auto;
        white-space: normal;
        animation: toastIn 0.22s ease;

        &.tone-danger {
          border-left-color: var(--danger);
        }

        &.tone-teal {
          border-left-color: var(--teal);
        }

        &.tone-cobalt {
          border-left-color: var(--cobalt);
        }
      }

      .toast-content {
        display: flex;
        flex-direction: column;
        gap: 2px;
        min-width: 0;
        padding: 0;
        border: 0;
        background: transparent;
        color: var(--ink);
        text-align: left;
        cursor: pointer;

        strong {
          font-family: var(--font-sans);
          font-size: 0.82rem;
          font-weight: 600;
        }

        span {
          display: -webkit-box;
          overflow: hidden;
          color: var(--ink-2);
          font-size: 0.74rem;
          line-height: 1.35;
          -webkit-box-orient: vertical;
          -webkit-line-clamp: 2;
        }

        &:focus-visible {
          outline: 2px solid var(--accent);
          outline-offset: 2px;
        }
      }

      .toast-close {
        display: grid;
        place-items: center;
        width: 26px;
        height: 26px;
        padding: 0;
        border: 0;
        border-radius: var(--r-sm);
        background: transparent;
        color: var(--muted);
        cursor: pointer;

        &:hover {
          background: var(--surface-3);
          color: var(--ink);
        }

        mat-icon {
          font-size: 16px;
          width: 16px;
          height: 16px;
        }
      }

      @keyframes panelIn {
        from {
          opacity: 0;
          transform: translateY(-6px) scale(0.98);
        }
        to {
          opacity: 1;
          transform: translateY(0) scale(1);
        }
      }

      @keyframes toastIn {
        from {
          opacity: 0;
          transform: translateX(16px);
        }
        to {
          opacity: 1;
          transform: translateX(0);
        }
      }

      @media (max-width: 720px) {
        .panel {
          top: calc(var(--nav-h) + 6px);
          right: 8px;
          left: 8px;
          width: auto;
          max-height: calc(100dvh - var(--nav-h) - 12px);
        }

        .toast-stack {
          top: calc(var(--nav-h) + 6px);
          right: 8px;
          left: 8px;
          width: auto;
        }
      }
    `,
  ],
})
export class NotificationBellComponent {
  readonly notifications = inject(NotificationsService);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly open = signal(false);
  readonly bellLabel = computed(() => {
    const count = this.notifications.unreadCount();
    return count > 0 ? `Notifications, ${count} non lues` : 'Notifications';
  });

  readonly icon = notificationIcon;
  readonly tone = notificationTone;
  readonly time = notificationTime;

  toggle(): void {
    const next = !this.open();
    this.open.set(next);
    if (next) {
      void this.notifications.reload();
    }
  }

  close(): void {
    this.open.set(false);
  }

  onDocumentClick(event: MouseEvent): void {
    if (this.open() && !this.host.nativeElement.contains(event.target as Node)) {
      this.close();
    }
  }

  openNotification(notification: GameNotification): void {
    this.close();
    this.notifications.open(notification);
  }
}
