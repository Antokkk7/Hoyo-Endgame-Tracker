// Pastille de compte en bas à droite de chaque page, et fenêtre de connexion / création de compte.
import { h, remplacer } from "./dom.js";
import { rpc, ErreurApi } from "./api.js";
import { baseConfiguree } from "./config.js";
import { chargerManifeste, portrait } from "./portraits.js";
import { compte, effacerSession, enregistrerSession, estConnecte, jeton, majCompte } from "./session.js";

export const AVATAR_DEFAUT = { jeu: "genshin", nom: "Freminet" };

let pastille = null;
let menuOuvert = false;
let masquerSiDeconnecte = false;

// masquerDeconnecte : sur l'accueil, la section de connexion remplace le bouton de la pastille.
export function initConnexion({ masquerDeconnecte = false } = {}) {
    masquerSiDeconnecte = masquerDeconnecte;
    pastille = h("div", { classe: "pastille-compte" });
    document.body.append(pastille);
    window.addEventListener("het:session", dessiner);
    document.addEventListener("click", (e) => {
        if (menuOuvert && e.target.isConnected && !pastille.contains(e.target)) {
            menuOuvert = false;
            dessiner();
        }
    });
    chargerManifeste().then(dessiner);
    dessiner();
    if (estConnecte() && baseConfiguree()) {
        rpc("mon_compte", { p_token: jeton() }).then(majCompte).catch(() => {});
    }
}

function dessiner() {
    if (!pastille) return;
    const c = compte();
    if (!estConnecte() || !c) {
        menuOuvert = false;
        pastille.hidden = masquerSiDeconnecte;
        remplacer(pastille, h("button", { classe: "pastille-bouton", type: "button", onclick: () => ouvrirConnexion() }, "Connexion"));
        return;
    }
    pastille.hidden = false;
    const avatar = { jeu: c.avatar_jeu || AVATAR_DEFAUT.jeu, nom: c.avatar_nom || AVATAR_DEFAUT.nom };
    const bouton = h("button", {
        classe: "pastille-bouton", type: "button", "aria-expanded": String(menuOuvert),
        onclick: () => {
            menuOuvert = !menuOuvert;
            dessiner();
        },
    },
    portrait(avatar.jeu, avatar.nom, { taille: 30, rond: true }),
    h("span", {}, c.pseudo),
    c.role === "administrateur" ? h("span", { classe: "badge-role" }, "Admin") : null);
    const enfants = [bouton];
    if (menuOuvert) {
        enfants.unshift(h("div", { classe: "pastille-menu" },
            h("a", { href: "compte.html" }, "Mon compte"),
            h("button", { type: "button", onclick: deconnecter }, "Déconnexion")));
    }
    remplacer(pastille, ...enfants);
}

async function deconnecter() {
    const t = jeton();
    effacerSession();
    try {
        await rpc("deconnexion", { p_token: t });
    } catch {
        // la session locale est déjà effacée
    }
}

// Formulaire connexion / création de compte, utilisé par la fenêtre et par la section de l'accueil.
export function formulaireAuth({ surSucces, surAnnuler = null, idPrefixe = "f" }) {
    let mode = "connexion";
    const message = h("p", { classe: "message", role: "alert" });
    const pseudo = h("input", { id: `${idPrefixe}-pseudo`, type: "text", autocomplete: "username", required: true, maxlength: "24" });
    const mdp = h("input", { id: `${idPrefixe}-mdp`, type: "password", required: true, maxlength: "72" });
    const confirmation = h("input", { id: `${idPrefixe}-mdp2`, type: "password", autocomplete: "new-password", maxlength: "72" });
    const champConfirmation = h("div", { classe: "champ" }, h("label", { for: `${idPrefixe}-mdp2` }, "Confirmer le mot de passe"), confirmation);
    const onglets = h("div", { classe: "fenetre-onglets", role: "tablist" });
    const valider = h("button", { classe: "bouton bouton-principal", type: "submit" }, "Se connecter");

    function majMode() {
        valider.textContent = mode === "connexion" ? "Se connecter" : "Créer le compte";
        mdp.autocomplete = mode === "connexion" ? "current-password" : "new-password";
        champConfirmation.classList.toggle("cache", mode === "connexion");
        message.textContent = "";
        message.className = "message";
        remplacer(onglets,
            h("button", { type: "button", role: "tab", "aria-selected": String(mode === "connexion"), onclick: () => { mode = "connexion"; majMode(); } }, "Connexion"),
            h("button", { type: "button", role: "tab", "aria-selected": String(mode === "inscription"), onclick: () => { mode = "inscription"; majMode(); } }, "Créer un compte"));
    }

    const formulaire = h("form", {
        novalidate: true,
        onsubmit: async (e) => {
            e.preventDefault();
            message.className = "message";
            message.textContent = "";
            if (mode === "inscription" && mdp.value !== confirmation.value) {
                message.className = "message erreur";
                message.textContent = "Les deux mots de passe ne correspondent pas.";
                return;
            }
            valider.disabled = true;
            try {
                const resultat = mode === "connexion"
                    ? await rpc("connexion", { p_pseudo: pseudo.value.trim(), p_mdp: mdp.value })
                    : await rpc("creer_compte", { p_pseudo: pseudo.value.trim(), p_mdp: mdp.value });
                if (resultat?.erreur) throw new ErreurApi(resultat.erreur);
                enregistrerSession(resultat.token, resultat.compte);
                surSucces?.();
            } catch (err) {
                message.className = "message erreur";
                message.textContent = err.message;
            }
            valider.disabled = false;
        },
    },
    h("div", { classe: "champ" }, h("label", { for: `${idPrefixe}-pseudo` }, "Pseudo"), pseudo),
    h("div", { classe: "champ" }, h("label", { for: `${idPrefixe}-mdp` }, "Mot de passe"), mdp),
    champConfirmation,
    message,
    h("div", { classe: "fenetre-actions" },
        surAnnuler ? h("button", { classe: "bouton", type: "button", onclick: surAnnuler }, "Annuler") : null, valider));

    majMode();
    if (!baseConfiguree()) {
        message.className = "message erreur";
        message.textContent = "La base de données n'est pas encore configurée (js/config.js).";
    }
    return { element: h("div", {}, onglets, formulaire), focus: () => pseudo.focus() };
}

export function ouvrirConnexion() {
    if (document.querySelector(".fenetre-fond")) return;
    const fond = h("div", { classe: "fenetre-fond" });

    function fermer() {
        document.removeEventListener("keydown", surTouche);
        fond.remove();
    }

    function surTouche(e) {
        if (e.key === "Escape") fermer();
    }

    const auth = formulaireAuth({ surSucces: fermer, surAnnuler: fermer });
    fond.append(h("div", { classe: "fenetre", role: "dialog", "aria-modal": "true", "aria-label": "Connexion" }, h("h2", {}, "Connexion"), auth.element));
    fond.addEventListener("mousedown", (e) => {
        if (e.target === fond) fermer();
    });
    document.addEventListener("keydown", surTouche);
    document.body.append(fond);
    auth.focus();
}
