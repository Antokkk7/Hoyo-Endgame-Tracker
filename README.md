# Hoyo Endgame Tracker

Application de bureau (Java 21 + JavaFX) pour suivre ses endgames :

- **Genshin Impact** : Abysses spiralées, Théâtre de l'imaginarium, Carnage chtonien ;
- **Honkai: Star Rail** : Salle oubliée (MOC), Pure fiction (PF), Fantômes de l'apocalypse (APOC) et Arbitre de l'instable
  (mis en avant).

## Lancer

Prérequis : JDK 21+ et Maven.

```bash
mvn javafx:run
```

> Lancez la commande **depuis la racine du projet** : les dossiers `data/genshin/` et `data/starrail/` sont lus et écrits à côté de `src/`.
> Pour utiliser un autre dossier, ajoutez `-Dhoyo.data.dir=C:\chemin\data\genshin` (Genshin) ou
> `-Dhoyo.starrail.dir=C:\chemin\data\starrail` (Star Rail) aux options de la JVM
> (dans IntelliJ : ouvrir le `pom.xml`, puis lancer `com.hoyoendgame.App` avec la racine du projet comme répertoire de travail).

## Structure

```
data/genshin/                  <- vos données (même niveau que src/)
  genshinCharacters.json       personnages (nom + élément)
  genshinWeapons.json          catalogue d'armes (lecture seule)
  genshinBoss.json             boss et dates de chaque patch du Carnage (lecture seule)
  genshinEquipes.json          équipes enregistrées
  genshinAbysses.json          historique des Abysses spiralées
  genshinTheatre.json          historique du Théâtre de l'imaginarium
  genshinCarnage.json          historique du Carnage chtonien
  personnagesIMG/              images des personnages
  bossIMG/                     images des boss
  armesIMG/                    images des armes
  assetsOthers/                diff1.png ... diff6.png (badges de difficulté)
data/starrail/                 <- données Honkai: Star Rail
  honkaiStarRailCharacters.json  personnages (nom, rareté, élément, voie) - lecture seule
  honkaiStarRailCones.json       catalogue de cônes (lecture seule, à remplir par vous)
  honkaiStarRailEquipes.json     équipes enregistrées
  honkaiStarRailMoc.json         historique Salle oubliée
  honkaiStarRailPf.json          historique Pure fiction
  honkaiStarRailApoc.json        historique Fantômes de l'apocalypse
  honkaiStarRailArbitration.json historique Arbitre de l'instable
  personnagesIMG/  conesIMG/     images (mêmes règles que Genshin)
src/main/java/com/hoyoendgame/
  App.java                     fenêtre principale + navigation (tout reste dans la même fenêtre)
  model/                       classes de données (sérialisées en JSON)
  data/                        JsonStore (écriture atomique), DataStore, BossCatalog, ExcelService (Apache POI)
  ui/                          vues, formulaires et chargement des images
src/main/resources/
  style.css                    thème néon commun (couleurs, boutons, champs, panneaux)
  home.css                     accueil uniquement
  genshin.css                  Genshin uniquement (étages, difficulté, boss, modes du Théâtre)
  starrail.css                 Honkai: Star Rail uniquement (carte Arbitre mise en avant)
  fonts/                       Orbitron + Space Grotesk (licence SIL OFL, textes inclus)
```

## Sauvegarde des données

- Chaque modification (ajout, édition, suppression, import Excel) réécrit immédiatement le fichier JSON concerné,
  et `App.stop()` sauvegarde une dernière fois à la fermeture : rien n'est perdu au redémarrage.
- L'écriture est atomique (fichier `.tmp` puis renommage). Si un fichier JSON est illisible, il est conservé en `.bak`
  et l'application repart avec une liste vide pour ce fichier.
- Les fichiers d'historique manquants sont recréés au démarrage. `genshinWeapons.json` et `genshinBoss.json`
  sont maintenus à la main : l'application les lit mais ne les réécrit jamais.

## Formats JSON

Les fichiers de données ont la forme `{ "version": 1, "<clé>": [ ... ] }` (un tableau JSON nu est aussi accepté en lecture).
Les champs inconnus sont ignorés.

### `genshinCharacters.json` (clé `characters`)

```json
{ "id": "c-furina", "name": "Furina", "element": "HYDRO" }
```

Uniquement l'identité et l'élément : niveau, doublons, arme et raffinement se règlent **par équipe**, car un même
personnage change selon la situation. `element` : PYRO, HYDRO, ANEMO, ELECTRO, DENDRO, CRYO, GEO
(les noms français sont acceptés aussi).

### `genshinWeapons.json`

```json
{ "weapons": { "5_star": ["Aquila Favonia", "..."], "4_star": ["..."], "3_star": ["..."] } }
```

Catalogue utilisé par le sélecteur d'arme (avec recherche). Ajoutez-y les nouvelles armes à la main à chaque patch.

### `genshinBoss.json`

```json
[
  { "version": "7.1", "debut": "2026-09-30", "fin": "2026-11-11", "boss": ["Churin", "Automaton", "Pavlina"] }
]
```

Pour chaque patch : les 3 boss du Carnage et les dates de la rotation. Dans le formulaire d'ajout d'un clear de Carnage,
dès que le patch est saisi, les 3 noms de boss sont pré-remplis et la période (`2026.09.30 - 2026.11.11`)
est calculée automatiquement. Elle n'est pas modifiable à la main. Pour un nouveau patch, ajoutez simplement une entrée.

### `genshinEquipes.json` (clé `teams`)

```json
{
  "id": "t-1", "name": "Hydro Furina", "usage": "Abysses · seconde moitié",
  "members": [
    { "characterId": "c-furina", "name": "Furina", "element": "HYDRO",
      "level": 90, "constellation": 2, "weapon": "Splendor of Tranquil Waters", "refinement": 1 }
  ]
}
```

Chaque membre est un instantané autonome (niveau, doublons, nom de l'arme tel qu'écrit dans `genshinWeapons.json`,
raffinement) ; `characterId` est optionnel.

### Endgames (clé `clears`)

- **Abysses** : `patch`, `rotation`, `floor9`..`floor12` (étoiles), `team1`, `team2` (listes de membres).
- **Théâtre** : `year`, `month`, `patch`, `mode` (EASY, NORMAL, HARD, VISIONARY, LUNAR), `elements` (3),
  `starFlags` (une valeur par étoile : obtenue ou non), `arcanum`, `mvp`, `mvpTrial` (personnage d'introduction / de prêt).
- **Carnage** : `patch`, `period`, `difficulty` (1 à 6), `bosses` : `[{ name, seconds, team }]`.

Tri des historiques : le Théâtre du mois le plus récent au plus ancien, le Carnage du patch le plus récent au plus ancien
(et non par date d'ajout).

## Honkai: Star Rail

Vocabulaire : doublons = **E0 à E6** (éidolons), arme = **Cône**, raffinement = **Superposition** (S1 à S5), niveau max **80**,
4 personnages par équipe. `Trailblazer (Destruction)` s'affiche `Trailblazer`.

- **Salle oubliée** : 12 salles (étoiles + cycles **utilisés**), total /36 et cycles totaux. Le mode stellaire de la salle 12 donne l'étoile
  violette et une 3e équipe.
- **Pure fiction** : score de chacune des 4 salles (80 000 max, 120 000 pour la salle 4 en mode stellaire), score total, équipes de la dernière salle.
- **Fantômes de l'apocalypse** : au choix Difficulté 3 (2 boss), Difficulté 4 (2 boss) ou Difficulté 4 stellaire (3 boss) ; score de
  chaque boss (4 000 max), total /8 000 ou /12 000, une équipe par boss.
- **Arbitre de l'instable** : cycles utilisés contre chacun des 3 Cavaliers (0-2 : 3 étoiles, 3-4 : 2, 5-6 : 1, plus : 0), case « K.O. »
  (-1 étoile, jamais sous 0) ; Roi : cycles et étoiles (0 à 3), cadre calculé : 1 Bronze, 2 Argent, 3 Dorée, 3 en 0 cycle Prismatique.
  Seule l'équipe du Roi est enregistrée.

`honkaiStarRailCharacters.json` s'écrit `{ "personnages": [ { "nom", "rarete", "element", "voie" } ] }` (éléments en français : Physique,
Feu, Glace, Foudre, Vent, Quantique, Imaginaire). Il n'est jamais réécrit. `honkaiStarRailCones.json` a la même forme que
`genshinWeapons.json` (`{ "cones": { "5_star": [...], "4_star": [...], "3_star": [...] } }`) ; tant qu'il est vide, tapez le nom du cône
dans le sélecteur puis « Utiliser le texte saisi ». L'import Excel de la page Star Rail ne fusionne que les équipes.

## Images

Toutes les images sont facultatives : sans image, l'application affiche une pastille de couleur (initiales du personnage,
« ? » pour un boss ou une arme, carré coloré pour la difficulté).

| Dossier | Contenu | Nom du fichier |
|---|---|---|
| `personnagesIMG/` | portrait d'un personnage | nom du personnage : `Furina.png`, `Kamisato Ayaka` ↔ `kamisatoayaka.png` |
| `armesIMG/` | carré à gauche du nom de l'arme dans l'éditeur d'équipe (équipes, Abysses, Carnage) | nom de l'arme comme dans `genshinWeapons.json` : `Skyward Blade.png` |
| `bossIMG/` | image d'un boss, tous patchs confondus | nom du boss tel qu'écrit dans `genshinBoss.json` : `Tulpa.png` |
| `assetsOthers/` | badge de difficulté | `diff1.png` à `diff6.png` |

- La correspondance ignore les majuscules, les espaces et la ponctuation.
- Formats lus : png, jpg, jpeg, gif, bmp. **WebP n'est pas supporté par JavaFX**, même renommé en `.png` (un message
  s'affiche dans la console) : convertissez-le avec `python tools/convert_to_png.py data/genshin/armesIMG` (nécessite `pip install pillow`).
- Les images sont recadrées au centre pour remplir leur cadre.
- Les images sont lues au premier affichage : relancez l'application après avoir ajouté un nouveau dossier.

## Affichage des noms

Les noms sont raccourcis à l'écran uniquement (les données gardent le nom complet) : `Traveler (Anemo)` devient
`Traveler`, `Yumeglorp Glorp` devient `Mizuki`, `Kuki Shinobu` devient `Kuki`, `Wriothesley` devient `Wrio`.
Pour en ajouter, modifier `SHORT_NAMES` dans `ui/Ui.java`. Les noms de boss en CamelCase sont écrits avec des espaces
(`LavaDrake` devient `Lava Drake`).

## Import / export Excel

Page Genshin, boutons en haut à droite. Le classeur contient deux feuilles :

- `Personnages` : Nom | Élément
- `Equipes` : Équipe | Usage | Emplacement | Personnage | Élément | Niveau | Doublons | Arme | Raffinement

À l'import, vous choisissez **Fusionner** (mise à jour par nom) ou **Remplacer**.

## Thème

Le style complet est dans `style.css` (couleurs en variables en haut du fichier : `-neon-cyan`, `-neon-magenta`,
`-neon-green`...). Dans l'éditeur d'équipe, chaque emplacement prend la couleur de l'élément de son personnage.
JavaFX n'ayant pas de `text-transform`, les boutons et pastilles sont mis en majuscules dans `Ui.upper(...)`.
Orbitron ne contient pas les symboles ★ · « », JavaFX les affiche alors avec une police de secours.

## Notes

- Pour `java -jar`, il faudra un jar « fat » (plugin shade) avec une classe de lancement non-`Application` ;
  `mvn javafx:run` suffit en développement.
- Les polices Orbitron et Space Grotesk sont sous licence SIL OFL (textes dans `src/main/resources/fonts/`).
