// Bloc « Équipe et statistiques en combat » du suivi Star Rail : choix de 3 coéquipiers (éidolon, cône et superposition, sets de
// reliques et ornement planaire) et statistiques du personnage avec les bonus de l'équipe.
import { h, nombre, remplacer } from "./dom.js";
import { calculerCombat, donneesBuffsStarRail, equipeVide, formaterEffet, idsOrnements, libelleStat } from "./combat-starrail.js";
import { element as infoElement } from "./elements.js";
import { referenceStarRail, VOIES, ELEMENTS_HSR } from "./enka-starrail.js";
import { imageEnka, urlsArme, urlsPerso, urlsSet } from "./icones.js";
import { selecteur } from "./selecteur.js";

function format(valeur, pct) {
    const texte = Number.isInteger(valeur) ? nombre(valeur) : new Intl.NumberFormat("fr-FR", { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valeur);
    return pct ? `${texte} %` : texte;
}

const miniature = (urls) => () => imageEnka(urls, { classe: "sg-mini", alt: "" });

function listeNombres(min, max, prefixe, valeur, surChoix, etiquette) {
    const sel = h("select", { "aria-label": etiquette });
    for (let i = min; i <= max; i++) sel.append(h("option", { value: String(i) }, `${prefixe}${i}`));
    sel.value = String(valeur);
    sel.addEventListener("change", () => surChoix(Number(sel.value)));
    return sel;
}

function elementCle(info) {
    return ELEMENTS_HSR[info.element] || String(info.element || "").toUpperCase();
}

function optionsPersonnages(principal, equipe, indice) {
    const ref = referenceStarRail();
    const pris = new Set([principal.id, ...equipe.filter((m, i) => m && i !== indice).map((m) => m.id)]);
    return Object.entries(ref.persos)
        .filter(([id, p]) => !pris.has(id) && p.en !== principal.en)
        .sort((a, b) => b[1].rarete - a[1].rarete || a[1].nom.localeCompare(b[1].nom, "fr"))
        .map(([id, p]) => ({ id, libelle: p.nom, sous: `${infoElement(elementCle(p)).nom} · ${VOIES[p.voie] || ""} · ${p.rarete}★`, visuel: miniature(urlsPerso("starrail", p.en)) }));
}

function optionsCones(membre) {
    const ref = referenceStarRail();
    const voie = membre?.id ? ref.persos[membre.id]?.voie : null;
    const avecBuff = donneesBuffsStarRail().cones;
    return Object.entries(ref.cones)
        .filter(([, c]) => !voie || c.voie === voie)
        .sort((a, b) => b[1].rarete - a[1].rarete || a[1].nom.localeCompare(b[1].nom, "fr"))
        .map(([id, c]) => ({ id, libelle: c.nom, sous: `${c.rarete}★ · ${VOIES[c.voie] || ""}${(avecBuff[c.en] || []).some((x) => x.cible === "equipe") ? " · bonus d'équipe" : ""}`, visuel: miniature(urlsArme("starrail", c.en)) }));
}

function optionsSets(ornement) {
    const ref = referenceStarRail();
    const ornements = idsOrnements();
    const avecBuff = donneesBuffsStarRail().sets;
    return Object.entries(ref.sets)
        .filter(([id]) => ornements.has(id) === ornement)
        .sort((a, b) => a[1].nom.localeCompare(b[1].nom, "fr"))
        .map(([id, s]) => {
            const d = avecBuff[s.en];
            const equipe = d && [...(d.deux || []), ...(d.quatre || [])].some((x) => x.cible === "equipe");
            return { id, libelle: s.nom, sous: `${s.en}${equipe ? " · bonus d'équipe" : ""}`, visuel: miniature(urlsSet("starrail", s.en)) };
        });
}

const superpositionDefaut = (idCone) => (referenceStarRail().cones[idCone]?.rarete < 5 ? 5 : 1);

function emplacement(principal, equipe, indice, surChange, noeudDetails) {
    let membre = equipe[indice];
    const zone = h("div", { classe: "sg-slot" });
    const maj = () => {
        equipe[indice] = membre?.id ? membre : null;
        surChange();
    };

    function dessiner() {
        const sp = selecteur({
            placeholder: "Choisir un personnage",
            options: () => optionsPersonnages(principal, equipe, indice),
            onChoix: (o) => {
                if (!o) membre = null;
                else {
                    const voie = referenceStarRail().persos[o.id]?.voie;
                    const compat = !membre?.cone || referenceStarRail().cones[membre.cone.id]?.voie === voie;
                    membre = { id: o.id, e: membre?.e ?? 0, cone: compat ? membre?.cone || null : null, sets: membre?.sets || { m: "aucun", a: null, b: null, o: null } };
                }
                maj();
                dessiner();
            },
        });
        sp.definir(membre?.id);
        const entete = h("div", { classe: "sg-slot-ligne" }, h("div", { classe: "sg-slot-champ" }, sp.element));
        if (membre?.id) entete.append(listeNombres(0, 6, "E", membre.e ?? 0, (v) => { membre.e = v; maj(); }, "Éidolon"));

        const contenu = [h("strong", { classe: "sg-slot-titre" }, `Coéquipier ${indice + 1}`), entete];
        if (membre?.id) {
            const sc = selecteur({
                placeholder: "Aucun cône",
                options: () => optionsCones(membre),
                onChoix: (o) => {
                    membre.cone = o ? { id: o.id, r: membre.cone?.r || superpositionDefaut(o.id) } : null;
                    maj();
                    dessiner();
                },
            });
            sc.definir(membre.cone?.id);
            const ligneCone = h("div", { classe: "sg-slot-ligne" }, h("div", { classe: "sg-slot-champ" }, sc.element));
            if (membre.cone) ligneCone.append(listeNombres(1, 5, "S", membre.cone.r ?? 1, (v) => { membre.cone.r = v; maj(); }, "Superposition"));
            contenu.push(ligneCone);

            const sets = membre.sets || (membre.sets = { m: "aucun", a: null, b: null, o: null });
            const modes = [["aucun", "Aucun set"], ["4", "4 pièces"], ["22", "2 + 2"]];
            contenu.push(h("div", { classe: "sg-modes", role: "group", "aria-label": "Set de reliques" }, modes.map(([m, texte]) =>
                h("button", { type: "button", classe: "sg-mode" + (sets.m === m ? " actif" : ""), "aria-pressed": String(sets.m === m), onclick: () => { sets.m = m; maj(); dessiner(); } }, texte))));
            const choixSet = (cle, ornement, texte) => {
                const s = selecteur({ placeholder: texte, options: () => optionsSets(ornement), onChoix: (o) => { sets[cle] = o ? o.id : null; maj(); } });
                s.definir(sets[cle]);
                return s.element;
            };
            if (sets.m === "4") contenu.push(choixSet("a", false, "Choisir un set de reliques"));
            if (sets.m === "22") contenu.push(choixSet("a", false, "Premier set"), choixSet("b", false, "Second set"));
            contenu.push(choixSet("o", true, "Ornement planaire"));
        }
        remplacer(zone, h("div", { classe: "sg-slot-corps" }, contenu), noeudDetails);
    }
    dessiner();
    return zone;
}

// Renvoie { equipe: noeud, resultat: noeud } ; surSauvegarde(donnees) est appelé à chaque modification.
export function blocCombatStarRail(perso, donneesEnregistrees, surSauvegarde) {
    const donnees = { ...equipeVide(), ...(donneesEnregistrees || {}) };
    donnees.equipe = [0, 1, 2].map((i) => donnees.equipe?.[i] || null);
    donnees.etat = donnees.etat || {};
    const tableau = h("div", { classe: "sg-combat" });
    const cles = ["p", 0, 1, 2];
    const details = Object.fromEntries(cles.map((c) => [c, h("details", { classe: "sg-effets" })]));

    function afficher() {
        const calcul = calculerCombat(perso, donnees.equipe, donnees.etat);
        if (!calcul) {
            remplacer(tableau, h("p", { classe: "doux petit" }, "Rechargez l'UID pour activer le calcul des statistiques en combat."));
            for (const c of cles) remplacer(details[c]);
            return;
        }
        remplacer(tableau,
            h("div", { classe: "sg-combat-tete doux petit" }, h("span", {}, "Statistique"), h("span", {}, "Base"), h("span", {}, "En combat")),
            h("ul", { classe: "sg-liste-stats sg-liste-combat" }, calcul.lignes.map((l) => {
                const delta = l.final - l.base;
                return h("li", {}, h("span", {}, l.libelle), h("span", { classe: "doux" }, format(Math.round(l.base * 10) / 10, l.pct)),
                    h("strong", { classe: delta > 0.05 ? "plus" : "" }, format(Math.round(l.final * 10) / 10, l.pct)));
            })),
            calcul.autres.length
                ? h("div", { classe: "sg-autres" }, h("span", { classe: "doux petit" }, "Autres bonus"), h("ul", {}, calcul.autres.map((a) =>
                    h("li", {}, h("span", {}, a.libelle), h("strong", {}, formaterEffet(a.cle, Math.round(a.valeur * 10) / 10))))))
                : null);

        for (const c of cles) {
            const liste = calcul.effets.filter((x) => x.qui === c);
            const actifs = liste.filter((x) => x.statut === "actif").length;
            const noeud = details[c];
            const ouvert = noeud.open;
            remplacer(noeud,
                h("summary", {}, `Détail des bonus (${actifs} actif${actifs > 1 ? "s" : ""} sur ${liste.length})`),
                liste.length
                    ? h("ul", { classe: "sg-effets-liste" }, liste.map((x) => {
                        const case_ = h("input", { type: "checkbox", disabled: x.statut === "hors" });
                        case_.checked = x.statut === "actif";
                        case_.addEventListener("change", () => { donnees.etat[x.id] = case_.checked; surSauvegarde(donnees); afficher(); });
                        return h("li", { classe: `sg-effet ${x.statut}` },
                            h("label", {}, case_, h("span", { classe: "sg-effet-texte" }, h("strong", {}, x.origine), h("span", { classe: "doux sg-effet-desc", title: x.c || "" }, x.c || libelleStat(x.s))),
                                x.v !== undefined && x.v !== null ? h("span", { classe: "sg-effet-valeur" }, `${formaterEffet(x.s, Math.round(x.v * 10) / 10)} ${libelleStat(x.s)}`) : null),
                            x.statut === "hors" ? h("span", { classe: "sg-effet-note petit" }, x.raison) : x.al ? h("span", { classe: "sg-effet-note petit" }, "Allié ciblé : à cocher si c'est le personnage principal") : x.en ? h("span", { classe: "sg-effet-note petit" }, "Effet sur l'ennemi") : null);
                    }))
                    : h("p", { classe: "doux petit" }, "Aucun bonus renseigné pour l'instant."));
            noeud.open = ouvert;
        }
    }

    const surChange = () => {
        surSauvegarde(donnees);
        afficher();
    };
    const principal = h("div", { classe: "sg-slot sg-slot-principal" },
        h("strong", { classe: "sg-slot-titre" }, "Personnage principal"), h("span", { classe: "sg-slot-nom" }, perso.nom), details.p);
    const equipe = h("div", { classe: "sg-equipe" }, principal, [0, 1, 2].map((i) => emplacement(perso, donnees.equipe, i, surChange, details[i])));
    afficher();
    return { equipe, resultat: tableau };
}
