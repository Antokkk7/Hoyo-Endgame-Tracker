# Armes de Genshin Impact dont le passif apporte un bonus à l'équipe (ou au porteur), avec les valeurs de R1 à R5.
# Valeurs en points de pourcentage. "v" est une liste de 5 valeurs (R1 à R5). Voir buffs_sets.py pour les champs.
from buffs_sets import e

ARMES = {
    "Elegy for the End": [
        e("atk_pct", [20, 25, 30, 35, 40], "Chant du millénaire : après réaction élémentaire", cible="equipe", g="millenial:atk"),
        e("em", [100, 125, 150, 175, 200], "Chant du millénaire", cible="equipe", g="millenial:em"),
    ],
    "Thrilling Tales of Dragon Slayers": [e("atk_pct", [24, 30, 36, 42, 48], "Après le changement de personnage", cible="equipe", g="ttds")],
    "Wolf's Gravestone": [
        e("atk_pct", [20, 25, 30, 35, 40], "Passif du porteur", cible="soi"),
        e("atk_pct", [40, 50, 60, 70, 80], "Coup sur un ennemi sous 30 % de PV", cible="equipe", g="wolf", d=True),
    ],
    "Song of Broken Pines": [
        e("atk_pct", [20, 25, 30, 35, 40], "Chant du millénaire", cible="equipe", g="millenial:atk"),
        e("autre", [12, 15, 18, 21, 24], "Vitesse d'attaque +12 à 24 %", cible="equipe", g="millenial:spd"),
    ],
    "Freedom-Sworn": [
        e("atk_pct", [20, 25, 30, 35, 40], "Chant du millénaire", cible="equipe", g="millenial:atk"),
        e("dmg_na", [16, 20, 24, 28, 32], "Chant du millénaire", cible="equipe", g="millenial:dmg"),
        e("dmg_ca", [16, 20, 24, 28, 32], "Chant du millénaire", cible="equipe", g="millenial:dmg"),
        e("dmg_pa", [16, 20, 24, 28, 32], "Chant du millénaire", cible="equipe", g="millenial:dmg"),
    ],
    "Key of Khaj-Nisut": [e("em", [80, 100, 120, 140, 160], "0,2 à 0,4 % des PV du porteur (≈ 40 000 PV)", cible="equipe", g="khaj")],
    "Hakushin Ring": [e("dmg_ELEM", [10, 12.5, 15, 17.5, 20], "Après une réaction élémentaire Électro", cible="equipe", g="hakushin")],
    "Peak Patrol Song": [e("dmg_ELEM", [10, 12.5, 15, 17.5, 20], "À 2 cumuls", cible="equipe", g="patrol")],
    "Sapwood Blade": [e("em", [60, 75, 90, 105, 120], "Après une réaction Dendro", cible="equipe", g="leafcon")],
    "Forest Regalia": [e("em", [60, 75, 90, 105, 120], "Après une réaction Dendro", cible="equipe", g="leafcon")],
}
