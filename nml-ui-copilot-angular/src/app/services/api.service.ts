import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthResponse, LoginRequest, Player, Equipment } from '../models';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  // Auth endpoints
  login(credentials: LoginRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/login`, credentials);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/auth/logout`, {});
  }

  refreshToken(): Observable<{ accessToken: string }> {
    return this.http.post<{ accessToken: string }>(`${this.baseUrl}/auth/refresh`, {});
  }

  // Player endpoints
  getPlayer(username: string): Observable<Player> {
    return this.http.get<Player>(`${this.baseUrl}/players/${username}`);
  }

  getAllPlayers(): Observable<Player[]> {
    return this.http.get<Player[]>(`${this.baseUrl}/players`);
  }

  // Equipment endpoints
  getEquipments(): Observable<Equipment[]> {
    return this.http.get<Equipment[]>(`${this.baseUrl}/equipment`);
  }
}
