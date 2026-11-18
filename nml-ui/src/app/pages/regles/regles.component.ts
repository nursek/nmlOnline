import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  computed,
  inject,
  signal,
  viewChild,
  viewChildren,
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatAccordion, MatExpansionModule, MatExpansionPanel } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { RouterLink } from '@angular/router';
import { unitClassLabel } from '../../core/labels';
import { normaliserTexte, occurrences } from '../../core/recherche-texte';
import { SALE_MAX_QUANTITY, SALE_MULTIPLIERS, saleValue } from '../../core/sale-multiplier';

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

interface RecrueUnite {
  type: string;
  cout: number;
  dispo: number;
  limite: string;
}

interface VehiculeDispo {
  nom: string;
  dispo: number;
}

interface RessourceJeu {
  nom: string;
  ventes: readonly string[];
}

// ponytail: catalogue figé (resources.csv) ; passer par l'API publique s'il devient éditable.
const RESSOURCES = [
  { nom: "Caisse d'armes", valeur: 600 },
  { nom: 'Prométhéum', valeur: 800 },
  { nom: 'Plastacier', valeur: 1000 },
  { nom: 'Adamentium', valeur: 1100 },
  { nom: 'Conteneur blindé', valeur: 1400 },
  { nom: 'Auramite', valeur: 1700 },
  { nom: 'Noctilithe', valeur: 2000 },
] as const;

const NOMBRE_FR = new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 3 });

function formatNombre(valeur: number): string {
  return NOMBRE_FR.format(valeur);
}

@Component({
  selector: 'app-regles',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    MatButtonModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    RouterLink,
  ],
  templateUrl: './regles.component.html',
  styleUrls: ['./regles.component.scss'],
})
export class ReglesComponent {
  readonly sommaire: readonly SectionRegle[] = [
    { id: 'but', label: 'But du jeu', icon: 'my_location' },
    { id: 'preparation', label: 'Préparation (tour 0)', icon: 'construction' },
    { id: 'stats', label: 'Statistiques des unités', icon: 'insights' },
    { id: 'niveaux', label: 'Niveaux et évolution', icon: 'military_tech' },
    { id: 'classes', label: 'Classes', icon: 'badge' },
    { id: 'recrutement', label: 'Recrutement des unités', icon: 'person_add' },
    { id: 'deplacements', label: 'Déplacements', icon: 'directions_walk' },
    { id: 'vehicules', label: 'Véhicules', icon: 'directions_car' },
    { id: 'batiments', label: 'Bâtiments', icon: 'apartment' },
    { id: 'economie', label: 'Économie et échange', icon: 'payments' },
    { id: 'ressources', label: 'Ressources', icon: 'diamond' },
    { id: 'boutique', label: 'Boutique et équipements', icon: 'shopping_bag' },
    { id: 'recolte', label: 'Récolte des revenus', icon: 'agriculture' },
    { id: 'combat', label: 'Affrontements', icon: 'sports_esports' },
    { id: 'impasse', label: 'Impasse mexicaine', icon: 'groups' },
    { id: 'alliances', label: 'Alliances', icon: 'handshake' },
    { id: 'frontieres', label: 'Bonus de frontières', icon: 'border_outer' },
    { id: 'experience', label: 'Expérience de combat', icon: 'workspace_premium' },
    { id: 'victoire', label: 'Fin de partie et podium', icon: 'emoji_events' },
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
      role: "Absorbe les dégâts en premier. Restaurée après chaque combat pour les personnages ; l'infanterie la récupère intégralement à la fin du tour.",
    },
    {
      nom: 'Esquive',
      sigle: 'Esq',
      css: 'esq',
      role: "Pourcentage de chance (1 à 100) d'annuler une attaque : l'attaquant perd son tir. Les véhicules n'ont pas d'esquive.",
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
      effet:
        'Peut parcourir 2 secteurs par tour au lieu de 1. Attaquée dans le secteur intermédiaire, sa Défense et son Armure sont divisées par deux.',
      actif: true,
    },
    {
      nom: unitClassLabel('MASTODONTE'),
      effet: 'Réduit de 25 % les dégâts de PdF et de PdC reçus.',
      actif: true,
    },
    {
      nom: unitClassLabel('TIREUR'),
      effet:
        'À chaque phase de tir (PdF, PdC, ATK), 20 % de chances de voir ses dégâts multipliés par 1,5 pour la phase.',
      actif: true,
    },
    {
      nom: unitClassLabel('SNIPER'),
      effet:
        'Combat à distance : un seul jet de HIT, tir de soutien pendant un combat au sol, ' +
        'tir isolé hors combat et contre-tir.',
      actif: true,
    },
    {
      nom: unitClassLabel('PILOTE_DESTRUCTEUR'),
      effet:
        "Priorité de ciblage : tire d'abord sur les véhicules ennemis. Obligatoire pour piloter un véhicule avec une unité ; " +
        'les armes anti-véhicules (Bombes collantes, Lance-roquettes, Lance-grenades) doublent les dégâts contre leur catégorie de cible.',
      actif: true,
    },
  ];

  readonly recrues: readonly RecrueUnite[] = [
    {
      type: 'LARBIN',
      cout: 400,
      dispo: 2,
      limite: '20 / tour, puis 30 au tour 5 et 40 au tour 10',
    },
    { type: 'VOYOU', cout: 1500, dispo: 5, limite: '5 / tour, puis 10 au tour 10' },
    { type: 'MALFRAT', cout: 5000, dispo: 10, limite: '5 / tour' },
  ];

  readonly vehiculesDispo: readonly VehiculeDispo[] = [
    { nom: 'Véhicule à tourelle', dispo: 4 },
    { nom: 'VTT léger', dispo: 5 },
    { nom: 'VTT blindé', dispo: 7 },
    { nom: 'Tank de combat', dispo: 8 },
    { nom: 'Hélicoptère de combat', dispo: 8 },
    { nom: 'Avion de transport', dispo: 12 },
  ];

  readonly maxVente = SALE_MAX_QUANTITY;

  readonly quantites: readonly number[] = SALE_MULTIPLIERS.map((_, index) => index + 1);

  readonly ressources: readonly RessourceJeu[] = RESSOURCES.map(({ nom, valeur }) => ({
    nom,
    ventes: SALE_MULTIPLIERS.map((_, index) => formatNombre(saleValue(valeur, index + 1))),
  }));

  readonly recherche = signal('');
  readonly rechercheActive = computed(() => normaliserTexte(this.recherche().trim()).length > 0);

  private readonly panneaux = viewChildren(MatExpansionPanel);
  private readonly panneauxEl = viewChildren(MatExpansionPanel, { read: ElementRef<HTMLElement> });
  private readonly accordeon = viewChild(MatAccordion);
  private readonly destroyRef = inject(DestroyRef);
  private derniereSectionTrouvee: string | null = null;

  // Les panneaux ne sont pas pilotés par `sommaire` : l'id DOM fait foi, comme pour `allerA`.
  readonly sectionsTrouvees = computed<ReadonlySet<string>>(() => {
    const terme = normaliserTexte(this.recherche().trim());
    const trouvees = new Set<string>();
    if (!terme) return trouvees;
    for (const panneau of this.panneauxEl()) {
      const element = panneau.nativeElement;
      if (normaliserTexte(this.texteVisible(element)).includes(terme)) {
        trouvees.add(element.id);
      }
    }
    return trouvees;
  });

  readonly sommaireFiltre = computed(() =>
    this.rechercheActive()
      ? this.sommaire.filter((section) => this.sectionsTrouvees().has(section.id))
      : this.sommaire,
  );

  constructor() {
    this.destroyRef.onDestroy(() => {
      if (typeof CSS !== 'undefined' && CSS.highlights) {
        CSS.highlights.delete('regles-recherche');
      }
    });
  }

  changerRecherche(valeur: string): void {
    this.recherche.set(valeur);
    this.appliquerRecherche();
  }

  effacerRecherche(): void {
    this.recherche.set('');
    this.appliquerRecherche();
  }

  private appliquerRecherche(): void {
    const terme = normaliserTexte(this.recherche().trim());
    if (!terme) {
      this.panneauxEl().forEach((panneau) =>
        panneau.nativeElement.classList.remove('recherche-masquee'),
      );
      this.derniereSectionTrouvee = null;
      if (typeof CSS !== 'undefined' && CSS.highlights) {
        CSS.highlights.delete('regles-recherche');
      }
      return;
    }
    const trouvees = this.sectionsTrouvees();
    this.panneauxEl().forEach((panneau, index) => {
      const trouvee = trouvees.has(panneau.nativeElement.id);
      panneau.nativeElement.classList.toggle('recherche-masquee', !trouvee);
      if (trouvee) this.panneaux()[index]?.open();
    });
    const premiere = trouvees.values().next().value;
    if (!premiere) {
      this.derniereSectionTrouvee = null;
    } else if (premiere !== this.derniereSectionTrouvee) {
      this.derniereSectionTrouvee = premiere;
      document.getElementById(premiere)?.scrollIntoView?.({ behavior: 'smooth', block: 'start' });
    }
    this.surligner(this.recherche().trim(), trouvees);
  }

  private texteVisible(racine: HTMLElement): string {
    const walker = document.createTreeWalker(racine, NodeFilter.SHOW_TEXT);
    let texte = '';
    for (let noeud = walker.nextNode(); noeud; noeud = walker.nextNode()) {
      // Les ligatures Material Icons ("my_location") ne sont pas du texte de règle.
      if (noeud.parentElement?.closest('mat-icon')) continue;
      texte += noeud.textContent ?? '';
    }
    return texte;
  }

  private surligner(terme: string, trouvees: ReadonlySet<string>): void {
    // API Custom Highlight : progressive, la recherche filtre et déplie même sans elle.
    if (typeof CSS === 'undefined' || !CSS.highlights || typeof Highlight === 'undefined') return;
    const surbrillance = new Highlight();
    let total = 0;
    for (const panneau of this.panneauxEl()) {
      if (!trouvees.has(panneau.nativeElement.id) || total >= 300) continue;
      const racine = panneau.nativeElement;
      const walker = document.createTreeWalker(racine, NodeFilter.SHOW_TEXT);
      for (let noeud = walker.nextNode(); noeud && total < 300; noeud = walker.nextNode()) {
        if (noeud.parentElement?.closest('mat-icon')) continue;
        for (const { debut, fin } of occurrences(noeud.textContent ?? '', terme)) {
          if (total >= 300) break;
          const range = document.createRange();
          range.setStart(noeud, debut);
          range.setEnd(noeud, fin);
          surbrillance.add(range);
          total++;
        }
      }
    }
    CSS.highlights.set('regles-recherche', surbrillance);
  }

  allerA(id: string): void {
    const index = this.sommaire.findIndex((section) => section.id === id);
    this.panneaux()[index]?.open();
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  toutDeplier(): void {
    this.accordeon()?.openAll();
  }

  toutReplier(): void {
    this.accordeon()?.closeAll();
  }
}
