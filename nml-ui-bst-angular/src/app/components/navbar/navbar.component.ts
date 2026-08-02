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
  { path: '/boutique', label: 'Boutique', icon: 'shopping_bag' },
  { path: '/ordres', label: 'Mes ordres', icon: 'list_alt' },
  { path: '/regles', label: 'Règles', icon: 'menu_book' },
];

const ADMIN_LINK: NavLink = { path: '/admin', label: 'Admin', icon: 'admin_panel_settings' };

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
                <a mat-button [routerLink]="item.path" routerLinkActive="active">
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
        background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
        border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      }

      .navbar-content {
        display: flex;
        align-items: center;
        width: 100%;
        max-width: 1400px;
        margin: 0 auto;
        gap: 16px;
      }

      .logo-link {
        display: flex;
        align-items: center;
        text-decoration: none;
        color: inherit;
        gap: 8px;
      }

      .logo-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
        color: #6366f1;
      }

      .logo-text {
        font-size: 1.25rem;
        font-weight: 700;
        background: linear-gradient(135deg, #6366f1 0%, #8b5cf6 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }

      .nav-links {
        display: flex;
        gap: 8px;
        margin-left: 24px;
      }

      .nav-links a {
        color: rgba(255, 255, 255, 0.7);

        &:hover {
          background: rgba(255, 255, 255, 0.08);
        }

        &.active {
          background: #6366f1;
          color: white;
        }

        mat-icon {
          margin-right: 8px;
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
        background: rgba(0, 0, 0, 0.5);
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
        background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
        z-index: 1000;
        box-shadow: 4px 0 15px rgba(0, 0, 0, 0.4);
        animation: slideIn 0.25s ease;
        overflow-y: auto;
        overscroll-behavior: contain;
        -webkit-overflow-scrolling: touch;
        border-top: 1px solid rgba(99, 102, 241, 0.3);

        mat-nav-list {
          padding-top: 8px;
        }

        a {
          color: rgba(255, 255, 255, 0.8);

          mat-icon {
            color: rgba(255, 255, 255, 0.7);
          }

          &:hover {
            background: rgba(255, 255, 255, 0.08);
          }

          &.active {
            background: rgba(99, 102, 241, 0.2);
            color: #818cf8;

            mat-icon {
              color: #818cf8;
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
    this.isAdmin() ? [...BASE_MENU_ITEMS, ADMIN_LINK] : BASE_MENU_ITEMS,
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
