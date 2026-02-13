import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ResourceSaleResponse } from '../models';

@Injectable({
  providedIn: 'root'
})
export class ResourceService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiBaseUrl || '/api';

  /**
   * Vend une ressource du joueur
   * @param resourceId L'ID de la ressource (PlayerResource.id)
   * @param quantity La quantité à vendre
   */
  sellResource(resourceId: number, quantity: number): Observable<ResourceSaleResponse> {
    return this.http.post<ResourceSaleResponse>(
      `${this.apiUrl}/players/resources/sell/${resourceId}`,
      null,
      { params: { quantity: quantity.toString() } }
    );
  }
}
