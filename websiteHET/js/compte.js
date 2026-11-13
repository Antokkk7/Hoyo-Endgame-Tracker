// Page Compte : image de profil, pseudo et mot de passe.
import { h, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { AVATAR_DEFAUT, initConnexion } from "./connexion.js";
import { blocSynthese } from "./synthese.js";
import { chargerManifeste, listePortraits, portrait } from "./portraits.js";
import { compte, estConnecte, jeton, majCompte } from "./session.js";

function versConnexion() {
    location.replace("index.html#connexion");
}

if (!estConnecte()) versConnexion();

const racine = document.getElementById("compte");
let retour = null; // message à afficher après le redessin provoqué par une mise à jour du compte

function message() {
    return h("p", { classe: "message", role: "status" });
}

function afficherMessage(el, texte, erreur) {
    el.className = "message " + (erreur ? "erreur" : "succes");
    el.textContent = texte;
}

function blocIdentite() {
    const c = compte();
    return h("section", { classe: "bloc-compte identite" },
        portrait(c.avatar_jeu || AVATAR_DEFAUT.jeu, c.avatar_nom || AVATAR_DEFAUT.nom, { taille: 88, rond: true }),
        h("div", {}, h("h1", {}, c.pseudo), c.role === "administrateur" ? h("span", { classe: "badge-role" }, "Administrateur") : h("span", { classe: "doux petit" }, "Compte utilisateur")));
}

function blocAvatar() {
    let jeu = compte().avatar_jeu || AVATAR_DEFAUT.jeu;
    let filtre = "";
    const msg = message();
    const grille = h("div", { classe: "avatar-grille" });
    const onglets = h("div", { classe: "avatar-onglets", role: "tablist" });
    const recherche = h("input", { id: "recherche-avatar", type: "search", placeholder: "Rechercher un personnage", autocomplete: "off" });

    function dessiner() {
        const courant = compte();
        remplacer(onglets,
            [["genshin", "Genshin Impact"], ["starrail", "Honkai: Star Rail"]].map(([id, nom]) =>
                h("button", { type: "button", role: "tab", "aria-selected": String(jeu === id), onclick: () => { jeu = id; dessiner(); } }, nom)));
        const portraits = listePortraits(jeu).filter((p) => p.nom.toLowerCase().includes(filtre));
        if (!portraits.length) {
            remplacer(grille, h("p", { classe: "doux" }, "Aucune image disponible pour ce jeu."));
            return;
        }
        remplacer(grille, portraits.map((p) =>
            h("button", {
                classe: "avatar-choix", type: "button", title: p.nom,
                "aria-pressed": String((courant.avatar_jeu || AVATAR_DEFAUT.jeu) === jeu && (courant.avatar_nom || AVATAR_DEFAUT.nom) === p.nom),
                onclick: async () => {
                    try {
                        const maj = await rpc("modifier_avatar", { p_token: jeton(), p_jeu: jeu, p_nom: p.nom });
                        retour = "Image de profil enregistrée.";
                        majCompte(maj);
                    } catch (err) {
                        afficherMessage(msg, err.message, true);
                    }
                },
            }, portrait(jeu, p.nom, { taille: 52 }), h("span", {}, p.nom))));
    }

    recherche.addEventListener("input", () => {
        filtre = recherche.value.trim().toLowerCase();
        dessiner();
    });
    dessiner();
    return h("section", { classe: "bloc-compte" }, h("h2", {}, "Image de profil"), onglets,
        h("div", { classe: "champ", style: "max-width:320px;margin:0" }, h("label", { for: "recherche-avatar", classe: "sr-only" }, "Rechercher"), recherche), msg, grille);
}

function blocPseudo() {
    const msg = message();
    const champ = h("input", { id: "nouveau-pseudo", type: "text", maxlength: "24", required: true, value: compte().pseudo, autocomplete: "username" });
    return h("section", { classe: "bloc-compte" }, h("h2", {}, "Pseudo"),
        h("form", {
            novalidate: true,
            onsubmit: async (e) => {
                e.preventDefault();
                try {
                    const maj = await rpc("modifier_pseudo", { p_token: jeton(), p_pseudo: champ.value.trim() });
                    retour = "Pseudo modifié.";
                    majCompte(maj);
                } catch (err) {
                    afficherMessage(msg, err.message, true);
                }
            },
        }, h("div", { classe: "champ" }, h("label", { for: "nouveau-pseudo" }, "Nouveau pseudo"), champ), msg,
        h("button", { classe: "bouton bouton-principal", type: "submit" }, "Modifier le pseudo")));
}

function blocMotDePasse() {
    const msg = message();
    const ancien = h("input", { id: "mdp-ancien", type: "password", required: true, autocomplete: "current-password", maxlength: "72" });
    const nouveau = h("input", { id: "mdp-nouveau", type: "password", required: true, autocomplete: "new-password", maxlength: "72" });
    const confirmation = h("input", { id: "mdp-confirmation", type: "password", required: true, autocomplete: "new-password", maxlength: "72" });
    return h("section", { classe: "bloc-compte" }, h("h2", {}, "Mot de passe"),
        h("form", {
            novalidate: true,
            onsubmit: async (e) => {
                e.preventDefault();
                if (nouveau.value !== confirmation.value) return afficherMessage(msg, "Les deux nouveaux mots de passe ne correspondent pas.", true);
                try {
                    await rpc("modifier_mdp", { p_token: jeton(), p_ancien: ancien.value, p_nouveau: nouveau.value });
                    ancien.value = nouveau.value = confirmation.value = "";
                    afficherMessage(msg, "Mot de passe modifié. Vous restez connecté sur cet appareil ; les autres appareils sont déconnectés.", false);
                } catch (err) {
                    afficherMessage(msg, err.message, true);
                }
            },
        },
        h("div", { classe: "champ" }, h("label", { for: "mdp-ancien" }, "Mot de passe actuel"), ancien),
        h("div", { classe: "champ" }, h("label", { for: "mdp-nouveau" }, "Nouveau mot de passe (8 caractères minimum)"), nouveau),
        h("div", { classe: "champ" }, h("label", { for: "mdp-confirmation" }, "Confirmer le nouveau mot de passe"), confirmation),
        msg, h("button", { classe: "bouton bouton-principal", type: "submit" }, "Modifier le mot de passe")));
}

let synthese = null;

async function chargerSynthese(zone) {
    try {
        remplacer(zone, await blocSynthese());
    } catch (err) {
        remplacer(zone, h("section", { classe: "bloc-compte" }, h("h2", {}, "Derniers clears"), h("p", { classe: "doux" }, err.message || "Données indisponibles.")));
    }
}

function dessinerPage() {
    if (!estConnecte() || !compte()) return versConnexion();
    const info = h("p", { classe: "message succes", role: "status" }, retour || "");
    retour = null;
    const zoneSynthese = synthese ?? h("div", {});
    if (!synthese) {
        synthese = zoneSynthese;
        chargerSynthese(zoneSynthese);
    }
    remplacer(racine, info, h("div", { classe: "compte-grille" }, blocIdentite(), zoneSynthese, blocAvatar(), blocPseudo(), blocMotDePasse()));
}

initConnexion();
if (estConnecte()) {
    chargerManifeste().then(dessinerPage);
    window.addEventListener("het:session", dessinerPage);
}
