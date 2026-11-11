import {
  Component,
  ChangeDetectionStrategy,
  computed,
  effect,
  inject,
  signal,
} from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatListModule } from '@angular/material/list';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map } from 'rxjs/operators';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../services/auth.service';
import { InstanceService } from '../../services/instance.service';
import { DevScenarioService } from '../../services/dev-scenario.service';
import { SigilComponent } from '../../shared/sigil/sigil.component';
import { ThemeToggleComponent } from '../../shared/theme-toggle/theme-toggle.component';

interface NavLink {
  path: string;
  label: string;
  icon: string;
}

const BASE_MENU_ITEMS: NavLink[] = [
  { path: '/accueil', label: 'Accueil', icon: 'home' },
  { path: '/carte', label: 'Carte', icon: 'map' },
  { path: '/joueur', label: 'Mon Joueur', icon: 'person' },
  { path: '/ordres', label: 'Mes ordres', icon: 'list_alt' },
  { path: '/rapports', label: 'Rapports', icon: 'history' },
  { path: '/boutique', label: 'Boutique', icon: 'shopping_bag' },
  { path: '/regles', label: 'Règles', icon: 'menu_book' },
];

const ADMIN_MENU_ITEMS: NavLink[] = [
  { path: '/accueil', label: 'Accueil', icon: 'home' },
  { path: '/carte', label: 'Carte', icon: 'map' },
  { path: '/admin', label: 'Admin', icon: 'admin_panel_settings' },
  { path: '/admin/tour', label: 'Gestion du tour', icon: 'event_repeat' },
  { path: '/admin/joueurs', label: 'Gestion des joueurs', icon: 'groups' },
  { path: '/admin/scenarios', label: 'Scénarios de tests', icon: 'science' },
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
    MatMenuModule,
    MatListModule,
    SigilComponent,
    ThemeToggleComponent,
  ],
  template: `
    @if (isAuthenticated()) {
      <mat-toolbar class="navbar">
        <div class="navbar-content">
          <!-- Logo -->
          <a routerLink="/" class="logo-link">
            <span class="logo-badge">
              <app-sigil [size]="26" />
            </span>
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

          <div class="user-section">
            <app-theme-toggle />

            @if (user(); as user) {
              <button
                mat-button
                class="user-chip"
                [matMenuTriggerFor]="userMenu"
                [attr.aria-label]="'Compte de ' + user.username"
              >
                <mat-icon>person</mat-icon>
                <span class="user-name">{{ user.username }}</span>
                <mat-icon iconPositionEnd>arrow_drop_down</mat-icon>
              </button>

              <mat-menu #userMenu="matMenu">
                @if (canSwitchInstance()) {
                  <button mat-menu-item (click)="switchInstance()">
                    <mat-icon>grid_view</mat-icon>
                    <span>Changer de partie</span>
                  </button>
                }
                <button mat-menu-item (click)="logout()">
                  <mat-icon>logout</mat-icon>
                  <span>Déconnexion</span>
                </button>
              </mat-menu>
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
        height: var(--nav-h); /* Reserve space for fixed navbar */
      }

      .navbar {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        z-index: 1001;
        height: var(--nav-h);
        background:
          linear-gradient(180deg, color-mix(in srgb, var(--ink) 4%, transparent), transparent 55%),
          linear-gradient(180deg, var(--surface-2), var(--paper));
        border-bottom: 3px solid var(--line-strong);
        box-shadow: 0 6px 0 rgba(0, 0, 0, 0.25);
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

      .logo-badge {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 38px;
        height: 38px;
        color: var(--accent);
        background:
          linear-gradient(180deg, rgba(230, 222, 203, 0.06), transparent), var(--surface-3);
        border: 1px solid var(--line-strong);
        border-radius: var(--r-sm);
        transform: rotate(-3deg);
        box-shadow: var(--glow-amber);
      }

      .logo-text {
        font-family: var(--font-display);
        font-size: 1.15rem;
        font-weight: 700;
        letter-spacing: 0.08em;
        text-transform: uppercase;
        color: var(--ink);
      }

      .nav-links {
        display: flex;
        gap: 2px;
        margin-left: 12px;
      }

      .nav-links a {
        font-family: var(--font-mono);
        font-size: 0.72rem;
        font-weight: 600;
        letter-spacing: 0.1em;
        text-transform: uppercase;
        color: var(--ink-2);
        border-radius: var(--r-sm);

        &:hover {
          background: color-mix(in srgb, var(--ink) 8%, transparent);
          color: var(--ink);
        }

        &.active {
          background: var(--accent-tint);
          color: var(--accent-ink);
          box-shadow: inset 0 -2px 0 var(--accent);
        }

        mat-icon {
          margin-right: 6px;
          font-size: 18px;
          width: 18px;
          height: 18px;
        }
      }

      .spacer {
        flex: 1;
      }

      .user-section {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .user-chip {
        background: var(--accent-tint);
        color: var(--accent-ink);
        border-radius: 999px;
      }

      .user-name {
        max-width: 12rem;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .mobile-drawer-backdrop {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.7);
        z-index: 999;
        animation: fadeIn 0.2s ease;
        touch-action: none;
      }

      .mobile-drawer {
        position: fixed;
        top: var(--nav-h);
        left: 0;
        width: 280px;
        max-width: 80vw;
        height: calc(100dvh - var(--nav-h));
        background: var(--paper-2);
        z-index: 1000;
        border-right: 3px solid var(--line-strong);
        box-shadow: var(--shadow-pop);
        animation: slideIn 0.25s ease;
        overflow-y: auto;
        overscroll-behavior: contain;
        -webkit-overflow-scrolling: touch;

        mat-nav-list {
          padding-top: 8px;
        }

        a {
          font-family: var(--font-mono);
          color: var(--ink-2);
          font-size: 0.78rem;
          font-weight: 600;
          letter-spacing: 0.08em;
          text-transform: uppercase;

          mat-icon {
            color: var(--ink-3);
          }

          &:hover {
            background: color-mix(in srgb, var(--ink) 6%, transparent);
          }

          &.active {
            background: var(--accent-tint);
            color: var(--accent-ink);

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
  private readonly instance = inject(InstanceService);
  private readonly router = inject(Router);
  private readonly devScenarios = inject(DevScenarioService);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly document = inject(DOCUMENT);

  readonly isAuthenticated = this.auth.isAuthenticated;
  readonly isAdmin = this.auth.isAdmin;
  readonly canAdmin = computed<boolean>(() => {
    const role = this.instance.selectedRole();
    return this.auth.isAdmin() || role === 'ADMIN' || role === 'MJ';
  });
  readonly user = this.auth.user;
  readonly canSwitchInstance = computed(() => this.instance.instances().length > 1);

  readonly isMobile = toSignal(
    this.breakpointObserver
      .observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
      .pipe(map((result) => result.matches)),
    { initialValue: false },
  );

  readonly drawerOpen = signal(false);

  readonly menuItems = computed<NavLink[]>(() => {
    if (!this.canAdmin()) {
      return BASE_MENU_ITEMS;
    }
    return this.devScenarios.available()
      ? ADMIN_MENU_ITEMS
      : ADMIN_MENU_ITEMS.filter((item) => item.path !== '/admin/scenarios');
  });

  // Block body scroll while the mobile drawer is open (DOM/3rd-party sync effect).
  constructor() {
    effect(() => {
      const locked = this.drawerOpen();
      const body = this.document.body;
      body.style.overflow = locked ? 'hidden' : '';
    });
  }

  switchInstance(): void {
    this.drawerOpen.set(false);
    void this.router.navigate(['/parties']);
  }

  toggleDrawer(): void {
    this.drawerOpen.update((v) => !v);
  }

  logout(): void {
    void this.auth.logout();
  }
}
