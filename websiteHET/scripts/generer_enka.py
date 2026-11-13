#!/usr/bin/env python3
"""Génère assets/donnees/enka_genshin.json à partir des données de référence d'Enka Network.

Récupérez le dépôt https://github.com/EnkaNetwork/API-docs puis, depuis la racine du repo :

    python websiteHET/scripts/generer_enka.py --enka chemin/vers/API-docs/store/gi

Le fichier contient les personnages (nom, élément, arme, icônes), les armes (nom, rareté, type, icône), les noms
français des équipements et des statistiques, et la table des améliorations d'artefacts. Le site l'utilise pour
traduire la réponse de l'API Enka et pour le planificateur.
"""
import argparse
import json
from pathlib import Path

SITE = Path(__file__).resolve().parent.parent

ELEMENTS = {"Fire": "PYRO", "Water": "HYDRO", "Electric": "ELECTRO", "Wind": "ANEMO", "Ice": "CRYO", "Rock": "GEO", "Grass": "DENDRO"}
TYPES_ARME = {"WEAPON_SWORD_ONE_HAND": "sword", "WEAPON_CLAYMORE": "claymore", "WEAPON_POLE": "polearm", "WEAPON_BOW": "bow", "WEAPON_CATALYST": "catalyst"}
# Armes connues, pour retrouver la numérotation des types d'armes de weapons.json.
REPERES = {"Aquila Favonia": "sword", "Wolf's Gravestone": "claymore", "Skyward Spine": "polearm", "Amos' Bow": "bow", "Skyward Atlas": "catalyst"}


def icone(chemin):
    return chemin.rsplit("/", 1)[-1].removesuffix(".png") if chemin else None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--enka", required=True, help="dossier store/gi du dépôt EnkaNetwork/API-docs")
    args = parser.parse_args()
    src = Path(args.enka)
    avatars = json.loads((src / "avatars.json").read_text(encoding="utf-8"))
    weapons = json.loads((src / "weapons.json").read_text(encoding="utf-8"))
    relics = json.loads((src / "relics.json").read_text(encoding="utf-8"))
    locs = json.loads((src / "locs.json").read_text(encoding="utf-8"))
    affixes = json.loads((src / "affixes.json").read_text(encoding="utf-8"))
    fr, en = locs["fr"], locs["en"]

    persos = {}
    for cle, a in avatars.items():
        h = str(a["NameTextMapHash"])
        side = icone(a.get("SideIconName"))
        persos[cle] = {
            "nom": fr.get(h) or en.get(h) or cle, "en": en.get(h) or cle,
            "element": ELEMENTS.get(a.get("Element")), "rarete": 5 if "ORANGE" in a.get("QualityType", "") else 4,
            "arme": TYPES_ARME.get(a.get("WeaponType")),
            "icone": side, "portrait": side.replace("UI_AvatarIcon_Side_", "UI_Gacha_AvatarImg_") if side else None,
            "talents": a.get("SkillOrder", []), "proud": a.get("ProudMap", {}),
        }

    numero_type = {}
    for w in weapons.values():
        nom_en = en.get(str(w["NameTextMapHash"]))
        if nom_en in REPERES:
            numero_type[w["WeaponType"]] = REPERES[nom_en]
    if len(numero_type) != 5:
        raise SystemExit(f"Types d'armes non identifiés : {numero_type}")
    armes = {}
    for cle, w in weapons.items():
        h = str(w["NameTextMapHash"])
        armes[cle] = {"nom": fr.get(h) or en.get(h), "en": en.get(h), "rarete": w["Rarity"], "type": numero_type[w["WeaponType"]], "icone": icone(w.get("Icon"))}

    noms = {}
    for hash_nom in [str(a["NameTextMapHash"]) for a in avatars.values()] + [str(w["NameTextMapHash"]) for w in weapons.values()] \
            + [s["Name"] for s in relics["Sets"].values()]:
        if hash_nom in fr:
            noms[hash_nom] = fr[hash_nom]
    libelles = {k: v for k, v in fr.items() if k.startswith("FIGHT_PROP_")}
    ameliorations = {cle: a["PropType"] for cle, a in affixes.items()}

    sortie = {"persos": persos, "armes": armes, "noms": noms, "libelles": libelles, "ameliorations": ameliorations}
    chemin = SITE / "assets" / "donnees" / "enka_genshin.json"
    chemin.parent.mkdir(parents=True, exist_ok=True)
    chemin.write_text(json.dumps(sortie, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(len(persos), "personnages,", len(armes), "armes,", len(noms), "noms ;", chemin.stat().st_size // 1024, "Ko")


if __name__ == "__main__":
    main()
