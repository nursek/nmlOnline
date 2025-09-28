# nmlOnline

nmlOnline est une application Java destinée à la gestion, la simulation et l'analyse d'armées et d'équipements pour des jeux de rôle ou de stratégie. Elle permet de modéliser des unités, de leur attribuer des classes et des équipements, de calculer leurs statistiques, et d'importer/exporter des armées via des fichiers structurés (JSON, TXT, CSV).

## Objectifs du projet

- Fournir un outil d'aide à la création et à la gestion d'armées pour MJ et joueurs.
- Permettre l'import/export de données d'armées et d'équipements.
- Simuler l'évolution, l'équipement et les bonus/malus des unités.
- Offrir une base extensible pour des outils web ou desktop.

## Fonctionnalités principales

- **Gestion des unités** : création, évolution, double classe, expérience.
- **Gestion des équipements** : compatibilité, bonus, inventaire joueur, coût.
- **Import/export** :
  - Import d'armées depuis des fichiers texte structurés ou JSON.
  - Import d'équipements depuis un CSV.
- **Calcul automatique** des statistiques finales (attaque, défense, PDF, PDC, armure, esquive).
- **Affichage détaillé** des armées et de l'inventaire.
- **Tri et regroupement** des unités selon expérience, défense, etc.
- **Bonus/malus globaux** appliqués au joueur ou à l'armée.

## Structure du projet

- `src/main/java/com/mg/nmlonline/model/` : Modèles principaux (`Player`, `Unit`, `Equipment`, etc.).
- `src/main/java/com/mg/nmlonline/service/` : Services d'import/export et logique métier.
- `src/main/java/com/mg/nmlonline/test/` : Classes de démonstration et tests unitaires.
- `src/main/resources/` :
  - `equipments.csv` : Liste des équipements disponibles.
  - `army_example.txt` : Exemple de fichier d'armée.
  - `players/` : Exemples de joueurs au format JSON.

## Technologies utilisées

- **Java 17+**
- **Lombok** pour la génération de code (getters/setters, etc.)
- **Fichiers CSV/JSON/TXT** pour la persistance des données

## Installation

1. **Cloner le dépôt :**
   ```bash
   git clone https://github.com/nursek/nmlOnline.git
   cd nmlOnline
   ```

2. **Ouvrir dans votre IDE Java** (IntelliJ, Eclipse, VS Code...).

3. **Compiler le projet** (Maven/Gradle ou via l'IDE).

4. **Lancer les classes de test/démo** dans `src/main/java/com/mg/nmlonline/test/` pour voir des exemples d'utilisation.

## Utilisation

### Exemple : Import d'une armée depuis un fichier texte

```java
Player player = new Player("Ratcatcher");
player.fromFile("src/main/resources/army_example.txt");
player.displayArmy();
player.displayEquipments();
```

### Exemple : Import de joueurs depuis JSON

```java
PlayerService playerService = new PlayerService();
Player player = playerService.importPlayerFromJson("src/main/resources/players/player1.json");
player.displayArmy();
```

### Exemple : Création et équipement manuel d'une unité

```java
Unit larbin = new Unit(1, "Larbin Léger", UnitClass.LEGER);
larbin.equip(EquipmentFactory.createFromName("Pistolet-mitrailleur"));
```

## Format des fichiers

- **CSV équipements** : voir `src/main/resources/equipments.csv`
- **TXT armée** : voir `src/main/resources/army_example.txt`
- **JSON joueur** : voir `src/main/resources/players/player1.json`

## Contribution

Les contributions sont les bienvenues !  
Pour contribuer :

1. Forkez le projet.
2. Créez une branche (`git checkout -b ma-fonctionnalite`).
3. Commitez vos modifications (`git commit -am 'Ajout d'une fonctionnalité'`).
4. Poussez la branche (`git push origin ma-fonctionnalite`).
5. Ouvrez une Pull Request.

## Licence

Ce projet est sous licence MIT. Voir le fichier `LICENSE` pour plus d'informations.

## Auteurs

- [Nursek](https://github.com/nursek)
- [Luriot](https://github.com/luriot)

---

v1 : doc file helper, gestion d'armées et d'équipements, import/export JSON/TXT/CSV.
