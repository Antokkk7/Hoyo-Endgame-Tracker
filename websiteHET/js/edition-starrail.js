// Formulaires d'ajout et de modification des clears Honkai: Star Rail : Salle oubliée, Pure fiction, Fantômes de l'apocalypse
// et Arbitre de l'instable. Chaque formulaire remplace la liste dans la page ; surFin(donnees) enregistre.
import { h, remplacer } from "./dom.js";
import { cadre, champ, entier, nombreChamp, section } from "./edition-commun.js";
import { editeurEquipe } from "./editeur-equipe.js";
import { MODES_APOC, SCORE_BOSS, SCORE_SALLE, SCORE_SALLE_STELLAIRE } from "./regles.js";

function caseACocher(libelle, coche) {
    const entree = h("input", { type: "checkbox", checked: Boolean(coche) });
    return { entree, element: h("label", { classe: "case-edition" }, entree, h("span", {}, libelle)) };
}

function champPatch(existant) {
    return h("input", { type: "text", value: existant?.patch || "", placeholder: "ex. 4.0", maxlength: "6", style: "max-width:120px" });
}

function verifierPatch(patch) {
    if (!patch.value.trim()) throw new Error("Indiquez le patch (ex. 4.0).");
    return patch.value.trim();
}

// n éditeurs d'équipe titrés, préremplis avec les équipes du clear existant.
function editeursEquipes({ catalogue, equipes, titre, existantes, n }) {
    const editeurs = Array.from({ length: n }, (_, i) => {
        const editeur = editeurEquipe({ jeu: "starrail", catalogue, colonnes: 2, modeles: equipes });
        if (existantes?.[i]?.length) editeur.definir(existantes[i]);
        return editeur;
    });
    const blocs = editeurs.map((e, i) => h("div", { classe: "equipe-edition" }, h("h4", {}, titre(i)), e.element));
    return { editeurs, blocs, lire: () => editeurs.map((e) => e.lire()).filter((t) => t.length) };
}

const AIDE_EQUIPE = h("p", { classe: "doux petit" }, "Choisir une équipe enregistrée remplit les 4 emplacements, elle reste toutefois modifiable. Les équipes vides ne sont pas enregistrées.");

export function formulaireMoc({ catalogue, equipes, existant, surFin, annuler }) {
    const patch = champPatch(existant);
    const stellaire = caseACocher("Étoile stellaire obtenue", existant?.stellaire);
    const total = h("strong", { classe: "total-vert" });
    const salles = [8, 9, 10, 11].map((i) => {
        const auto = i === 8 ? caseACocher("Auto clear", existant?.auto9) : i === 9 ? caseACocher("Auto clear", existant?.auto10) : null;
        const etoiles = nombreChamp(existant?.etoiles?.[i] ?? 3, 0, 3, { largeur: "80px" });
        const cycles = nombreChamp(existant?.cycles?.[i] ?? 0, 0, 99, { largeur: "80px" });
        return { auto, etoiles, cycles };
    });
    function majTotal() {
        const base = 24;
        const somme = salles.reduce((a, s) => a + (s.auto?.entree.checked ? 3 : entier(s.etoiles)), 0);
        total.textContent = `${base + somme} / 36 ★${stellaire.entree.checked ? " + ★" : ""}`;
    }
    function majAuto() {
        for (const s of salles) {
            if (!s.auto) continue;
            const actif = s.auto.entree.checked;
            s.etoiles.disabled = actif;
            s.cycles.disabled = actif;
            if (actif) {
                s.etoiles.value = "3";
                s.cycles.value = "0";
            }
        }
        majTotal();
    }
    salles.forEach((s) => {
        s.etoiles.addEventListener("input", majTotal);
        s.auto?.entree.addEventListener("change", majAuto);
    });
    stellaire.entree.addEventListener("change", majTotal);
    majAuto();
    const eq = editeursEquipes({ catalogue, equipes, titre: (i) => `Équipe ${i + 1}`, existantes: existant?.equipes, n: 2 });

    return cadre(existant ? "Modifier le clear" : "Ajouter un clear", "Salle oubliée : étoiles et cycles des salles 9 à 12, puis les équipes.", [
        section("1. Patch et salles",
            h("div", { classe: "rangee-champs" }, champ("Patch", patch), champ("Stellaire", stellaire.element), champ("Total", total)),
            h("div", { classe: "rangee-champs" }, salles.flatMap((s, k) => [
                champ(`Salle ${k + 9} : étoiles (/3)`, s.etoiles), champ(`Salle ${k + 9} : cycles`, s.cycles), s.auto ? champ("Salle " + (k + 9), s.auto.element) : null,
            ].filter(Boolean))),
            h("p", { classe: "doux petit" }, "Les salles 1 à 8 sont comptées à 3 étoiles. « Auto clear » met la salle à 3 étoiles sans cycle.")),
        section("2. Équipes utilisées", eq.blocs, AIDE_EQUIPE),
    ], {
        surFin, annuler, libelleEnregistrer: "Enregistrer",
        collecter: () => ({
            patch: verifierPatch(patch), stars: salles.map((s) => (s.auto?.entree.checked ? 3 : entier(s.etoiles))), cycles: salles.map((s) => (s.auto?.entree.checked ? 0 : entier(s.cycles))),
            stellar: stellaire.entree.checked, auto9: salles[0].auto.entree.checked, auto10: salles[1].auto.entree.checked, teams: eq.lire(),
        }),
    });
}

export function formulairePf({ catalogue, equipes, existant, surFin, annuler }) {
    const patch = champPatch(existant);
    const stellaire = caseACocher("Étoile stellaire obtenue", existant?.stellaire);
    const autos = [caseACocher("Auto clear", existant?.auto1), caseACocher("Auto clear", existant?.auto2), null, null];
    const scores = [0, 1, 2, 3].map((i) => nombreChamp(existant?.scores?.[i] ?? 0, 0, i === 3 ? SCORE_SALLE_STELLAIRE : SCORE_SALLE, { largeur: "130px" }));
    const total = h("strong", { classe: "total-vert" });
    function majTotal() {
        const somme = scores.reduce((a, s, i) => a + (autos[i]?.entree.checked ? SCORE_SALLE : entier(s)), 0);
        total.textContent = `${new Intl.NumberFormat("fr-FR").format(somme)} / ${new Intl.NumberFormat("fr-FR").format(SCORE_SALLE * 3 + (stellaire.entree.checked ? SCORE_SALLE_STELLAIRE : SCORE_SALLE))}`;
    }
    function majAuto() {
        autos.forEach((a, i) => {
            if (!a) return;
            scores[i].disabled = a.entree.checked;
            if (a.entree.checked) scores[i].value = String(SCORE_SALLE);
        });
        scores[3].max = String(stellaire.entree.checked ? SCORE_SALLE_STELLAIRE : SCORE_SALLE);
        majTotal();
    }
    scores.forEach((s) => s.addEventListener("input", majTotal));
    autos.forEach((a) => a?.entree.addEventListener("change", majAuto));
    stellaire.entree.addEventListener("change", majAuto);
    majAuto();
    const eq = editeursEquipes({ catalogue, equipes, titre: (i) => `Équipe ${i + 1}`, existantes: existant?.equipes, n: 2 });

    return cadre(existant ? "Modifier le clear" : "Ajouter un clear", "Pure fiction : score de chaque salle, puis les équipes.", [
        section("1. Patch et scores",
            h("div", { classe: "rangee-champs" }, champ("Patch", patch), champ("Stellaire", stellaire.element), champ("Total", total)),
            h("div", { classe: "rangee-champs" }, scores.flatMap((s, i) => [
                champ(`Salle ${i + 1} (/${new Intl.NumberFormat("fr-FR").format(i === 3 ? SCORE_SALLE_STELLAIRE : SCORE_SALLE)})`, s), autos[i] ? champ(`Salle ${i + 1}`, autos[i].element) : null,
            ].filter(Boolean))),
            h("p", { classe: "doux petit" }, "La salle 4 monte à 120 000 avec l'étoile stellaire. « Auto clear » compte 80 000 points.")),
        section("2. Équipes utilisées", eq.blocs, AIDE_EQUIPE),
    ], {
        surFin, annuler, libelleEnregistrer: "Enregistrer",
        collecter: () => ({
            patch: verifierPatch(patch), scores: scores.map((s, i) => (autos[i]?.entree.checked ? SCORE_SALLE : entier(s))), stellar: stellaire.entree.checked,
            auto1: autos[0].entree.checked, auto2: autos[1].entree.checked, teams: eq.lire(),
        }),
    });
}

export function formulaireApoc({ catalogue, equipes, existant, surFin, annuler }) {
    const patch = champPatch(existant);
    const mode = h("select", {}, Object.entries(MODES_APOC).map(([cle, m]) => h("option", { value: cle, selected: (existant?.mode || "D4") === cle }, m.nom)));
    const scores = [0, 1, 2].map((i) => nombreChamp(existant?.scores?.[i] ?? 0, 0, SCORE_BOSS, { largeur: "110px" }));
    const champsScores = scores.map((s, i) => champ(`Boss ${i + 1} (/${SCORE_BOSS})`, s));
    const total = h("strong", { classe: "total-vert" });
    const eq = editeursEquipes({ catalogue, equipes, titre: (i) => `Boss ${i + 1}`, existantes: existant?.equipes, n: 3 });
    function majMode() {
        const n = MODES_APOC[mode.value].boss;
        champsScores.forEach((c, i) => c.classList.toggle("cache", i >= n));
        eq.blocs.forEach((b, i) => b.classList.toggle("cache", i >= n));
        total.textContent = `${scores.slice(0, n).reduce((a, s) => a + entier(s), 0)} / ${n * SCORE_BOSS}`;
    }
    mode.addEventListener("change", majMode);
    scores.forEach((s) => s.addEventListener("input", majMode));
    majMode();

    return cadre(existant ? "Modifier le clear" : "Ajouter un clear", "Fantômes de l'apocalypse : mode et score de chaque boss, puis les équipes.", [
        section("1. Patch et scores",
            h("div", { classe: "rangee-champs" }, champ("Patch", patch), champ("Mode", mode), champsScores, champ("Total", total))),
        section("2. Équipes utilisées", eq.blocs, AIDE_EQUIPE),
    ], {
        surFin, annuler, libelleEnregistrer: "Enregistrer",
        collecter: () => {
            const n = MODES_APOC[mode.value].boss;
            return { patch: verifierPatch(patch), mode: mode.value, scores: scores.slice(0, n).map(entier), teams: eq.editeurs.slice(0, n).map((e) => e.lire()).filter((t) => t.length) };
        },
    });
}

export function formulaireArbitre({ catalogue, equipes, existant, surFin, annuler }) {
    const patch = champPatch(existant);
    const cycles = [0, 1, 2].map((i) => nombreChamp(existant?.cycles?.[i] ?? 0, 0, 99, { largeur: "80px" }));
    const ko = [0, 1, 2].map((i) => caseACocher("K.O.", existant?.ko?.[i]));
    const cyclesRoi = nombreChamp(existant?.cycles_roi ?? 0, 0, 99, { largeur: "80px" });
    const koRoi = caseACocher("K.O.", existant?.roi_ko);
    const eq = editeursEquipes({ catalogue, equipes, titre: (i) => `Cavalier ${i + 1}`, existantes: existant?.equipes_cavaliers, n: 3 });
    const editeurRoi = editeurEquipe({ jeu: "starrail", catalogue, colonnes: 2, modeles: equipes });
    if (existant?.equipe_roi?.length) editeurRoi.definir(existant.equipe_roi);

    return cadre(existant ? "Modifier le clear" : "Ajouter un clear", "Arbitre de l'instable : cycles et K.O. de chaque cavalier et du King, puis les équipes.", [
        section("1. Patch et combats",
            h("div", { classe: "rangee-champs" }, champ("Patch", patch)),
            h("div", { classe: "rangee-champs" }, cycles.flatMap((c, i) => [champ(`Cavalier ${i + 1} : cycles`, c), champ(`Cavalier ${i + 1}`, ko[i].element)])),
            h("div", { classe: "rangee-champs" }, champ("Le King : cycles", cyclesRoi), champ("Le King", koRoi.element)),
            h("p", { classe: "doux petit" }, "Les étoiles (3 pour 1 à 2 cycles, 2 pour 3 à 4, 1 pour 5 à 6, moins 1 en cas de K.O.) et le cadre sont calculés d'après les cycles. Le King à 0 cycle donne le cadre prismatique.")),
        section("2. Équipes utilisées", eq.blocs, h("div", { classe: "equipe-edition" }, h("h4", {}, "Le King"), editeurRoi.element), AIDE_EQUIPE),
    ], {
        surFin, annuler, libelleEnregistrer: "Enregistrer",
        collecter: () => ({
            patch: verifierPatch(patch), cycles: cycles.map(entier), ko: ko.map((k) => k.entree.checked), kingCycles: entier(cyclesRoi), kingKo: koRoi.entree.checked,
            riderTeams: eq.lire(), kingTeam: editeurRoi.lire(),
        }),
    });
}
