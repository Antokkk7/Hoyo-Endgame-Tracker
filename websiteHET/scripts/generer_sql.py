#!/usr/bin/env python3
"""Génère les fichiers SQL de données à partir des JSON de l'application de bureau.

Usage (depuis la racine du repo) :
    python websiteHET/scripts/generer_sql.py

Options :
    --genshin  data/genshin     dossier des JSON Genshin
    --starrail data/starrail    dossier des JSON Star Rail
    --sortie   websiteHET/sql   dossier de sortie

Fichiers produits :
    03_donnees_reference.sql    catalogues de personnages et équipes recommandées
    04_compte_antok.sql         compte administrateur Antok et ses données (Genshin, puis Star Rail si présent)
"""
import argparse
import json
from pathlib import Path

ELEMENTS_HSR = {
    "physique": "PHYSICAL", "feu": "FIRE", "glace": "ICE", "foudre": "LIGHTNING",
    "vent": "WIND", "quantique": "QUANTUM", "imaginaire": "IMAGINARY",
}

RECOS = [
    ("genshin", "Mavuika", ["Mavuika", "Iansan", "Bennett", "Chevreuse"], "Équipe Pyro de démonstration"),
    ("starrail", "Acheron", ["Acheron", "Mortenax Blade", "Ashveil", "Hyacine"], "Équipe Nihilité de démonstration"),
]


def lit(valeur):
    """Littéral SQL texte (ou null)."""
    if valeur is None:
        return "null"
    return "'" + str(valeur).replace("'", "''") + "'"


def nombre(valeur, defaut="null"):
    return defaut if valeur is None else str(int(valeur))


def jsonb(valeur):
    texte = json.dumps(valeur, ensure_ascii=False, separators=(",", ":"))
    if "$j$" in texte:
        raise ValueError("séquence $j$ interdite dans les données")
    return "$j$" + texte + "$j$::jsonb"


def tableau(valeurs, type_sql, guillemets=False):
    if not valeurs:
        return "'{}'::" + type_sql + "[]"
    if guillemets:
        corps = ",".join(lit(v) for v in valeurs)
    else:
        corps = ",".join(str(v).lower() if isinstance(v, bool) else str(int(v)) for v in valeurs)
    return "array[" + corps + "]::" + type_sql + "[]"


def lire(dossier, nom):
    chemin = Path(dossier) / nom
    if not chemin.exists():
        return None
    return json.loads(chemin.read_text(encoding="utf-8"))


def liste(donnees, *cles):
    if donnees is None:
        return []
    if isinstance(donnees, list):
        return donnees
    for cle in cles:
        if cle in donnees:
            return donnees[cle]
    return []


def membres(equipe):
    return [{
        "characterId": m.get("characterId"), "name": m.get("name"), "element": m.get("element"),
        "level": m.get("level"), "constellation": m.get("constellation"),
        "weapon": m.get("weapon"), "refinement": m.get("refinement"),
    } for m in (equipe or [])]


def sql_equipe(jeu, nom, usage, equipe):
    lignes = [
        f"    insert into public.equipe (compte_id, jeu, nom, usage) values (v_compte, {lit(jeu)}, {lit(nom)}, {lit(usage or 'Libre')})",
        "    returning id into v_equipe;",
    ]
    for position, m in enumerate(equipe):
        lignes.append(
            "    insert into public.equipe_membre (equipe_id, position, personnage_id, nom, element, niveau, doublons, arme, raffinement) values "
            f"(v_equipe, {position}, {lit(m.get('characterId'))}, {lit(m.get('name'))}, {lit(m.get('element'))}, "
            f"{nombre(m.get('level'))}, {nombre(m.get('constellation'))}, {lit(m.get('weapon'))}, {nombre(m.get('refinement'))});")
    return lignes


def donnees_reference(genshin, starrail):
    out = ["-- Généré par scripts/generer_sql.py : catalogues et équipes recommandées.",
           "-- Relançable : les lignes existantes sont remplacées.", "",
           "delete from public.personnage_catalogue;", ""]
    for c in liste(lire(genshin, "genshinCharacters.json"), "characters", "personnages"):
        out.append("insert into public.personnage_catalogue (jeu, nom, element) values "
                   f"('genshin', {lit(c['name'])}, {lit(c.get('element'))});")
    out.append("")
    fichier = Path(starrail) / "honkaiStarRailCharacters.json"
    for c in liste(lire(starrail, "honkaiStarRailCharacters.json"), "characters", "personnages"):
        nom = c.get("name") or c.get("nom")
        element = c.get("element")
        element = ELEMENTS_HSR.get(str(element).lower(), element)
        out.append("insert into public.personnage_catalogue (jeu, nom, element, rarete, voie) values "
                   f"('starrail', {lit(nom)}, {lit(element)}, {nombre(c.get('rarity', c.get('rarete')))}, "
                   f"{lit(c.get('path', c.get('voie')))});")
    out += ["", "delete from public.reco_equipe;", ""]
    for jeu, perso, equipe, note in RECOS:
        out.append("insert into public.reco_equipe (jeu, personnage, membres, note) values "
                   f"({lit(jeu)}, {lit(perso)}, {tableau(equipe, 'text', True)}, {lit(note)});")
    return "\n".join(out) + "\n"


def compte_antok(genshin, starrail):
    out = ["-- Généré par scripts/generer_sql.py : compte administrateur Antok et ses données.",
           "-- Mot de passe initial : admin123 (à changer depuis la page Compte).",
           "-- Relançable : le compte et ses données sont recréés.", "",
           "delete from public.compte where lower(pseudo) = 'antok';", "",
           "do $$", "declare", "    v_compte uuid;", "    v_equipe uuid;", "begin",
           "    insert into public.compte (pseudo, mdp_hash, role, avatar_jeu, avatar_nom)",
           "    values ('Antok', extensions.crypt('admin123', extensions.gen_salt('bf', 10)), 'administrateur', 'genshin', 'Freminet')",
           "    returning id into v_compte;", ""]

    for e in liste(lire(genshin, "genshinEquipes.json"), "teams", "equipes"):
        out += sql_equipe("genshin", e.get("name"), e.get("usage"), membres(e.get("members")))
    for a in liste(lire(genshin, "genshinAbysses.json"), "clears"):
        out.append("    insert into public.genshin_abysse (compte_id, patch, rotation, etage, etoiles_base, salle1, salle2, salle3, equipe) values "
                   f"(v_compte, {lit(a.get('patch'))}, {lit(a.get('rotation'))}, {nombre(a.get('floor'), '12')}, {nombre(a.get('baseStars'), '0')}, "
                   f"{nombre(a.get('room1'), '0')}, {nombre(a.get('room2'), '0')}, {nombre(a.get('room3'), '0')}, {jsonb(membres(a.get('team')))});")
    for t in liste(lire(genshin, "genshinTheatre.json"), "clears"):
        mvp = membres([t["mvp"]])[0] if t.get("mvp") else None
        out.append("    insert into public.genshin_theatre (compte_id, annee, mois, patch, mode, elements, etoiles, etoiles_flags, arcanum, meilleur_acte, mvp, mvp_pret) values "
                   f"(v_compte, {nombre(t.get('year'))}, {nombre(t.get('month'))}, {lit(t.get('patch'))}, {lit(t.get('mode'))}, "
                   f"{tableau(t.get('elements') or [], 'text', True)}, {nombre(t.get('stars'), '0')}, {tableau(t.get('starFlags') or [], 'boolean')}, "
                   f"{nombre(t.get('arcanum'), '0')}, {nombre(t.get('bestAct'))}, {jsonb(mvp) if mvp else 'null'}, {str(bool(t.get('mvpTrial'))).lower()});")
    for c in liste(lire(genshin, "genshinCarnage.json"), "clears"):
        boss = [{"name": b.get("name"), "seconds": b.get("seconds"), "team": membres(b.get("team"))} for b in c.get("bosses") or []]
        out.append("    insert into public.genshin_carnage (compte_id, patch, periode, difficulte, boss) values "
                   f"(v_compte, {lit(c.get('patch'))}, {lit(c.get('period'))}, {nombre(c.get('difficulty'), '1')}, {jsonb(boss)});")

    for e in liste(lire(starrail, "honkaiStarRailEquipes.json"), "teams", "equipes"):
        out += sql_equipe("starrail", e.get("name"), e.get("usage"), membres(e.get("members")))
    for x in liste(lire(starrail, "honkaiStarRailMoc.json"), "clears"):
        equipes = [membres(t) for t in x.get("teams") or []]
        out.append("    insert into public.starrail_moc (compte_id, patch, etoiles, cycles, stellaire, auto9, auto10, equipes) values "
                   f"(v_compte, {lit(x.get('patch'))}, {tableau(x.get('stars') or [], 'smallint')}, {tableau(x.get('cycles') or [], 'smallint')}, "
                   f"{str(bool(x.get('stellar'))).lower()}, {str(bool(x.get('auto9'))).lower()}, {str(bool(x.get('auto10'))).lower()}, {jsonb(equipes)});")
    for x in liste(lire(starrail, "honkaiStarRailPf.json"), "clears"):
        equipes = [membres(t) for t in x.get("teams") or []]
        out.append("    insert into public.starrail_pf (compte_id, patch, scores, stellaire, auto1, auto2, equipes) values "
                   f"(v_compte, {lit(x.get('patch'))}, {tableau(x.get('scores') or [], 'integer')}, {str(bool(x.get('stellar'))).lower()}, "
                   f"{str(bool(x.get('auto1'))).lower()}, {str(bool(x.get('auto2'))).lower()}, {jsonb(equipes)});")
    for x in liste(lire(starrail, "honkaiStarRailApoc.json"), "clears"):
        equipes = [membres(t) for t in x.get("teams") or []]
        out.append("    insert into public.starrail_apoc (compte_id, patch, mode, scores, equipes) values "
                   f"(v_compte, {lit(x.get('patch'))}, {lit(x.get('mode') or 'D4')}, {tableau(x.get('scores') or [], 'integer')}, {jsonb(equipes)});")
    for x in liste(lire(starrail, "honkaiStarRailArbitration.json"), "clears"):
        cavaliers = [membres(t) for t in x.get("horsemenTeams") or []]
        out.append("    insert into public.starrail_arbitre (compte_id, patch, cycles, ko, cycles_roi, roi_ko, equipes_cavaliers, equipe_roi) values "
                   f"(v_compte, {lit(x.get('patch'))}, {tableau(x.get('cycles') or [], 'smallint')}, {tableau(x.get('down') or [], 'boolean')}, "
                   f"{nombre(x.get('kingCycles'), '0')}, {str(bool(x.get('kingDown'))).lower()}, {jsonb(cavaliers)}, {jsonb(membres(x.get('team')))});")
    out += ["end $$;"]
    return "\n".join(out) + "\n"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--genshin", default="data/genshin")
    p.add_argument("--starrail", default="data/starrail")
    p.add_argument("--sortie", default="websiteHET/sql")
    a = p.parse_args()
    sortie = Path(a.sortie)
    sortie.mkdir(parents=True, exist_ok=True)
    (sortie / "03_donnees_reference.sql").write_text(donnees_reference(a.genshin, a.starrail), encoding="utf-8")
    (sortie / "04_compte_antok.sql").write_text(compte_antok(a.genshin, a.starrail), encoding="utf-8")
    print("SQL écrit dans", sortie)


if __name__ == "__main__":
    main()
