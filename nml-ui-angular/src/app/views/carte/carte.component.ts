import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

interface Zone {
  id: string;
  name: string;
  owner: string;
  troops: number;
  color: string;
  path: string;
  centerX: number;
  centerY: number;
}

@Component({
  selector: 'app-carte',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './carte.component.html',
  styleUrl: './carte.component.css'
})
export class CarteComponent {
  zoom = signal(1);
  selectedZone = signal<Zone | null>(null);
  hoveredZone = signal<Zone | null>(null);

  zones: Zone[] = [
    {
      id: 'zone1',
      name: 'Nord',
      owner: 'Vous',
      troops: 50,
      color: '#3d5a3c',
      path: 'M 100,50 L 400,50 L 450,150 L 350,200 L 150,180 Z',
      centerX: 270,
      centerY: 125
    },
    {
      id: 'zone2',
      name: 'Est',
      owner: 'Ennemi Alpha',
      troops: 35,
      color: '#c1272d',
      path: 'M 450,150 L 550,100 L 700,150 L 650,250 L 500,280 L 350,200 Z',
      centerX: 520,
      centerY: 190
    },
    {
      id: 'zone3',
      name: 'Sud',
      owner: 'Neutre',
      troops: 10,
      color: '#4a5568',
      path: 'M 150,300 L 350,320 L 500,350 L 450,450 L 250,480 L 100,400 Z',
      centerX: 300,
      centerY: 390
    },
    {
      id: 'zone4',
      name: 'Ouest',
      owner: 'Vous',
      troops: 40,
      color: '#3d5a3c',
      path: 'M 100,50 L 150,180 L 100,300 L 50,350 L 30,200 L 50,100 Z',
      centerX: 85,
      centerY: 200
    },
    {
      id: 'zone5',
      name: 'Centre',
      owner: 'Ennemi Bravo',
      troops: 60,
      color: '#c1272d',
      path: 'M 150,180 L 350,200 L 500,280 L 500,350 L 350,320 L 150,300 Z',
      centerX: 325,
      centerY: 265
    }
  ];

  selectZone(zone: Zone): void {
    this.selectedZone.set(zone);
  }

  getZoneCenter(zone: Zone): { x: number; y: number } {
    return { x: zone.centerX, y: zone.centerY };
  }

  getZoneLevel(zone: Zone): number {
    return Math.floor(zone.troops / 10);
  }

  getZoneProduction(zone: Zone): number {
    return zone.troops * 10;
  }

  getZoneDefense(zone: Zone): number {
    return Math.min(100, zone.troops * 1.5);
  }

  zoomIn(): void {
    this.zoom.update(z => Math.min(2, z + 0.2));
  }

  zoomOut(): void {
    this.zoom.update(z => Math.max(0.5, z - 0.2));
  }

  resetView(): void {
    this.zoom.set(1);
    this.selectedZone.set(null);
  }
}

