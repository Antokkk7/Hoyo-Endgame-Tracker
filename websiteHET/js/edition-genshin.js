// Formulaires d'ajout et de modification des clears Genshin Impact : Abysses, Théâtre et Carnage chtonien.
// Chaque formulaire remplace la liste dans la page (pas de fenêtre séparée) ; surFin(donnees) enregistre.
import { h, remplacer } from "./dom.js";
import { bossDuPatch, nomBoss, urlBoss, urlDifficulte } from "./donnees.js";
import { editeurEquipe } from "./editeur-equipe.js";
import { ELEMENTS } from "./elements.js";
import { portrait } from "./portraits.js";
import { COULEURS_DIFFICULTE, MODES_THEATRE, MOIS } from "./regles.js";

const ELEMENTS_GENSHIN = ["PYRO", "HYDRO", "ANEMO", "ELECTRO", "DENDRO", "CRYO", "GEO"];

function champ(libelle, controle, classe = "") {
    return h("div", { classe: `champ ${classe}`.trim() }, h("label", {}, libelle), controle);
}

function section(titre, ...contenu) {
    return h("section", { classe: "section-edition" }, h("h3", {}, titre), ...contenu);
}

function nombreChamp(valeur, min, max, { largeur = null, id = null } = {}) {
    return h("input", { type: "number", min: String(min), max: String(max), value: String(valeur), id, style: largeur ? `max-width:${largeur}` : null });
}

function entier(el, defaut = 0) {
    const n = parseInt(el.value, 10);
    return Number.isFinite(n) ? n : defaut;
}

// Enveloppe commune : titre, message d'erreur, boutons Annuler / Enregistrer.
function cadre(titre, sousTitre, sections, { surFin, annuler, collecter, libelleEnregistrer }) {
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

export function formulaireAbysse({ catalogue, equipes, existant, surFin, annuler }) {
    const patch = h("input", { type: "text", value: existant?.patch || "", placeholder: "ex. 7.1", maxlength: "6", style: "max-width:120px" });
    const rotation = h("select", {}, ["Rotation 1", "Rotation 2"].map((r) => h("option", { value: r, selected: existant?.rotation === r }, r)));
    const etage = h("select", {}, [9, 10, 11, 12].map((n) => h("option", { value: String(n), selected: (existant?.etage ?? 12) === n }, `Étage ${n}`)));
    const base = nombreChamp(existant?.etoiles_base ?? 27, 0, 27, { largeur: "90px" });
    const baseMax = h("span", { classe: "doux" });
    const salles = [existant?.salle1 ?? 3, existant?.salle2 ?? 3, existant?.salle3 ?? 3].map((v) => nombreChamp(v, 0, 3, { largeur: "80px" }));
    const total = h("strong", { classe: "total-vert" });
    const editeur = editeurEquipe({ jeu: "genshin", catalogue, colonnes: 2, modeles: equipes });
    if (existant) editeur.definir(existant.equipe);

    function majEtage(initial = false) {
        const f = entier(etage, 12);
        const max = 9 * (f - 9);
        base.max = String(max);
        baseMax.textContent = `/ ${max}`;
        if (!initial || entier(base) > max) base.value = String(Math.min(entier(base, max), max));
        if (!initial && !existant) base.value = String(max);
        majTotal();
    }

    function majTotal() {
        const f = entier(etage, 12);
        total.textContent = `${entier(base) + salles.reduce((a, s) => a + entier(s), 0)} / ${9 * (f - 8)}`;
    }

    etage.addEventListener("change", () => majEtage(false));
    [base, ...salles].forEach((s) => s.addEventListener("input", majTotal));
    majEtage(true);

    return cadre(existant ? "Modifier le clear" : "Ajouter un clear", "Étoiles obtenues, puis l'équipe utilisée.", [
        section("1. Patch et étoiles",
            h("div", { classe: "rangee-champs" },
                champ("Patch", patch), champ("Rotation", rotation), champ("Étage du clear", etage),
                champ("Étoiles avant l'étage", h("div", { classe: "inline" }, base, baseMax)),
                champ("Salle 1 (/3)", salles[0]), champ("Salle 2 (/3)", salles[1]), champ("Salle 3 (/3)", salles[2]),
                champ("Total", total)),
            h("p", { classe: "doux petit" }, "Étage 9 = 9 étoiles max, Étage 10 = 18, Étage 11 = 27, Étage 12 = 36. Le total = étoiles avant l'étage (modifiable) + les 3 salles de l'étage clear.")),
        section("2. Équipe utilisée", editeur.element,
            h("p", { classe: "doux petit" }, "Choisir une équipe enregistrée remplit les 4 emplacements, elle reste toutefois modifiable.")),
    ], {
        surFin, annuler, libelleEnregistrer: "Enregistrer",
        collecter: () => {
            if (!patch.value.trim()) throw new Error("Indiquez le patch (ex. 7.1).");
            return {
                patch: patch.value.trim(), rotation: rotation.value, floor: entier(etage, 12), baseStars: entier(base),
                room1: entier(salles[0]), room2: entier(salles[1]), room3: entier(salles[2]), team: editeur.lire(),
            };
        },
    });
}

export function formulaireTheatre({ catalogue, existant, surFin, annuler }) {
    const aujourdhui = new Date();
    let mode = existant?.mode || "VISIONARY";
    const elements = new Set(existant?.elements || []);
    let flags = existant ? [...(existant.etoiles_flags || [])] : [];
    let mvp = existant?.mvp?.name || "";
    const compteurElements = h("span", { classe: "doux petit" });
    const rangeeElements = h("div", { classe: "elements-choix" });
    const patch = h("input", { type: "text", value: existant?.patch || "", placeholder: "ex. 7.1", maxlength: "6", style: "max-width:120px" });
    const moisChamp = h("select", {}, MOIS.map((m, i) => h("option", { value: String(i + 1), selected: (existant?.mois ?? aujourdhui.getMonth() + 1) === i + 1 }, m[0].toUpperCase() + m.slice(1))));
    const annee = nombreChamp(existant?.annee ?? aujourdhui.getFullYear(), 2020, 2100, { largeur: "100px" });
    const rangeeModes = h("div", { classe: "pilules pilules-larges" });
    const exigences = h("p", { classe: "doux petit" });
    const rangeeEtoiles = h("div", { classe: "etoiles-choix" });
    const compteurEtoiles = h("strong", { classe: "total-magenta" });
    const mvpGrille = h("div", { classe: "mvp-grille" });
    const mvpRecherche = h("input", { type: "search", placeholder: "Rechercher un personnage", autocomplete: "off", style: "max-width:300px" });
    const pret = h("input", { type: "checkbox", id: "mvp-pret", checked: Boolean(existant?.mvp_pret) });

    function dessinerElements() {
        remplacer(rangeeElements, ELEMENTS_GENSHIN.map((cle) => {
            const choisi = elements.has(cle);
            return h("button", {
                classe: "element-choix", type: "button", "aria-pressed": String(choisi), disabled: !choisi && elements.size >= 3,
                style: `--couleur:${ELEMENTS[cle].couleur}`,
                onclick: () => {
                    if (choisi) elements.delete(cle);
                    else elements.add(cle);
                    dessinerElements();
                },
            }, h("span", { classe: "element-point", "aria-hidden": "true" }), ELEMENTS[cle].nom);
        }));
        compteurElements.textContent = `Sélection : ${elements.size}/3`;
    }

    function dessinerModes() {
        remplacer(rangeeModes, Object.entries(MODES_THEATRE).map(([cle, m]) =>
            h("button", {
                classe: "pilule", type: "button", "aria-pressed": String(mode === cle),
                onclick: () => {
                    mode = cle;
                    dessinerModes();
                    dessinerEtoiles();
                },
            }, m.nom)));
        const m = MODES_THEATRE[mode];
        exigences.textContent = `${m.actes} actes${mode === "LUNAR" ? " + Arcanum" : ""}`;
    }

    function dessinerEtoiles() {
        const actes = MODES_THEATRE[mode].actes;
        while (flags.length > actes) flags.pop();
        while (flags.length < actes) flags.push(false);
        const debutArcanum = mode === "LUNAR" ? actes - 2 : actes;
        remplacer(rangeeEtoiles, flags.flatMap((on, i) => {
            const bouton = h("button", {
                classe: `etoile-choix${on ? " on" : ""}${i >= debutArcanum ? " arcanum" : ""}`, type: "button",
                "aria-pressed": String(on), "aria-label": `Astre ${i + 1}`,
                onclick: () => {
                    flags[i] = !flags[i];
                    dessinerEtoiles();
                },
            }, "★");
            return i === debutArcanum && debutArcanum < actes ? [h("span", { classe: "separateur", "aria-hidden": "true" }), bouton] : [bouton];
        }));
        compteurEtoiles.textContent = `${flags.filter(Boolean).length}/${actes}`;
    }

    function dessinerMvp() {
        const saisie = mvpRecherche.value.trim().toLowerCase();
        remplacer(mvpGrille, catalogue.filter((c) => !saisie || c.nom.toLowerCase().includes(saisie)).map((c) =>
            h("button", {
                classe: "avatar-choix", type: "button", title: c.nom, "aria-pressed": String(mvp === c.nom),
                onclick: () => {
                    mvp = mvp === c.nom ? "" : c.nom;
                    dessinerMvp();
                },
            }, portrait("genshin", c.nom, { taille: 52, elementCle: c.element }), h("span", {}, c.nom))));
    }

    mvpRecherche.addEventListener("input", dessinerMvp);
    dessinerElements();
    dessinerModes();
    dessinerEtoiles();
    dessinerMvp();

    return cadre(existant ? "Modifier le clear" : "Ajouter un clear", "Éléments du mois, période, mode, résultat et personnage MVP.", [
        section("1. Les 3 éléments du mois", rangeeElements, compteurElements),
        section("2. Période", h("div", { classe: "rangee-champs" }, champ("Mois", moisChamp), champ("Année", annee), champ("Patch", patch))),
        section("3. Mode", rangeeModes, exigences),
        section("4. Résultat", h("div", { classe: "inline" }, rangeeEtoiles, compteurEtoiles),
            h("p", { classe: "doux petit" }, "Astres maximum : Facile 3, Normal 6, Difficile 8, Visionnaire 10, Lunaire 12. Une étoile allumée = obtenue. En Défi Lunaire, les 2 étoiles dorées comptent comme Arcanum.")),
        section("5. Personnage MVP", mvpRecherche, mvpGrille,
            h("label", { classe: "case", for: "mvp-pret" }, pret, " Personnage d'introduction / de prêt"),
            h("p", { classe: "doux petit" }, "Choisi parmi les personnages du jeu (cliquez à nouveau pour désélectionner).")),
    ], {
        surFin, annuler, libelleEnregistrer: "Enregistrer le clear",
        collecter: () => {
            if (elements.size !== 3) throw new Error("Sélectionnez exactement 3 éléments.");
            if (!patch.value.trim()) throw new Error("Indiquez le patch (ex. 7.1).");
            const info = catalogue.find((c) => c.nom === mvp);
            return {
                year: entier(annee), month: entier(moisChamp), patch: patch.value.trim(), mode, elements: [...elements], starFlags: [...flags],
                mvp: mvp ? { name: mvp, element: info?.element || null, level: 90, constellation: 0, weapon: "", refinement: 1 } : null,
                mvpTrial: pret.checked,
            };
        },
    });
}

export function formulaireCarnage({ catalogue, equipes, existant, surFin, annuler }) {
    const patch = h("input", { type: "text", value: existant?.patch || "", placeholder: "ex. 7.1", maxlength: "6", style: "max-width:120px" });
    const periode = h("input", { type: "text", value: existant?.periode || "", readonly: true, placeholder: "automatique d'après le patch", tabindex: "-1" });
    let difficulte = existant?.difficulte ?? 6;
    const rangeeDifficulte = h("div", { classe: "difficultes" });
    const total = h("strong", { classe: "total-cyan" });
    const blocs = [];

    function dessinerDifficulte() {
        remplacer(rangeeDifficulte, [1, 2, 3, 4, 5, 6].map((d) =>
            h("button", {
                classe: "difficulte-choix", type: "button", "aria-pressed": String(difficulte === d), title: `Difficulté ${d}`,
                style: `--couleur:${COULEURS_DIFFICULTE[d - 1]}`,
                onclick: () => {
                    difficulte = d;
                    dessinerDifficulte();
                },
            }, h("img", { src: urlDifficulte(d), alt: `Difficulté ${d}`, onerror: (e) => { e.target.replaceWith(document.createTextNode(String(d))); } }))));
    }

    function majTotal() {
        total.textContent = `${blocs.reduce((a, b) => a + entier(b.secondes), 0)} s`;
    }

    for (let i = 0; i < 3; i++) {
        const prior = existant?.boss?.[i];
        const nom = h("input", { type: "text", value: prior?.name || "", placeholder: `Nom du boss ${i + 1}`, maxlength: "60" });
        const apercu = h("span", { classe: "boss-apercu" });
        const secondes = nombreChamp(prior?.seconds ?? 0, 0, 999, { largeur: "100px" });
        const editeur = editeurEquipe({ jeu: "genshin", catalogue, colonnes: 2, modeles: equipes });
        if (prior) editeur.definir(prior.team);
        const majApercu = () => {
            const url = urlBoss(nom.value);
            remplacer(apercu, url ? h("img", { src: url, alt: nomBoss(nom.value), onerror: (e) => e.target.remove() }) : null);
        };
        nom.addEventListener("input", majApercu);
        secondes.addEventListener("input", majTotal);
        majApercu();
        blocs.push({ nom, secondes, editeur, apercu });
    }

    // Les 3 boss du patch viennent de boss.json ; un nom saisi à la main n'est jamais écrasé.
    const auto = ["", "", ""];
    function remplirBoss() {
        const infos = bossDuPatch(patch.value);
        blocs.forEach((b, i) => {
            const voulu = infos.boss[i] || "";
            const courant = b.nom.value.trim();
            if (!courant || courant === auto[i]) {
                b.nom.value = voulu;
                b.nom.dispatchEvent(new Event("input"));
            }
            auto[i] = voulu;
        });
        if (infos.periode) periode.value = infos.periode;
        else if (!existant || existant.patch !== patch.value.trim()) periode.value = "";
    }
    patch.addEventListener("input", remplirBoss);
    dessinerDifficulte();
    majTotal();
    if (!existant) remplirBoss();

    const combats = blocs.map((b, i) =>
        h("div", { classe: "bloc-combat" },
            h("div", { classe: "combat-tete" }, b.apercu, champ(`Boss ${i + 1}`, b.nom, "combat-nom"), champ("Durée (s)", b.secondes)),
            b.editeur.element));

    return cadre(existant ? "Modifier le clear" : "Ajouter un clear", "Difficulté, durée de chaque combat et équipes utilisées.", [
        section("1. Patch, période et difficulté",
            h("div", { classe: "rangee-champs" }, champ("Patch", patch), champ("Période", periode, "champ-large")),
            champ("Difficulté (1 à 6)", rangeeDifficulte)),
        section("2. Les 3 combats", combats, h("p", { classe: "inline" }, h("span", { classe: "doux" }, "Durée totale : "), total)),
    ], {
        surFin, annuler, libelleEnregistrer: "Enregistrer le clear",
        collecter: () => {
            if (!patch.value.trim()) throw new Error("Indiquez le patch (ex. 7.1).");
            const bosses = blocs.map((b) => ({ name: b.nom.value.trim(), seconds: entier(b.secondes), team: b.editeur.lire() }))
                .filter((b) => b.name || b.seconds > 0 || b.team.length);
            return { patch: patch.value.trim(), period: periode.value.trim(), difficulty: difficulte, bosses };
        },
    });
}
