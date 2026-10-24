import { Injectable, computed, inject } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from './auth.service';
import { PlayerStatus } from '../models';
import { environment } from '../../environments/environment';

const ACTIVE: PlayerStatus = {
  eliminated: false,
  eliminationCause: null,
  eliminatedTurn: null,
  eliminatedByPlayerId: null,
};

@Injectable({ providedIn: 'root' })
export class PlayerStatusService {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  private readonly statusRef = httpResource<PlayerStatus>(() => {
    if (!this.auth.initialized() || !this.auth.user()) {
      return undefined;
    }
    return { url: `${environment.apiBaseUrl}/players/me/status` };
  });

  readonly status = computed(() => this.statusRef.value() ?? ACTIVE);
  readonly eliminated = computed(() => this.status().eliminated);

  constructor() {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.statusRef.reload());
    // L'élimination est décidée par un autre joueur/MJ : rafraîchir sans navigation.
    window.setInterval(() => this.statusRef.reload(), 30_000);
  }

  reload(): void {
    this.statusRef.reload();
  }
}
