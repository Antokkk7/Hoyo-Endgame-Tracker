// Page Honkai: Star Rail : historique de la Salle oubliée, de la Pure fiction, des Fantômes de l'apocalypse
// et de l'Arbitre de l'instable.
import { h } from "./dom.js";
import { nombre } from "./dom.js";
import { rpc } from "./api.js";
import { rangeeEquipe } from "./equipes.js";
import { initJeu, pastille, etoiles, sousOnglets, vide } from "./jeu-commun.js";
import { bilanApoc, bilanArbitre, bilanMoc, cycles, maxSallePf, SCORE_BOSS, scoreSallePf } from "./regles.js";
import { jeton } from "./session.js";

const JEU = "starrail";
const PREFIXES = { doublons: "E", raffinement: "S" };

function equipes(donnees, prefixeTitre) {
    return h("div", {}, (donnees || []).filter((e) => e.length).map((e, i, tout) =>
        h("div", {}, tout.length > 1 ? h("div", { classe: "equipe-titre" }, `${prefixeTitre} ${i + 1}`) : null, rangeeEquipe(JEU, e, PREFIXES))));
}

function ligne(nom, ...valeurs) {
    return h("div", { classe: "ligne" }, h("span", { classe: "ligne-nom" }, nom), h("span", { classe: "ligne-valeur" }, ...valeurs));
}

function carteMoc(c) {
    const { total, cycles: cyclesTotaux } = bilanMoc(c);
    const salles = [8, 9, 10, 11].map((i) => {
        const auto = (i === 8 && c.auto9) || (i === 9 && c.auto10);
        const droite = [etoiles(c.etoiles[i] ?? 0, 3)];
        if (i === 11) droite.push(etoiles(c.stellaire ? 1 : 0, 1, "prisme"));
        droite.push(h("span", { classe: "doux petit" }, auto ? "auto clear" : `${c.cycles[i] ?? 0} cyc.`));
        return ligne(`Salle ${i + 1}`, ...droite);
    });
    return h("article", { classe: "carte" },
        h("div", { classe: "carte-tete" }, h("h3", {}, `Patch ${c.patch}`), h("span", { classe: "doux" }, cycles(cyclesTotaux)),
            h("span", { classe: "carte-total" }, `${total}`, h("small", {}, " / 36 ★"))),
        c.stellaire ? h("div", { classe: "pastilles" }, pastille("★ stellaire", "#C77DFF")) : null,
        h("div", { classe: "lignes" }, salles),
        equipes(c.equipes, "Équipe"));
}

function cartePf(c) {
    const maxSalle = (i) => maxSallePf(c, i);
    const score = (i) => scoreSallePf(c, i);
    return h("article", { classe: "carte" },
        h("div", { classe: "carte-tete" }, h("h3", {}, `Patch ${c.patch}`),
            h("span", { classe: "carte-total" }, nombre(score(3)), h("small", {}, ` / ${nombre(maxSalle(3))}`))),
        c.stellaire ? h("div", { classe: "pastilles" }, pastille("★ stellaire", "#C77DFF")) : null,
        h("div", { classe: "lignes" }, [0, 1, 2, 3].map((i) =>
            ligne(`Salle ${i + 1}`,
                h("strong", {}, (i === 0 && c.auto1) || (i === 1 && c.auto2) ? "auto clear" : nombre(score(i))),
                h("span", { classe: "doux petit" }, `/ ${nombre(maxSalle(i))}`)))),
        equipes(c.equipes, "Équipe"));
}

function carteApoc(c) {
    const { mode, total } = bilanApoc(c);
    return h("article", { classe: "carte" },
        h("div", { classe: "carte-tete" }, h("h3", {}, `Patch ${c.patch}`), h("span", { classe: "doux" }, mode.nom),
            h("span", { classe: "carte-total" }, nombre(total), h("small", {}, ` / ${nombre(mode.boss * SCORE_BOSS)}`))),
        c.mode === "D4_STELLAR" ? h("div", { classe: "pastilles" }, pastille("★ stellaire", "#C77DFF")) : null,
        h("div", { classe: "lignes" }, Array.from({ length: mode.boss }, (_, i) =>
            ligne(`Boss ${i + 1}`, h("strong", {}, nombre(c.scores[i] ?? 0)), h("span", { classe: "doux petit" }, `/ ${nombre(SCORE_BOSS)}`)))),
        equipes(c.equipes, "Boss"));
}

function carteArbitre(c) {
    const { etoilesRoi, etoilesCavaliers, total, cadre } = bilanArbitre(c);

    const lignes = [0, 1, 2].map((i) =>
        ligne(`Cavalier ${i + 1}`, h("span", { classe: "doux petit" }, `${cycles(c.cycles[i] ?? 0)}${c.ko[i] ? " · K.O." : ""}`), etoiles(etoilesCavaliers[i], 3)));
    lignes.push(ligne("Le King", h("span", { classe: "doux petit" }, `${cycles(c.cycles_roi)}${c.roi_ko ? " · K.O." : ""}`), etoiles(etoilesRoi, 3)));

    return h("article", { classe: "carte carte-arbitre", style: cadre ? `--cadre:${cadre.couleur}` : null },
        h("div", { classe: "carte-tete" }, h("h3", {}, `Patch ${c.patch}`),
            cadre ? h("span", { classe: "nom-cadre" }, cadre.nom) : h("span", { classe: "doux" }, "Pas de cadre"),
            h("span", { classe: "carte-total" }, `${total}`, h("small", {}, " / 12 ★"))),
        cadre ? h("div", { classe: "arbitre-cadre" },
            // Image facultative (assets/img/cadres/<cadre>.png) : masquée si le fichier n'existe pas.
            h("img", { src: new URL(`../assets/img/cadres/${cadre.image}`, import.meta.url).href, alt: cadre.nom, onerror: (e) => e.target.remove() }),
            etoiles(etoilesRoi, 3)) : null,
        h("div", { classe: "arbitre-cavaliers" }, etoilesCavaliers.map((n) => etoiles(n, 3))),
        h("div", { classe: "lignes", style: "margin-top:14px" }, lignes),
        equipes(c.equipes_cavaliers, "Cavalier"),
        c.equipe_roi?.length ? h("div", {}, h("div", { classe: "equipe-titre" }, "Le King"), rangeeEquipe(JEU, c.equipe_roi, PREFIXES)) : null);
}

function liste(elements, rendu, texteVide) {
    if (!elements.length) return vide(texteVide);
    return h("div", { classe: "historique" }, elements.map(rendu));
}

async function endgames(zone) {
    const d = await rpc("endgames_starrail", { p_token: jeton() });
    sousOnglets(zone, [
        { titre: "Salle oubliée", rendu: () => liste(d.moc, carteMoc, "Aucun clear de Salle oubliée enregistré.") },
        { titre: "Pure fiction", rendu: () => liste(d.pf, cartePf, "Aucun clear de Pure fiction enregistré.") },
        { titre: "Fantômes de l'apocalypse", rendu: () => liste(d.apoc, carteApoc, "Aucun clear des Fantômes de l'apocalypse enregistré.") },
        { titre: "Arbitre de l'instable", rendu: () => liste(d.arbitre, carteArbitre, "Aucun clear de l'Arbitre enregistré.") },
    ]);
}

initJeu({ jeu: JEU, nomJeu: "Honkai: Star Rail", prefixes: PREFIXES, endgames });
