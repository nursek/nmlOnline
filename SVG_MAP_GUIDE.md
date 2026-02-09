# Guide : Création de cartes personnalisées SVG

Ce guide explique comment créer une carte personnalisée pour NML Online en utilisant une image JPG de fond et un fichier SVG pour les zones cliquables.

## Architecture

- **Image JPG** : L'image de fond de la carte (visuel statique)
- **SVG Overlay** : Un fichier SVG transparent superposé contenant les contours des secteurs cliquables

## Prérequis du fichier SVG

### 1. Structure obligatoire

Chaque secteur doit être un élément `<path>` ou `<polygon>` avec un attribut `id` au format :
```
id="sector-{number}"
```
où `{number}` correspond au numéro du secteur en base de données.

**Exemple :**
```xml
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 1920 1080">
  <path id="sector-1" d="M100,100 L300,100 L300,250 L100,250 Z" />
  <path id="sector-2" d="M320,100 L520,100 L500,280 L310,260 Z" />
  <polygon id="sector-3" points="550,100 750,100 730,280 540,260" />
</svg>
```

### 2. ViewBox

Le `viewBox` du SVG **DOIT** correspondre aux dimensions de l'image JPG pour un alignement parfait.

- Si votre image fait 1920x1080 pixels : `viewBox="0 0 1920 1080"`
- Si votre image fait 800x600 pixels : `viewBox="0 0 800 600"`

### 3. Pas de styles inline

N'ajoutez **PAS** de `fill`, `stroke`, ou `style` aux éléments path. Le système appliquera dynamiquement les couleurs selon le propriétaire du secteur.

❌ **Mauvais :**
```xml
<path id="sector-1" fill="red" stroke="black" d="..." />
```

✅ **Bon :**
```xml
<path id="sector-1" d="..." />
```

### 4. Attributs supportés

- `id` : **Obligatoire** - Identifiant du secteur (format : `sector-{number}`)
- `d` : Chemin du path (pour `<path>`)
- `points` : Points du polygone (pour `<polygon>`)

## Création avec Illustrator

1. **Nouveau document** : Créez un document aux dimensions exactes de votre image JPG
2. **Importer l'image** : Placez votre image JPG en arrière-plan (calque verrouillé)
3. **Dessiner les zones** : Sur un nouveau calque, dessinez les contours de chaque secteur avec l'outil Plume
4. **Nommer les calques** : Renommez chaque path avec `sector-1`, `sector-2`, etc.
5. **Exporter** :
   - Fichier → Exporter → Exporter pour les écrans
   - Format : SVG
   - Options : "Inline Style" décoché, "Responsive" coché

## Création avec Inkscape

1. **Nouveau document** : Définissez les dimensions aux dimensions de votre image JPG
2. **Importer l'image** : Fichier → Importer votre JPG (calque de référence)
3. **Dessiner les zones** : Utilisez l'outil Bézier pour tracer les contours
4. **Définir les IDs** : Sélectionnez chaque path → Objet → Propriétés de l'objet → ID = `sector-1`, etc.
5. **Exporter** :
   - Fichier → Enregistrer sous → SVG simple
   - Supprimer le JPG de référence avant l'export

## Configuration dans le projet

### 1. Placer les fichiers

```
nml-ui-bst-angular/src/assets/maps/
├── ma-carte.jpg          # Image de fond
├── ma-carte-overlay.svg  # SVG des zones
└── README.md
```

### 2. Configurer le board.json

```json
{
  "name": "Ma Carte",
  "mapImageUrl": "/assets/maps/ma-carte.jpg",
  "svgOverlayUrl": "/assets/maps/ma-carte-overlay.svg",
  "sectors": [
    {
      "number": 1,
      "name": "Secteur Nord",
      "income": 2500,
      "neighbors": [2, 5]
    },
    ...
  ]
}
```

**Note :** Les champs `x` et `y` ne sont plus nécessaires car la position est définie par le path SVG.

## Comportement interactif

Le système gère automatiquement :

| État | Comportement |
|------|--------------|
| Normal | Contour avec la couleur du propriétaire, fond transparent |
| Hover | Fond semi-transparent (20% opacité) |
| Sélectionné | Fond semi-transparent (25% opacité) + ombre portée + contour épais |
| Voisin du sélectionné | Fond jaune semi-transparent + contour jaune |
| Filtré (autre joueur) | Opacité réduite (30%) |

## Fallback

Si le SVG ne peut pas être chargé (erreur réseau, fichier manquant), le système basculera automatiquement sur l'affichage en grille classique utilisant les coordonnées `x` et `y` des secteurs.

## Conseils

1. **Zones bien définies** : Assurez-vous que les zones ne se chevauchent pas
2. **Contours fermés** : Les paths doivent être fermés (terminer par `Z` ou revenir au point de départ)
3. **Test** : Ouvrez le SVG dans un navigateur pour vérifier que les zones sont correctes
4. **IDs uniques** : Vérifiez que chaque `sector-{number}` correspond à un secteur existant en base

