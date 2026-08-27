# Assets de la boutique — NML Online

Vignettes affichées dans la boutique (onglets Équipements et Véhicules).
Format attendu : **PNG**, ~200×120 px (object-fit cover).

## Emplacement

```
src/assets/shop/
├── equipment/   ← 1 PNG par équipement
├── vehicles/    ← 1 PNG par type de véhicule
└── resources/   ← 1 PNG par type de ressource
```

## Convention de nommage

### Équipements

`{slug}.png` où le slug = nom de l'équipement :

- minuscules
- accents supprimés (é→e, à→a, ç→c…)
- tout non-alphanumérique (espaces, apostrophes, crochets) → `-`

Exemples : `Pistolet 9mm` → `assets/shop/equipment/pistolet-9mm.png`, `Mini machine gun [CM] [MP]` → `mini-machine-gun-cm-mp.png`.

### Véhicules

`{typename}.png` où typename = nom de l'enum `VehicleType` en minuscules.

## Fichiers attendus

### Équipements (57)

#### Armes à feu (FIREARM)

| Fichier                                   | Nom en jeu                          |
| ----------------------------------------- | ----------------------------------- |
| `assets/shop/equipment/pistolet-9mm.png`                        | Pistolet 9mm                        |
| `assets/shop/equipment/pistolet-mitrailleur.png`                | Pistolet-mitrailleur                |
| `hk-mp7.png`                              | HK-MP7                              |
| `mitrailleuse.png`                        | Mitrailleuse                        |
| `fusil-d-assaut.png`                      | Fusil d'assaut                      |
| `mini-machine-gun.png`                    | Mini machine gun                    |
| `mini-machine-gun-cm.png`                 | Mini machine gun [CM]               |
| `mini-machine-gun-mp.png`                 | Mini machine gun [MP]               |
| `mini-machine-gun-cm-mp.png`              | Mini machine gun [CM] [MP]          |
| `fusil-a-pompe.png`                       | Fusil à pompe                       |
| `winchester.png`                          | Winchester                          |
| `tromblon.png`                            | Tromblon                            |
| `tromblon-dm.png`                         | Tromblon [DM]                       |
| `tromblon-mgc.png`                        | Tromblon [MGC]                      |
| `tromblon-dm-mgc.png`                     | Tromblon [DM] [MGC]                 |
| `pistolet-chauffant.png`                  | Pistolet chauffant                  |
| `fusil-a-impulsion-electromagnetique.png` | Fusil à impulsion électromagnétique |
| `assets/shop/equipment/canon-a-glace.png`                       | Canon à glace                       |
| `lance-flammes.png`                       | Lance-flammes                       |
| `bombes-collantes.png`                    | Bombes collantes                    |
| `lance-roquettes.png`                     | Lance-roquettes                     |
| `lance-grenades.png`                      | Lance-grenades                      |
| `fusil-de-sniper-leger.png`               | Fusil de sniper léger               |
| `fusil-de-sniper-lourd.png`               | Fusil de sniper lourd               |
| `fusil-de-sniper-de-combat.png`           | Fusil de sniper de combat           |

#### Armes de corps-à-corps (MELEE)

| Fichier                     | Nom en jeu            |
| --------------------------- | --------------------- |
| `poing-americain.png`       | Poing américain       |
| `matraque-telescopique.png` | Matraque télescopique |
| `batte-de-metal.png`        | Batte de métal        |
| `machette.png`              | Machette              |
| `hache-de-bucheron.png`     | Hache de bûcheron     |
| `tronconneuse.png`          | Tronçonneuse          |
| `matraque-electrique.png`   | Matraque électrique   |
| `gantelet-electrique.png`   | Gantelet électrique   |
| `panachurros.png`           | Panachurros           |
| `panachouquette.png`        | Panachouquette        |
| `panachoucroute.png`        | Panachoucroute        |
| `couteau-de-cuisine.png`    | Couteau de cuisine    |
| `couteau-de-combat.png`     | Couteau de combat     |

#### Équipement défensif (DEFENSIVE)

| Fichier                                | Nom en jeu                       |
| -------------------------------------- | -------------------------------- |
| `tenue-ultra-legere.png`               | Tenue ultra légère               |
| `grenade-lacrymogene.png`              | Grenade lacrymogène              |
| `gilet-pare-balles-leger.png`          | Gilet pare-balles léger          |
| `gilet-pare-balles-moyen.png`          | Gilet pare-balles moyen          |
| `equipement-militaire-complet.png`     | Équipement militaire complet     |
| `protection-dorsale.png`               | Protection dorsale               |
| `bouclier-anti-emeutes.png`            | Bouclier anti-émeutes            |
| `bouclier-balistique.png`              | Bouclier balistique              |
| `grenade-assourdissante.png`           | Grenade assourdissante           |
| `tenue-legere-en-fibre-chauffante.png` | Tenue légère en fibre chauffante |
| `armure-conductrice.png`               | Armure conductrice               |
| `armure-thermoresistante.png`          | Armure thermorésistante          |
| `armure-isolante.png`                  | Armure isolante                  |
| `armure-thermique.png`                 | Armure thermique                 |
| `protege-dents.png`                    | Protège-dents                    |
| `casque-militaire.png`                 | Casque militaire                 |
| `gilet-kevlar.png`                     | Gilet Kevlar                     |
| `treillis-de-camouflage-urbain.png`    | Treillis de camouflage urbain    |
| `gilet-de-camouflage-optique.png`      | Gilet de camouflage optique      |

### Véhicules (6)

| Fichier               | Type                  |
| --------------------- | --------------------- |
| `tourelle.png`        | Véhicule à tourelle   |
| `vtt_leger.png`       | VTT léger             |
| `vtt_blinde.png`      | VTT blindé            |
| `assets/shop/resources/tank.png`            | Tank de combat        |
| `helicoptere.png`     | Hélicoptère de combat |
| `avion_transport.png` | Avion de transport    |

### Ressources (7)

`{slug}.png` — même convention de slug que les équipements.

| Fichier          | Ressource  |
| ---------------- | ---------- |
| `cigares.png`    | Cigares    |
| `alcool.png`     | Alcool     |
| `antiquites.png` | Antiquités |
| `ivoire.png`     | Ivoire     |
| `uranium.png`    | Uranium    |
| `or.png`         | Or         |
| `joyaux.png`     | Joyaux     |

## Comportement en l'absence d'image

Si un fichier PNG est absent ou introuvable, la carte affiche un fallback
(icône `image_not_supported`). Aucune erreur n'est levée — déposez les
images au fur et à mesure.
