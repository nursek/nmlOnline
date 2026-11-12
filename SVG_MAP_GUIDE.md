# Guide : créer une carte personnalisée (6 joueurs / 25 secteurs)

Ce guide part de la carte actuelle (43 secteurs) et du code existant pour expliquer comment
fabriquer une **nouvelle carte jouable à 6 joueurs sur 25 secteurs** : image de fond, overlay SVG,
`board.json`, import et attribution des secteurs.

## 1. Comment la carte fonctionne

Le plateau est la combinaison de trois choses :

| Élément | Rôle | Où |
|---|---|---|
| `Board.mapImageUrl` | Image de fond, purement décorative | `nml-ui/src/assets/maps/` (démo) ou `/boards/<uuid>.png` (upload admin) |
| `Board.svgOverlayUrl` | SVG transparent des zones cliquables | idem |
| `Board.sectors` | État du jeu : numéro, nom, revenu, ressource, voisins, propriétaire | table `sectors` en base |

Le front (`nml-ui/src/app/pages/carte/carte.component.ts`) fait le lien tout seul :

1. il affiche l'image (`<img>`), charge l'overlay (`httpResource.text`), le nettoie (`core/svg-sanitize.ts`) puis l'injecte ;
2. chaque `<path>` ou `<polygon>` dont l'`id` vaut `pathN` représente le **secteur N** ;
3. il ajoute les numéros (`<text class="sector-label">`) au centre visuel de chaque zone ;
4. il colorie chaque zone avec la couleur du joueur propriétaire (`MAP_THEME.playerPalette`,
   attribuée par ordre d'`id` joueur, pas par la couleur stockée en base) ;
5. un clic ouvre le détail du secteur, le survol l'éclaircit.

Conséquences directes :

- **Ne dessinez pas les numéros ni les noms** dans l'image ou le SVG : ils sont générés.
- **Ne mettez pas de styles** (`fill`, `stroke`, CSS) : le front les écrase à chaque rendu.
- Un secteur **neutre** est rempli d'un motif de hachures **opaque** ; un secteur possédé est teinté
  à ~50 % au-dessus de l'image. Le décor n'est donc visible que hors secteurs et sous les secteurs
  possédés.
- `Sector.x` / `Sector.y` existent encore mais ne sont **plus utilisés** : la position vient du SVG.
  Il n'y a plus de repli en grille si le SVG manque : sans overlay, l'image s'affiche mais rien
  n'est cliquable.

### Le contrat d'ID

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1600 1000">
  <path id="path1" d="M10,10 L190,10 L190,140 L10,140 Z" />
  <polygon id="path2" points="210,10 390,10 390,140 210,140" />
</svg>
```

Le front lit `parseInt(id.replace('path', ''), 10)` : le préfixe doit être **`path`** (le détecteur
de voisins accepte aussi `sector-`, mais l'interface non). Le `viewBox` doit avoir le **même ratio
que l'image**, sinon l'overlay est étiré de travers (`preserveAspectRatio="none"` est forcé).

Le SVG est refusé (upload admin) ou neutralisé (client) s'il contient `<script>`, `<foreignObject>`,
`<style>`, un `href` externe, `javascript:` ou un `url(...)` non local. Un SVG « simple » exporté
d'Inkscape passe généralement sans retouche (vérifiez toujours le résultat de l'upload).

## 2. Le plan 6 joueurs / 25 secteurs : 12 rayons, moitié neutre

La carte est une roue de **12 rayons alternés** autour d'un hub central : 6 rayons joueurs de
2 secteurs et 6 rayons neutres de 2 secteurs.

- **12 secteurs possédés** : 2 par joueur, contigus, de l'intérieur vers l'extérieur ;
- **13 secteurs neutres** : 6 bandes de 2 secteurs **partagées par les deux joueurs voisins** + le hub.

Soit 6 × 2 + 6 × 2 + 1 = 25. Chaque bande neutre est un champ de bataille à deux joueurs ; le hub
est le centre à six joueurs.

| Joueur | Secteurs | QG | Bandes neutres adjacentes |
|---|---|---|---|
| Joueur 1 | 1, 2 | 1 | 3-4 (droite), 23-24 (gauche) |
| Joueur 2 | 5, 6 | 5 | 7-8, 3-4 |
| Joueur 3 | 9, 10 | 9 | 11-12, 7-8 |
| Joueur 4 | 13, 14 | 13 | 15-16, 11-12 |
| Joueur 5 | 17, 18 | 17 | 19-20, 15-16 |
| Joueur 6 | 21, 22 | 21 | 23-24, 19-20 |
| Neutres | 3,4,7,8,11,12,15,16,19,20,23,24,25 | — | — |

Voisinages :

- secteur **joueur intérieur** (1, 5, …) : 4 voisins — le hub, son extérieur, les 2 secteurs
  intérieurs des bandes voisines ;
- secteur **joueur extérieur** (2, 6, …) : 3 voisins — son intérieur, les 2 extérieurs des bandes
  voisines ;
- secteur **neutre intérieur** (3, 7, …) : 4 voisins — le hub, son extérieur, les 2 joueurs voisins ;
- secteur **neutre extérieur** (4, 8, …) : 3 voisins — son intérieur, les 2 joueurs voisins ;
- **hub 25** : 12 voisins (tous les secteurs intérieurs).

Conséquence : au départ, **aucun joueur ne touche directement un autre joueur**. Tout passe par une
bande neutre ou par le hub. Chaque joueur a 4 secteurs neutres à sa porte + le hub : la carte est
« très agressive », avec seulement 2 secteurs de départ (4 000/tour). Le calque **Frontières** est
donc vide au démarrage ; il s'allume dès qu'une bande neutre (ou le hub) est capturée.

**Économie et ressources** (valeurs du générateur) :

| Zone | Revenu | Ressource |
|---|---|---|
| Secteur joueur intérieur | 2 000 | Prométhéum |
| Secteur joueur extérieur | 2 000 | Plastacier |
| Bande neutre intérieure | 2 500 | Auramite |
| Bande neutre extérieure | 3 000 | Noctilithe |
| Hub 25 | 4 000 | Noctilithe |

Les bandes neutres rapportent plus que les secteurs de départ : c'est volontaire, sinon personne ne
se bat pour elles. Ajuste ces valeurs (et la dotation de départ des joueurs) si la partie est trop
rapide.

**Variante territoriale** (`--variant rings`) : 6 quartiers × 4 anneaux + hub, 4 secteurs par
joueur, 1 seul neutre (le hub), 2 joueurs voisins directs. Moins de conflits, économie de départ
plus confortable, quartiers lisibles.

**Variante arcs (3 / 2 / 6)** : 3 neutres à 3 joueurs, 9 à 2, hub à 6 — voir
[CARTE_ARCS_6_JOUEURS.md](CARTE_ARCS_6_JOUEURS.md). C'est la variante la plus conflictuelle ; elle
ajoute des arcs autour de 3 joueurs et un léger déséquilibre (5 vs 6 voisins neutres).

**Carte organique** : vous pouvez dessiner une carte (îles, provinces) à condition de conserver
cette topologie ou de refaire les listes `neighbors`. Les numéros sont l'identité du secteur : une
fois la partie commencée, les renuméroter change le plateau sous les pieds des joueurs.

## 3. Fabriquer l'image de fond

### Contraintes

- **Même ratio que le `viewBox`**, idéalement les mêmes dimensions en pixels (ex. 1600 × 1000).
- **PNG ou JPEG**, 10 Mo maximum (limite d'upload), et une taille raisonnable : la carte s'affiche
  en pleine largeur, 1600 px suffisent.
- Le dossier `maps/` est **exclu** du pipeline WebP (`scripts/optimize-images.mjs`) : votre PNG est
  servi tel quel.
- L'image subit un filtre CSS (`saturate(0.8) contrast(1.06) brightness(0.92)`). Prévoyez-la un peu
  plus claire et peu saturée que le rendu final.
- Pas de numéros, pas de noms, pas de texte : tout est superposé par l'application.
- Les secteurs neutres sont recouverts d'un motif opaque : investissez le décor **hors des secteurs**
  et sur les contours, pas sur les détails internes. Ici **13 secteurs sur 25 sont neutres** au
  départ : la moitié de la carte sera en hachures. C'est le prix de la variante « conflit ».

### Deux façons d'obtenir l'image

**A. Partir du gabarit généré** (le plus rapide pour tester toute la chaîne) : le script
`tools/generate-map.mjs` produit un fond sombre aux 25 zones déjà alignées, avec les 6 bandes
joueurs plus claires et les 6 bandes neutres plus sombres (repère visuel, pas une propriété — voir
§4). Rastérisez-le :

```bash
# depuis la racine du dépôt
node tools/generate-map.mjs --out carte-25

cd nml-ui
node -e "require('sharp')('../carte-25/fond.svg').png().toFile('../carte-25/fond.png')"
cd ..
```

`sharp` est déjà installé dans `nml-ui/` (aucune dépendance à ajouter).

**B. Peindre votre propre décor** : importez `carte-25/fond.svg` (ou l'overlay) comme gabarit dans
Krita/Photoshop/GIMP, peignez sur un calque au-dessus, masquez le gabarit, exportez en PNG **aux
mêmes dimensions**. L'alignement est garanti puisque le quadrillage de départ ne bouge pas. C'est la
méthode recommandée : la géométrie des secteurs reste la source de vérité, l'illustration s'y adapte.

### Alignement

L'image définit la hauteur du conteneur (`width: 100%`, `height: auto`) et l'overlay est étiré sur
100 % × 100 %. Donc :

- même ratio image / `viewBox` = alignement parfait à toutes les largeurs ;
- un décalage constant peut se corriger avec `MAP_THEME.overlay.offsetX` / `offsetY` dans
  `nml-ui/src/app/pages/carte/carte.config.ts`, mais ce sont des **pixels CSS** (non mis à
  l'échelle) : c'est un rustine pour une largeur donnée, pas une solution.

## 4. Fabriquer l'overlay SVG

### Gabarit généré

```bash
# depuis la racine du dépôt
node tools/generate-map.mjs --out carte-25
```

Par défaut, `--variant spokes` : 12 rayons, 25 secteurs, 13 neutres. `--variant rings` produit la
variante territoriale (6 quartiers × 4 anneaux, 4 secteurs par joueur + hub neutre). `--variant arcs`
produit la variante 3 / 2 / 6 (3 arcs à 3 joueurs, 9 neutres à 2, hub à 6 — voir
[CARTE_ARCS_6_JOUEURS.md](CARTE_ARCS_6_JOUEURS.md)). `--width` / `--height` changent la résolution
(même ratio).

Le script écrit dans `carte-25/` :

- `overlay.svg` : 25 `<path id="path1">` … `path25`, sans style ;
- `fond.svg` : le même découpage en image de fond (à rastériser, §3) ;
- `board.json` : les 25 secteurs avec noms, revenus, ressources, voisinages symétriques et hub neutre.

Il affiche aussi la répartition : `J1 1,2 (QG 1) | J2 5,6 (QG 5) | …` et les 13 numéros neutres.
`board.json` ne contient **pas** les propriétaires : les neutres sont simplement les secteurs que tu
n'assignes à aucun joueur à l'import (§8).

Le script vérifie lui-même la symétrie des voisinages et la connexité du graphe avant d'écrire.

Copiez ensuite les assets dans le projet depuis la racine (chemins attendus par le `board.json`
généré) :

```bash
cp carte-25/overlay.svg nml-ui/src/assets/maps/carte-25-overlay.svg
cp carte-25/fond.png   nml-ui/src/assets/maps/carte-25-fond.png
```

Gardez `carte-25/board.json` versionné avec les deux assets : l'API n'expose **pas** d'export au
format d'import, c'est votre seule copie du plateau.

Puis personnalisez : noms des secteurs, revenus, ressources, et éventuellement les formes (en
gardant les mêmes `id`, ou en refaisant `neighbors` si la topologie change).

### À la main dans Inkscape

1. **Document** : dimensions exactes de l'image (ex. 1600 × 1000), unités en px.
2. **Image de référence** : importez le PNG, posez-le en (0, 0), verrouillez son calque.
3. **Calque « secteurs »** : tracez chaque zone à l'outil plume/Bézier, refermez chaque contour
   (clic sur le premier nœud). Vous pouvez laisser un remplissage temporaire pour repérer les trous
   et les chevauchements ; l'application l'écrasera.
4. **IDs** : sélectionnez chaque tracé → `Objet > Propriétés de l'objet` → `ID = path1`, `path2`…
   (ou l'éditeur XML, `Ctrl+Shift+X`). Un `pathN` par secteur, sans doublon.
5. **Nettoyage** : supprimez le calque de l'image de référence avant d'enregistrer (sinon le SVG
   embarque ou pointe l'image).
6. **Export** : `Fichier > Enregistrer sous > SVG simple (*.svg)`. Évitez le SVG Inkscape complet
   (métadonnées inutiles) et tout bloc `<style>`.
7. **Contrôle** : ouvrez le SVG dans un navigateur (les zones doivent former un puzzle sans trou),
   puis vérifiez les IDs avec la commande du §6.

Conseils : des zones fermées, sans chevauchement ; des contours assez larges pour rester cliquables
sur mobile ; pas de `<g transform="...">` autour des paths si vous comptez utiliser le détecteur de
voisins (il lit les coordonnées brutes du `d` et ignore les transformations — le navigateur, lui,
les applique).

### À la main dans Illustrator

Même principe : document aux dimensions de l'image, image en calque verrouillé, contours à la plume
sur un calque séparé, `id` de chaque tracé = `pathN`, export SVG sans styles inline ni image liée.
Le plus sûr reste d'ouvrir le fichier exporté dans un éditeur de texte et de vérifier le `viewBox`
et les `id`.

## 5. Écrire `board.json`

L'import admin attend une **liste plate** de secteurs (ce n'est pas le format renvoyé par
`GET /api/boards`, qui est une map). Les champs inconnus sont **refusés**.

```json
{
  "name": "Carte 6 Joueurs",
  "mapImageUrl": "/assets/maps/carte-25-fond.png",
  "svgOverlayUrl": "/assets/maps/carte-25-overlay.svg",
  "sectors": [
    {
      "number": 1,
      "name": "Avant-Poste",
      "income": 2000,
      "army": [],
      "neighbors": [2, 3, 23, 25],
      "resource": "Prométhéum"
    }
  ]
}
```

| Champ | Obligatoire | Remarque |
|---|---|---|
| `name` | non | Nom du plateau (upsert par nom). Toujours le renseigner. |
| `mapImageUrl` | non | Chemin same-origin recommandé (`/assets/...` ou `/boards/...`) ; non validé côté serveur. |
| `svgOverlayUrl` | non | Chemin same-origin commençant par `/`, sans hôte ni schéma. |
| `sectors[].number` | oui | Entier ≥ 1, unique. C'est l'identité du secteur. |
| `sectors[].name` | oui | Non nul en base. |
| `sectors[].income` | oui | Revenu par tour. Absent = 0 (pas 2000). |
| `sectors[].resource` | non | Un des 7 noms exacts ci-dessous. |
| `sectors[].neighbors` | non | Liste d'entiers ; **aucune validation serveur** (ni existence, ni réciprocité). |
| `sectors[].army` | non | Laisser `[]` pour un nouveau plateau. |
| `sectors[].x`, `.y` | non | Héritage, inutilisés par la carte. |

Ressources valides (fichiers `nml-ui/src/assets/shop/resources/`) : `Caisse d'armes`, `Prométhéum`,
`Plastacier`, `Adamentium`, `Conteneur blindé`, `Auramite`, `Noctilithe`. Un autre nom passe
l'import mais n'affiche pas d'icône dans le calque « Ressources ».

**Pièges** :

- `ownerId`, `color`, `buildings`, `stats` dans un secteur = champ inconnu = import refusé.
- Le voisinage n'est pas symétrisé par le serveur : `check-map.mjs` (§6) le vérifie.
- Le générateur écrit déjà des voisinages corrects. Pour une carte dessinée à la main, partez du
  détecteur existant puis corrigez à la main :

```bash
node tools/svg-neighbor-detector.js \
  --svg nml-ui/src/assets/maps/carte-25-overlay.svg \
  --threshold 12 --output carte-25/neighbors.json
```

Ses limites : il ne lit que les `<path>` (pas les `<polygon>`), ignore les `transform`, échantillonne
les extrémités de commandes (approximation pour les courbes) et compte un simple contact de coin
comme un voisin. Sur la carte actuelle, le seuil 10 ne produit aucun voisin en trop mais oublie
12 liens (sur 43 secteurs) : c'est une aide, pas une vérité.

N'utilisez **pas** `--generate` sur `nml-ms/src/main/resources/boards/board.json` : cela écrase le
plateau de démo (et son contenu généré est faux pour ce projet : URLs codées en dur, revenus
aléatoires, ressources obsolètes). Travaillez sur un fichier temporaire.

## 6. Vérifier avant d'importer

```bash
# depuis la racine du dépôt
node tools/check-map.mjs carte-25/overlay.svg carte-25/board.json
```

Le script vérifie : `pathN` présents et uniques, correspondance exacte avec les numéros du board,
`name`/`income` renseignés, voisinages réciproques et sans doublon, voisins existants, graphe
connexe. Sortie attendue :

```
✓ 25 secteurs, voisinage réciproque et graphe connexe (25 entrées board.json)
```

Il accepte aussi la carte actuelle (43 secteurs) pour contrôle de non-régression.

## 7. Importer le plateau

### En production (recommandé)

Le profil `prod` démarre sans données de démo (`app.import-demo-data=false`) : sur une base vierge,
le premier import crée le plateau définitif.

1. Connectez-vous en admin (`admin` / `ADMIN_PASSWORD`).
2. Page **Carte** → section **Gestion du board** → **Importer le board**.
3. Choisissez `board.json` **et** l'image **et** le SVG dans la même boîte de dialogue (l'image et
   le SVG vont ensemble, ou aucun des deux). Les URLs uploadées (`/boards/<uuid>…`) remplacent
   celles du JSON.
4. Vérifiez le message : `Board "Carte 6 Joueurs" importé (25 secteurs)`.
   En Docker, montez le volume `nml-boards` sur `/app/static/boards` (voir README) : sinon les
   fichiers uploadés disparaissent au redéploiement du conteneur.
5. Contrôlez `GET /api/boards` : **un seul** plateau, 25 secteurs, `mapImageUrl`/`svgOverlayUrl`
   non nuls.

Si un plateau existe déjà (remplacement de carte en cours de partie), **supprimez-le d'abord** :
l'import fusionne par nom puis par numéro et ne supprime jamais les secteurs absents du nouveau
fichier. Un plateau de 43 secteurs réimporté avec 25 secteurs en garde 18, avec leurs propriétaires
et armées. La suppression passe par l'API (pas de bouton dans l'UI) :

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/login \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"admin\",\"password\":\"$ADMIN_PASSWORD\"}" \
  | node -pe "JSON.parse(require('fs').readFileSync(0,'utf8')).token")

curl -s http://localhost:8080/api/boards -H "Authorization: Bearer $TOKEN"
curl -X DELETE http://localhost:8080/api/boards/1 -H "Authorization: Bearer $TOKEN"
```

En dev, remplacez `$ADMIN_PASSWORD` par `admin` (compte `admin/admin`).

La suppression emporte les secteurs et les armées/bâtiments/personnages/véhicules qui s'y trouvent
(les joueurs, eux, restent). À faire entre deux parties, pas en plein tour.

Plusieurs plateaux peuvent coexister : la partie active est choisie par l'en-tête `X-Board-Id`
(sélecteur après connexion), jamais par l'ordre de `findAll()`.

### En développement (H2 en mémoire)

En mode démo (`app.import-demo-data=true`, défaut dev/test), le démarrage importe **deux parties** :
la carte principale 43 secteurs (7 joueurs) et la carte 6 joueurs (arcs, 6 joueurs) — voir
[CARTE_ARCS_6_JOUEURS.md](CARTE_ARCS_6_JOUEURS.md). Le profil `dev` réimporte cette démo **à chaque
démarrage** (H2 en mémoire). Deux options :

**A. Démarrer sans la démo** (test isolé). L'état est perdu à chaque redémarrage (H2 en mémoire),
mais on repart d'une base propre :

```bash
cd nml-ms
JWT_SECRET=... JWT_PEPPER=... ./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.arguments=--app.import-demo-data=false
```

Placez l'image et le SVG dans `nml-ui/src/assets/maps/` et importez le `board.json` **sans**
sélectionner les deux fichiers : le JSON garde ses URLs `/assets/...`, servies par `ng serve`.
C'est nécessaire car `ng serve` ne proxifie que `/api` (pas `/boards`) : un asset uploadé serait
introuvable sur `localhost:4200`.

**B. Ajouter votre carte** : importez-la via l'admin ; elle rejoint les deux parties de démo dans le
sélecteur de partie (aucune suppression nécessaire).

Ne remplacez pas `nml-ms/src/main/resources/boards/board.json` ni les `players/*.json` pour une
simple nouvelle partie : ces fixtures alimentent la démo **et de nombreux tests** qui codent en dur
des numéros (41, 43, 38, 32…) et des joueurs (`cegorach`, `trazyn`…). Une nouvelle carte destinée au
jeu passe par l'import admin.

### Ajouter une partie à la démo

La démo est la liste de `PlayerStartupImporter.DemoGame` (nom, `boards/*.json`, fixtures
`players-*/*.json`) : chaque entrée est importée au démarrage. Pour une nouvelle carte, déposez son
`board.json` et ses joueurs dans `nml-ms/src/main/resources/` puis ajoutez un `DemoGame` — aucune
propriété à basculer. Les fixtures existantes (`boards/board.json`, `players/`) alimentent de
nombreux tests qui codent des numéros (41, 43, 38, 32…) et des joueurs (`cegorach`, `trazyn`…) en
dur : ne les remplacez pas pour une simple nouvelle carte.

## 8. Attribuer les 25 secteurs aux 6 joueurs

Un plateau importé est **entièrement neutre** : `board.json` ne porte pas de propriétaire. Il faut
ensuite créer/remplacer les 6 joueurs avec leurs secteurs.

### Méthode recommandée : import de joueurs

Page **Admin > Gestion des joueurs > Importer un joueur**, un fichier par joueur. Le dialogue
demande un mot de passe : c'est celui du compte de connexion (laisser vide = joueur sans compte).

```json
{
  "name": "cegorach",
  "race": "ORKS",
  "money": 30000,
  "sectors": [
    { "sectorNumber": 1 },
    { "sectorNumber": 2 }
  ],
  "buildings": [{ "type": "HEADQUARTERS", "sectorNumber": 1 }]
}
```

- `name` = nom du joueur **et** identifiant de connexion ; nommez le fichier pareil.
- `race` : `ORKS` ou `NECRONS` (sinon pas de traduction d'équipement en boutique).
- `money` : dotation de départ (les fixtures utilisent 30 000).
- `sectors[].sectorNumber` : 2 numéros par joueur, colonne du tableau du §2. Les 13 autres
  (3,4,7,8,11,12,15,16,19,20,23,24,25) restent neutres : ne les mets dans **aucun** fichier joueur.
- `buildings` : le QG dans le secteur intérieur. Sans QG, pas d'élimination par capture de QG.
- `army` est optionnel : laissez vide et achetez les unités en boutique (elles arrivent en réserve,
  puis se déploient sur la carte).

Importez **le plateau d'abord**, les joueurs ensuite : l'import joueur assigne les secteurs du
plateau sélectionné (`X-Board-Id`). Réimporter un joueur existant le remplace (ses anciens secteurs redeviennent
neutres), ce qui permet d'itérer.

| Fichier | Joueur | Secteurs | QG |
|---|---|---|---|
| `cegorach.json` | cegorach | 1, 2 | 1 |
| `mortarion.json` | mortarion | 5, 6 | 5 |
| `angron.json` | angron | 9, 10 | 9 |
| `lurio.json` | lurio | 13, 14 | 13 |
| `nursek.json` | nursek | 17, 18 | 17 |
| `imotekh.json` | imotekh | 21, 22 | 21 |

En `dev` ces six noms existent déjà comme comptes ; réutilisez-les (le mot de passe saisi à l'import
devient celui du compte) ou prenez vos propres noms, en `dev` comme en `prod` : un compte est créé
avec le mot de passe saisi.

### Variante : API secteur par secteur

L'interface de gestion des joueurs ne permet de réaffecter qu'un secteur **déjà possédé** (onglet
« Secteurs » du joueur courant) : elle ne sait pas attribuer un secteur neutre. Pour des affectations
ponctuelles, passez par l'API :

```bash
curl -X PUT "http://localhost:8080/api/admin/sectors/1/owner" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"playerId": 3}'
```

Le tour doit être ouvert (pas de résolution en cours). Le QG s'ajoute ensuite dans le dialogue du
joueur, onglet **Bâtiments > Ajouter un bâtiment > Quartier Général (QG)**, uniquement dans un
secteur qu'il possède.

### Vérification

- `/carte` : 12 secteurs colorés (6 couleurs), 13 en hachures neutres (dont le hub 25), 25 numéros
  lisibles. Au démarrage, la moitié de la carte est donc volontairement neutre.
- L'onglet « Secteurs » de chaque joueur liste ses 2 secteurs.
- La page **Accueil/Classements** ne doit pas montrer de secteur orphelin (un `ownerId` sans joueur).

## 9. Ajuster le rendu

Tout le thème de la carte est dans `nml-ui/src/app/pages/carte/carte.config.ts` :

| Réglage | Effet |
|---|---|
| `overlay.offsetX` / `offsetY` | Décale l'overlay (px CSS) — rustine d'alignement. |
| `label.fontPx` / `weight` / `stroke*` | Taille et halo des numéros (doublés sur petit écran). |
| `playerPalette` | 12 couleurs attribuées par `id` joueur trié. |
| `fill.normalAlpha` / `hoverAlpha` | Opacité de la teinte de propriété. |
| `neutralColor` / `neutralPattern` | Rendu des secteurs neutres. |
| `resourceIcon.size` | Taille des pastilles du calque « Ressources ». |

Après une modification de code (pas de simple asset), passez les contrôles du projet :

```bash
cd nml-ui && npm test && npm run lint && npm run build
```

## 10. Dépannage

| Symptôme | Cause probable |
|---|---|
| Écran bloqué sur « Chargement de la carte… » | SVG vide ou invalide (racine ≠ `<svg>`) : le sanitizer renvoie `null`. Une URL 404 affiche la carte d'erreur. |
| Les secteurs ne réagissent pas | `id` ≠ `pathN`, mauvais préfixe (`sector-3`), ou élément autre que `<path>`/`<polygon>`. |
| Un secteur est cliquable mais sans nom/couleur | `pathN` sans secteur N dans `board.json` (ou l'inverse : `check-map.mjs`). |
| Numéros mal centrés | Placement automatique dans les formes irrégulières ; ajustez la géométrie, pas le SVG. |
| Tout est gris hachuré | Tous les secteurs sont neutres : attribuez-les aux joueurs. |
| La moitié de la carte reste en hachures après l'assignation | Normal en variante rayons : 13 secteurs neutres au départ. |
| Décalage image / zones | Ratio du `viewBox` ≠ ratio de l'image, ou image remplacée sans le même cadrage. |
| Anciens secteurs toujours là | L'import fusionne par nom/numéro : supprimez le plateau avant de réimporter. |
| La carte importée n'apparaît pas dans le sélecteur | Le compte n'y a ni joueur, ni rôle MJ : importez ses joueurs (ou connectez-vous en ADMIN). |
| Plateau perdu au redémarrage | Profil `dev`/défaut = H2 en mémoire, démo réimportée. Utilisez `prod`/PostgreSQL. |
| Icône de ressource absente | Nom hors des 7 ressources (ou faute d'accent/apostrophe). |
| Upload image refusé (413) | Fichier > 10 Mo : compressez le PNG/JPEG. |
| Image uploadée absente en dev | `ng serve` ne proxifie pas `/boards` : utilisez `src/assets/maps/`. |
| SVG refusé à l'upload | Contenu actif, `href` externe, `<style>` ou mauvais Content-Type (`image/svg+xml`). |
| Voisins incohérents | Détecteur approximatif (coins, courbes, transforms) : corrigez `neighbors` et relancez `check-map.mjs`. |

## 11. Checklist

- [ ] Image et `viewBox` au même ratio, image PNG/JPEG < 10 Mo.
- [ ] 25 `<path id="path1">` … `path25`, uniques, sans style ni texte.
- [ ] `board.json` : 25 secteurs, noms/revenus renseignés, ressources valides.
- [ ] `node tools/check-map.mjs` : vert (IDs, symétrie, connexité).
- [ ] Partie visée sélectionnée (`X-Board-Id`).
- [ ] 6 joueurs × 2 secteurs + 13 neutres, un QG par joueur.
- [ ] Carte vérifiée en vue admin **et** en vue joueur (couleurs, numéros, calques).
