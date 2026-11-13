// Page « Équipes » : liste des équipes du compte, création, modification et suppression.
import { h, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { editeurEquipe } from "./editeur-equipe.js";
import { rangeeEquipe } from "./equipes.js";
import { jeton } from "./session.js";

export const USAGES = {
    genshin: ["Libre", "Abysses · première moitié", "Abysses · seconde moitié", "Théâtre de l'imaginarium", "Carnage chtonien"],
    starrail: ["Libre", "Salle oubliée", "Pure fiction", "Fantômes de l'apocalypse", "Arbitre de l'instable"],
};

export async function rendreEquipes(zone, cfg) {
    const [initiales, catalogue] = await Promise.all([
        rpc("equipes", { p_token: jeton(), p_jeu: cfg.jeu }),
        rpc("catalogue", { p_jeu: cfg.jeu }),
    ]);
    let equipes = initiales;
    let aSupprimer = null;

    function liste() {
        const entete = h("div", { classe: "barre-actions" },
            h("p", { classe: "doux" }, `${equipes.length} ${equipes.length > 1 ? "équipes enregistrées" : "équipe enregistrée"}`),
            h("button", { classe: "bouton bouton-principal", type: "button", onclick: () => formulaire(null) }, "Nouvelle équipe"));
        if (!equipes.length) {
            remplacer(zone, entete, h("div", { classe: "etat-vide" }, "Aucune équipe enregistrée. Créez-en une pour la réutiliser dans vos clears."));
            return;
        }
        remplacer(zone, entete, h("div", { classe: "equipes-liste" }, equipes.map((e) =>
            h("article", { classe: "equipe-carte" },
                h("div", { classe: "carte-tete" }, h("h3", {}, e.nom), h("span", { classe: "doux petit" }, e.usage || "")),
                rangeeEquipe(cfg.jeu, e.membres, cfg.prefixes),
                h("div", { classe: "carte-actions" },
                    h("button", { classe: "bouton bouton-petit", type: "button", onclick: () => formulaire(e) }, "Modifier"),
                    aSupprimer === e.id
                        ? [h("button", { classe: "bouton bouton-petit bouton-danger", type: "button", onclick: () => supprimer(e) }, "Confirmer la suppression"),
                            h("button", { classe: "bouton bouton-petit", type: "button", onclick: () => { aSupprimer = null; liste(); } }, "Garder")]
                        : h("button", { classe: "bouton bouton-petit bouton-danger", type: "button", onclick: () => { aSupprimer = e.id; liste(); } }, "Supprimer"))))));
    }

    async function supprimer(e) {
        try {
            equipes = await rpc("equipe_supprimer", { p_token: jeton(), p_jeu: cfg.jeu, p_id: e.id });
        } catch (err) {
            aSupprimer = null;
            liste();
            zone.prepend(h("p", { classe: "message erreur", role: "alert" }, err.message));
            return;
        }
        aSupprimer = null;
        liste();
    }

    function formulaire(existante) {
        const nom = h("input", { id: "equipe-nom", type: "text", maxlength: "40", value: existante?.nom || "", placeholder: "Nom de l'équipe" });
        const usage = h("select", { id: "equipe-usage" }, USAGES[cfg.jeu].map((u) => h("option", { value: u, selected: existante?.usage === u }, u)));
        const editeur = editeurEquipe({ jeu: cfg.jeu, catalogue, colonnes: 2 });
        if (existante) editeur.definir(existante.membres);
        const message = h("p", { classe: "message", role: "alert" });
        const enregistrer = h("button", { classe: "bouton bouton-principal", type: "submit" }, "Enregistrer l'équipe");
        remplacer(zone, h("form", {
            classe: "edition", novalidate: true,
            onsubmit: async (ev) => {
                ev.preventDefault();
                message.className = "message erreur";
                if (!nom.value.trim()) {
                    message.textContent = "Donnez un nom à l'équipe.";
                    return;
                }
                const membres = editeur.lire();
                if (!membres.length) {
                    message.textContent = "Choisissez au moins un personnage.";
                    return;
                }
                enregistrer.disabled = true;
                try {
                    equipes = await rpc("equipe_enregistrer", {
                        p_token: jeton(), p_jeu: cfg.jeu, p_id: existante?.id ?? null, p_nom: nom.value.trim(), p_usage: usage.value, p_membres: membres,
                    });
                    liste();
                } catch (err) {
                    message.textContent = err.message;
                    enregistrer.disabled = false;
                }
            },
        },
        h("header", { classe: "edition-tete" }, h("h2", {}, existante ? "Modifier l'équipe" : "Nouvelle équipe"),
            h("p", { classe: "doux" }, `Choisissez jusqu'à 4 personnages avec leur niveau, leurs doublons et ${cfg.jeu === "genshin" ? "l'arme équipée" : "le cône équipé"}.`)),
        h("div", { classe: "rangee-champs" },
            h("div", { classe: "champ champ-large" }, h("label", { for: "equipe-nom" }, "Nom de l'équipe"), nom),
            h("div", { classe: "champ" }, h("label", { for: "equipe-usage" }, "Usage"), usage)),
        editeur.element, message,
        h("div", { classe: "edition-actions" },
            h("button", { classe: "bouton", type: "button", onclick: liste }, "Annuler"), enregistrer)));
    }

    liste();
}
