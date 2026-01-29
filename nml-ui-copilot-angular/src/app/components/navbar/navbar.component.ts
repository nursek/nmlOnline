import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Store } from '@ngrx/store';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { BreakpointObserver, Breakpoints } from '@angular/cdk/layout';
import { map } from 'rxjs/operators';
import { selectIsAuthenticated, selectUser } from '../../store/auth/auth.selectors';
import { AuthActions } from '../../store/auth/auth.actions';

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
    MatSidenavModule,
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
              <mat-icon>menu</mat-icon>
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
              <mat-chip-set>
                <mat-chip highlighted>
                  <mat-icon matChipAvatar>person</mat-icon>
                  {{ user.username }}
                </mat-chip>
              </mat-chip-set>
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

      <!-- Mobile drawer -->
      <mat-sidenav-container class="sidenav-container" [hasBackdrop]="true">
        <mat-sidenav #sidenav mode="over" [opened]="drawerOpen()">
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
        </mat-sidenav>
      </mat-sidenav-container>
    }
  `,
  styles: [`
    .navbar {
      position: sticky;
      top: 0;
      z-index: 1000;
      background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
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

    .sidenav-container {
      position: absolute;
      top: 64px;
      left: 0;
      right: 0;
      height: 0;
    }

    mat-sidenav {
      width: 250px;
      padding-top: 16px;
    }

    mat-nav-list a.active {
      background: rgba(99, 102, 241, 0.1);
      color: #6366f1;
    }
  `]
})
export class NavbarComponent {
  private store = inject(Store);
  private breakpointObserver = inject(BreakpointObserver);

  isAuthenticated$ = this.store.select(selectIsAuthenticated);
  user$ = this.store.select(selectUser);

  isMobile$ = this.breakpointObserver.observe([Breakpoints.Handset, Breakpoints.TabletPortrait])
    .pipe(map(result => result.matches));

  drawerOpen = signal(false);

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
