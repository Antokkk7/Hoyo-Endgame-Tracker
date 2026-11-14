// Page « Recommandations d'équipes » (Genshin Impact et Honkai: Star Rail).
// Une catégorie = un type de gameplay d'un personnage principal : équipe recommandée, meilleures armes ou cônes,
// sets d'artéfacts et remplacements possibles par emplacement. Format des données : voir sql/09_recommandations.sql.
import { h, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { urlArme } from "./donnees.js";
import { element as infoElement } from "./elements.js";
import { portrait } from "./portraits.js";
import { selecteur } from "./selecteur.js";

const TEXTES = {
    genshin: { armes: "Meilleures armes" },
    starrail: { armes: "Meilleurs cônes" },
};

function pluriel(n, mot) {
    return `${n} ${mot}${n > 1 ? "s" : ""}`;
}

function iconeArme(jeu, nom) {
    const url = urlArme(jeu, nom);
    return url
        ? h("img", { classe: "reco-arme-icone", src: url, alt: "", loading: "lazy", onerror: (e) => e.target.replaceWith(h("span", { classe: "reco-arme-glyphe", "aria-hidden": "true" }, "⚔")) })
        : h("span", { classe: "reco-arme-glyphe", "aria-hidden": "true" }, "⚔");
}

function ligneBuild(jeu, option) {
    const armes = (option.armes || []).slice(0, 3);
    const sets = (option.sets || []).slice(0, 3);
    return h("div", { classe: "reco-build" },
        h("div", { classe: "reco-bloc" },
            h("span", { classe: "reco-etiquette" }, TEXTES[jeu].armes),
            armes.length
                ? h("div", { classe: "reco-puces" }, armes.map((a, i) =>
                    h("span", { classe: `reco-puce${i === 0 ? " premier" : ""}` }, iconeArme(jeu, a), h("span", { classe: "reco-rang" }, `#${i + 1}`), h("span", { classe: "reco-nom" }, a))))
                : h("span", { classe: "doux petit" }, "Pas de recommandation pour l'instant.")),
        h("div", { classe: "reco-bloc" },
            h("span", { classe: "reco-etiquette" }, "Sets d'artéfacts"),
            sets.length
                ? h("div", { classe: "reco-puces" }, sets.map((s, i) =>
                    h("span", { classe: `reco-puce${i === 0 ? " premier" : ""}` },
                        h("span", { classe: `reco-badge b${i}` }, s.type || "4P"), h("span", { classe: "reco-nom" }, s.nom))))
                : h("span", { classe: "doux petit" }, "Pas de recommandation pour l'instant.")));
}

function carteCategorie(jeu, cat, catalogue) {
    const slots = cat.slots.map((s) => (Array.isArray(s) ? s : [s]).filter((o) => o && o.perso));
    const choix = slots.map(() => 0);
    const infos = (nom) => catalogue.find((c) => c.nom.toLowerCase() === String(nom).toLowerCase());
    let ouvert = true;
    const carte = h("div", { classe: "reco-carte" });

    function tuile(nom, taille) {
        return portrait(jeu, nom, { taille, elementCle: infos(nom)?.element });
    }

    function dessiner() {
        const actuels = slots.map((s, i) => s[choix[i]] || s[0]);
        const noms = actuels.map((o) => o.perso);
        const bascule = h("button", { classe: "reco-bascule", type: "button", "aria-expanded": String(ouvert), "aria-label": ouvert ? "Replier" : "Déplier", onclick: () => { ouvert = !ouvert; dessiner(); } },
            h("span", { classe: `reco-chevron${ouvert ? " ouvert" : ""}`, "aria-hidden": "true" }));
        const tete = h("div", { classe: "reco-tete" },
            h("div", { classe: "reco-tuiles" }, noms.map((n) => h("span", { classe: "reco-tuile" }, tuile(n, 64), h("span", { classe: "reco-tuile-nom" }, n)))),
            h("div", { classe: "reco-resume" }, h("strong", {}, noms.join(", ")), h("span", { classe: "doux petit" }, "Équipe recommandée")),
            bascule);
        const corps = ouvert
            ? h("div", { classe: "reco-corps" },
                h("div", { classe: "reco-gauche" }, actuels.map((o, i) =>
                    h("div", { classe: "reco-ligne" },
                        h("div", { classe: "reco-perso" }, tuile(o.perso, 64), h("strong", {}, o.perso), h("span", { classe: "doux petit" }, `Slot ${i + 1}`)),
                        ligneBuild(jeu, o)))),
                h("aside", { classe: "reco-droite" },
                    h("h4", {}, "Remplacements possibles"),
                    slots.map((s, i) => h("div", { classe: "reco-slot" },
                        h("span", { classe: "reco-slot-num" }, `Slot ${i + 1}`),
                        h("div", { classe: "reco-options" }, s.map((o, j) =>
                            h("button", {
                                classe: `reco-option${j === (choix[i] || 0) ? " choisi" : ""}`, type: "button", "aria-pressed": String(j === (choix[i] || 0)),
                                onclick: () => { choix[i] = j; dessiner(); },
                            }, tuile(o.perso, 24), h("span", { classe: "reco-nom" }, o.perso)))),
                        s.length === 1 && i === 0 ? h("span", { classe: "reco-toujours" }, "Toujours") : null))))
            : null;
        remplacer(carte, tete, corps);
    }

    dessiner();
    return h("section", { classe: "reco-categorie" },
        h("div", { classe: "reco-titre" }, h("h3", {}, cat.titre), cat.gameplay ? h("span", { classe: "doux petit" }, cat.gameplay) : null),
        carte);
}

// contexte : { personnage, depuisPerso } ; le résumé du personnage n'apparaît que si l'on vient d'un personnage.
export async function rendreRecos(zone, cfg, contexte = null) {
    const jeu = cfg.jeu;
    const catalogue = await rpc("catalogue", { p_jeu: jeu });
    const resultats = h("div", { classe: "reco-resultats" });
    const resume = h("div", { classe: "reco-chip cache" });
    let depuisPerso = Boolean(contexte?.depuisPerso);

    const choixPerso = selecteur({
        placeholder: "Tapez le nom d'un personnage",
        options: () => catalogue.map((c) => ({ id: c.nom, libelle: c.nom, sous: infoElement(c.element).nom || c.voie || "", visuel: () => portrait(jeu, c.nom, { taille: 30, elementCle: c.element }) })),
        onChoix: (o) => { depuisPerso = false; afficher(o?.id || ""); },
    });

    async function afficher(nom) {
        resume.classList.add("cache");
        remplacer(resultats);
        if (!nom) return;
        const perso = catalogue.find((c) => c.nom.toLowerCase() === nom.toLowerCase());
        try {
            const categories = await rpc("recos_equipes", { p_jeu: jeu, p_personnage: nom });
            if (depuisPerso && perso) {
                remplacer(resume, portrait(jeu, perso.nom, { taille: 44, elementCle: perso.element }),
                    h("div", {}, h("strong", {}, perso.nom),
                        h("div", { classe: "doux petit" }, `${pluriel(categories.length, "catégorie")} · ${pluriel(categories.length, "équipe")} ${categories.length > 1 ? "conseillées" : "conseillée"}`)));
                resume.classList.remove("cache");
            }
            if (!categories.length) {
                remplacer(resultats, h("div", { classe: "etat-vide" }, `Aucune équipe recommandée pour ${nom} pour l'instant.`));
                return;
            }
            remplacer(resultats, categories.map((c) => carteCategorie(jeu, c, catalogue)));
        } catch (err) {
            remplacer(resultats, h("div", { classe: "etat-vide" }, err?.message || "Une erreur est survenue."));
        }
    }

    const retour = depuisPerso
        ? h("button", { classe: "bouton bouton-petit", type: "button", onclick: () => window.dispatchEvent(new CustomEvent("het:onglet", { detail: { id: "suivi" } })) }, "← Retour au suivi")
        : null;
    remplacer(zone,
        h("div", { classe: "reco-recherche" },
            h("div", { classe: "champ" }, h("label", {}, "Personnage"), choixPerso.element),
            resume, retour),
        resultats);
    if (!catalogue.length) {
        resultats.append(h("div", { classe: "etat-vide" }, "Le catalogue de personnages est vide (voir sql/03_donnees_reference.sql)."));
        return;
    }
    if (contexte?.personnage) {
        const trouve = catalogue.find((c) => c.nom.toLowerCase() === contexte.personnage.toLowerCase());
        const nom = trouve ? trouve.nom : contexte.personnage;
        choixPerso.definir(nom, true);
        await afficher(nom);
    }
}
