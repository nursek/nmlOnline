import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { LoginRequest, LoginResponse, RegisterRequest, RefreshResponse } from '../models/auth.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly API_URL = environment.apiUrl || 'http://localhost:8080/api';
  
  // Signal pour gérer l'état d'authentification
  private readonly isAuthenticatedSignal = signal<boolean>(false);
  private readonly currentUserSignal = signal<{ id: number; username: string } | null>(null);
  
  public readonly isAuthenticated = this.isAuthenticatedSignal.asReadonly();
  public readonly currentUser = this.currentUserSignal.asReadonly();

  constructor(private http: HttpClient) {
    this.checkAuth();
  }

  /**
   * Connexion de l'utilisateur
   */
  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.API_URL}/login`, credentials, {
      withCredentials: true // Important pour gérer les cookies httpOnly
    }).pipe(
      tap(response => {
        this.setToken(response.token);
        this.isAuthenticatedSignal.set(true);
        this.currentUserSignal.set({ id: response.id, username: response.username });
      })
    );
  }

  /**
   * Inscription d'un nouvel utilisateur
   */
  register(data: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/register`, data);
  }

  /**
   * Rafraîchissement du token via le cookie httpOnly
   */
  refreshToken(): Observable<RefreshResponse> {
    return this.http.post<RefreshResponse>(`${this.API_URL}/auth/refresh`, {}, {
      withCredentials: true
    }).pipe(
      tap(response => {
        if (response.valid && response.token) {
          this.setToken(response.token);
          this.isAuthenticatedSignal.set(true);
          if (response.id && response.name) {
            this.currentUserSignal.set({ id: response.id, username: response.name });
          }
        } else {
          this.clearAuth();
        }
      })
    );
  }

  /**
   * Déconnexion de l'utilisateur
   */
  logout(): Observable<void> {
    return this.http.post<void>(`${this.API_URL}/auth/logout`, {}, {
      withCredentials: true
    }).pipe(
      tap(() => this.clearAuth())
    );
  }

  /**
   * Vérifie si l'utilisateur est authentifié au démarrage
   */
  private checkAuth(): void {
    const token = this.getToken();
    if (token) {
      this.refreshToken().subscribe({
        error: () => this.clearAuth()
      });
    }
  }

  /**
   * Récupère le token stocké
   */
  getToken(): string | null {
    return localStorage.getItem('access_token');
  }

  /**
   * Stocke le token
   */
  private setToken(token: string): void {
    localStorage.setItem('access_token', token);
  }

  /**
   * Nettoie les données d'authentification
   */
  private clearAuth(): void {
    localStorage.removeItem('access_token');
    this.isAuthenticatedSignal.set(false);
    this.currentUserSignal.set(null);
  }
}
