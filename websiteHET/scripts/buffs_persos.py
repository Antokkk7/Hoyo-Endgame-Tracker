# Buffs des personnages de Genshin Impact (talents niveau 10, personnage niveau 90).
# "on"  : bonus que le personnage s'applique à lui-même quand il est sur le terrain (talents, passifs, constellations) ;
#         ce qui touche aussi l'équipe y figure également, car il en profite.
# "off" : bonus qu'il apporte à un autre personnage quand il est hors terrain, sans arme ni artéfact.
# Les bonus variables (cumuls, durées, paliers) sont pris à 70 % de leur maximum. Un bonus qui dépend d'une statistique du
# personnage utilise une valeur typique, précisée dans la condition.
# Champs : voir buffs_sets.py ; en plus cm = constellation minimale, pour = éléments du bénéficiaire, eq = condition d'équipe.
from buffs_sets import e, v70, EL4

TOUS_RES = ["res_PYRO", "res_HYDRO", "res_ELECTRO", "res_CRYO", "res_ANEMO", "res_GEO", "res_DENDRO", "res_PHYS"]
P4 = EL4


def deux(liste):
    """Même liste de bonus pour le personnage lui-même et pour ses coéquipiers."""
    return {"on": [dict(x) for x in liste], "off": [dict(x) for x in liste]}


def perso(on=None, off=None, note=None):
    d = {"on": on or [], "off": off or []}
    if note:
        d["note"] = note
    return d


PERSOS = {}

# Pyro
PERSOS["Bennett"] = deux([
    e("atk", 805, "Déchaînement : 100,8 % de l'ATQ de base (≈ 800 avec une arme de 600 ATQ)"),
    e("atk", 160, "C1 : +20 % de l'ATQ de base", cm=1),
    e("dmg_PYRO", 15, "C6 : mêlée infusée Pyro", cm=6),
])
PERSOS["Xiangling"] = deux([e("atk_pct", 10, "A4 : après le déchaînement"), e("res_PYRO", 15, "C1 : après un coup de Guoba", cm=1)])
PERSOS["Klee"] = {
    "on": [e("dmg_ca", 50, "A1 : attaque chargée après une attaque normale"), e("def_red", 23, "C2 : mines", cm=2), e("dmg_PYRO", 50, "C6 : sur le terrain", cm=6)],
    "off": [e("def_red", 23, "C2 : mines", cm=2), e("dmg_PYRO", 10, "C6 : après le déchaînement", cm=6)],
}
PERSOS["Hu Tao"] = perso(on=[
    e("atk", None, "Compétence : 6,26 % des PV max en ATQ (limité à 400 % de l'ATQ de base)", f={"b": "hp", "k": 0.0626, "mx": 3100}),
    e("dmg_PYRO", 33, "A4 : PV inférieurs ou égaux à 50 %", d=True),
    e("cr", 12, "A1 : après une attaque critique"),
    e("cr", 12, "C4 : après avoir vaincu un ennemi", cm=4),
    e("cr", 100, "C6 : PV inférieurs à 25 %", cm=6, d=True),
], off=[e("cr", 12, "A1 : équipe après une attaque critique"), e("cr", 12, "C4 : équipe", cm=4)])
PERSOS["Diluc"] = perso(on=[e("dmg_PYRO", 20, "A4 : après le déchaînement"), e("atk_pct", 21, "C2 : +10 % par cumul (3 max, 70 % du max)", cm=2), e("atk_pct", 30, "C6 : après la compétence", cm=6)])
PERSOS["Yoimiya"] = perso(on=[e("dmg_PYRO", v70(20), "A1 : +2 % par cumul (10 max, 70 % du max)")], off=[e("atk_pct", v70(20), "A4 : +10 % de base, +1 % par cumul (max 20 %, 70 % du max)")])
PERSOS["Amber"] = deux([e("atk_pct", 15, "A4 : après le déchaînement"), e("atk_pct", 15, "C6 : après le déchaînement", cm=6)])
PERSOS["Gaming"] = perso(on=[e("atk_pct", 20, "C2 : sous les PV 50 %", cm=2, d=True), e("cr", 20, "C6 : compétence plongeante", cm=6, d=True), e("cd", 40, "C6 : compétence plongeante", cm=6, d=True)])
PERSOS["Mavuika"] = perso(on=[e("atk_pct", 40, "C1 : Âme nocturne", cm=1), e("dmg", 10, "C4 : après le déchaînement", cm=4), e("def_red", 20, "C2 : forme de l'anneau", cm=2)],
                          off=[e("dmg", 10, "C4 : après le déchaînement", cm=4), e("def_red", 20, "C2", cm=2)])
PERSOS["Arlecchino"] = perso(on=[e("cr", 10, "C6 : attaques normales et déchaînement", cm=6), e("cd", 70, "C6 : attaques normales et déchaînement", cm=6)])
PERSOS["Chevreuse"] = perso(off=[
    e(["res_PYRO", "res_ELECTRO"], 40, "A1 : après Surcharge (équipe composée uniquement de Pyro et d'Électro)", eq="pyroelectro"),
    e("atk_pct", 28, "A4 : +1 % par 1000 PV (40 % max, 70 %) : équipe Pyro/Électro", eq="pyroelectro"),
    e(["dmg_PYRO", "dmg_ELECTRO"], v70(60), "C6 : +20 % par cumul (3 max, 70 % du max)", eq="pyroelectro", cm=6),
], on=[e("atk_pct", 28, "A4 : +1 % par 1000 PV (40 % max, 70 %) : équipe Pyro/Électro", eq="pyroelectro")])
PERSOS["Lyney"] = deux([e("res_PYRO", 20, "C4", cm=4)])

# Hydro
PERSOS["Furina"] = deux([e("dmg", 52.5, "Déchaînement : +0,25 % par point de Fanfare (300 max, 70 % du max)")])
PERSOS["Nilou"] = deux([e("res_HYDRO", 35, "C2 : après un coup de la compétence", cm=2), e("res_DENDRO", 35, "C2 : après un coup de la compétence", cm=2), e("em", 100, "A1 : après compétence et coup")])
PERSOS["Xingqiu"] = perso(on=[e("dmg_HYDRO", 20, "A4 : sous l'effet de la compétence"), e("res_HYDRO", 15, "C2", cm=2), e("dmg_burst", 50, "C4", cm=4)], off=[e("res_HYDRO", 15, "C2 : après un coup d'épée", cm=2)])
PERSOS["Yelan"] = deux([e("dmg", 35, "A4 : +1 % par seconde, jusqu'à 50 % (70 % du max)"), e("hp_pct", 28, "C4 : +10 % par ennemi vaincu (4 cumuls max, 70 %)", cm=4)])
PERSOS["Mona"] = {
    "on": [e("dmg", 60, "Déchaînement : Présage (ennemi sous bulle)"), e("cr", 15, "C4 : ennemis marqués du Présage", cm=4), e("em", 80, "C2 : après une attaque chargée", cm=2)],
    "off": [e("dmg", 60, "Déchaînement : Présage (ennemi sous bulle)"), e("cr", 15, "C4 : ennemis marqués du Présage", cm=4), e("em", 80, "C2 : après une attaque chargée", cm=2)],
}
PERSOS["Barbara"] = deux([e("dmg_HYDRO", 15, "C2 : sous l'effet du déchaînement", cm=2)])
PERSOS["Kamisato Ayato"] = deux([e("dmg_na", 20, "Déchaînement : Éclat de l'eau")])
PERSOS["Candace"] = perso(off=[e("dmg_na", 20, "Déchaînement : infusion Hydro (attaques normales)")])
PERSOS["Neuvillette"] = perso(on=[e("dmg_HYDRO", 21, "A4 : +60 % par goutte, jusqu'à 30 % (70 % du max)"), e("cd", 29.4, "C2 : attaque chargée, +14 % par cumul (3 max, 70 %)", cm=2)])
PERSOS["Wriothesley"] = perso(on=[e("dmg_na", 50, "A1 : PV supérieurs à 60 %", d=True), e("dmg_ca", 50, "A1 : PV supérieurs à 60 %", d=True), e("atk_pct", 21, "A4 : +6 % par cumul (5 max, 70 %)")])
PERSOS["Sigewinne"] = perso(off=[e("res_HYDRO", 35, "C2 : après un coup", cm=2)], on=[e("res_HYDRO", 35, "C2 : après un coup", cm=2)])
PERSOS["Vodyanitsa"] = perso(off=[e(["res_HYDRO", "res_CRYO"], 30, "Compétence : après un coup"), e("res_ANEMO", 35, "A1 : vortex")], on=[e(["res_HYDRO", "res_CRYO"], 30, "Compétence : après un coup")])

# Cryo
PERSOS["Shenhe"] = deux([
    e(["res_CRYO", "res_PHYS"], 15, "Déchaînement : Cérémonie de la déesse"),
    e("dmg_CRYO", 15, "A1 : après la compétence"),
    e("dmg_skill", 15, "A4 : compétence appuyée"), e("dmg_burst", 15, "A4 : compétence appuyée"),
    e("dmg_na", 15, "A4 : compétence maintenue", d=True), e("dmg_ca", 15, "A4 : compétence maintenue", d=True), e("dmg_pa", 15, "A4 : compétence maintenue", d=True),
    e("cd", 15, "C2 : dégâts Cryo", cm=2, pour=["CRYO"]),
])
PERSOS["Ganyu"] = perso(on=[e("dmg_CRYO", 20, "A4 : dans la zone du déchaînement"), e("cr", 20, "A1 : après une flèche gelée"), e("res_CRYO", 15, "C1 : après un coup", cm=1)],
                        off=[e("dmg_CRYO", 20, "A4 : dans la zone du déchaînement"), e("res_CRYO", 15, "C1 : après un coup", cm=1)])
PERSOS["Kamisato Ayaka"] = perso(on=[e("dmg_na", 30, "A1 : après la compétence"), e("dmg_ca", 30, "A1 : après la compétence"), e("dmg_CRYO", 18, "A4 : après un dash"), e("def_red", 30, "C4 : après le déchaînement", cm=4)],
                                 off=[e("def_red", 30, "C4 : après le déchaînement", cm=4)])
PERSOS["Chongyun"] = deux([e("res_CRYO", 10, "A4 : après un coup de la compétence")])
PERSOS["Rosaria"] = perso(on=[e("cr", 12, "A1 : après une attaque dans le dos")], off=[e("cr", 15, "A4 : 15 % de son taux CRIT (plafonné à 15 %)"), e("res_PHYS", 20, "C6", cm=6)])
PERSOS["Mika"] = deux([e("dmg_PHYS", 21, "A4 : +10 % par cumul (3 max, 70 %)"), e("autre", 60, "C6 : DGT CRIT physiques +60 %", cm=6)])
PERSOS["Citlali"] = perso(off=[e("res_PYRO", 20, "A1 : après Fonte ou Gel"), e("res_HYDRO", 20, "A1 : après Fonte ou Gel"), e("res_PYRO", 20, "C2", cm=2), e("res_HYDRO", 20, "C2", cm=2), e("em", 250, "C2 : bouclier actif", cm=2),
                                e("dmg_PYRO", 42, "C6 : +1,5 % par point d'âme nocturne (40 max, 70 %)", cm=6), e("dmg_HYDRO", 42, "C6 : +1,5 % par point d'âme nocturne (40 max, 70 %)", cm=6)])
PERSOS["Eula"] = perso(on=[e(["res_CRYO", "res_PHYS"], 25, "Compétence : Marque du glaçon (par cumul)"), e("dmg_PHYS", 30, "C1 : après un coup de compétence (cumuls max)", cm=1), e("dmg_burst", 25, "C4 : ennemi sous 50 % de PV", cm=4, d=True)],
                        off=[e(["res_CRYO", "res_PHYS"], 25, "Compétence : Marque du glaçon")])
PERSOS["Aloy"] = perso(on=[e("atk_pct", 16, "A1 : Glace fusante"), e("dmg_CRYO", 24.5, "A4 : +3,5 % par seconde (35 % max, 70 %)")], off=[e("atk_pct", 8, "A1 : Glace fusante (équipe)")])
PERSOS["Kaeya"] = perso(on=[e("cr", 15, "C1 : ennemis affectés par le Cryo", cm=1)])
PERSOS["Diona"] = perso(off=[e("em", 200, "C6 : PV supérieurs à 50 %", cm=6)])
PERSOS["Lohen"] = perso(off=[e("atk_pct", 15, "A4 : maître du Cryo"), e("em", 200, "C2", cm=2)])
PERSOS["Escoffier"] = perso(on=[e("cd", 60, "C1 : dégâts Cryo", cm=1, pour=["CRYO"])])

# Électro
PERSOS["Raiden Shogun"] = perso(off=[e("dmg_burst", 21, "Œil du Châtiment : +0,3 % par point d'énergie (déchaînement de 70)"), e("atk_pct", 30, "C4 : après le déchaînement", cm=4)])
PERSOS["Beidou"] = deux([e(["res_ELECTRO", "res_CRYO"], 15, "C6 : arc Électro", cm=6)])
PERSOS["Razor"] = deux([e("def_red", 15, "C4 : après une attaque de la compétence", cm=4)])
PERSOS["Keqing"] = perso(on=[e("cr", 15, "A4 : après la compétence"), e("er", 15, "A4 : après la compétence"), e("atk_pct", 25, "C4 : après une réaction Électro", cm=4), e("dmg_ELECTRO", 16.8, "C6 : +6 % par cumul (4 max, 70 %)", cm=6)])
PERSOS["Lisa"] = deux([e("def_red", 15, "A4 : après un coup de la compétence maintenue")])
PERSOS["Yae Miko"] = perso(on=[e("dmg_ELECTRO", 20, "C4 : après les attaques de la compétence", cm=4), e("def_ign", 60, "C6", cm=6)], off=[e("dmg_ELECTRO", 20, "C4 : après le déchaînement", cm=4)])
PERSOS["Kujou Sara"] = perso(off=[e("atk", 540, "Compétence : 77,3 % de l'ATQ de base (≈ 700)"), e("cd", 60, "C6 : dégâts Électro", cm=6, pour=["ELECTRO"])])
PERSOS["Cyno"] = perso(on=[e("em", 200, "C1 : sous Jugement", cm=1), e("dmg_ELECTRO", 35, "C2 : +10 % par cumul (5 max, 70 %)", cm=2)])
PERSOS["Sethos"] = perso(on=[e("cr", 15, "C1 : ombre", cm=1), e("dmg_ELECTRO", 21, "C2 : +15 % par cumul (2 max, 70 %)", cm=2)], off=[e("em", 80, "C4", cm=4)])
PERSOS["Ororon"] = perso(off=[e("atk_pct", 21, "C6 : +10 % par cumul (3 max, 70 %)", cm=6)])
PERSOS["Iansan"] = perso(off=[e("atk", 483, "Déchaînement : ATQ jusqu'à 690 (70 % du max)"), e("atk_pct", 30, "C2", cm=2), e("dmg", 25, "C6", cm=6)])
PERSOS["Mitya"] = perso(off=[e("cd", 40, "C1 : zone du noyau", cm=1), e(["res_CRYO", "res_ELECTRO"], 20, "C2", cm=2)])
PERSOS["Valeriy"] = perso(off=[e("em", 100, "C2 : compétence", cm=2), e("cr", 10, "C6", cm=6, pour=["ELECTRO"]), e("cd", 40, "C6", cm=6, pour=["ELECTRO"])])
PERSOS["Flins"] = perso(off=[e("res_ELECTRO", 25, "C2", cm=2)])
PERSOS["Ineffa"] = perso(off=[e("em", 144, "A4 : 6 % de l'ATQ d'Ineffa (≈ 2400) après le déchaînement")])
PERSOS["Nicole"] = perso(off=[e("atk", 420, "Compétence : jusqu'à 600 d'ATQ (70 %)"), e(TOUS_RES, 25, "C2", cm=2)])

# Anémo
PERSOS["Kaedehara Kazuha"] = {
    "on": [e("em", 200, "C2", cm=2)],
    "off": [e("dmg_ELEM", 40, "A4 : 0,04 % par EM (EM ≈ 1000) après un Tourbillon", pour=P4), e("em", 200, "C2", cm=2)],
    "note": "C6 : +0,2 % de dégâts d'attaque par EM non inclus (conditionnel).",
}
PERSOS["Sucrose"] = perso(off=[e("em", 180, "A4 : 20 % de l'EM de Sucrose (≈ 900)"), e("em", 50, "A1 : après un Tourbillon (élément de l'élément absorbé)", pour=P4), e("dmg_ELEM", 20, "C6 : élément absorbé", cm=6, pour=P4)])
PERSOS["Venti"] = deux([e(["res_ANEMO", "res_PHYS"], 12, "C2", cm=2), e("dmg_ELEM", 25, "C4 : élément absorbé", cm=4, pour=P4), e("res_ELEM", 20, "C6 : élément absorbé", cm=6, pour=P4)])
PERSOS["Faruzan"] = deux([e("dmg_ANEMO", 32.4, "Déchaînement : Bénédiction du vent"), e("res_ANEMO", 30, "Déchaînement : Effondrement pressurisé"), e("cd", 40, "C6 : dégâts Anémo", cm=6, pour=["ANEMO"])])
PERSOS["Jean"] = deux([e("res_ANEMO", 40, "C4 : dans le Pré de pissenlits", cm=4)])
PERSOS["Xiao"] = perso(on=[e("dmg_na", 95.2, "Déchaînement : Fléau du mal"), e("dmg_ca", 95.2, "Déchaînement : Fléau du mal"), e("dmg_pa", 95.2, "Déchaînement : Fléau du mal"),
                           e("dmg", 17.5, "A1 : +5 % toutes les 3 s (25 % max, 70 %)"), e("dmg_skill", 31.5, "A4 : +15 % par cumul (3 max, 70 %)"), e("er", 25, "C2 : PV inférieurs à 50 %", cm=2, d=True)])
PERSOS["Lynette"] = perso(off=[e("atk_pct", 20, "A1 : après le déchaînement"), e("dmg_ANEMO", 20, "C6", cm=6, pour=["ANEMO"])])
PERSOS["Lan Yan"] = perso(off=[e("em", 60, "C4 : après le déchaînement", cm=4)])
PERSOS["Xianyun"] = perso(off=[e("atk_pct", 20, "C2", cm=2)])
PERSOS["Jahoda"] = perso(off=[e("em", 100, "A4 : après un soin"), e("cr", 5, "C6", cm=6), e("cd", 40, "C6", cm=6)])
PERSOS["Yumemizuki Mizuki"] = perso(off=[e("em", 100, "A4 : sous l'effet du rêve")])
PERSOS["Wanderer"] = perso(on=[e("atk_pct", 30, "A1 : réaction Pyro", d=True), e("cr", 20, "A1 : réaction Cryo", d=True)])

# Géo
PERSOS["Zhongli"] = deux([e(TOUS_RES, 20, "Compétence : Bouclier de jade actif")])
PERSOS["Albedo"] = perso(off=[e("em", 125, "A4 : après le déchaînement"), e("dmg_pa", 30, "C4 : après le déchaînement", cm=4), e("dmg", 17, "C6 : avec un bouclier de cristallisation", cm=6)])
PERSOS["Gorou"] = perso(off=[e("def", 371, "Compétence : au moins un personnage Géo", eq="GEO>=1"), e("dmg_GEO", 15, "Compétence : au moins deux personnages Géo", eq="GEO>=2"), e("def_pct", 25, "A1 : après le déchaînement"),
                             e("cd", 40, "C6 : dégâts Géo", cm=6, pour=["GEO"])])
PERSOS["Chiori"] = perso(off=[e("dmg_GEO", 20, "A4 : personnages proches")], on=[e("dmg_GEO", 20, "A4")])
PERSOS["Ningguang"] = perso(on=[e("dmg_GEO", 12, "A4 : après avoir traversé l'écran de jade")])
PERSOS["Navia"] = perso(off=[e("res_GEO", 20, "C4 : après un coup du déchaînement", cm=4)], on=[e("atk_pct", 20, "A4 : après un éclat de cristallisation")])
PERSOS["Noelle"] = perso(on=[e("atk", None, "Déchaînement : 72 % de la DÉF en ATQ", f={"b": "def", "k": 0.72})])
PERSOS["Yun Jin"] = perso(off=[e("dmg_na", 15, "C2", cm=2)])
PERSOS["Xilonen"] = perso(off=[e("res_GEO", 36, "Compétence : échantillon actif"), e("atk_pct", 45, "C2 : bénéficiaire Pyro", cm=2, pour=["PYRO"]), e("hp_pct", 45, "C2 : bénéficiaire Hydro", cm=2, pour=["HYDRO"]),
                               e("cd", 60, "C2 : bénéficiaire Cryo", cm=2, pour=["CRYO"]), e("dmg", 50, "C2 : bénéficiaire Géo", cm=2, pour=["GEO"])])
PERSOS["Linnea"] = perso(off=[e("res_GEO", 15, "A1 : Lumi")])

# Dendro
PERSOS["Tighnari"] = perso(on=[e("dmg_DENDRO", 20, "C2", cm=2)], off=[e("em", 60, "C4 : après le déchaînement", cm=4)])
PERSOS["Collei"] = perso(off=[e("em", 60, "C4 : après le déchaînement", cm=4)], on=[e("er", 20, "C1", cm=1)])
PERSOS["Baizhu"] = perso(off=[e("em", 80, "C4", cm=4)])
PERSOS["Alhaitham"] = perso(on=[e("em", 140, "C2 : +50 par cumul (4 max, 70 %)", cm=2)])
PERSOS["Nahida"] = perso(off=[e("def_red", 30, "C2 : ennemis marqués", cm=2)])
PERSOS["Emilie"] = perso(off=[e("res_DENDRO", 30, "C2", cm=2)])
PERSOS["Lauma"] = perso(off=[e(["res_DENDRO", "res_HYDRO"], 25, "Compétence : après un coup")])
PERSOS["Yaoyao"] = perso(off=[e("dmg_DENDRO", 15, "C1", cm=1)])
PERSOS["Kinich"] = perso(off=[e("res_DENDRO", 30, "C2", cm=2)])
PERSOS["Kirara"] = perso(off=[e("dmg_ELEM", 12, "C6", cm=6)])
PERSOS["Nefer"] = perso(off=[e("res_DENDRO", 20, "C4", cm=4)])

# Autres
PERSOS["Xinyan"] = deux([e("dmg_PHYS", 15, "A4 : avec un bouclier"), e("res_PHYS", 15, "C4", cm=4)])
PERSOS["Aino"] = perso(off=[e("em", 80, "C1 : après la compétence", cm=1)])
PERSOS["Columbina"] = perso(off=[e("cd", 80, "C6 : Hydro, Électro, Dendro, Géo", cm=6, pour=["HYDRO", "ELECTRO", "DENDRO", "GEO"])])

# Mise à jour d'après genshin.gg : bonus à leur maximum (paliers de statistiques atteints, cumuls à leur maximum).
EL_SWIRL = ["PYRO", "HYDRO", "ELECTRO", "CRYO"]
PERSOS["Alyosha"] = perso(
    on=[e("dmg", 70, "A4 : +0,35 % de DGT par 1 % de recharge d'énergie (70 % max)"), e("rx_stellar_conduct", 20, "Passif : Conduction stellaire +20 %"),
        e("em", 100, "C6 : 2 cumuls de Précision du chasseur", cm=6)],
    off=[e("atk_pct", 21.2, "Compétence : cible marquée"), e("em", 100, "C6 : 2 cumuls de Précision du chasseur", cm=6)])
PERSOS["Odette"] = perso(
    on=[e("rx_stellar", 60, "A1 : Splendeur merveilleuse, 4 cumuls de +15 %"), e("rx_stellar", 50, "Déchaînement : +50 % pendant 20 s"),
        e("base_stellar", 14, "Passif : DGT de base stellaires (plafond à 2000 ATQ)"), e("stellar_direct", 30, "A4 : DGT stellaires directs (plafond à 3000 ATQ)"),
        e("atk_pct", 28, "C2 : +7 % d'ATQ par cumul (4 cumuls)", cm=2),
        e(["res_CRYO", "res_ELECTRO", "res_ANEMO"], 20, "C2 : RÉS réduite près du double de danse", cm=2),
        e("res_PHYS", 40, "Conduction stellaire : RÉS Physique -40 %"),
        e("dmg_ELEM", 40, "Conduction stellaire : bonus Électro et Cryo (maximum 40 %)", pour=["ELECTRO", "CRYO"])],
    off=[e("rx_stellar", 15, "A1 : 1 cumul de Splendeur merveilleuse", g="odette:a1"), e("base_stellar", 14, "Passif : DGT de base stellaires (plafond à 2000 ATQ)", g="stellar:base"),
         e(["res_CRYO", "res_ELECTRO", "res_ANEMO"], 20, "C2 : RÉS réduite près du double de danse", cm=2),
         e("res_PHYS", 40, "Conduction stellaire : RÉS Physique -40 %"),
         e("dmg_ELEM", 40, "Conduction stellaire : bonus Électro et Cryo (maximum 40 %)", pour=["ELECTRO", "CRYO"])])
PERSOS["Durin"] = perso(
    on=[e(["res_PYRO", "res_DENDRO", "res_ELECTRO", "res_ANEMO", "res_GEO"], 35, "A1 : forme de flamme blanche (20 %, 35 % avec 2 Hexerei)"),
        e(["rx_vaporize", "rx_melt"], 70, "A1 : forme de décomposition (40 %, 70 % avec 2 Hexerei)"),
        e("dmg_burst", 75, "A4 : Fusion primordiale, +3 % par 100 ATQ (75 % max)"), e("dmg_burst", 40, "C4", cm=4),
        e("dmg_PYRO", 50, "C2 : après le déchaînement", cm=2), e("dmg_ELEM", 50, "C2 : élément de la réaction", cm=2, pour=["HYDRO", "CRYO", "DENDRO", "ELECTRO"]),
        e("def_ign", 30, "C6", cm=6), e("def_red", 30, "C6 : forme de pureté", cm=6)],
    off=[e(["res_PYRO", "res_DENDRO", "res_ELECTRO", "res_ANEMO", "res_GEO"], 35, "A1 : forme de flamme blanche (20 %, 35 % avec 2 Hexerei)"),
         e("dmg_PYRO", 50, "C2 : après le déchaînement", cm=2), e("dmg_ELEM", 50, "C2 : élément de la réaction", cm=2, pour=["HYDRO", "CRYO", "DENDRO", "ELECTRO"]),
         e("def_red", 30, "C6 : forme de pureté", cm=6)])

PERSOS["Arataki Itto"] = perso(
    on=[e("atk", None, "Déchaînement : ATQ + 57,6 % de la DÉF", f={"b": "def", "k": 0.576}), e("cd", 70, "C6 : attaques chargées", cm=6),
        e("atk_pct", 20, "C4 : fin du déchaînement", cm=4), e("def_pct", 20, "C4 : fin du déchaînement", cm=4)],
    off=[e("atk_pct", 20, "C4 : fin du déchaînement", cm=4), e("def_pct", 20, "C4 : fin du déchaînement", cm=4)])
PERSOS["Charlotte"] = perso(on=[e("heal", 15, "A4 : 3 autres Fontainais"), e("dmg_CRYO", 15, "A4 : 3 autres Fontainais"), e("atk_pct", 30, "C2 : 3 ennemis touchés", cm=2)])
PERSOS["Chasca"] = perso(on=[e("dmg_ca", 65, "A1 : projectiles d'ombre brillante, 3 types d'éléments"), e("cd", 120, "C6 : projectiles d'ombre", cm=6)])
PERSOS["Clorinde"] = perso(on=[e("cr", 20, "A4 : 2 cumuls de +10 %"), e("cr", 10, "C6", cm=6), e("cd", 70, "C6", cm=6)])
PERSOS["Dahlia"] = deux([e("shield", 25, "C2 : bouclier issu d'une Faveur", cm=2)])
PERSOS["Dehya"] = perso(on=[e("hp_pct", 20, "C1", cm=1), e("cr", 10, "C6 : Morsure léonine", cm=6), e("cd", 60, "C6 : 4 coups critiques", cm=6)])
PERSOS["Dori"] = perso(off=[e("er", 30, "C4 : lien du Djinn, énergie sous 50 %", cm=4)])
PERSOS["Freminet"] = perso(on=[e("dmg", 40, "A4 : Pression de l'éclat après un Éclatement"), e("cr", 15, "C1 : Pression de l'éclat", cm=1),
                               e("atk_pct", 18, "C4 : 2 cumuls de +9 %", cm=4), e("cd", 24, "C6 : 2 cumuls de +12 % (2/3 de 3)", cm=6)])
PERSOS["Ifa"] = perso(on=[e("em", 80, "A4 : après un Éclat d'âme nocturne"), e("em", 100, "C4 : après le champ de sédation", cm=4)])
PERSOS["Illuga"] = deux([
    e("cr", 5, "A1 : Serment du gardien de la lumière (DGT Géo)", pour=["GEO"]), e("cd", 10, "A1 : Serment du gardien de la lumière (DGT Géo)", pour=["GEO"]),
    e("em", 50, "A1 : avec la Lueur ascendante"),
    e("cr", 5, "C6 : Serment amélioré (DGT Géo, 10 % au total)", cm=6, pour=["GEO"]), e("cd", 20, "C6 : Serment amélioré (30 % au total)", cm=6, pour=["GEO"]),
    e("em", 30, "C6 : Serment amélioré (80 au total)", cm=6), e("def", 200, "C4 : pendant le chant du loriot", cm=4)])
PERSOS["Kachina"] = deux([e("def_pct", 20, "C4 : champ de forage, 4 ennemis ou plus", cm=4)])
PERSOS["Kaveh"] = perso(on=[e("em", 100, "A4 : 4 cumuls de +25 pendant le déchaînement")])
PERSOS["Kuki Shinobu"] = perso(on=[e("heal", 15, "A1 : PV à 50 % ou moins", d=True), e("em", 150, "C6 : PV sous 25 %", cm=6, d=True)])
PERSOS["Layla"] = deux([e("shield", 24, "A1 : 4 Étoiles de nuit (rideau)")])
PERSOS["Prune"] = perso(
    on=[e("atk_pct", 60, "Hexerei : Rite secret (2 Hexerei dans l'équipe)", d=True), e("atk_pct", 40, "C2 : Cloche de sorcière", cm=2), e("atk", 350, "C6 : Rallye du tintement", cm=6)],
    off=[e("dmg", 50, "A4 : Rallye du tintement, +0,025 % par ATQ au-dessus de 2000 (plafond à 4000)"), e("atk", 350, "C6 : Rallye du tintement", cm=6)])
PERSOS["Sandrone"] = perso(
    on=[e("em", 160, "A4 : +8 par 100 ATQ (plafond à 2000 ATQ)"), e("cd", 80, "C2 : +40 % puis 2 cumuls de +20 % (faisceaux de Fagio)", cm=2),
        e("base_stellar", 14, "Passif : DGT de base Conduction stellaire +0,7 % par 100 ATQ (14 % max)", g="stellar:base"),
        e("rx_stellar_conduct", 30, "C1", cm=1), e("rx_stellar_conduct", 20, "C6", cm=6)],
    off=[e("base_stellar", 14, "Passif : DGT de base Conduction stellaire +0,7 % par 100 ATQ (14 % max)", g="stellar:base"), e("rx_stellar_conduct", 30, "C1", cm=1)])
PERSOS["Sangonomiya Kokomi"] = perso(on=[e("dmg_HYDRO", 40, "C6 : après un soin à un allié à 80 % de PV ou plus", cm=6)])
PERSOS["Shikanoin Heizou"] = perso(
    on=[e("cr", 16, "C6 : 4 cumuls de +4 %", cm=6), e("cd", 32, "C6 : Conviction", cm=6)],
    off=[e("em", 80, "A4 : après Heartstopper Strike (équipe sans Heizou)")])
PERSOS["Skirk"] = perso(on=[e("atk_pct", 70, "C2 : 12,5 s après Havoc: Extinction", cm=2), e("atk_pct", 40, "C4 : 3 cumuls de Traversée de la mort", cm=4)])
PERSOS["Thoma"] = deux([e("shield", 25, "A1 : 5 cumuls de +5 %"), e(["dmg_na", "dmg_ca", "dmg_pa"], 15, "C6 : Cœur brûlant", cm=6)])
PERSOS["Varesa"] = perso(on=[e("atk_pct", 70, "A4 : 2 cumuls de +35 %"), e("cr", 10, "C6 : attaques plongeantes", cm=6), e("cd", 100, "C6 : attaques plongeantes", cm=6)])
PERSOS["Varka"] = perso(
    on=[e("dmg_ELEM", 25, "A1 : DGT Anémo (+10 % par 1000 ATQ, 25 % max)"), e("dmg_ELEM", 25, "A1 : DGT de l'élément du Tourbillon", pour=EL_SWIRL),
        e(["dmg_na", "dmg_ca"], 30, "A4 : 4 cumuls de +7,5 %"), e("cd", 80, "C6 : 4 cumuls de +20 %", cm=6),
        e("dmg_ELEM", 20, "C4 : après un Tourbillon", cm=4), e("dmg_ELEM", 20, "C4 : élément du Tourbillon", cm=4, pour=EL_SWIRL)],
    off=[e("dmg_ANEMO", 20, "C4 : après un Tourbillon", cm=4), e("dmg_ELEM", 20, "C4 : élément du Tourbillon", cm=4, pour=EL_SWIRL)])
PERSOS["Vesna"] = perso(on=[e("atk_pct", 18, "A4 : Tourbillon stellaire, 3 alliés Cryo ou Anémo"), e("atk_pct", 36, "C4 : bonus triplé (54 % au total)", cm=4),
                            e("em", 75, "A4 : 3 alliés d'un autre élément"), e("em", 150, "C4 : bonus triplé (225 au total)", cm=4),
                            e("atk_pct", 60, "C2 : 6 cumuls de Mesure disciplinaire", cm=2)])
PERSOS["Yanfei"] = perso(on=[e("dmg_PYRO", 15, "A1 : 3 Sceaux consommés par une attaque chargée"), e("dmg_PYRO", 5, "C6 : 4e Sceau", cm=6), e("cr", 20, "C2 : ennemi sous 50 % de PV", cm=2)])
PERSOS["Zibai"] = perso(
    on=[e("def_pct", 15, "A4 : par coéquipier Géo", par="meme"), e("em", 60, "A4 : par coéquipier Hydro (1 compté)"), e("rx_lunar_crystallize", 30, "C2 : nuance de phase lunaire", cm=2)],
    off=[e("rx_lunar_crystallize", 30, "C2 : nuance de phase lunaire", cm=2)])
