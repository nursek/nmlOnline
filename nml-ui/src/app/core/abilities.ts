export type AbilityKind = 'character' | 'army' | 'territory';

export interface Ability {
  kind: AbilityKind;
  name: string;
  icon: string;
  description: string;
  bonus: string | null;
}

export interface CharacterAbilities {
  description: string;
  character: Ability;
  army: Ability;
  territory: Ability;
}

export const ABILITY_KIND_LABELS: Readonly<Record<AbilityKind, string>> = {
  character: 'Personnage',
  army: 'Armée',
  territory: 'Territoire',
};

const ability = (
  kind: AbilityKind,
  name: string,
  icon: string,
  description: string,
  bonus: string | null,
): Ability => ({ kind, name, icon, description, bonus });

// Capacités purement descriptives : aucun effet sur le moteur de combat, le bonus
// affiché n'est pas appliqué aux calculs.
export const ABILITIES_BY_PLAYER: Readonly<Record<string, CharacterAbilities>> = {
  lurio: {
    description:
      'Seigneur nécron au protocole de réparation sans faille, Lurio avance lentement mais ne recule jamais. Chaque ligne brisée se reconstruit avant la suivante, et sa Dîme de Crypte alimente une guerre d’usure qu’il compte gagner.',
    character: ability(
      'character',
      'Protocole de Réparation',
      'healing',
      'Le personnage régénère 25 points de défense supplémentaires à chaque tour.',
      '+25 Def / tour',
    ),
    army: ability(
      'army',
      'Ligne de Gauss',
      'precision_manufacturing',
      "Les unités du secteur où il se tient gagnent en pénétration d'armure.",
      '+10 % PDC',
    ),
    territory: ability(
      'territory',
      'Dîme de Crypte',
      'account_balance',
      'Chaque secteur possédé verse un tribut supplémentaire.',
      '+5 % revenus',
    ),
  },
  nursek: {
    description:
      'Charognard des ruines, le Ratcatcher prospère là où les armées s’effondrent. Il coordonne ses troupes et transforme chaque épave en ressource, faisant de la pénurie son terrain de chasse.',
    character: ability(
      'character',
      'Chasseur de Vermine',
      'radar',
      'Le personnage frappe plus durement les unités déjà blessées.',
      '+15 % Atk vs blessés',
    ),
    army: ability(
      'army',
      'Assaut Coordonné',
      'groups',
      "L'armée du secteur frappe de concert.",
      '+10 % Atk',
    ),
    territory: ability(
      'territory',
      'Récupération de Ferraille',
      'handyman',
      'Les équipements récupérés se revendent à meilleur prix.',
      '+10 % revente',
    ),
  },
  imotekh: {
    description:
      'Stratège éternel, Imotekh orchestre chaque offensive comme une partie d’échecs. Rien n’échappe à son Protocole de Guerre : ses lignes concentrent le feu tandis que son territoire verse le tribut triarchique.',
    character: ability(
      'character',
      'Stratège Éternel',
      'military_tech',
      'Le personnage orchestre un ordre de déplacement supplémentaire par tour.',
      '+1 ordre / tour',
    ),
    army: ability(
      'army',
      'Protocole de Guerre',
      'gps_fixed',
      "L'armée du secteur concentre ses tirs.",
      '+10 % Pdf',
    ),
    territory: ability(
      'territory',
      'Dîme Triarchique',
      'account_balance',
      'Le territoire verse un tribut de guerre.',
      '+10 % revenus',
    ),
  },
  trazyn: {
    description:
      'Collectionneur immortel, Trazyn pille les époques et les champs de bataille avec la même avidité. Ses chambres d’archive débordent de reliques volées, et ses arcanes sèment la discorde chez ses ennemis.',
    character: ability(
      'character',
      "Voleur d'Époques",
      'museum',
      'Le personnage récupère un équipement après chaque combat victorieux.',
      '1 équipement / combat',
    ),
    army: ability(
      'army',
      'Arcanes Volées',
      'auto_fix_high',
      "L'armée du secteur manie des armes d'un autre âge.",
      '+10 % Pdc',
    ),
    territory: ability(
      'territory',
      "Chambres d'Archive",
      'inventory_2',
      "Les caches d'armes du territoire gagnent en capacité.",
      '+5 % stockage',
    ),
  },
};

const UNDEFINED_ABILITIES: CharacterAbilities = {
  description: 'Commandeur sans légende consignée : ses faits d’armes restent à écrire.',
  character: ability(
    'character',
    'Capacité de personnage',
    'face',
    'Capacité non définie pour ce personnage.',
    null,
  ),
  army: ability(
    'army',
    "Capacité d'armée",
    'groups',
    'Capacité non définie pour cette armée.',
    null,
  ),
  territory: ability(
    'territory',
    'Capacité de territoire',
    'public',
    'Capacité non définie pour ce territoire.',
    null,
  ),
};

/** Clé = nom du compte (dossier d'assets), insensible à la casse ; les comptes sans entrée tombent sur le lot par défaut. */
export function abilitiesFor(playerName: string | null | undefined): CharacterAbilities {
  return ABILITIES_BY_PLAYER[playerName?.toLowerCase() ?? ''] ?? UNDEFINED_ABILITIES;
}
