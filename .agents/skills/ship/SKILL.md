---
name: ship
description: Expédie une ou plusieurs pull requests prêtes et revues du dépôt nmlOnline de bout en bout — master local à jour, rebase, résolution des conflits (migrations Flyway et specs incluses), push, attente de la CI GitHub, corrections d'échec (2 tentatives max), puis squash & merge et suppression de branche. Utiliser dès que l'utilisateur dit /ship, « ship la PR 143 », « expédie la PR », « merge la PR après rebase », « enchaîne les PRs 143 puis 144 », ou décrit ce workflow de ship/rebase/CI/merge. Ne pas utiliser pour une revue de code seule, ni pour merger sans rebase et sans attendre la CI.
---

# Ship — expédier une PR nmlOnline

But : amener une PR prête sur `master` en squash, avec une CI verte, sans jamais contourner un échec. Ce skill a été extrait des ship réels des PR #143, #144, #145 et #141 du dépôt.

## Entrées

- Un ou plusieurs numéros de PR, traités **dans l'ordre donné**.
- Sans numéro : prendre la PR de la branche courante (`gh pr view --json number`) ; si c'est ambigu, demander.

Lire `AGENTS.md` avant de commencer (tests, migrations, conventions). Le dépôt est à la racine du workspace ; toutes les commandes partent de là.

## Garde-fous

- Arbre propre : `git status --porcelain --untracked-files=no` doit être vide. Sinon stop et signale, ne pas stasher en douce.
- `gh auth status` valide, `origin` pointe sur le dépôt GitHub attendu.
- La PR doit être `OPEN`, non `isDraft`, avec `baseRefName` = `master`. Sinon stop et explique.
- Ne jamais merger avec une CI rouge ou en cours, ni `gh pr merge --admin`, ni pousser directement sur `master`, ni modifier une migration déjà présente sur `master`.
- Arrêter tout le lot dès qu'une PR nécessite une décision humaine : les PRs suivantes doivent rebaser sur un master complet.

## Boucle par PR

### 1. Mettre le master local à jour

```bash
git fetch origin --prune
git checkout master
git merge --ff-only origin/master
```

Si le `--ff-only` échoue, stop : le master local contient des commits non poussés, ne pas les écraser.

### 2. Récupérer la branche et rebaser

```bash
gh pr view <n> --json number,title,state,isDraft,baseRefName,headRefName,mergeable
git checkout <headRefName>        # si absente : git checkout -B <headRefName> origin/<headRefName>
git rebase origin/master
```

Si la branche locale diverge d'`origin/<headRefName>` (commits locaux non poussés), stop au lieu d'écraser. Après un push précédent, `origin` est la référence.

### 3. Résoudre les conflits

```bash
git status --short                     # UU = conflit
git show :2:<fichier>                  # côté master (ours)
git show :3:<fichier>                  # côté PR (theirs)
```

Lire les deux côtés et l'intention de la PR, puis éditer le fichier à la main. Ne jamais résoudre en prenant un côté en bloc. Ensuite `git add`, puis `GIT_EDITOR=true git rebase --continue`. Si un commit devient réellement vide, `git rebase --skip` est acceptable ; sinon, investiguer.

Une fois le rebase passé, **ne pas pousser tout de suite** : un rebase sans conflit textuel casse souvent des choses en aval parce que `master` a refactoré les tests entre-temps. Lancer les checks locaux, corriger, puis pousser.

### 4. Conflits sémantiques (specs / API déplacées)

Le dépôt refactorise régulièrement ses tests. Un rebase propre peut laisser des specs qui utilisent des APIs déplacées ou supprimées (ex. une boîte de dialogue scindée en deux, un helper déplacé). Les tests doivent suivre le déplacement :

- Adapter la spec au nouveau composant/helper en conservant son intention (déplacer le test vers le nouveau fichier plutôt que le supprimer).
- Ne jamais affaiblir ou désactiver une assertion juste pour passer ; si le test révèle un vrai bug de la PR, corriger le code de la PR.
- Commit dédié, en français, par exemple : `test(déplacements, rapports): aligne les specs sur le refactor des écrans`.

### 5. Renuméroter la migration Flyway si nécessaire

Les migrations sont dans `nml-ms/src/main/resources/db/migration/` et Flyway exige des versions uniques.

```bash
git ls-tree --name-only origin/master nml-ms/src/main/resources/db/migration/ | sort -V | tail
git diff --name-status origin/master -- nml-ms/src/main/resources/db/migration/
```

- Seuls les fichiers **ajoutés par la PR** peuvent être renumérotés. Ne jamais toucher une migration déjà sur `master` (appliquée en prod).
- Si le numéro est déjà pris : `git mv V<dup>__desc.sql V<max+1>__desc.sql`, contenu SQL inchangé, références éventuelles mises à jour, puis commiter le renommage (l'amender dans le commit de migration s'il s'agit du tip).
- Si la PR change le schéma d'une entité sans migration, c'est une décision produit : stop et demande.
- Toujours tester avec `clean` : un `target/classes/db/migration` périmé fait échouer Flyway sur de fausses versions dupliquées.

### 6. Checks locaux avant push

Lancer les commandes exactes du CI, uniquement pour les stacks touchées :

```bash
# backend, si nml-ms/ est modifié
cd nml-ms && ./mvnw -B clean verify

# frontend, si nml-ui/ est modifié
cd nml-ui && npm run lint && npm test -- --ci --coverage && npm run build
```

- `npm run build` est obligatoire : c'est le seul contrôle des templates stricts (`strictTemplates`).
- Si `./mvnw verify` est bloqué par l'environnement (dépôt Maven non inscriptible), `clean test` suffit, mais le signaler dans le rapport ; la CI fera le `verify`.
- Piège d'environnement : si `npm test` échoue dès le `pretest` avec `EROFS ... mkdtemp`, relancer avec `TMPDIR=<dossier inscriptible>`.
- Lancer backend et frontend en parallèle si possible. Écrire les logs dans un dossier jetable ignoré par git, puis le supprimer.
- Corriger et relancer jusqu'au vert avant de pousser.

### 7. Pousser

```bash
git status --short
git push --force-with-lease origin <headRefName>
```

`--force-with-lease` est nécessaire (le rebase réécrit l'historique) et évite d'écraser un push concurrent. Jamais de `--force` nu.

### 8. Attendre la CI

```bash
sleep 20 && gh pr checks <n> --watch --interval 15
```

- Juste après un push, `gh pr checks` peut répondre « no checks reported » : réessayer après quelques secondes, ce n'est pas un échec.
- Vert signifie : tous les checks attendus en `SUCCESS` **et** `mergeStateStatus` = `CLEAN`.

```bash
gh pr view <n> --json mergeable,mergeStateStatus,statusCheckRollup
```

### 9. CI rouge — deux tentatives maximum

Partir de `tentatives = 0`. Tant que la CI est rouge :

1. Identifier le job et le vrai message :

```bash
gh pr checks <n>
gh run view <run-id> --log-failed
```

2. Diagnostiquer la cause racine, et la reproduire localement quand c'est possible.
3. Échec d'infrastructure évident (réseau, runner, checkout) sans assertion en échec : relancer une fois `gh run rerun <run-id> --failed`. Cela ne consomme pas une tentative de correctif.
4. Sinon, corriger, commit, pousser (un nouveau commit, push simple), relancer la CI, `tentatives += 1`.
5. Après la deuxième correction, si c'est encore rouge : **stop**. Rapport avec la PR, l'URL du run, le job et l'extrait d'erreur, les correctifs déjà poussés et la décision attendue. Ne pas merger, ne pas désactiver de check, ne pas affaiblir un test, ne pas utiliser `--admin`.

### 10. Squash & merge

Seulement si la CI est verte et `mergeStateStatus` = `CLEAN` :

```bash
gh pr merge <n> --squash --delete-branch
```

Vérifier :

```bash
gh pr view <n> --json state,mergedAt,mergeCommit   # state = MERGED
git ls-remote --heads origin <headRefName>         # doit être vide
```

Le squash est la convention du dépôt. Ne pas supprimer d'autres branches.

- Si le merge est refusé (protection de branche, permissions), stop et rapporte le message exact ; ne pas contourner.
- Si la branche distante subsiste après le merge, la supprimer avec `git push origin --delete <headRefName>` — uniquement une fois le merge confirmé.

### 11. PR suivante

Après chaque merge :

```bash
git fetch origin --prune
```

Puis rebaser la PR suivante sur le nouveau `master`, qui contient déjà les merges précédents. Ne jamais rebaser tout le lot à l'avance : les bases seraient périmées et les conflits inutiles.

## Fin de lot

```bash
git checkout master
git merge --ff-only origin/master
git status --short
```

Rapport en français, un bloc par PR puis un bilan final :

- base de rebase (sha court) ;
- conflits résolus, fichiers et raison ;
- migration renumérotée (ancien → nouveau numéro) ;
- specs adaptées et pourquoi ;
- checks locaux : nombre de tests, lint, build ;
- CI : nombre de passes et résultat ;
- sha du squash merge, branche supprimée ;
- sha final de `master`, arbre propre.

En cas de stop, remplacer le bilan par : étape atteinte, URL du run, erreur exacte, correctifs tentés, décision humaine attendue. Ne jamais annoncer un merge qui n'a pas eu lieu.
