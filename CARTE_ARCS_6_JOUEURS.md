# Carte 6 joueurs — variante « arcs » (3 / 2 / 6)

Variante de la carte 25 secteurs où les neutres n'ont pas tous le même nombre de voisins joueurs :

- **3 arcs neutres** adjacents à **3 joueurs** (les zones de bataille à trois) ;
- **9 neutres** adjacents à **2 joueurs** ;
- **le hub 25** adjacent aux **6 joueurs**.

Toujours **25 secteurs**, **6 joueurs × 2 secteurs** (12 possédés), **13 neutres**.

## Géométrie

Roue de 12 rayons de 30° autour du hub à 12 côtés. Les 6 joueurs occupent les rayons alternés
(0-30°, 60-90°, 120-150°, 180-210°, 240-270°, 300-330°) et sont découpés en **secteur intérieur +
secteur extérieur**.

Les 3 arcs sont autour des joueurs 1, 3 et 5 (0-30°, 120-150°, 240-270°) : chaque arc couvre
**90°** (le rayon du joueur + les deux rayons neutres voisins) et enveloppe le secteur extérieur du
joueur, qui devient une poche en retrait à 90 % du rayon. Les 3 autres joueurs (2, 4, 6) ont un
secteur extérieur plein jusqu'au bord.

Les 9 neutres à 2 joueurs sont les cellules de l'anneau intérieur : 6 interstices, dont 3 sont
coupés radialement en deux.

| Type | Numéros | Voisins joueurs |
|---|---|---|
| Joueurs (intérieur + extérieur) | 1-2, 3-4, 5-6, 7-8, 9-10, 11-12 | — |
| Arcs à 3 joueurs | 13, 14, 15 | 3 |
| Neutres à 2 joueurs | 16-24 | 2 |
| Hub | 25 | 6 |

- Joueur 1 : 1, 2 (QG 1) — neutres voisins 13, 16, 17, 24, 25.
- Joueur 2 : 3, 4 (QG 3) — neutres voisins 13, 14, 16, 17, 18, 25.
- Joueur 3 : 5, 6 (QG 5) — neutres voisins 14, 18, 19, 20, 25.
- Joueur 4 : 7, 8 (QG 7) — neutres voisins 14, 15, 19, 20, 21, 25.
- Joueur 5 : 9, 10 (QG 9) — neutres voisins 15, 21, 22, 23, 25.
- Joueur 6 : 11, 12 (QG 11) — neutres voisins 13, 15, 22, 23, 24, 25.

**Équilibre** : les joueurs 1, 3 et 5 (les « enveloppés ») ont 5 voisins neutres ; les joueurs 2, 4
et 6 en ont 6 et perdent moins de surface extérieure. Le total des liens joueur-neutre fait 33, non
divisible par 6 : c'est le compromis de cette géométrie. À compenser par le revenu/les ressources
si nécessaire.

**Revenus/ressources du gabarit** : joueurs 2 000 (Prométhéum/Plastacier), arcs 3 000 (Noctilithe),
neutres 16-24 2 200-2 500 (Adamentium/Auramite), hub 4 000 (Noctilithe).

## Démarrage automatique

En mode démo (`app.import-demo-data=true`, défaut dev/test), `PlayerStartupImporter` importe **deux
parties** : la carte principale 43 secteurs (7 joueurs) et cette carte arcs (`boards/carte-25.json`,
6 joueurs de `players-carte-25/`). Les comptes existent déjà : chaque joueur arcs démarre avec
2 secteurs + QG, 30 000 ₡, sans unité ni personnage (à acheter ou ajouter via l'admin), et bascule
de partie via le sélecteur (`X-Board-Id`).

Comptes du profil `dev` : `admin/admin` pour l'admin, puis `cegorach/cegorach`,
`mortarion/mortarion`, `angron/angron`, `lurio/lurio`, `nursek/nursek`, `imotekh/imotekh` pour
les six joueurs.

## Générer le plateau

```bash
# depuis la racine du dépôt
node tools/generate-map.mjs --variant arcs --out carte-25

cd nml-ui
node -e "require('sharp')('../carte-25/fond.svg').png().toFile('../carte-25/fond.png')"
cd ..

node tools/check-map.mjs carte-25/overlay.svg carte-25/board.json
```

Sortie attendue : `25 secteurs : 6 joueurs × 2 secteurs + 3 arcs neutres à 3 joueurs + 9 neutres à
2 joueurs + hub 6`, puis `✓ 25 secteurs...`. Copiez `overlay.svg` et `fond.png` dans
`nml-ui/src/assets/maps/` sous les noms attendus par le `board.json` :

```bash
cp carte-25/overlay.svg nml-ui/src/assets/maps/carte-25-overlay.svg
cp carte-25/fond.png   nml-ui/src/assets/maps/carte-25-fond.png
```

Le `board.json` porte le nom **Carte 6 Joueurs (Arcs)** ; importez-le via l'admin (ou utilisez-le
comme plateau de démo, voir le guide principal). Les joueurs s'importent ensuite avec **2 secteurs
chacun** : `1,2`, `3,4`, `5,6`, `7,8`, `9,10`, `11,12`, QG `1,3,5,7,9,11`.

Les 13 secteurs neutres (dont les 3 arcs) sont recouverts d'un motif de hachures **opaque** : le
décor sous ces secteurs ne se voit qu'après capture. Les secteurs possédés sont teintés à ~50 % par
la couleur du joueur.

## Vérification

- `node tools/check-map.mjs ...` doit être vert.
- En jeu : 12 secteurs colorés (6 couleurs), 13 en hachures, 25 numéros.
- Les 3 arcs doivent apparaître comme des zones neutres de 3 voisins joueurs ; le hub comme le
  centre à 6.
