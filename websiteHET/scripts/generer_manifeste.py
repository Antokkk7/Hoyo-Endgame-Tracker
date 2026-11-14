#!/usr/bin/env python3
"""Génère assets/personnages/manifeste.json : les images locales de personnages qui remplacent celles d'Enka Network.

Les portraits viennent d'Enka (scripts/generer_icones.py). Ce dossier ne garde que ce qu'Enka ne fournit pas :
les Traveler de Genshin (Traveler (Pyro).png, etc.). Une image locale est toujours prioritaire sur celle d'Enka :
copiez-la dans websiteHET/assets/personnages/genshin/ ou starrail/ (le nom du fichier est le nom du personnage),
puis lancez, depuis la racine du repo :

    python websiteHET/scripts/generer_manifeste.py

La correspondance ignore la casse, les espaces et la ponctuation.
"""
import json
import re
from pathlib import Path

RACINE = Path(__file__).resolve().parent.parent / "assets" / "personnages"
EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp"}


def cle(nom):
    return re.sub(r"[^a-z0-9]", "", nom.lower())


def main():
    manifeste = {}
    for jeu in ("genshin", "starrail"):
        dossier = RACINE / jeu
        fichiers = {}
        if dossier.is_dir():
            for f in sorted(dossier.iterdir()):
                if f.suffix.lower() in EXTENSIONS:
                    fichiers[cle(f.stem)] = f.name
        manifeste[jeu] = fichiers
    (RACINE / "manifeste.json").write_text(json.dumps(manifeste, ensure_ascii=False, indent=1), encoding="utf-8")
    print({jeu: len(f) for jeu, f in manifeste.items()}, "portraits référencés")


if __name__ == "__main__":
    main()
