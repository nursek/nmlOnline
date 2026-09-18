import {
  Component,
  ChangeDetectionStrategy,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../services/auth.service';

interface NavLink {
  path: string;
  label: string;
  icon: string;
}

const BASE_MENU_ITEMS: NavLink[] = [
  { path: '/carte', label: 'Carte', icon: 'map' },
  { path: '/joueur', label: 'Mon Joueur', icon: 'person' },
  { path: '/ordres', label: 'Mes ordres', icon: 'list_alt' },
  { path: '/rapports', label: 'Rapports', icon: 'history' },
  { path: '/boutique', label: 'Boutique', icon: 'shopping_bag' },
  { path: '/regles', label: 'Règles', icon: 'menu_book' },
];

const ADMIN_MENU_ITEMS: NavLink[] = [
  { path: '/carte', label: 'Carte', icon: 'map' },
  { path: '/admin', label: 'Admin', icon: 'admin_panel_settings' },
  { path: '/admin/resolution', label: 'Fin de tour', icon: 'skip_next' },
  { path: '/regles', label: 'Règles', icon: 'menu_book' },
];

@Component({
  selector: 'app-navbar',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '[class.visible]': 'isAuthenticated()',
  },
  imports: [
    RouterLink,
    RouterLinkActive,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatListModule,
  ],
  template: `
    @if (isAuthenticated()) {
      <mat-toolbar class="navbar">
        <div class="navbar-content">
          <!-- Logo -->
          <a routerLink="/" class="logo-link">
            <mat-icon class="logo-icon">shield</mat-icon>
            <span class="logo-text">NML Online</span>
          </a>

          <!-- Menu mobile toggle -->
          @if (isMobile()) {
            <button
              mat-icon-button
              (click)="toggleDrawer()"
              [attr.aria-label]="drawerOpen() ? 'Fermer le menu' : 'Ouvrir le menu'"
            >
              <mat-icon>{{ drawerOpen() ? 'close' : 'menu' }}</mat-icon>
            </button>
          }

          <!-- Menu desktop -->
          @if (!isMobile()) {
            <nav class="nav-links">
              @for (item of menuItems(); track item.path) {
                <a
                  mat-button
                  [routerLink]="item.path"
                  routerLinkActive="active"
                  [routerLinkActiveOptions]="{ exact: true }"
                >
                  <mat-icon>{{ item.icon }}</mat-icon>
                  {{ item.label }}
                </a>
              }
            </nav>
          }

          <div class="spacer"></div>

          <!-- User info -->
          <div class="user-section">
            @if (user(); as user) {
              @if (user.username) {
                <mat-chip-set>
                  <mat-chip highlighted>
                    <mat-icon matChipAvatar>person</mat-icon>
                    {{ user.username }}
                  </mat-chip>
                </mat-chip-set>
              }
            }

            @if (!isMobile()) {
              <button mat-stroked-button color="warn" (click)="logout()">
                <mat-icon>logout</mat-icon>
                Déconnexion
              </button>
            } @else {
              <button mat-icon-button color="warn" (click)="logout()" aria-label="Déconnexion">
                <mat-icon>logout</mat-icon>
              </button>
            }
          </div>
        </div>
      </mat-toolbar>

      <!-- Mobile drawer overlay -->
      @if (isMobile() && drawerOpen()) {
        <div
          class="mobile-drawer-backdrop"
          (click)="toggleDrawer()"
          role="button"
          tabindex="-1"
          aria-label="Fermer le menu"
        ></div>
        <div class="mobile-drawer" role="navigation" aria-label="Menu principal">
          <mat-nav-list>
            @for (item of menuItems(); track item.path) {
              <a
                mat-list-item
                [routerLink]="item.path"
                routerLinkActive="active"
                [routerLinkActiveOptions]="{ exact: true }"
                (click)="toggleDrawer()"
              >
                <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
                <span matListItemTitle>{{ item.label }}</span>
              </a>
            }
          </mat-nav-list>
        </div>
      }
    }
  `,
  styles: [
    `
      :host {
        display: none; /* Hidden by default */
      }

      :host.visible {
        display: block;
        height: 56px; /* Reserve space for fixed navbar */
      }

      .navbar {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        z-index: 1001;
        background: var(--paper-2);
        border-bottom: 3px solid var(--ink);
        box-shadow: 0 6px 0 rgba(23, 21, 15, 0.06);
      }

      .navbar::after {
        content: '';
        position: absolute;
        left: 0;
        bottom: -3px;
        width: 140px;
        height: 3px;
        background: var(--accent);
      }

      .navbar-content {
        display: flex;
        align-items: center;
        width: 100%;
        max-width: 1400px;
        margin: 0 auto;
        gap: 18px;
      }

      .logo-link {
        display: flex;
        align-items: center;
        text-decoration: none;
        color: var(--ink);
        gap: 10px;
      }

      .logo-icon {
        font-size: 20px;
        width: 34px;
        height: 34px;
        line-height: 34px;
        text-align: center;
        color: var(--surface);
        background: var(--accent);
        border: 1.5px solid var(--ink);
        border-radius: var(--r-sm);
        transform: rotate(-3deg);
      }

      .logo-text {
        font-family: var(--font-display);
        font-size: 1.3rem;
        font-weight: 800;
        letter-spacing: -0.02em;
        color: var(--ink);
      }

      .nav-links {
        display: flex;
        gap: 4px;
        margin-left: 18px;
      }

      .nav-links a {
        font-size: 0.78rem;
        font-weight: 700;
        letter-spacing: 0.09em;
        text-transform: uppercase;
        color: var(--ink-2);

        &:hover {
          background: rgba(23, 21, 15, 0.08);
          color: var(--ink);
        }

        &.active {
          background: var(--line-2);
          color: var(--ink);
        }

        mat-icon {
          margin-right: 6px;
        }
      }

      .spacer {
        flex: 1;
      }

      .user-section {
        display: flex;
        align-items: center;
        gap: 16px;
      }

      .mobile-drawer-backdrop {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(23, 21, 15, 0.55);
        z-index: 999;
        animation: fadeIn 0.2s ease;
        touch-action: none;
      }

      .mobile-drawer {
        position: fixed;
        top: 56px;
        left: 0;
        width: 280px;
        max-width: 80vw;
        height: calc(100dvh - 56px);
        background: var(--paper);
        z-index: 1000;
        border-top: 3px solid var(--ink);
        box-shadow: 6px 0 0 rgba(23, 21, 15, 0.18);
        animation: slideIn 0.25s ease;
        overflow-y: auto;
        overscroll-behavior: contain;
        -webkit-overflow-scrolling: touch;

        mat-nav-list {
          padding-top: 8px;
        }

        a {
          color: var(--ink-2);
          font-size: 0.82rem;
          font-weight: 600;
          letter-spacing: 0.05em;
          text-transform: uppercase;

          mat-icon {
            color: var(--ink-3);
          }

          &:hover {
            background: rgba(23, 21, 15, 0.06);
          }

          &.active {
            background: var(--line-2);
            color: var(--ink);

            mat-icon {
              color: var(--accent-ink);
            }
          }
        }
      }

      @keyframes fadeIn {
        from {
          opacity: 0;
        }
        to {
          opacity: 1;
        }
      }

      @keyframes slideIn {
        from {
          transform: translateX(-100%);
        }
        to {
          transform: translateX(0);
        }
      }
    `,
  ],
})
export class NavbarComponent {
  private readonly auth = inject(AuthService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly document = inject(DOCUMENT);

  readonly isAuthenticated = this.auth.isAuthenticated;
  readonly isAdmin = this.auth.isAdmin;
  readonly user = this.auth.user;

  readonly isMobile = toSignal(
    this.breakpointObserver
      .observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
      .pipe(map((result) => result.matches)),
    { initialValue: false },
  );

  readonly drawerOpen = signal(false);

  readonly menuItems = computed<NavLink[]>(() =>
    this.isAdmin() ? ADMIN_MENU_ITEMS : BASE_MENU_ITEMS,
  );

  // Block body scroll while the mobile drawer is open (DOM/3rd-party sync effect).
  constructor() {
    effect(() => {
      const locked = this.drawerOpen();
      const body = this.document.body;
      body.style.overflow = locked ? 'hidden' : '';
    });
  }

  toggleDrawer(): void {
    this.drawerOpen.update((v) => !v);
  }

  logout(): void {
    void this.auth.logout();
  }
}
