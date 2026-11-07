#!/usr/bin/env python3
"""Convertit en VRAI PNG toutes les images d'un dossier dont le contenu n'est pas du PNG/JPG/GIF/BMP
(typiquement des WebP simplement renommés en .png, que JavaFX ne sait pas lire).

Usage (depuis la racine du projet) :
    pip install pillow
    python tools/convert_to_png.py data/genshin/armesIMG data/genshin/personnagesIMG data/genshin/bossIMG
Les fichiers sont réécrits sous le même nom (extension .png).
"""
import os
import sys

from PIL import Image

OK = {"PNG", "JPEG", "GIF", "BMP"}

for folder in sys.argv[1:] or ["data/genshin/armesIMG"]:
    for name in sorted(os.listdir(folder)):
        path = os.path.join(folder, name)
        if not os.path.isfile(path):
            continue
        try:
            with Image.open(path) as im:
                fmt = im.format
                im.load()
                img = im.convert("RGBA")
        except Exception:
            continue
        ext = os.path.splitext(name)[1].lower()
        if fmt in OK and ext in (".png", ".jpg", ".jpeg", ".gif", ".bmp"):
            continue
        out = os.path.splitext(path)[0] + ".png"
        img.save(out, format="PNG", optimize=True)
        if out != path:
            os.remove(path)
        print(f"converti : {path} ({fmt}) -> {out}")
