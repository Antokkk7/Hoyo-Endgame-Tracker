// Page Genshin Impact : historique des Abysses spiralées, du Théâtre de l'imaginarium et du Carnage chtonien.
import { h } from "./dom.js";
import { rpc } from "./api.js";
import { rangeeEquipe, listeEquipes, normaliserMembre } from "./equipes.js";
import { initJeu, pastille, pastilleElement, etoiles, sousOnglets, vide } from "./jeu-commun.js";
import { portrait } from "./portraits.js";
import { bilanAbysse, bilanTheatre, couleurDifficulte, MOIS } from "./regles.js";
import { jeton } from "./session.js";

const JEU = "genshin";
const PREFIXES = { doublons: "C", raffinement: "R" };

function carteAbysse(a) {
    const { total, max } = bilanAbysse(a);
    return h("article", { classe: "carte" },
        h("div", { classe: "carte-tete" },
            h("h3", {}, `Patch ${a.patch}`),
            h("span", { classe: "doux" }, a.rotation || ""),
            h("span", { classe: "carte-total" }, `${total}`, h("small", {}, ` / ${max} ★`))),
        h("div", { classe: "pastilles" }, pastille(`Étage ${a.etage}`, "#3FD0C0")),
        h("div", { classe: "lignes" }, [a.salle1, a.salle2, a.salle3].map((s, i) =>
            h("div", { classe: "ligne" }, h("span", { classe: "ligne-nom" }, `Salle ${i + 1}`),
                h("span", { classe: "ligne-valeur" }, etoiles(s, 3))))),
        rangeeEquipe(JEU, a.equipe, PREFIXES));
}

function carteTheatre(t) {
    const { mode, flags, total: compteur } = bilanTheatre(t);
    const rangee = h("span", { classe: "etoiles" }, flags.map((on, i) =>
        h("span", { classe: `etoile ${i < 10 ? "bleue" : ""} ${on ? "on" : ""}`.replace(/\s+/g, " ").trim(), "aria-hidden": "true" }, "★")));
    return h("article", { classe: "carte carte-theatre" },
        h("div", { classe: "carte-tete" },
            h("h3", {}, `${MOIS[t.mois - 1] || t.mois} ${t.annee}`),
            h("span", { classe: "doux" }, t.patch ? `Patch ${t.patch}` : ""),
            h("span", { classe: "carte-total" }, `${compteur}`, h("small", {}, ` / ${flags.length} ★`))),
        h("div", { classe: "pastilles" },
            pastille(mode.nom, "#3FD0C0"),
            t.arcanum ? pastille(`Arcanum ${t.arcanum}`, "#C77DFF") : null,
            t.meilleur_acte ? pastille(`Meilleur acte ${t.meilleur_acte}`, "#FFC83D") : null,
            (t.elements || []).map(pastilleElement)),
        h("div", { classe: "lignes" }, h("div", { classe: "ligne" }, h("span", { classe: "ligne-nom" }, "Astres"), h("span", { classe: "ligne-valeur" }, rangee))),
        t.mvp ? mvp(t) : null);
}

function mvp(t) {
    const m = normaliserMembre(t.mvp);
    return h("div", { classe: "mvp" },
        portrait(JEU, m.nom, { taille: 52, elementCle: m.element }),
        h("div", {}, h("strong", {}, m.nom), h("div", { classe: "doux petit" }, t.mvp_pret ? "MVP · personnage de prêt" : "MVP")));
}

function carteCarnage(c) {
    const couleur = couleurDifficulte(c.difficulte);
    return h("article", { classe: "carte" },
        h("div", { classe: "carte-tete" },
            h("h3", {}, `Patch ${c.patch}`),
            h("span", { classe: "doux" }, c.periode || ""),
            h("span", { classe: "badge-difficulte", style: `--couleur:${couleur};margin-left:auto`, title: `Difficulté ${c.difficulte}` }, String(c.difficulte))),
        (c.boss || []).map((b) =>
            h("div", { classe: "carnage-boss" },
                h("div", { classe: "carnage-boss-tete" },
                    h("strong", {}, String(b.name || "").replace(/([a-z])([A-Z])/g, "$1 $2")),
                    h("span", { classe: "carnage-temps" }, `${b.seconds} s`)),
                rangeeEquipe(JEU, b.team, PREFIXES))));
}

function liste(elements, rendu, texteVide) {
    if (!elements.length) return vide(texteVide);
    return h("div", { classe: "historique" }, elements.map(rendu));
}

async function endgames(zone) {
    const donnees = await rpc("endgames_genshin", { p_token: jeton() });
    sousOnglets(zone, [
        { titre: "Abysses spiralées", rendu: () => liste(donnees.abysses, carteAbysse, "Aucun clear d'Abysse enregistré.") },
        { titre: "Théâtre de l'imaginarium", rendu: () => liste(donnees.theatre, carteTheatre, "Aucun clear de Théâtre enregistré.") },
        { titre: "Carnage chtonien", rendu: () => liste(donnees.carnage, carteCarnage, "Aucun clear de Carnage enregistré.") },
        { titre: "Équipes", rendu: () => listeEquipes(JEU, donnees.equipes, PREFIXES) },
    ]);
}

initJeu({ jeu: JEU, nomJeu: "Genshin Impact", prefixes: PREFIXES, endgames });
