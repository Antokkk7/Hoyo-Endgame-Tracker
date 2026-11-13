#!/usr/bin/env python3
"""Génère assets/personnages/manifeste.json : la liste des portraits disponibles pour le site.

Copiez vos images dans websiteHET/assets/personnages/genshin/ et websiteHET/assets/personnages/starrail/
(par exemple depuis data/genshin/personnagesIMG et data/starrail/personnagesIMG), puis lancez, depuis la racine du repo :

    python websiteHET/scripts/generer_manifeste.py

Le nom du fichier doit être le nom du personnage (Mavuika.png). La correspondance ignore la casse,
les espaces et la ponctuation.
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
