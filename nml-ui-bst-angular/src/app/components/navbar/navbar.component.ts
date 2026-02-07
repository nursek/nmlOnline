import { Component, inject, signal, effect, HostBinding } from '@angular/core';
import { CommonModule, DOCUMENT } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map } from 'rxjs/operators';
import { AuthActions, selectIsAuthenticated, selectUser } from '../../store';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatListModule,
  ],
  template: `
    @if (isAuthenticated$ | async) {
      <mat-toolbar class="navbar">
        <div class="navbar-content">
          <!-- Logo -->
          <a routerLink="/" class="logo-link">
            <mat-icon class="logo-icon">shield</mat-icon>
            <span class="logo-text">NML Online</span>
          </a>

          <!-- Menu mobile toggle -->
          @if (isMobile$ | async) {
            <button mat-icon-button (click)="toggleDrawer()">
              <mat-icon>{{ drawerOpen() ? 'close' : 'menu' }}</mat-icon>
            </button>
          }

          <!-- Menu desktop -->
          @if (!(isMobile$ | async)) {
            <nav class="nav-links">
              @for (item of menuItems; track item.path) {
                <a mat-button
                   [routerLink]="item.path"
                   routerLinkActive="active">
                  <mat-icon>{{ item.icon }}</mat-icon>
                  {{ item.label }}
                </a>
              }
            </nav>
          }

          <div class="spacer"></div>

          <!-- User info -->
          <div class="user-section">
            @if (user$ | async; as user) {
              @if (user.username) {
                <mat-chip-set>
                  <mat-chip highlighted>
                    <mat-icon matChipAvatar>person</mat-icon>
                    {{ user.username }}
                  </mat-chip>
                </mat-chip-set>
              }
            }

            @if (!(isMobile$ | async)) {
              <button mat-stroked-button color="warn" (click)="logout()">
                <mat-icon>logout</mat-icon>
                Déconnexion
              </button>
            } @else {
              <button mat-icon-button color="warn" (click)="logout()">
                <mat-icon>logout</mat-icon>
              </button>
            }
          </div>
        </div>
      </mat-toolbar>

      <!-- Mobile drawer overlay -->
      @if ((isMobile$ | async) && drawerOpen()) {
        <div class="mobile-drawer-backdrop" (click)="toggleDrawer()"></div>
        <div class="mobile-drawer">
          <mat-nav-list>
            @for (item of menuItems; track item.path) {
              <a mat-list-item
                 [routerLink]="item.path"
                 routerLinkActive="active"
                 (click)="toggleDrawer()">
                <mat-icon matListItemIcon>{{ item.icon }}</mat-icon>
                <span matListItemTitle>{{ item.label }}</span>
              </a>
            }
          </mat-nav-list>
        </div>
      }
    }
  `,
  styles: [`
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
      from { opacity: 0; }
      to { opacity: 1; }
    }

    @keyframes slideIn {
      from { transform: translateX(-100%); }
      to { transform: translateX(0); }
    }
  `]
})
export class NavbarComponent {
  private readonly store = inject(Store);
  private readonly breakpointObserver = inject(BreakpointObserver);
  private readonly document = inject(DOCUMENT);

  isAuthenticated$ = this.store.select(selectIsAuthenticated);
  private readonly isAuthenticatedSignal = toSignal(this.isAuthenticated$, { initialValue: false });

  @HostBinding('class.visible')
  get isVisible(): boolean {
    return this.isAuthenticatedSignal();
  }

  user$ = this.store.select(selectUser);

  isMobile$ = this.breakpointObserver.observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
    .pipe(map(result => result.matches));

  drawerOpen = signal(false);

  constructor() {
    // Bloquer le scroll du body quand le drawer est ouvert
    effect(() => {
      if (this.drawerOpen()) {
        this.document.body.style.overflow = 'hidden';
      } else {
        this.document.body.style.overflow = '';
      }
    });
  }

  menuItems = [
    { path: '/carte', label: 'Carte', icon: 'map' },
    { path: '/joueur', label: 'Mon Joueur', icon: 'person' },
    { path: '/boutique', label: 'Boutique', icon: 'shopping_bag' },
    { path: '/regles', label: 'Règles', icon: 'menu_book' },
  ];

  toggleDrawer(): void {
    this.drawerOpen.update(v => !v);
  }

  logout(): void {
    this.store.dispatch(AuthActions.logout());
  }
}
