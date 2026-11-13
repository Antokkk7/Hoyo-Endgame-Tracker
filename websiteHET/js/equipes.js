// Affichage des équipes : rangée de portraits avec niveau, doublons (C ou E) et arme (R ou S).
import { h } from "./dom.js";
import { portrait } from "./portraits.js";

// Les équipes d'endgame stockent les champs anglais de l'application (name, level...),
// les équipes enregistrées stockent des colonnes françaises (nom, niveau...).
export function normaliserMembre(m) {
    return {
        nom: m.name ?? m.nom ?? "",
        element: m.element ?? null,
        niveau: m.level ?? m.niveau ?? null,
        doublons: m.constellation ?? m.doublons ?? 0,
        arme: m.weapon ?? m.arme ?? "",
        raffinement: m.refinement ?? m.raffinement ?? 1,
    };
}

export function rangeeEquipe(jeu, membres, prefixes) {
    const rangee = h("div", { classe: "equipe" });
    for (const brut of membres || []) {
        const m = normaliserMembre(brut);
        let meta = `${prefixes.doublons}${m.doublons}`;
        if (m.arme) meta += ` · ${prefixes.raffinement}${m.raffinement}`;
        rangee.append(
            h("div", { classe: "membre", title: m.arme ? `${m.nom} - ${m.arme}` : m.nom },
                portrait(jeu, m.nom, { taille: 56, elementCle: m.element }),
                m.niveau ? h("span", { classe: "membre-niveau" }, `Niv.${m.niveau}`) : null,
                h("span", { classe: "membre-meta" }, meta)));
    }
    return rangee;
}

export function listeEquipes(jeu, equipes, prefixes) {
    const liste = h("div", { classe: "equipes-liste" });
    if (!equipes?.length) {
        liste.append(h("div", { classe: "etat-vide" }, "Aucune équipe enregistrée pour ce compte."));
        return liste;
    }
    for (const e of equipes) {
        liste.append(
            h("article", { classe: "equipe-carte" },
                h("div", { classe: "carte-tete" }, h("h3", {}, e.nom), h("span", { classe: "doux petit" }, e.usage || "")),
                rangeeEquipe(jeu, e.membres, prefixes)));
    }
    return liste;
}
