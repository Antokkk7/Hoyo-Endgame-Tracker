# Données des sets d'artéfacts de Genshin Impact (jusqu'aux sets présents dans les données Enka) et de leurs bonus.
# Utilisé par generer_buffs.py. Valeurs en points de pourcentage (20 = +20 %) ; em, atq, pv, def en valeur brute.
#
# Champs d'un effet :
#   s      statistique (ou liste) : hp_pct atk_pct def_pct hp atk def em cr cd er dmg dmg_PYRO.. dmg_PHYS dmg_ELEM dmg_na dmg_ca dmg_pa dmg_skill dmg_burst
#          heal shield res_PYRO.. res_ELEM (réduction de RÉS de l'ennemi) def_red autre (effet non chiffré sur le panneau)
#   v      valeur ; f = {b: stat de base, k: coefficient, mx: plafond} pour un effet proportionnel à une statistique
#   c      condition (texte) ; d = désactivé par défaut ; cible : "soi" ou "equipe" ; g : groupe de non-cumul
#   perm   déjà inclus dans les statistiques d'Enka (bonus permanent du set)
#   tw     types d'arme du porteur pour lesquels l'effet s'applique
#   par    multiplicateur : "meme" ou "autre" = nombre de coéquipiers de même élément / d'élément différent (3 maximum)


def e(s, v=None, c=None, **kw):
    d = {"s": s}
    if v is not None:
        d["v"] = v
    if c:
        d["c"] = c
    d.update(kw)
    return d


def v70(maximum):
    return round(maximum * 0.7, 1)


EL4 = ["PYRO", "HYDRO", "ELECTRO", "CRYO"]
RES4 = ["res_PYRO", "res_HYDRO", "res_ELECTRO", "res_CRYO"]

# (identifiant du jeu, nom anglais, effets à 2 pièces, effets à 4 pièces)
SETS = [
    (10001, "Resolution of Sojourner", [e("atk_pct", 18, perm=True)], [e("autre", 30, "Taux CRIT des attaques chargées +30 %")]),
    (10002, "Brave Heart", [e("atk_pct", 18, perm=True)], [e("dmg", 30, "Ennemi avec plus de 50 % de PV")]),
    (10003, "Defender's Will", [e("def_pct", 30, perm=True)], [e("autre", None, "RÉS élémentaires +30 % pour chaque élément de l'équipe")]),
    (10004, "Tiny Miracle", [e("autre", None, "Toutes les RÉS élémentaires +20 %")], [e("autre", None, "RÉS +30 % à l'élément du dernier dégât subi")]),
    (10005, "Berserker", [e("cr", 12, perm=True)], [e("cr", 24, "PV inférieurs à 70 %")]),
    (10006, "Martial Artist", [e("dmg_na", 15), e("dmg_ca", 15)], [e("dmg_na", 25, "Après la compétence"), e("dmg_ca", 25, "Après la compétence")]),
    (10007, "Instructor", [e("em", 80, perm=True)], [e("em", 120, "Après une réaction élémentaire", cible="equipe", g="inst4")]),
    (10008, "Gambler", [e("dmg_skill", 20)], [e("autre", None, "Chance de réinitialiser le temps de recharge de la compétence")]),
    (10009, "The Exile", [e("er", 20, perm=True)], [e("autre", None, "Après le déchaînement : énergie aux coéquipiers", cible="equipe", g="exile4")]),
    (10010, "Adventurer", [e("hp", 1000, perm=True)], [e("autre", None, "Soin à l'ouverture d'un coffre")]),
    (10011, "Lucky Dog", [e("def", 100, perm=True)], [e("autre", None, "Soin à la collecte de Mora")]),
    (10012, "Scholar", [e("er", 20, perm=True)], [e("autre", None, "Énergie aux archers et catalyseurs")]),
    (10013, "Traveling Doctor", [e("autre", None, "Soins reçus +20 %")], [e("autre", None, "Soin après le déchaînement")]),
    (14001, "Blizzard Strayer", [e("dmg_CRYO", 15, perm=True)], [e("cr", 20, "Ennemi affecté par le Cryo"), e("cr", 20, "Ennemi gelé", d=True)]),
    (14002, "Thundersoother", [e("autre", None, "RÉS Électro +40 %")], [e("dmg", 35, "Ennemi affecté par l'Électro")]),
    (14003, "Lavawalker", [e("autre", None, "RÉS Pyro +40 %")], [e("dmg", 35, "Ennemi affecté par le Pyro")]),
    (14004, "Maiden Beloved", [e("autre", None, "Bonus de soins +15 %", perm=True)], [e("autre", None, "Soins reçus +20 % après la compétence ou le déchaînement", cible="equipe", g="maiden4")]),
    (15001, "Gladiator's Finale", [e("atk_pct", 18, perm=True)], [e("dmg_na", 35, tw=["sword", "claymore", "polearm"])]),
    (15002, "Viridescent Venerer", [e("dmg_ANEMO", 15, perm=True)], [e("autre", 60, "Dégâts de Tourbillon +60 %"), e(RES4, 40, "Après un Tourbillon : RÉS de l'élément réduite de 40 %", cible="equipe", g="vv4")]),
    (15003, "Wanderer's Troupe", [e("em", 80, perm=True)], [e("dmg_ca", 35, tw=["catalyst", "bow"])]),
    (15004, "Glacier and Snowfield", [e("dmg_CRYO", 15, perm=True)], [e("cr", 20, "Ennemi affecté par le Cryo"), e("cr", 20, "Ennemi gelé", d=True)]),
    (15005, "Thundering Fury", [e("dmg_ELECTRO", 15, perm=True)], [e("autre", 40, "Surcharge, Électrocution, Supraconduction, Hyperfloraison +40 % ; Amplification +20 %")]),
    (15006, "Crimson Witch of Flames", [e("dmg_PYRO", 15, perm=True)], [e("dmg_PYRO", v70(22.5), "+7,5 % par utilisation de compétence (3 cumuls max, 70 % du max)"), e("autre", 40, "Surcharge et Brûlure +40 % ; Vaporisation et Fonte +15 %")]),
    (15007, "Noblesse Oblige", [e("dmg_burst", 20)], [e("atk_pct", 20, "Après le déchaînement", cible="equipe", g="no4")]),
    (15008, "Bloodstained Chivalry", [e("dmg_PHYS", 25, perm=True)], [e("dmg_ca", 50, "Après avoir vaincu un ennemi")]),
    (15009, "Prayers for Illumination", [], [e("autre", None, "Durée des effets Pyro subis réduite de 40 %")]),
    (15010, "Prayers for Destiny", [], [e("autre", None, "Durée des effets Hydro subis réduite de 40 %")]),
    (15011, "Prayers for Wisdom", [], [e("autre", None, "Durée des effets Électro subis réduite de 40 %")]),
    (15012, "Prayers to the Firmament", [], [e("autre", None, "Durée des effets Anémo subis réduite de 40 %")]),
    (15013, "Prayers to Springtime", [], [e("autre", None, "Durée des effets Cryo subis réduite de 40 %")]),
    (15014, "Archaic Petra", [e("dmg_GEO", 15, perm=True)], [e("dmg_ELEM", 35, "Après la collecte d'un éclat de cristallisation : bonus de l'élément de l'éclat", cible="equipe", g="ap4")]),
    (15015, "Retracing Bolide", [e("shield", 35)], [e("dmg_na", 40, "Avec un bouclier actif"), e("dmg_ca", 40, "Avec un bouclier actif")]),
    (15016, "Heart of Depth", [e("dmg_HYDRO", 15, perm=True)], [e("dmg_na", 30, "Après la compétence"), e("dmg_ca", 30, "Après la compétence")]),
    (15017, "Tenacity of the Millelith", [e("hp_pct", 20, perm=True)], [e("atk_pct", 20, "Après un coup de compétence", cible="equipe", g="totm4"), e("shield", 30, "Après un coup de compétence", cible="equipe", g="totm4")]),
    (15018, "Pale Flame", [e("dmg_PHYS", 25, perm=True)], [e("atk_pct", 18, "2 cumuls après la compétence"), e("dmg_PHYS", 25, "2 cumuls")]),
    (15019, "Shimenawa's Reminiscence", [e("atk_pct", 18, perm=True)], [e("dmg_na", 50, "Avec 15 d'énergie consommés"), e("dmg_ca", 50, "Avec 15 d'énergie consommés"), e("dmg_pa", 50, "Avec 15 d'énergie consommés")]),
    (15020, "Emblem of Severed Fate", [e("er", 20, perm=True)], [e("dmg_burst", None, "25 % de la Recharge d'énergie (75 % max)", f={"b": "er", "k": 0.25, "mx": 75})]),
    (15021, "Husk of Opulent Dreams", [e("def_pct", 30, perm=True)], [e("def_pct", v70(24), "+6 % par cumul (4 max, 70 % du max)"), e("dmg_GEO", v70(24), "+6 % par cumul (4 max, 70 % du max)")]),
    (15022, "Ocean-Hued Clam", [e("autre", None, "Bonus de soins +15 %", perm=True)], [e("autre", None, "Bulle de dégâts après un soin")]),
    (15023, "Vermillion Hereafter", [e("atk_pct", 18, perm=True)], [e("atk_pct", 8, "Après le déchaînement"), e("atk_pct", v70(40), "+10 % par PV perdu (4 cumuls max, 70 % du max)")]),
    (15024, "Echoes of an Offering", [e("atk_pct", 18, perm=True)], [e("autre", None, "Attaque normale : +70 % ATQ avec une probabilité de 36 % (jusqu'à 50 %)")]),
    (15025, "Deepwood Memories", [e("dmg_DENDRO", 15, perm=True)], [e("res_DENDRO", 30, "Après compétence ou déchaînement : RÉS Dendro de l'ennemi -30 %", cible="equipe", g="dm4")]),
    (15026, "Gilded Dreams", [e("em", 80, perm=True)], [e("atk_pct", 14, "+14 % ATQ par coéquipier de même élément (3 max)", par="meme"), e("em", 50, "+50 EM par coéquipier d'élément différent (3 max)", par="autre")]),
    (15027, "Desert Pavilion Chronicle", [e("dmg_ANEMO", 15, perm=True)], [e("dmg_na", 40, "Après une attaque chargée"), e("dmg_ca", 40, "Après une attaque chargée"), e("dmg_pa", 40, "Après une attaque chargée")]),
    (15028, "Flower of Paradise Lost", [e("em", 80, perm=True)], [e("autre", v70(140), "Floraison, Hyperfloraison, Brasier +40 % (+10 % par cumul, 4 max)")]),
    (15029, "Nymph's Dream", [e("dmg_HYDRO", 15, perm=True)], [e("atk_pct", v70(25), "Cumuls de coups : ATQ +7/16/25 % (70 % du max)"), e("dmg_HYDRO", v70(15), "Cumuls de coups : +4/9/15 % (70 % du max)")]),
    (15030, "Vourukasha's Glow", [e("hp_pct", 20, perm=True)], [e("dmg_skill", 10), e("dmg_burst", 10), e("dmg_skill", v70(40), "+8 % par cumul après dégâts subis (5 max, 70 % du max)"), e("dmg_burst", v70(40), "+8 % par cumul après dégâts subis (5 max, 70 % du max)")]),
    (15031, "Marechaussee Hunter", [e("dmg_na", 15), e("dmg_ca", 15)], [e("cr", v70(36), "+12 % par cumul (3 max, 70 % du max)")]),
    (15032, "Golden Troupe", [e("dmg_skill", 20)], [e("dmg_skill", 25), e("dmg_skill", 25, "Porteur en dehors du terrain")]),
    (15033, "Song of Days Past", [e("autre", None, "Bonus de soins +15 %", perm=True)], [e("autre", None, "Après un soin : dégâts de l'équipe +8 % des PV soignés (15 000 max)", cible="equipe", g="sdp4")]),
    (15034, "Nighttime Whispers in the Echoing Woods", [e("atk_pct", 18, perm=True)], [e("dmg_GEO", 20, "Après la compétence"), e("dmg_GEO", 30, "Après cristallisation (en plus)")]),
    (15035, "Fragment of Harmonic Whimsy", [e("atk_pct", 18, perm=True)], [e("dmg", v70(54), "+18 % par modification de PV (3 cumuls max, 70 % du max)")]),
    (15036, "Unfinished Reverie", [e("atk_pct", 18, perm=True)], [e("dmg", v70(50), "+10 % par cumul (5 max, 70 % du max)")]),
    (15037, "Scroll of the Hero of Cinder City", [e("autre", None, "Pas de bonus à 2 pièces")], [e("dmg_ELEM", 12, "Après une réaction élémentaire", cible="equipe", g="scroll4"), e("dmg_ELEM", 28, "Avec Bénédiction de l'âme nocturne (en plus)", cible="equipe", g="scroll4ns", d=True)]),
    (15038, "Obsidian Codex", [e("dmg", 15, "Avec Bénédiction de l'âme nocturne", d=True)], [e("cr", 40, "Après consommation de points d'âme nocturne", d=True)]),
    (15039, "Long Night's Oath", [e("dmg_pa", 25)], [e("dmg_pa", v70(75), "+15 % par cumul (5 max, 70 % du max)")]),
    (15040, "Finale of the Deep Galleries", [e("dmg_CRYO", 15, perm=True)], [e("dmg_na", 60, "Énergie à 0 (sans déchaînement récent)"), e("dmg_burst", 60, "Énergie à 0 (sans attaque normale récente)", d=True)]),
    (15041, "Night of the Sky's Unveiling", [e("em", 80, perm=True)], [e("cr", 15, "Signe lunaire : Lueur de la lune"), e("autre", 10, "Dégâts lunaires +10 % (équipe)", cible="equipe", g="nsu4")]),
    (15042, "Silken Moon's Serenade", [e("er", 20, perm=True)], [e("em", 60, "Signe lunaire : Lueur de la lune (120 avec 2)", cible="equipe", g="sms4"), e("autre", 10, "Dégâts lunaires +10 % (équipe)", cible="equipe", g="sms4")]),
    (15043, "Aubade of Morningstar and Moon", [e("em", 80, perm=True)], [e("autre", 20, "Dégâts lunaires +20 % (40 % avec Lueur de la lune)")]),
    (15044, "A Day Carved From Rising Winds", [e("atk_pct", 18, perm=True)], [e("atk_pct", 25, "Effet du set 4 pièces actif")]),
    (15045, "Celestial Gift", [e("er", 20, perm=True)], [e("dmg_ELEM", 20, "Hexerei : bonus de l'élément du porteur", cible="equipe", g="cg4", d=True)]),
    (15046, "Disenchantment in Deep Shadow", [e("atk_pct", 18, perm=True)], [e("autre", 80, "Supraconduction +80 % ; Conduction stellaire +40 %"), e("cr", 16, "Sur coup, condition du set", d=True)]),
    (15047, "Scarlet Proof", [e("atk_pct", 18, perm=True)], [e("cr", 16, "Condition du set 4 pièces"), e("autre", 40, "Tourbillon stellaire +40 %")]),
    (15048, "Heart of the Furnace", [e("atk_pct", 18, perm=True)], [e("atk_pct", 12, "Condition stellaire"), e("autre", 50, "Réactions stellaires +50 % (équipe)", cible="equipe", g="hotf4")]),
]
