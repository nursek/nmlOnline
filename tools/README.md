# SVG Neighbor Detector

Outil pour analyser automatiquement un fichier SVG de carte et détecter les secteurs voisins en fonction de leur proximité géométrique.

## Installation

```bash
cd tools
npm install
```

## Usage

```bash
# Utilisation par défaut
npm run detect-neighbors

# Avec options personnalisées
node svg-neighbor-detector.js --svg <chemin> --threshold <px> --output <fichier> --samples <n>
```

### Options

| Option | Description | Défaut |
|--------|-------------|--------|
| `--svg` | Chemin vers le fichier SVG | `../nml-ui-bst-angular/src/assets/maps/main-map-overlay.svg` |
| `--threshold` | Distance max (px) pour considérer deux secteurs voisins | `30` |
| `--output` | Fichier JSON de sortie | `neighbors-output.json` |
| `--samples` | Nombre de points échantillonnés par path | `100` |

## Comment ça marche

1. **Parse le SVG** : Extrait tous les `<path>` avec un ID au format `pathX` ou `sector-X`
2. **Échantillonne les points** : Convertit chaque path en une liste de points (coordonnées absolues)
3. **Calcule les distances** : Pour chaque paire de secteurs, calcule la distance minimale entre leurs points
4. **Détecte les voisins** : Si la distance est ≤ seuil, les secteurs sont considérés comme voisins

## Format de sortie

Le fichier JSON généré contient :

```json
{
  "generatedAt": "2026-02-14T00:00:00.000Z",
  "totalSectors": 43,
  "threshold": 17,
  "sectors": [
    { "number": 1, "neighbors": [3, 9] },
    { "number": 2, "neighbors": [10, 11, 17, 19] },
    ...
  ]
}
```

## Intégration avec board.json

Pour mettre à jour `nml-ms/src/main/resources/boards/board.json` :

1. Exécutez le script
2. Copiez les arrays `neighbors` de la sortie console vers chaque secteur du board.json
3. Ou utilisez le fichier `neighbors-output.json` pour une fusion automatique

## Ajuster le seuil

- **Espacement pour la carte Risk** : Utilisez `--threshold 17` (marge parfaitement adaptée à la carte Risk)
- **Secteurs collés** : Utilisez `--threshold 5`
- **Trop de voisins détectés** : Diminuez le seuil
- **Pas assez de voisins** : Augmentez le seuil

## Prérequis du SVG

Les paths doivent avoir un ID au format :
- `pathX` (ex: `path1`, `path42`)
- `sector-X` (ex: `sector-1`, `sector-16`)

Les paths sans ID ou avec un ID non reconnu sont ignorés.

