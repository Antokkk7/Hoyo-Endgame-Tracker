#!/usr/bin/env python3
"""Génère les données statiques du site à partir du dossier data/ de l'application de bureau.

Depuis la racine du repo :

    python websiteHET/scripts/generer_donnees.py

Produit :
  assets/donnees/armes.json   armes Genshin et cônes Star Rail (nom, rareté, image)
  assets/donnees/boss.json    boss et période du Carnage chtonien par patch
et copie les images utiles (armes, cônes, boss, difficultés) dans assets/.
"""
import argparse
import json
import re
import shutil
from pathlib import Path

SITE = Path(__file__).resolve().parent.parent


def cle(nom):
    return re.sub(r"[^a-z0-9]", "", nom.lower())


def indexer(source):
    """Fichiers images d'un dossier, indexés par nom normalisé."""
    return {cle(f.stem): f for f in sorted(source.glob("*")) if f.suffix.lower() in {".png", ".jpg", ".jpeg", ".webp"}}


def copier(fichiers, cible):
    cible.mkdir(parents=True, exist_ok=True)
    for f in fichiers:
        shutil.copy2(f, cible / f.name)


def armes_genshin(data, images):
    brut = json.loads((data / "genshin" / "genshinWeapons.json").read_text(encoding="utf-8"))["weapons"]
    sortie = []
    for groupe, noms in brut.items():
        rarete = int(groupe.split("_")[0])
        for nom in noms:
            sortie.append({"nom": nom, "rarete": rarete, "image": images[cle(nom)].name if cle(nom) in images else None})
    return sorted(sortie, key=lambda a: (-a["rarete"], a["nom"]))


def cones_starrail(data, images):
    brut = json.loads((data / "starrail" / "honkaiStarRailCones.json").read_text(encoding="utf-8"))
    sortie = [{"nom": c["nom"], "rarete": c.get("rarete", 0), "image": images[cle(c["nom"])].name if cle(c["nom"]) in images else None}
              for c in brut.get("cones_de_lumiere", [])]
    return sorted(sortie, key=lambda a: (-a["rarete"], a["nom"]))


def boss(data):
    brut = json.loads((data / "genshin" / "genshinBoss.json").read_text(encoding="utf-8"))
    brut = brut if isinstance(brut, list) else next((brut[k] for k in ("patches", "bosses", "data") if k in brut), [])
    sortie = {}
    for n in brut:
        noms = n.get("boss") or n.get("bosses") or []
        periode = ""
        if n.get("debut") and n.get("fin"):
            periode = n["debut"].replace("-", ".") + " - " + n["fin"].replace("-", ".")
        sortie[n["version"]] = {"periode": periode, "boss": noms}
    return sortie


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--data", default=str(SITE.parent / "data"), help="dossier data/ de l'application")
    args = parser.parse_args()
    data = Path(args.data)

    # Les images de cônes Star Rail du dépôt portent un identifiant numérique : seules celles qui portent
    # le nom du cône sont utilisées.
    img_g = indexer(data / "genshin" / "armesIMG")
    img_s = indexer(data / "starrail" / "conesIMG")
    armes = {"genshin": armes_genshin(data, img_g), "starrail": cones_starrail(data, img_s)}
    for jeu, images in (("genshin", img_g), ("starrail", img_s)):
        utilises = {a["image"] for a in armes[jeu] if a["image"]}
        copier([f for f in images.values() if f.name in utilises], SITE / "assets" / "armes" / jeu)
    copier(indexer(data / "genshin" / "bossIMG").values(), SITE / "assets" / "img" / "boss")
    copier([f for f in (data / "genshin" / "assetsOthers").glob("diff[1-6].png")], SITE / "assets" / "img" / "difficulte")

    dossier = SITE / "assets" / "donnees"
    dossier.mkdir(parents=True, exist_ok=True)
    (dossier / "armes.json").write_text(json.dumps(armes, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (dossier / "boss.json").write_text(json.dumps(boss(data), ensure_ascii=False, indent=1), encoding="utf-8")
    sans_image = [a["nom"] for j in armes.values() for a in j if not a["image"]]
    print({j: len(v) for j, v in armes.items()}, "armes ; sans image :", len(sans_image))


if __name__ == "__main__":
    main()
