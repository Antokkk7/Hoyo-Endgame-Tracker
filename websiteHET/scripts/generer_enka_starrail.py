#!/usr/bin/env python3
"""Génère assets/donnees/enka_starrail.json : données de référence de Honkai: Star Rail pour le suivi par UID (Enka Network).

Depuis la racine du repo, avec le dépôt https://github.com/EnkaNetwork/API-docs téléchargé :

    python websiteHET/scripts/generer_enka_starrail.py --enka chemin/vers/API-docs/store

Le fichier contient, en français : les personnages (nom, élément, voie, rareté), les cônes de lumière, les sets de reliques et leurs bonus,
les reliques (identifiant -> set, emplacement, icône), les tables des statistiques principales et secondaires, les statistiques de base
des personnages par palier d'ascension et les bonus des traces. Il permet de calculer les statistiques sans appeler d'autre service.
"""
import argparse
import json
import re
from pathlib import Path

SITE = Path(__file__).resolve().parent.parent


def lire(chemin):
    return json.loads(Path(chemin).read_text(encoding="utf-8"))


def texte(table, h):
    t = table.get(str(h), "") or ""
    # Le pionnier : « {M#Le pionnier}{F#La pionnière} » -> version masculine.
    m = re.match(r"^\{M#([^}]*)\}", t)
    return (m.group(1) if m else t).replace(" ", " ")


def arrondi(v, n=6):
    return round(float(v), n)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--enka", required=True)
    src = Path(parser.parse_args().enka) / "hsr"
    loc = lire(src / "hsr.json")
    fr, en = loc["fr"], loc["en"]
    meta = lire(src / "honker_meta.json")["avatar"]

    persos = {}
    base = {}
    for k, a in lire(src / "avatars.json").items():
        h = a["AvatarName"]["Hash"]
        persos[k] = {"nom": texte(fr, h), "en": texte(en, h), "element": a["Element"], "voie": a["AvatarBaseType"], "rarete": a["Rarity"]}
        base[k] = {p: [arrondi(v[c]) for c in ("HPBase", "HPAdd", "AttackBase", "AttackAdd", "DefenceBase", "DefenceAdd", "SpeedBase", "CriticalChance", "CriticalDamage")]
                   for p, v in meta.get(k, {}).items()}

    cones = {}
    for k, w in lire(src / "weapons.json").items():
        h = w["EquipmentName"]["Hash"]
        promo = {p: [arrondi(v["BaseHP"]), arrondi(v["BaseHPAdd"]), arrondi(v["BaseAttack"]), arrondi(v["BaseAttackAdd"]), arrondi(v["BaseDefence"]), arrondi(v["BaseDefenceAdd"])]
                 for p, v in w["Promotion"].items()}
        cones[k] = {"nom": texte(fr, h), "en": texte(en, h), "rarete": w["Rarity"], "voie": w["AvatarBaseType"], "base": promo}

    reliques = lire(src / "relics.json")
    sets = {}
    for sid, s in reliques["Sets"].items():
        sets[sid] = {"nom": texte(fr, s["Name"]), "en": texte(en, s["Name"]),
                     "bonus": {n: d["props"] for n, d in s["SetSkills"].items() if d.get("props")}}
    items = {tid: [str(i["SetID"]), i["Type"], i["Rarity"], i["Icon"].lstrip("/").removeprefix("ui/"), i["MainAffixGroup"], i["SubAffixGroup"]]
             for tid, i in reliques["Items"].items()}

    affixes = lire(src / "affixes.json")
    principales = {g: {a: [v["Property"], arrondi(v["BaseValue"]), arrondi(v["LevelAdd"])] for a, v in d.items()} for g, d in affixes["MainAffix"].items()}
    secondaires = {g: {a: [v["Property"], arrondi(v["BaseValue"]), arrondi(v["StepValue"])] for a, v in d.items()} for g, d in affixes["SubAffix"].items()}

    arbre = {}
    for pid, niveaux in lire(src / "tree.json").items():
        garde = {n: d["props"] for n, d in niveaux.items() if d.get("props")}
        if garde:
            arbre[pid] = garde

    sortie = {"version": 1, "persos": persos, "base": base, "cones": cones, "sets": sets, "reliques": items,
              "principales": principales, "secondaires": secondaires, "arbre": arbre}
    chemin = SITE / "assets" / "donnees" / "enka_starrail.json"
    chemin.write_text(json.dumps(sortie, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(chemin.name, chemin.stat().st_size // 1024, "Ko", len(persos), "personnages", len(cones), "cônes", len(sets), "sets")


if __name__ == "__main__":
    main()
