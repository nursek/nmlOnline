import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NavbarComponent } from './components/navbar/navbar.component';
import { AuthService } from './services/auth.service';
import { ShopService } from './services/shop.service';

@Component({
  selector: 'app-root',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet, NavbarComponent],
  template: `
    <div class="app-container">
      <app-navbar></app-navbar>
      <main class="main-content">
        <router-outlet></router-outlet>
      </main>
    </div>
  `,
  styles: [
    `
      .app-container {
        min-height: 100vh;
        display: flex;
        flex-direction: column;
      }

      .main-content {
        flex: 1;
      }
    `,
  ],
})
export class App {
  private readonly auth = inject(AuthService);
  // Eagerly instantiate ShopService so the cart is hydrated from session storage
  // at bootstrap (matches the previous ShopActions.loadCart behaviour).
  private readonly shop = inject(ShopService);

  constructor() {
    void this.auth.initSession();
    void this.shop;
  }
}
