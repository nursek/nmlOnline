# Assets des cartes - NML Online

Ce dossier contient les assets visuels des cartes du jeu.

## Structure des fichiers

Pour chaque carte, vous devez fournir :

1. **`{nom}-map.jpg`** : L'image de fond de la carte (visuel)
2. **`{nom}-map-overlay.svg`** : Le SVG avec les zones cliquables des secteurs

## Fichiers actuels

- `main-map-overlay.svg` : SVG exemple avec 16 secteurs (grille 4x4)
- `main-map.jpg` : **À CRÉER** - Remplacez ce placeholder par votre image de carte

## Comment créer votre carte

Consultez le guide complet : **[SVG_MAP_GUIDE.md](../../../SVG_MAP_GUIDE.md)** à la racine du projet.

### Résumé rapide

1. Créez votre image JPG aux dimensions souhaitées (ex: 800x600, 1920x1080)
2. Créez un SVG avec le même viewBox que les dimensions de l'image
3. Dessinez les contours de chaque secteur avec des `<path>` ou `<polygon>`
4. Attribuez à chaque élément un `id="sector-{number}"` correspondant au numéro en base
5. N'ajoutez PAS de styles (fill, stroke) - le système les appliquera automatiquement

### Exemple de SVG

```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 800 600">
  <path id="sector-1" d="M10,10 L190,10 L190,140 L10,140 Z" />
  <path id="sector-2" d="M210,10 L390,10 L390,140 L210,140 Z" />
  <!-- ... autres secteurs ... -->
</svg>
```

## Configuration

Les URLs des assets sont définies dans `nml-ms/src/main/resources/boards/board.json` :

```json
{
  "mapImageUrl": "/assets/maps/main-map.jpg",
  "svgOverlayUrl": "/assets/maps/main-map-overlay.svg"
}
```


