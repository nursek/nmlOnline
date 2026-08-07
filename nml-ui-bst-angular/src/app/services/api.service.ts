import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  AuthResponse,
  LoginRequest,
  Player,
  MovementOrder,
  AdminMovementOrder,
  MovementResolutionResult,
  MovementStatusFilter,
  Unit,
  VehicleTypeInfo,
  BuyEquipmentItem,
  Vehicle,
  BuyVehicleBatchItem,
  SellResourceBatchItem,
  ResourceBatchSaleResponse,
  Board,
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

  // Board — import en 2 étapes : upload des assets visuels (image + SVG),
  // puis import du board.json en lui passant les URLs renvoyées par l'étape 1.
  adminUploadBoardAssets(
    mapImage: File,
    svgOverlay: File,
  ): Observable<{ mapImageUrl: string; svgOverlayUrl: string; svgSectorCount: number }> {
    const formData = new FormData();
    formData.append('mapImage', mapImage);
    formData.append('svgOverlay', svgOverlay);
    return this.http.post<{ mapImageUrl: string; svgOverlayUrl: string; svgSectorCount: number }>(
      `${this.baseUrl}/admin/boards/assets`,
      formData,
    );
  }

  adminImportBoard(file: File, mapImageUrl?: string, svgOverlayUrl?: string): Observable<Board> {
    const formData = new FormData();
    formData.append('file', file);
    if (mapImageUrl) formData.append('mapImageUrl', mapImageUrl);
    if (svgOverlayUrl) formData.append('svgOverlayUrl', svgOverlayUrl);
    return this.http.post<Board>(`${this.baseUrl}/admin/boards/import`, formData);
  }

  // Tour courant (admin) — source unique de vérité gérée par TurnService côté backend.
  adminGetCurrentTurn(): Observable<{ currentTurn: number }> {
    return this.http.get<{ currentTurn: number }>(`${this.baseUrl}/admin/turn/current`);
  }

  adminAdvanceTurn(): Observable<{ currentTurn: number }> {
    return this.http.post<{ currentTurn: number }>(`${this.baseUrl}/admin/turn/next`, {});
  }

  // Ordres de déplacement du tour courant (admin) — filtrables par statut.
  adminGetOrders(status?: MovementStatusFilter): Observable<AdminMovementOrder[]> {
    const params = status && status !== 'ALL' ? { status } : undefined;
    return this.http.get<AdminMovementOrder[]>(`${this.baseUrl}/admin/turn/orders`, {
      params,
    });
  }

  // Aperçu (dry-run) de la résolution des mouvements du tour courant :
  // calcule les conflits potentiels sans persister (ordres laissés PENDING).
  adminPreviewMovements(): Observable<MovementResolutionResult> {
    return this.http.post<MovementResolutionResult>(
      `${this.baseUrl}/admin/turn/movements/preview`,
      {},
    );
  }

  // Applique la résolution des mouvements du tour courant : déplace les
  // entités, marque les ordres RESOLVED/BLOCKED, persiste. Renvoie le compte-rendu.
  adminResolveMovements(): Observable<MovementResolutionResult> {
    return this.http.post<MovementResolutionResult>(
      `${this.baseUrl}/admin/turn/movements/resolve`,
      {},
    );
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

  // Unités (joueur authentifié) — équipement et ordres de déplacement.
  assignUnitEquipment(unitId: number, equipmentName: string): Observable<Unit> {
    return this.http.post<Unit>(`${this.baseUrl}/units/${unitId}/equipment`, {
      equipmentName,
    });
  }

  removeUnitEquipment(unitId: number, equipmentName: string): Observable<Unit> {
    return this.http.delete<Unit>(`${this.baseUrl}/units/${unitId}/equipment`, {
      body: { equipmentName },
    });
  }

  placeFootOrder(entityIds: number[], route: number[]): Observable<MovementOrder> {
    return this.http.post<MovementOrder>(`${this.baseUrl}/units/movement/foot`, {
      entityIds,
      route,
    });
  }

  getPlayerMovementOrders(): Observable<MovementOrder[]> {
    return this.http.get<MovementOrder[]>(`${this.baseUrl}/units/movement`);
  }

  cancelMovementOrder(orderId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/units/movement/${orderId}`);
  }
}
