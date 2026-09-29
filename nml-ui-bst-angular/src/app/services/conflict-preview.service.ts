import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiService } from './api.service';
import { MovementResolutionResult } from '../models';
import { httpErrorMessage } from '../core/http-error.interceptor';

@Injectable({ providedIn: 'root' })
export class ConflictPreviewService {
  private readonly api = inject(ApiService);

  private readonly _previewing = signal(false);
  private readonly _error = signal<string | null>(null);
  private readonly _report = signal<MovementResolutionResult | null>(null);

  readonly previewing = this._previewing.asReadonly();
  readonly error = this._error.asReadonly();
  readonly report = this._report.asReadonly();

  async loadPreview(): Promise<void> {
    this._previewing.set(true);
    this._error.set(null);
    this._report.set(null);
    try {
      this._report.set(await firstValueFrom(this.api.adminPreviewMovements()));
    } catch (error) {
      this._error.set(httpErrorMessage(error, "Erreur lors de l'aperçu des conflits"));
    } finally {
      this._previewing.set(false);
    }
  }
}
