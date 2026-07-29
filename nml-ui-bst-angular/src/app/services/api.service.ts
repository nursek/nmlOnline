import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AuthResponse,
  LoginRequest,
  Player,
  VehicleTypeInfo,
  BuyEquipmentItem,
  Vehicle,
  BuyVehicleBatchItem,
  SellResourceBatchItem,
  ResourceBatchSaleResponse,
} from '../models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  // Auth endpoints
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, credentials, {
      withCredentials: true,
    });
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {}, { withCredentials: true });
  }

  // Player endpoints
  getPlayer(username: string): Observable<Player> {
    return this.http.get<Player>(`${this.baseUrl}/players/${username}`);
  }

  // Resource endpoints
  sellResourcesBatch(items: SellResourceBatchItem[]): Observable<ResourceBatchSaleResponse> {
    return this.http.post<ResourceBatchSaleResponse>(
      `${this.baseUrl}/players/resources/sell-batch`,
      { items },
    );
  }

  // Admin endpoints
  adminExportPlayer(id: number): Observable<Record<string, unknown>> {
    return this.http.get<Record<string, unknown>>(`${this.baseUrl}/admin/players/${id}/export`);
  }

  adminImportPlayer(file: File, password?: string): Observable<Player> {
    const formData = new FormData();
    formData.append('file', file);
    if (password) {
      formData.append('password', password);
    }
    return this.http.post<Player>(`${this.baseUrl}/admin/players/import`, formData);
  }

  adminDeletePlayer(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/players/${id}`);
  }

  // Véhicules
  getVehicleTypes(): Observable<VehicleTypeInfo[]> {
    return this.http.get<VehicleTypeInfo[]>(`${this.baseUrl}/vehicles/types`);
  }

  getPlayerVehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>(`${this.baseUrl}/vehicles/my`);
  }

  buyVehiclesBatch(items: BuyVehicleBatchItem[]): Observable<Vehicle[]> {
    return this.http.post<Vehicle[]>(`${this.baseUrl}/vehicles/buy-batch`, { items });
  }

  placeVehicle(vehicleId: number, boardId: number, sectorNumber: number): Observable<Vehicle> {
    return this.http.post<Vehicle>(`${this.baseUrl}/vehicles/${vehicleId}/place`, {
      boardId,
      sectorNumber,
    });
  }

  buyEquipments(items: BuyEquipmentItem[]): Observable<Player> {
    return this.http.post<Player>(`${this.baseUrl}/players/equipment/buy`, items);
  }
}
