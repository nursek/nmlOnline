import { Injectable, effect, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { BattleReport } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

@Injectable({ providedIn: 'root' })
export class BattleReportService {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  private readonly _reports = signal<BattleReport[]>([]);
  private readonly _loading = signal(false);
  private readonly _error = signal<string | null>(null);

  readonly reports = this._reports.asReadonly();
  readonly loading = this._loading.asReadonly();
  readonly error = this._error.asReadonly();

  constructor() {
    effect(() => {
      if (this.auth.initialized() && !this.auth.user()) {
        this._reports.set([]);
        this._error.set(null);
      }
    });
  }

  async loadReports(): Promise<void> {
    this._loading.set(true);
    this._error.set(null);
    try {
      this._reports.set(await firstValueFrom(this.api.getBattleReports()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, 'Erreur lors du chargement des rapports de combat'));
    } finally {
      this._loading.set(false);
    }
  }
}
