import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NotificationsService } from '../../services/notifications.service';
import { GameNotification } from '../../models';
import {
  notificationIcon,
  notificationTime,
  notificationTone,
} from '../../core/notification.helpers';

type NotificationFilter = 'all' | 'unread';

@Component({
  selector: 'app-notifications',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatCardModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <section class="notifications-page">
      <header class="page-header">
        <div class="title">
          <mat-icon>notifications</mat-icon>
          <h1>Notifications</h1>
        </div>
        <span class="subtitle">
          {{ unreadCount() }} non lue(s) · {{ notifications.items().length }} au total
        </span>
      </header>

      @if (notifications.loading() && !notifications.hasLoaded()) {
        <div class="loading-state" role="status">
          <mat-spinner diameter="40"></mat-spinner>
          <p>Chargement des notifications...</p>
        </div>
      } @else if (notifications.error() && !notifications.items().length) {
        <div class="error-alert" role="alert">
          <mat-icon>error</mat-icon>
          <span>Impossible de charger les notifications.</span>
          <button
            mat-stroked-button
            type="button"
            class="readonly-allow"
            (click)="notifications.reload()"
          >
            Réessayer
          </button>
        </div>
      } @else {
        <div class="toolbar">
          <div class="filters" role="group" aria-label="Filtrer les notifications">
            <button
              type="button"
              class="filter-chip readonly-allow"
              [class.active]="filter() === 'all'"
              [attr.aria-pressed]="filter() === 'all'"
              (click)="filter.set('all')"
            >
              Toutes <span>{{ notifications.items().length }}</span>
            </button>
            <button
              type="button"
              class="filter-chip readonly-allow"
              [class.active]="filter() === 'unread'"
              [attr.aria-pressed]="filter() === 'unread'"
              (click)="filter.set('unread')"
            >
              Non lues <span>{{ unreadCount() }}</span>
            </button>
          </div>
          <button
            mat-stroked-button
            type="button"
            class="readonly-allow"
            [disabled]="unreadCount() === 0"
            (click)="notifications.markAllRead()"
          >
            <mat-icon>done_all</mat-icon>
            Tout marquer comme lu
          </button>
        </div>

        @if (!visible().length) {
          <mat-card class="empty-card">
            <mat-card-content>
              <mat-icon>notifications_none</mat-icon>
              <h2>
                {{ filter() === 'unread' ? 'Aucune notification non lue' : 'Aucune notification' }}
              </h2>
              <p>
                Les échanges, alliances, fins de tour, éliminations et nouvelles éditions du journal
                apparaîtront ici.
              </p>
            </mat-card-content>
          </mat-card>
        } @else {
          <div class="notification-list">
            @for (item of visible(); track item.id) {
              <button
                type="button"
                class="notification-row readonly-allow"
                [class.unread]="!item.readAt"
                (click)="notifications.open(item)"
              >
                <span class="row-icon" [class]="'tone-' + tone(item.type)">
                  <mat-icon>{{ icon(item.type) }}</mat-icon>
                </span>
                <span class="row-content">
                  <span class="row-head">
                    <strong>{{ item.title }}</strong>
                    <time [attr.datetime]="item.createdAt">{{ time(item.createdAt) }}</time>
                  </span>
                  <span class="row-message">{{ item.message }}</span>
                  @if (item.link) {
                    <span class="row-link">
                      Ouvrir
                      <mat-icon>arrow_forward</mat-icon>
                    </span>
                  }
                </span>
                @if (!item.readAt) {
                  <span class="unread-dot" aria-hidden="true"></span>
                }
              </button>
            }
          </div>
        }
      }
    </section>
  `,
  styles: [
    `
      @use '../../../styles/shared' as shared;

      .notifications-page {
        @include shared.page-container;
        max-width: 1040px;
      }

      .page-header {
        @include shared.page-header;
        flex-wrap: wrap;
        gap: 12px;
      }

      .title {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-icon {
          @include shared.header-icon-plate(42px, 24px);
        }

        h1 {
          @include shared.gradient-title;
        }
      }

      .subtitle {
        @include shared.subtitle;
        margin-left: auto;
      }

      .loading-state {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 12px;
        padding: 64px 16px;
        color: var(--muted);
      }

      .error-alert {
        @include shared.error-alert;

        button {
          margin-left: auto;
        }
      }

      .toolbar {
        display: flex;
        align-items: center;
        justify-content: space-between;
        flex-wrap: wrap;
        gap: 12px;
        margin-bottom: 16px;
      }

      .filters {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .filter-chip {
        @include shared.filter-chip;

        span {
          margin-left: 4px;
          opacity: 0.7;
          font-size: 0.66rem;
        }
      }

      .notification-list {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .notification-row {
        display: grid;
        grid-template-columns: 40px minmax(0, 1fr) 10px;
        align-items: start;
        gap: 12px;
        width: 100%;
        padding: 14px 16px;
        border: 1.5px solid var(--line-2);
        border-left: 3px solid transparent;
        border-radius: var(--r-md);
        background:
          linear-gradient(180deg, color-mix(in srgb, var(--ink) 3%, transparent), transparent 45%),
          var(--surface);
        color: var(--ink);
        text-align: left;
        cursor: pointer;
        transition:
          transform 0.16s ease,
          border-color 0.16s ease,
          background-color 0.16s ease;

        &:hover {
          transform: translate(-2px, -2px);
          border-color: var(--line-strong);
          box-shadow: var(--shadow-hard);
        }

        &:focus-visible {
          outline: 2px solid var(--accent);
          outline-offset: 1px;
        }

        &.unread {
          border-left-color: var(--accent);
          background:
            linear-gradient(
              90deg,
              color-mix(in srgb, var(--accent) 8%, transparent),
              transparent 45%
            ),
            var(--surface-2);
        }
      }

      .row-icon {
        display: grid;
        place-items: center;
        width: 40px;
        height: 40px;
        border: 1px solid var(--line-2);
        border-radius: var(--r-sm);
        background: var(--surface-3);

        mat-icon {
          font-size: 20px;
          width: 20px;
          height: 20px;
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

      .row-content {
        display: flex;
        flex-direction: column;
        gap: 4px;
        min-width: 0;
      }

      .row-head {
        display: flex;
        align-items: baseline;
        justify-content: space-between;
        gap: 12px;

        strong {
          font-family: var(--font-display);
          font-size: 0.95rem;
          font-weight: 600;
          letter-spacing: 0.02em;
        }

        time {
          flex: 0 0 auto;
          color: var(--muted);
          font-family: var(--font-mono);
          font-size: 0.68rem;
        }
      }

      .row-message {
        color: var(--ink-2);
        font-size: 0.85rem;
        line-height: 1.45;
      }

      .row-link {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        margin-top: 2px;
        color: var(--accent-ink);
        font-family: var(--font-mono);
        font-size: 0.65rem;
        font-weight: 600;
        letter-spacing: 0.06em;
        text-transform: uppercase;

        mat-icon {
          font-size: 14px;
          width: 14px;
          height: 14px;
        }
      }

      .unread-dot {
        width: 8px;
        height: 8px;
        margin-top: 6px;
        border-radius: 50%;
        background: var(--danger);
        box-shadow: 0 0 0 2px color-mix(in srgb, var(--danger) 20%, transparent);
      }

      .empty-card {
        @include shared.card;

        mat-card-content {
          display: flex;
          flex-direction: column;
          align-items: flex-start;
          gap: 8px;
          padding: 20px;

          mat-icon {
            color: var(--line-2);
            font-size: 38px;
            width: 38px;
            height: 38px;
          }

          h2 {
            margin: 0;
            font-family: var(--font-display);
            font-size: 1.1rem;
          }

          p {
            margin: 0;
            color: var(--muted);
            font-size: 0.85rem;
          }
        }
      }

      @media (max-width: 720px) {
        .subtitle {
          width: 100%;
          margin-left: 0;
        }

        .toolbar > button {
          width: 100%;
        }

        .row-head {
          flex-direction: column;
          gap: 2px;
        }
      }
    `,
  ],
})
export class NotificationsComponent {
  readonly notifications = inject(NotificationsService);
  readonly filter = signal<NotificationFilter>('all');

  readonly unreadCount = this.notifications.unreadCount;
  readonly visible = computed<GameNotification[]>(() =>
    this.filter() === 'unread'
      ? this.notifications.items().filter((item) => !item.readAt)
      : this.notifications.items(),
  );

  readonly icon = notificationIcon;
  readonly tone = notificationTone;
  readonly time = notificationTime;
}
