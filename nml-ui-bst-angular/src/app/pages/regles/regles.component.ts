import { ChangeDetectionStrategy, Component, viewChild, viewChildren } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatAccordion, MatExpansionModule, MatExpansionPanel } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { unitClassLabel } from '../../core/labels';

interface SectionRegle {
  id: string;
  label: string;
  icon: string;
}

interface StatUnite {
  nom: string;
  sigle: string;
  css: string;
  role: string;
}

interface NiveauUnite {
  type: string;
  niveau: string;
  exp: string;
  atk: number;
  def: number;
  armesFeu: number;
  corpsACorps: number;
  defensif: number;
}

interface ClasseUnite {
  nom: string;
  effet: string;
  actif: boolean;
}

@Component({
  selector: 'app-regles',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, MatExpansionModule, MatIconModule],
  templateUrl: './regles.component.html',
  styleUrls: ['./regles.component.scss'],
})
export class ReglesComponent {
  readonly sommaire: readonly SectionRegle[] = [
    { id: 'but', label: 'But du jeu', icon: 'my_location' },
    { id: 'stats', label: 'Statistiques des unités', icon: 'insights' },
    { id: 'niveaux', label: 'Niveaux et évolution', icon: 'military_tech' },
    { id: 'classes', label: 'Classes', icon: 'badge' },
    { id: 'boutique', label: 'Boutique et équipements', icon: 'shopping_bag' },
    { id: 'combat', label: 'Affrontements', icon: 'sports_esports' },
    { id: 'impasse', label: 'Impasse mexicaine', icon: 'groups' },
    { id: 'frontieres', label: 'Bonus de frontières', icon: 'border_outer' },
    { id: 'experience', label: 'Expérience de combat', icon: 'workspace_premium' },
    { id: 'victoire', label: 'Fin de partie et podium', icon: 'emoji_events' },
    { id: 'empereur', label: 'Objectif secret', icon: 'key' },
  ];

  readonly stats: readonly StatUnite[] = [
    {
      nom: 'Attaque',
      sigle: 'Atk',
      css: 'atk',
      role: "Points offensifs de base. Entre dans le total offensif de l'unité (Atk + PdF + PdC) et frappe pendant la phase ATK.",
    },
    {
      nom: 'Puissance de feu',
      sigle: 'PdF',
      css: 'pdf',
      role: 'Points de tir utilisés lors des phases PdF, en début de combat.',
    },
    {
      nom: 'Puissance de corps-à-corps',
      sigle: 'PdC',
      css: 'pdc',
      role: 'Points utilisés lorsque les unités arrivent au contact, après les phases de tir.',
    },
    {
      nom: 'Défense',
      sigle: 'Def',
      css: 'def',
      role: "Points défensifs de base. Les dégâts entament d'abord l'armure, puis la défense. L'unité meurt quand Armure + Défense ne suffisent plus à encaisser une attaque.",
    },
    {
      nom: 'Armure',
      sigle: 'Arm',
      css: 'arm',
      role: "Absorbe les dégâts en premier. Régénérée entre deux combats pour les personnages ; l'infanterie ne récupère pas son armure.",
    },
    {
      nom: 'Esquive',
      sigle: 'Esq',
      css: 'esq',
      role: "Pourcentage de chance (1 à 100) d'annuler une attaque. Une esquive réussie consomme tout de même des points à l'attaquant (Def + Arm de la cible).",
    },
  ];

  readonly niveaux: readonly NiveauUnite[] = [
    {
      type: 'LARBIN',
      niveau: '1',
      exp: '0 – 1',
      atk: 10,
      def: 10,
      armesFeu: 1,
      corpsACorps: 1,
      defensif: 1,
    },
    {
      type: 'VOYOU',
      niveau: '2',
      exp: '2 – 4',
      atk: 20,
      def: 20,
      armesFeu: 1,
      corpsACorps: 1,
      defensif: 2,
    },
    {
      type: 'MALFRAT',
      niveau: '3',
      exp: '5 – 7',
      atk: 50,
      def: 50,
      armesFeu: 1,
      corpsACorps: 2,
      defensif: 3,
    },
    {
      type: 'BRUTE',
      niveau: '4',
      exp: '8 +',
      atk: 100,
      def: 100,
      armesFeu: 1,
      corpsACorps: 3,
      defensif: 4,
    },
  ];

  readonly classes: readonly ClasseUnite[] = [
    {
      nom: unitClassLabel('LEGER'),
      effet: 'Peut parcourir 2 secteurs par tour au lieu de 1.',
      actif: true,
    },
    {
      nom: unitClassLabel('MASTODONTE'),
      effet: 'Réduit de 25 % les dégâts de PdF et de PdC reçus.',
      actif: true,
    },
    {
      nom: unitClassLabel('TIREUR'),
      effet: 'Critique défini (10 %, dégâts ×1,5) mais jamais appliqué en combat.',
      actif: false,
    },
    { nom: unitClassLabel('SNIPER'), effet: 'Aucun effet de combat implémenté.', actif: false },
    {
      nom: unitClassLabel('PILOTE_DESTRUCTEUR'),
      effet: 'Aucun effet de combat implémenté.',
      actif: false,
    },
    {
      nom: unitClassLabel('ELEMENTAIRE'),
      effet: 'Aucun effet de combat implémenté.',
      actif: false,
    },
  ];

  private readonly panneaux = viewChildren(MatExpansionPanel);
  private readonly accordeon = viewChild(MatAccordion);

  // L'ordre des panneaux du template suit celui de `sommaire`.
  allerA(index: number): void {
    const section = this.sommaire[index];
    this.panneaux()[index]?.open();
    document.getElementById(section.id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  toutDeplier(): void {
    this.accordeon()?.openAll();
  }

  toutReplier(): void {
    this.accordeon()?.closeAll();
  }
}
