#!/usr/bin/env python3
"""Génère assets/donnees/enka_icones.json : correspondance nom -> icône d'Enka Network pour Genshin Impact et Honkai: Star Rail.

Récupérez le dépôt https://github.com/EnkaNetwork/API-docs puis, depuis la racine du repo :

    python websiteHET/scripts/generer_icones.py --enka chemin/vers/API-docs/store

Les images ne sont plus stockées dans le site : elles sont chargées depuis https://enka.network/ui/ (voir js/icones.js).
Le fichier contient, par jeu :
  armes    nom de l'arme ou du cône (normalisé) -> icône
  sets     nom du set (normalisé) -> icône de la fleur (Genshin) ou de la tête / sphère planaire (Star Rail)
  persos   nom du personnage (normalisé) -> icône
  liste    personnages du catalogue du site [nom affiché, icône], pour le sélecteur d'image de profil

Les noms personnalisés du catalogue (ex. « Yumeglorp Glorp ») restent ceux du site : ALIAS_PERSOS les relie au personnage d'Enka.
Les Traveler de Genshin gardent vos images locales (assets/personnages/genshin/Traveler (Élément).png).
"""
import argparse
import json
import re
from pathlib import Path

SITE = Path(__file__).resolve().parent.parent

# Noms du site qui diffèrent de ceux d'Enka (nom normalisé du site -> nom anglais d'Enka).
ALIAS_PERSOS_GENSHIN = {
    "ayaka": "Kamisato Ayaka", "ayato": "Kamisato Ayato", "childe": "Tartaglia", "heizou": "Shikanoin Heizou",
    "itto": "Arataki Itto", "kazuha": "Kaedehara Kazuha", "kokomi": "Sangonomiya Kokomi", "raiden": "Raiden Shogun",
    "sara": "Kujou Sara", "yumeglorpglorp": "Yumemizuki Mizuki",
}
# Star Rail : plusieurs personnages portent le même nom dans Enka, on les relie par identifiant.
ID_PERSOS_STARRAIL = {
    "march7th": "1001", "march7ththehunt": "1224",
    "trailblazerdestruction": "8001", "trailblazerpreservation": "8003", "trailblazerharmony": "8005",
    "trailblazerremembrance": "8007", "trailblazerelation": "8009",
}
ALIAS_ARMES_GENSHIN = {"prizedisshinblade": "Kagotsurube Isshin"}
# Personnages gardant leurs images locales : les Traveler de Genshin.
LOCAUX_GENSHIN = re.compile(r"^traveler")


def cle(nom):
    return re.sub(r"[^a-z0-9]", "", str(nom or "").lower())


def lire(chemin):
    return json.loads(Path(chemin).read_text(encoding="utf-8"))


def catalogue_sql():
    texte = (SITE / "sql" / "03_donnees_reference.sql").read_text(encoding="utf-8")
    sortie = {"genshin": [], "starrail": []}
    for m in re.finditer(r"into public\.personnage_catalogue \(jeu, nom[^)]*\) values \('(\w+)', '((?:[^']|'')*)'", texte):
        sortie[m.group(1)].append(m.group(2).replace("''", "'"))
    return sortie


def genshin(src, noms_catalogue):
    locs = lire(src / "gi" / "locs.json")
    en, fr = locs["en"], locs["fr"]
    avatars = lire(src / "gi" / "avatars.json")
    persos, par_nom = {}, {}
    for k, a in avatars.items():
        if "-" in k or "PlayerBoy" in a["SideIconName"] or "PlayerGirl" in a["SideIconName"]:
            continue
        jeton = a["SideIconName"].rsplit("/", 1)[-1].removesuffix(".png").removeprefix("UI_AvatarIcon_Side_")
        h = str(a["NameTextMapHash"])
        for nom in (en.get(h), fr.get(h)):
            if nom:
                persos[cle(nom)] = jeton
        par_nom[cle(en.get(h))] = jeton
    for court, long in ALIAS_PERSOS_GENSHIN.items():
        persos[court] = par_nom[cle(long)]

    armes = {}
    for w in lire(src / "gi" / "weapons.json").values():
        h = str(w["NameTextMapHash"])
        icone = w.get("Icon", "").rsplit("/", 1)[-1].removesuffix(".png")
        for nom in (en.get(h), fr.get(h)):
            if nom and icone:
                armes[cle(nom)] = icone
    for court, long in ALIAS_ARMES_GENSHIN.items():
        armes[court] = armes[cle(long)]

    reliques = lire(src / "gi" / "relics.json")
    fleurs = {}
    for it in reliques["Items"].values():
        if it.get("EquipType") == 0 and it.get("SetId"):
            fleurs[str(it["SetId"])] = it["Icon"].rsplit("/", 1)[-1].removesuffix(".png")
    sets = {}
    for sid, s in reliques["Sets"].items():
        h = str(s["Name"])
        for nom in (en.get(h), fr.get(h)):
            if nom and sid in fleurs:
                sets[cle(nom)] = fleurs[sid]

    liste = []
    for nom in noms_catalogue:
        if LOCAUX_GENSHIN.match(cle(nom)):
            continue
        if cle(nom) in persos:
            liste.append([nom, persos[cle(nom)]])
    return {"persos": persos, "armes": armes, "sets": sets, "liste": liste}


def starrail(src, noms_catalogue):
    en = lire(src / "hsr" / "hsr.json")["en"]
    fr = lire(src / "hsr" / "hsr.json")["fr"]
    avatars = lire(src / "hsr" / "avatars.json")
    persos = {}
    for k, a in avatars.items():
        h = str(a["AvatarName"]["Hash"])
        for nom in (en.get(h), fr.get(h)):
            if nom and cle(nom) not in persos:
                persos[cle(nom)] = k
    persos.update(ID_PERSOS_STARRAIL)

    armes = {}
    for k, w in lire(src / "hsr" / "weapons.json").items():
        h = str(w["EquipmentName"]["Hash"])
        for nom in (en.get(h), fr.get(h)):
            if nom:
                armes[cle(nom)] = k

    reliques = lire(src / "hsr" / "relics.json")
    icones = {}
    for it in reliques["Items"].values():
        # Une pièce par set : la tête pour les sets d'artéfacts, la sphère planaire pour les ornements.
        if it.get("Type") in ("HEAD", "OBJECT"):
            icones[str(it["SetID"])] = it["Icon"].lstrip("/").removeprefix("ui/")
    sets = {}
    for sid, s in reliques["Sets"].items():
        h = str(s["Name"])
        for nom in (en.get(h), fr.get(h)):
            if nom and sid in icones:
                sets[cle(nom)] = icones[sid]

    liste = [[nom, persos[cle(nom)]] for nom in noms_catalogue if cle(nom) in persos]
    return {"persos": persos, "armes": armes, "sets": sets, "liste": liste}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--enka", required=True, help="dossier store du dépôt EnkaNetwork/API-docs")
    args = parser.parse_args()
    src = Path(args.enka)
    catalogue = catalogue_sql()
    sortie = {"version": 1, "base": "https://enka.network/ui/", "genshin": genshin(src, catalogue["genshin"]), "starrail": starrail(src, catalogue["starrail"])}

    armes = lire(SITE / "assets" / "donnees" / "armes.json")
    for jeu in ("genshin", "starrail"):
        manquantes = [a["nom"] for a in armes[jeu] if cle(a["nom"]) not in sortie[jeu]["armes"]]
        sans_perso = [n for n in catalogue[jeu] if cle(n) not in sortie[jeu]["persos"] and not (jeu == "genshin" and LOCAUX_GENSHIN.match(cle(n)))]
        print(jeu, ":", len(sortie[jeu]["persos"]), "noms de personnages,", len(sortie[jeu]["armes"]), "armes,", len(sortie[jeu]["sets"]), "sets ;",
              "armes sans icône :", manquantes, "; personnages sans icône :", sans_perso)

    chemin = SITE / "assets" / "donnees" / "enka_icones.json"
    chemin.write_text(json.dumps(sortie, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(chemin.name, chemin.stat().st_size // 1024, "Ko")


if __name__ == "__main__":
    main()
