# Hoyo Endgame Tracker

Application de bureau (Java 21 + JavaFX) pour suivre ses endgames **Genshin Impact** (Abysses spiralées,
Théâtre de l'imaginarium, Carnage chtonien). La page **Honkai: Star Rail** viendra plus tard.

## Lancer

Prérequis : JDK 21+ et Maven.

```bash
mvn javafx:run
```

> Lancez la commande **depuis la racine du projet** : le dossier `data/` est lu et écrit à côté de `src/`.
> Pour utiliser un autre dossier : `mvn javafx:run -Djavafx.args=` puis `-Dhoyo.data.dir=C:\chemin\data`
> (ou ajoutez `-Dhoyo.data.dir=...` aux options de la JVM dans votre IDE).

Dans IntelliJ : ouvrir le `pom.xml`, puis lancer `com.hoyoendgame.App` (répertoire de travail = racine du projet).

## Structure

```
data/                        <- vos données (même niveau que src/)
  genshinCharacters.json     roster de personnages
  genshinEquipes.json        équipes enregistrées (format PLACEHOLDER, voir ci-dessous)
  genshinAbysses.json        historique des Abysses spiralées
  genshinTheatre.json        historique du Théâtre de l'imaginarium
  genshinCarnage.json        historique du Carnage chtonien
src/main/java/com/hoyoendgame/
  App.java                   fenêtre principale + navigation
  model/                     classes de données (sérialisées en JSON)
  data/                      JsonStore (écriture atomique), DataStore, ExcelService (Apache POI)
  ui/                        vues et fenêtres JavaFX
src/main/resources/style.css thème sombre
```

## Sauvegarde des données

- Chaque modification (ajout, édition, suppression, import Excel) réécrit immédiatement le fichier JSON concerné,
  et `App.stop()` sauvegarde une dernière fois à la fermeture : rien n'est perdu au redémarrage.
- L'écriture est atomique (fichier `.tmp` puis renommage). Si un fichier JSON est illisible, il est conservé en `.bak`
  et l'application repart avec une liste vide pour ce fichier.
- Les fichiers manquants sont recréés au démarrage.

## Formats JSON

Tous les fichiers ont la forme `{ "version": 1, "<clé>": [ ... ] }` (un tableau JSON nu est aussi accepté en lecture).
Les champs inconnus sont ignorés.

### `genshinCharacters.json` (clé `characters`)

```json
{ "id": "c-furina", "name": "Furina", "element": "HYDRO", "level": 90, "constellation": 2, "weapon": "", "refinement": 1 }
```

`element` : PYRO, HYDRO, ANEMO, ELECTRO, DENDRO, CRYO, GEO (les noms français, avec ou sans accent, sont acceptés).
Alias acceptés en lecture : `nom`, `niveau`, `doublons`, `arme`, `raffinement`. L'`id` est généré s'il manque.

### `genshinEquipes.json` (clé `teams`) : PLACEHOLDER

Format provisoire, à remplacer par le vôtre (adapter `model/Team.java` et `model/TeamMember.java`) :

```json
{
  "id": "t-1", "name": "Hydro Furina", "usage": "Abysses · seconde moitié",
  "members": [
    { "characterId": "c-furina", "name": "Furina", "element": "HYDRO",
      "level": 90, "constellation": 2, "weapon": "", "refinement": 1 }
  ]
}
```

Chaque membre est un instantané autonome (niveau, doublons, arme, raffinement) ; `characterId` est optionnel.

### Endgames (clé `clears`)

- **Abysses** : `patch`, `rotation`, `floor9`..`floor12` (étoiles /9), `team1`, `team2` (listes de membres).
- **Théâtre** : `year`, `month`, `patch`, `mode` (EASY, NORMAL, HARD, VISIONARY, LUNAR), `elements` (3),
  `stars`, `arcanum`, `bestAct`, `mvp`.
- **Carnage** : `patch`, `period`, `difficulty` (1 à 6), `bosses` : `[{ name, seconds, team }]`.

## Import / export Excel

Page Genshin, boutons en haut à droite. Le classeur contient deux feuilles :

- `Personnages` : Nom | Élément | Niveau | Doublons | Arme | Raffinement
- `Equipes` : Équipe | Usage | Emplacement | Personnage | Élément | Niveau | Doublons | Arme | Raffinement

À l'import, vous choisissez **Fusionner** (mise à jour par nom) ou **Remplacer**.

## Notes

- Les portraits sont des pastilles colorées par élément avec les initiales du personnage (pas d'images du jeu).
- Pour `java -jar`, il faudra un jar « fat » (plugin shade) avec une classe de lancement non-`Application` ; `mvn javafx:run` suffit en développement.
