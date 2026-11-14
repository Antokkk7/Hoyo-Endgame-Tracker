# Génère les trois fichiers de bonus utilisés par le calcul de statistiques en combat du site :
#   assets/donnees/genshin_buffs_persos.json   bonus des personnages (sur le terrain et hors terrain)
#   assets/donnees/genshin_sets.json           sets d'artéfacts et leurs bonus (2 et 4 pièces, cumul)
#   assets/donnees/genshin_buffs_armes.json    armes à bonus d'équipe (R1 à R5)
# Usage : python scripts/generer_buffs.py --enka <dossier store/gi d'EnkaNetwork/API-docs>
import argparse
import json
import pathlib
import sys

ici = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(ici))
from buffs_armes import ARMES  # noqa: E402
from buffs_persos import PERSOS  # noqa: E402
from buffs_sets import SETS  # noqa: E402

RACINE = ici.parent
DONNEES = RACINE / "assets" / "donnees"

RESONANCES = {
    "PYRO": [{"s": "atk_pct", "v": 25, "c": "Résonance Pyro (2 personnages Pyro)"}],
    "CRYO": [{"s": "cr", "v": 15, "c": "Résonance Cryo (2 personnages Cryo) : ennemis gelés ou affectés par le Cryo"}],
    "HYDRO": [{"s": "hp_pct", "v": 25, "c": "Résonance Hydro (2 personnages Hydro)"}],
    "GEO": [{"s": "dmg", "v": 15, "c": "Résonance Géo (2 personnages Géo) : bouclier actif"}, {"s": "res_GEO", "v": 20, "c": "Résonance Géo : ennemis touchés"}],
    "DENDRO": [{"s": "em", "v": 50, "c": "Résonance Dendro (2 personnages Dendro)"}, {"s": "em", "v": 30, "c": "Résonance Dendro : après Embrasement, Floraison ou Propagation", "d": True},
               {"s": "em", "v": 20, "c": "Résonance Dendro : après Hyperfloraison, Brasier ou Prolifération", "d": True}],
    "ELECTRO": [{"s": "autre", "c": "Résonance Électro : énergie sur réaction Électro"}],
    "ANEMO": [{"s": "autre", "c": "Résonance Anémo : endurance et temps de recharge réduits"}],
}


def charger(chemin):
    with open(chemin, encoding="utf-8") as f:
        return json.load(f)


def principal():
    ap = argparse.ArgumentParser()
    ap.add_argument("--enka", required=True)
    args = ap.parse_args()
    enka = pathlib.Path(args.enka)
    relics = charger(enka / "relics.json")
    locs = charger(enka / "locs.json")
    ref = charger(DONNEES / "enka_genshin.json")

    sets = []
    for ident, en, deux, quatre in SETS:
        infos = relics["Sets"].get(str(ident))
        if not infos:
            print("Set absent des données Enka :", en)
            continue
        h = infos["Name"]
        nom_en = locs["en"].get(h, en)
        sets.append({"id": ident, "en": nom_en, "fr": locs["fr"].get(h, nom_en), "hash": h, "deux": deux, "quatre": quatre,
                     "stats2": infos.get("AddProps", {})})
    connus = {s[0] for s in SETS}
    manquants = [(k, locs["en"].get(v["Name"])) for k, v in relics["Sets"].items() if int(k) not in connus]
    if manquants:
        print("Sets sans données :", manquants)
    (DONNEES / "genshin_sets.json").write_text(json.dumps({"version": 1, "sets": sets}, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    noms_persos = {p["en"] for p in ref["persos"].values()}
    persos = {}
    for nom, d in PERSOS.items():
        if nom not in noms_persos:
            print("Personnage inconnu :", nom)
            continue
        persos[nom] = d
    sans = sorted(noms_persos - set(persos) - {"Traveler", "Aloy"} - {n for n in noms_persos if n.startswith("Manekin")})
    print("Personnages sans bonus renseignés :", len(sans))
    (DONNEES / "genshin_buffs_persos.json").write_text(json.dumps({
        "version": 1,
        "regle": "Bonus variables à 70 % du maximum ; talents niveau 10 ; valeurs typiques pour les bonus dépendant d'une statistique.",
        "resonances": RESONANCES, "persos": persos, "sans_bonus": sans,
    }, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

    noms_armes = {a["en"] for a in ref["armes"].values()}
    armes = {}
    for nom, effets in ARMES.items():
        if nom not in noms_armes:
            print("Arme inconnue :", nom)
            continue
        armes[nom] = effets
    (DONNEES / "genshin_buffs_armes.json").write_text(json.dumps({"version": 1, "armes": armes}, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    print(len(sets), "sets,", len(persos), "personnages,", len(armes), "armes")


if __name__ == "__main__":
    principal()
