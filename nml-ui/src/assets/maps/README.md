# Assets des cartes

Pour chaque carte : une image de fond (`{nom}-fond.png` ou `.jpg`) et un overlay SVG
(`{nom}-overlay.svg`) qui porte une zone cliquable par secteur.

## Fichiers actuels

- `main-map.png` : image de fond de la carte principale (1501 × 1004).
- `main-map-overlay.svg` : overlay de la carte principale (43 secteurs, `id="pathN"`).

## Créer une nouvelle carte

Guide complet (image, SVG, `board.json`, import, attribution aux joueurs) :
**[SVG_MAP_GUIDE.md](../../../../SVG_MAP_GUIDE.md)**.

Gabarit 6 joueurs / 25 secteurs (12 rayons : 6 joueurs × 2 secteurs + 6 bandes neutres × 2 + hub) :

```bash
# depuis la racine du dépôt — --variant rings pour l'ancien découpage 6 quartiers × 4
node tools/generate-map.mjs --out carte-25

cd nml-ui
node -e "require('sharp')('../carte-25/fond.svg').png().toFile('../carte-25/fond.png')"
cp ../carte-25/overlay.svg src/assets/maps/carte-25-overlay.svg
cp ../carte-25/fond.png   src/assets/maps/carte-25-fond.png
```

Contrôle avant import (depuis la racine) :

```bash
node tools/check-map.mjs carte-25/overlay.svg carte-25/board.json
```

Variante **arcs** (3 neutres à 3 joueurs, 9 à 2, hub à 6) :
[CARTE_ARCS_6_JOUEURS.md](../../../../CARTE_ARCS_6_JOUEURS.md).

## Règles de l'overlay

1. Un `<path>` ou `<polygon>` par secteur, fermé, avec `id="path{numéro}"` (`path1`, `path2`…).
2. `viewBox` au même ratio que l'image ; l'overlay est étiré sur la boîte de l'image.
3. Pas de styles (`fill`, `stroke`) ni de `<style>` : le front applique les couleurs.
4. Pas de texte : les numéros sont générés et centrés par l'application.
5. Pas de script, d'`href` externe ni de `<foreignObject>` (refusés à l'upload, neutralisés au chargement).

`assets/maps/` est exclu du pipeline WebP (`scripts/optimize-images.mjs`) : l'image est servie telle
quelle et doit rester sous 10 Mo (limite d'upload admin).
