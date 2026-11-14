#!/usr/bin/env python3
"""Génère assets/donnees/starrail_buffs.json : bonus d'équipe des personnages, cônes de lumière, sets de reliques et ornements de Star Rail.

Source : https://github.com/Mar-7th/StarRailRes (dossier index_min, langues en et fr). Récupérez les fichiers puis lancez :

    python websiteHET/scripts/generer_buffs_starrail.py --res chemin/vers/StarRailRes/index_min

Principe : les descriptions anglaises (avec leurs valeurs chiffrées par niveau, par superposition ou par éidolon) sont analysées par motifs
(« augmente l'ATQ de tous les alliés de X % », « réduit la DÉF de l'ennemi de X % », etc.). Chaque bonus reconnu devient un effet
{ s: statistique, v: valeur, c: texte, o: origine, cible, cm: éidolon minimum, d: 1 si désactivé par défaut }.
Un effet conditionnel (tours, déclencheur, empilements...) est désactivé par défaut : cochez-le dans le suivi quand il est actif.
Ne sont pas reconnus (donc absents) : les dégâts par type d'attaque, les formules qui dépendent des statistiques du soutien
(par exemple « ATQ de Robin × 22,8 % + 200 »), les effets défensifs et les effets sans statistique.
Niveau des compétences : 10 (talents compris) ; traces majeures et éidolons au niveau 1.
"""
import argparse
import json
import re
from pathlib import Path

SITE = Path(__file__).resolve().parent.parent

STATS = [
    ("CRIT Rate", "cr"), ("CRIT DMG", "cd"), ("Break Effect", "brk"), ("Energy Regeneration Rate", "err"), ("Effect Hit Rate", "ehr"),
    ("Effect RES", "eres"), ("Outgoing Healing", "heal"), ("Max HP", "hp"), ("ATK", "atk"), ("DEF", "def"), ("SPD", "spd"),
    ("All-Type RES PEN", "res_pen"),
]
RES_PEN_ELEMENT = re.compile(r"(Physical|Fire|Ice|Lightning|Wind|Quantum|Imaginary) RES PEN")
ELEMENTS = {"Physical": "PHYSICAL", "Fire": "FIRE", "Ice": "ICE", "Lightning": "LIGHTNING", "Wind": "WIND", "Quantum": "QUANTUM", "Imaginary": "IMAGINARY"}
TYPES_DEGATS = r"(Basic ATK|Skill|Ultimate|Follow-Up ATK|Follow-up ATK|Break|DoT|Additional|Elation|Memosprite|Super Break|Lightning-Lord|Counter|Enhanced)"
CONDITION = re.compile(
    r"\b(when|if|after|while|during|upon|whenever|each time|every time|for \d+ turn|for the next|lasting|last|per|stacks?|at the start|once|the more|against|"
    r"enter|until|up to|at most|can be|upto|is defeated|state|countdown)\b", re.I)
NOMS_TYPES = {"Normal": "Attaque normale", "BPSkill": "Compétence", "Ultra": "Ultime", "Talent": "Talent", "Maze": "Technique",
              "MemospriteSkill": "Compétence de mémosprite", "MemospriteTalent": "Talent de mémosprite", "Assist": "Assistance"}


def lire(res, langue, nom):
    return json.loads((Path(res) / langue / f"{nom}.json").read_text(encoding="utf-8"))


def nombre(v):
    v = round(v, 2)
    return int(v) if v == int(v) else v


def resoudre(texte, params):
    def remplace(m):
        k, fmt, pct = int(m.group(1)), m.group(2), m.group(3)
        if k - 1 >= len(params):
            return m.group(0)
        v = params[k - 1] * (100 if pct else 1)
        return f"{nombre(v)}{pct}"
    return re.sub(r"#(\d+)\[(i|f1|f2)\](%?)", remplace, texte)


def phrases(texte):
    return [p.strip() for p in re.split(r"(?<=[.!?])\s+|\n+", texte or "") if p.strip()]


def analyser(phrase):
    """Renvoie [(stat, valeur, cible)] ; cible : soi | equipe | allie | ennemi."""
    resultats = []
    debut = 0
    cible_prec = None
    verbe_prec = None
    debut_phrase = False
    for m in re.finditer(r"\bby (?:up to )?(\d+(?:\.\d+)?)(%?)", phrase):
        segment = phrase[debut:m.start()]
        debut = m.end()
        queue = re.split(r"[,;]|\band\b", phrase[m.end():m.end() + 60])[0]
        if re.match(r"\s*(?:of|plus|times)\b", queue):
            continue
        valeur = float(m.group(1))
        pct = bool(m.group(2))
        verbe = re.search(r"\b(increas|boost|rais|enhanc|reduc|decreas|lower|diminish)\w*", segment, re.I)
        if verbe:
            verbe_prec = verbe.group(1)
        elif verbe_prec and re.match(r"\s*(?:,|and|as well as|;)?\s*(?:the |this unit's |their |its |his |her |[A-Z][\w']+'s )?[A-Za-z' -]{0,40}$", segment) and debut_phrase:
            verbe = True
        if not verbe:
            continue
        negatif = bool(re.match(r"(reduc|decreas|lower|diminish)", verbe_prec, re.I))
        debut_phrase = True
        local = re.split(r"[,;]", segment)[-1]
        if not re.search(r"increas|boost|rais|reduc|decreas|lower|\bDMG\b|ATK|DEF|SPD|CRIT|HP|RES|Effect|Break|Energy", local):
            local = segment
        enn = bool(re.search(r"\benem", local, re.I))
        if enn:
            if re.search(r"DMG taken", segment) and not negatif:
                resultats.append(("vuln", valeur, "ennemi"))
            elif re.search(r"\bDEF\b", segment) and negatif and pct:
                resultats.append(("def_red", valeur, "ennemi"))
            elif re.search(r"RES", segment) and negatif and pct:
                resultats.append(("res_pen", valeur, "ennemi"))
            continue
        if negatif:
            continue
        zone = local + " " + queue
        sans_soi = bool(re.search(r"exclud|except|other than", zone, re.I))
        if re.search(r"all allies|allies'|teammates|all ally|all other allies|all characters", zone, re.I):
            cible = "equipe"
        elif re.search(r"(target|designated|that|an|the|one) ally|ally target|ally's|target's|allied", zone, re.I):
            cible = "allie"
        elif re.search(r"wearer|this unit|herself|himself|self\b", zone, re.I):
            cible = "soi"
        else:
            cible = cible_prec or "soi"
        cible_prec = cible
        original = segment
        propre = re.sub(r"(Enhanced )?(Basic|Follow-Up|Follow-up) ATK", "", segment)
        typee = bool(re.search(r"(Basic ATK|Follow-Up ATK|Follow-up ATK|Additional DMG|DoT|Break DMG|Super Break)", segment))
        segment = propre
        stat = None
        dernier = -1
        for nom, cle in STATS:
            for sm in re.finditer(r"(?<![\w-])" + re.escape(nom) + r"(?!\w)", segment):
                if sm.start() > dernier:
                    dernier, stat = sm.start(), cle
        m_rp = RES_PEN_ELEMENT.search(segment)
        if m_rp and pct:
            resultats.append((f"res_pen_{ELEMENTS[m_rp.group(1)]}", valeur, cible))
            continue
        m_el = re.search(r"(Physical|Fire|Ice|Lightning|Wind|Quantum|Imaginary) DMG", segment)
        if m_el and (m_el.start() > dernier):
            if re.search(TYPES_DEGATS + r"[^,.;]{0,12}DMG", original):
                continue
            resultats.append((f"dmg_{ELEMENTS[m_el.group(1)]}", valeur, cible))
            continue
        if stat is None and re.search(r"DMG", original):
            if (re.search(TYPES_DEGATS + r"[^,.;]{0,12}DMG", original) or re.search(r"DMG(?: dealt)?(?: by| through| from| of)[^,.;]{0,24}" + TYPES_DEGATS, original)
                    or re.search(r"DMG taken|vulnerab|absorb|max DMG", original, re.I)):
                continue
            stat = "dmg"
        if stat is None or (typee and stat in ("cr", "cd")):
            continue
        if sans_soi:
            resultats.append((stat if stat not in ("hp", "atk", "def", "spd") else (f"{stat}_pct" if pct else stat), valeur, "equipe_sans_soi"))
            continue
        if stat in ("hp", "atk", "def", "spd"):
            stat = f"{stat}_pct" if pct else stat
        elif not pct:
            continue
        resultats.append((stat, valeur, cible))
    return resultats


def clauses(texte_en, texte_fr, params_par_niveau):
    """Effets reconnus dans un texte : [(stat, [valeurs], cible, conditionnel, texte_fr)]."""
    sortie = []
    textes = [resoudre(texte_en, p) for p in params_par_niveau]
    ph = [phrases(t) for t in textes]
    ph_fr = phrases(resoudre(texte_fr, params_par_niveau[0])) if texte_fr else []
    meme = len(ph_fr) == len(ph[0])
    for i, phrase in enumerate(ph[0]):
        base = analyser(phrase)
        if not base:
            continue
        series = [analyser(ph[j][i]) if len(ph[j]) == len(ph[0]) else [] for j in range(len(ph))]
        for k, (stat, valeur, cible) in enumerate(base):
            valeurs = []
            for serie in series:
                valeurs.append(nombre(serie[k][1]) if len(serie) == len(base) and serie[k][0] == stat else None)
            if None in valeurs:
                continue
            sortie.append((stat, valeurs, cible, bool(CONDITION.search(phrase)), ph_fr[i] if meme else None))
    return sortie


def effet(stat, valeur, cible, conditionnel, texte, origine, cm=None, tableau=False):
    e = {"s": stat, "v": valeur, "o": origine}
    if texte:
        e["c"] = texte
    if cible == "equipe_sans_soi":
        cible = "equipe"
        e["xs"] = 1
    if cible in ("equipe", "allie", "ennemi"):
        e["cible"] = "equipe"
    if cible == "allie":
        e["al"] = 1
    if cible == "ennemi":
        e["en"] = 1
    if conditionnel or cible == "allie":
        e["d"] = 1
    if cm:
        e["cm"] = cm
    return e


def personnages(res):
    en_c, fr_c = lire(res, "en", "characters"), lire(res, "fr", "characters")
    sk_en, sk_fr = lire(res, "en", "character_skills"), lire(res, "fr", "character_skills")
    rk_en, rk_fr = lire(res, "en", "character_ranks"), lire(res, "fr", "character_ranks")
    tr_en, tr_fr = lire(res, "en", "character_skill_trees"), lire(res, "fr", "character_skill_trees")
    sortie = {}
    for cid, c in en_c.items():
        if "Trailblazer" in c["name"] and False:
            continue
        liste = []

        def ajouter(texte_en, texte_fr, params, origine, cm=None):
            for stat, valeurs, cible, cond, txt in clauses(texte_en, texte_fr, [params]):
                liste.append(effet(stat, valeurs[0], cible, cond, txt, origine, cm))

        for sid in c.get("skills", []):
            s = sk_en.get(sid)
            if not s or s["type"] == "MazeNormal" or not s.get("desc"):
                continue
            params = s.get("params") or [[]]
            niveau = params[min(len(params) - 1, 9)]
            nom = f"{NOMS_TYPES.get(s['type'], s['type'])} : {sk_fr.get(sid, {}).get('name', s['name'])}"
            ajouter(s["desc"], sk_fr.get(sid, {}).get("desc"), niveau, nom)
        for tid in c.get("skill_trees", []):
            t = tr_en.get(tid)
            if not t or not t.get("desc"):
                continue
            params = (t.get("params") or [[]])[0]
            ajouter(t["desc"], tr_fr.get(tid, {}).get("desc"), params, f"Trace : {tr_fr.get(tid, {}).get('name') or t['name']}")
        for rid in c.get("ranks", []):
            r = rk_en.get(rid)
            if r:
                ajouter(r["desc"], rk_fr.get(rid, {}).get("desc"), [], f"Éidolon {r['rank']} : {rk_fr.get(rid, {}).get('name', r['name'])}", cm=r["rank"])
        vus, uniques = set(), []
        for e in liste:
            cle = (e["s"], json.dumps(e["v"]), e.get("cible"), e["o"], e.get("cm"))
            if cle not in vus:
                vus.add(cle)
                uniques.append(e)
        on = [e for e in uniques if not e.get("al") and not e.get("xs")]
        off = [e for e in uniques if e.get("cible")]
        if on or off:
            sortie[cid] = {"on": on, "off": off}
    return sortie


def cones(res):
    lc_en, lr_en, lr_fr = lire(res, "en", "light_cones"), lire(res, "en", "light_cone_ranks"), lire(res, "fr", "light_cone_ranks")
    sortie = {}
    for cid, c in lc_en.items():
        r = lr_en.get(cid)
        if not r:
            continue
        liste = []
        for stat, valeurs, cible, cond, txt in clauses(r["desc"], lr_fr.get(cid, {}).get("desc"), r["params"]):
            liste.append(effet(stat, valeurs, cible, cond, txt, lr_fr.get(cid, {}).get("skill") or r["skill"]))
        if liste:
            sortie[c["name"]] = liste
    return sortie


def sets(res, bonus_sets):
    rs_en, rs_fr = lire(res, "en", "relic_sets"), lire(res, "fr", "relic_sets")
    sortie = {}
    for sid, s in rs_en.items():
        resultat = {"deux": [], "quatre": []}
        for indice, cle, pieces in ((0, "deux", "2"), (1, "quatre", "4")):
            if indice >= len(s["desc"]):
                continue
            fr = rs_fr.get(sid, {}).get("desc", [None, None])
            fr = fr[indice] if indice < len(fr) else None
            deja = pieces in bonus_sets.get(sid, {})
            for stat, valeurs, cible, cond, txt in clauses(s["desc"][indice], fr, [[]]):
                if cible == "soi" and not cond and (deja or pieces == "2"):
                    continue
                resultat[cle].append(effet(stat, valeurs[0], cible, cond, txt, f"{pieces} pièces"))
        if resultat["deux"] or resultat["quatre"]:
            sortie[s["name"]] = resultat
    return sortie


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--res", required=True, help="dossier index_min de StarRailRes (contenant en/ et fr/)")
    args = parser.parse_args()
    ref = json.loads((SITE / "assets" / "donnees" / "enka_starrail.json").read_text(encoding="utf-8"))
    bonus = {sid: s.get("bonus", {}) for sid, s in ref["sets"].items()}
    sortie = {
        "version": 2,
        "aide": "Généré par scripts/generer_buffs_starrail.py (voir son en-tête). Effet : s statistique, v valeur (tableau de 5 pour les cônes, une valeur par superposition), c texte, o origine, cible equipe (bonus apporté aux alliés ou débuff ennemi), al allié ciblé, en effet sur l'ennemi, cm éidolon minimum, d désactivé par défaut (effet conditionnel).",
        "persos": personnages(args.res), "cones": cones(args.res), "sets": sets(args.res, bonus),
    }
    chemin = SITE / "assets" / "donnees" / "starrail_buffs.json"
    chemin.write_text(json.dumps(sortie, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    n = lambda d: sum(len(v) if isinstance(v, list) else sum(len(x) for x in v.values()) for v in d.values())
    print(chemin.name, chemin.stat().st_size // 1024, "Ko ;", len(sortie["persos"]), "personnages,", len(sortie["cones"]), "cônes,", len(sortie["sets"]), "sets")


if __name__ == "__main__":
    main()
