// Éléments communs des formulaires d'ajout et de modification des clears (Genshin Impact et Honkai: Star Rail).
import { h } from "./dom.js";

export function champ(libelle, controle, classe = "") {
    return h("div", { classe: `champ ${classe}`.trim() }, h("label", {}, libelle), controle);
}

export function section(titre, ...contenu) {
    return h("section", { classe: "section-edition" }, h("h3", {}, titre), ...contenu);
}

export function nombreChamp(valeur, min, max, { largeur = null, id = null } = {}) {
    return h("input", { type: "number", min: String(min), max: String(max), value: String(valeur), id, style: largeur ? `max-width:${largeur}` : null });
}

export function entier(el, defaut = 0) {
    const n = parseInt(el.value, 10);
    return Number.isFinite(n) ? n : defaut;
}

// Enveloppe commune : titre, message d'erreur, boutons Annuler / Enregistrer.
export function cadre(titre, sousTitre, sections, { surFin, annuler, collecter, libelleEnregistrer }) {
    const message = h("p", { classe: "message", role: "alert" });
    const enregistrer = h("button", { classe: "bouton bouton-principal", type: "submit" }, libelleEnregistrer);
    const form = h("form", {
        classe: "edition", novalidate: true,
        onsubmit: async (e) => {
            e.preventDefault();
            message.className = "message";
            message.textContent = "";
            let donnees;
            try {
                donnees = collecter();
            } catch (err) {
                message.className = "message erreur";
                message.textContent = err.message;
                return;
            }
            enregistrer.disabled = true;
            try {
                await surFin(donnees);
            } catch (err) {
                message.className = "message erreur";
                message.textContent = err.message;
                enregistrer.disabled = false;
            }
        },
    },
    h("header", { classe: "edition-tete" }, h("h2", {}, titre), h("p", { classe: "doux" }, sousTitre)),
    sections, message,
    h("div", { classe: "edition-actions" },
        h("button", { classe: "bouton", type: "button", onclick: annuler }, "Annuler"), enregistrer));
    return form;
}
