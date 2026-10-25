import { Injectable, computed, effect, inject, signal } from '@angular/core';
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

  private readonly lastKnown = signal<PlayerStatus | null>(null);

  readonly status = computed(() => {
    if (!this.auth.user()) {
      return ACTIVE;
    }
    const value = this.statusRef.hasValue() ? this.statusRef.value() : null;
    return value ?? this.lastKnown() ?? ACTIVE;
  });
  // Le MJ reste opérateur : /api/admin/** échappe au filtre serveur, l'UI doit suivre.
  readonly eliminated = computed(() => this.status().eliminated && !this.auth.isAdmin());

  constructor() {
    // Une erreur réseau ne doit pas rouvrir les actions d'un joueur déjà éliminé.
    effect(() => {
      if (!this.auth.user()) {
        this.lastKnown.set(null);
      } else if (this.statusRef.hasValue()) {
        this.lastKnown.set(this.statusRef.value());
      }
    });
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => this.reload());
    // L'élimination est décidée par un autre joueur/MJ : rafraîchir sans navigation.
    window.setInterval(() => this.reload(), 30_000);
  }

  reload(): void {
    if (this.auth.initialized() && this.auth.user()) {
      this.statusRef.reload();
    }
  }
}
