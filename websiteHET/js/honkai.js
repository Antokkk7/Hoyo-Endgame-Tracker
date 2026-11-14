// Page Honkai: Star Rail : historique de la Salle oubliée, de la Pure fiction, des Fantômes de l'apocalypse
// et de l'Arbitre de l'instable.
import { h, nombre, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { rangeeEquipe } from "./equipes.js";
import { formulaireApoc, formulaireArbitre, formulaireMoc, formulairePf } from "./edition-starrail.js";
import { initJeu, pastille, etoiles, sousOnglets, vide } from "./jeu-commun.js";
import { rendreSuiviStarRail } from "./suivi-starrail.js";
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

const TYPES = [
    { cle: "moc", titre: "Salle oubliée", carte: carteMoc, formulaire: formulaireMoc, enregistrer: "moc_enregistrer", vide: "Aucun clear de Salle oubliée enregistré." },
    { cle: "pf", titre: "Pure fiction", carte: cartePf, formulaire: formulairePf, enregistrer: "pf_enregistrer", vide: "Aucun clear de Pure fiction enregistré." },
    { cle: "apoc", titre: "Fantômes de l'apocalypse", carte: carteApoc, formulaire: formulaireApoc, enregistrer: "apoc_enregistrer", vide: "Aucun clear des Fantômes de l'apocalypse enregistré." },
    { cle: "arbitre", titre: "Arbitre de l'instable", carte: carteArbitre, formulaire: formulaireArbitre, enregistrer: "arbitre_enregistrer", vide: "Aucun clear de l'Arbitre enregistré." },
];

async function endgames(zone) {
    let donnees = await rpc("endgames_starrail", { p_token: jeton() });
    const catalogue = await rpc("catalogue", { p_jeu: JEU });
    let ongletCourant = 0;
    let aSupprimer = null;
    let erreur = null;

    function afficherListe() {
        sousOnglets(zone, TYPES.map((def) => ({ titre: def.titre, rendu: () => contenu(def) })), ongletCourant, (i) => {
            if (i !== ongletCourant) {
                ongletCourant = i;
                aSupprimer = null;
                erreur = null;
            }
        });
    }

    function contenu(def) {
        const clears = donnees[def.cle];
        return h("div", {},
            h("div", { classe: "barre-actions" },
                h("h2", { classe: "titre-historique" }, def.titre),
                h("button", { classe: "bouton bouton-principal", type: "button", onclick: () => formulaire(def, null) }, "Ajouter un clear")),
            erreur ? h("p", { classe: "message erreur", role: "alert" }, erreur) : null,
            clears.length
                ? h("div", { classe: "historique" }, clears.map((c) => {
                    const carte = def.carte(c);
                    carte.append(h("div", { classe: "carte-actions" },
                        h("button", { classe: "bouton bouton-petit", type: "button", onclick: () => formulaire(def, c) }, "Modifier"),
                        aSupprimer === c.id
                            ? [h("button", { classe: "bouton bouton-petit bouton-danger", type: "button", onclick: () => supprimer(def, c) }, "Confirmer la suppression"),
                                h("button", { classe: "bouton bouton-petit", type: "button", onclick: () => { aSupprimer = null; afficherListe(); } }, "Garder")]
                            : h("button", { classe: "bouton bouton-petit bouton-danger", type: "button", onclick: () => { aSupprimer = c.id; afficherListe(); } }, "Supprimer")));
                    return carte;
                }))
                : vide(def.vide));
    }

    async function supprimer(def, clear) {
        aSupprimer = null;
        erreur = null;
        try {
            donnees = await rpc("clear_starrail_supprimer", { p_token: jeton(), p_type: def.cle, p_id: clear.id });
        } catch (err) {
            erreur = err.message;
        }
        afficherListe();
    }

    function formulaire(def, existant) {
        remplacer(zone, def.formulaire({
            catalogue, equipes: donnees.equipes, existant,
            annuler: () => afficherListe(),
            surFin: async (payload) => {
                donnees = await rpc(def.enregistrer, { p_token: jeton(), p_id: existant?.id ?? null, p_data: payload });
                erreur = null;
                afficherListe();
            },
        }));
    }

    afficherListe();
}

initJeu({ jeu: JEU, nomJeu: "Honkai: Star Rail", prefixes: PREFIXES, endgames, suivi: rendreSuiviStarRail });
