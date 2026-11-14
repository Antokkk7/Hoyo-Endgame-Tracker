// Bloc « Équipe et statistiques en combat » du suivi Genshin : choix de 3 coéquipiers (constellation, arme et raffinement, set
// d'artéfacts) et affichage des statistiques du personnage avec les bonus de l'équipe.
import { h, nombre, remplacer } from "./dom.js";
import { calculerCombat, donneesBuffs, equipeVide, formaterEffet, libelleStat } from "./combat-genshin.js";
import { element as infoElement } from "./elements.js";
import { referenceEnka, urlIconeEnka } from "./enka.js";
import { selecteur } from "./selecteur.js";

const TYPES_ARME = { sword: "Épée", claymore: "Claymore", polearm: "Arme d'hast", bow: "Arc", catalyst: "Catalyseur" };

function format(valeur, pct) {
    const texte = Number.isInteger(valeur) ? nombre(valeur) : new Intl.NumberFormat("fr-FR", { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valeur);
    return pct ? `${texte} %` : texte;
}

function miniature(nomIcone) {
    return () => (nomIcone ? h("img", { classe: "sg-mini", src: urlIconeEnka(nomIcone), alt: "", loading: "lazy", onerror: (e) => e.target.remove() }) : null);
}

function listeNombres(min, max, prefixe, valeur, surChoix, etiquette) {
    const sel = h("select", { "aria-label": etiquette });
    for (let i = min; i <= max; i++) sel.append(h("option", { value: String(i) }, `${prefixe}${i}`));
    sel.value = String(valeur);
    sel.addEventListener("change", () => surChoix(Number(sel.value)));
    return sel;
}

function optionsPersonnages(principal, equipe, indice) {
    const ref = referenceEnka();
    const pris = new Set([principal.id, ...equipe.filter((m, i) => m && i !== indice).map((m) => m.id)]);
    return Object.entries(ref.persos)
        .filter(([id, p]) => !id.includes("-") && p.en !== "Traveler" && !/^Manekin/.test(p.en) && !pris.has(id) && p.en !== principal.en)
        .sort((a, b) => b[1].rarete - a[1].rarete || a[1].nom.localeCompare(b[1].nom, "fr"))
        .map(([id, p]) => ({ id, libelle: p.nom, sous: `${infoElement(p.element).nom} · ${p.rarete}★`, visuel: miniature(p.icone) }));
}

function optionsArmes(membre) {
    const ref = referenceEnka();
    const type = membre?.id ? ref.persos[membre.id]?.arme : null;
    const avecBuff = donneesBuffs().armes;
    return Object.entries(ref.armes)
        .filter(([, a]) => a.rarete >= 3 && (!type || a.type === type))
        .sort((a, b) => b[1].rarete - a[1].rarete || a[1].nom.localeCompare(b[1].nom, "fr"))
        .map(([id, a]) => ({ id, libelle: a.nom, sous: `${a.rarete}★ · ${TYPES_ARME[a.type] || ""}${avecBuff[a.en] ? " · bonus d'équipe" : ""}`, visuel: miniature(a.icone) }));
}

function optionsSets() {
    return donneesBuffs().sets.map((s) => ({
        id: String(s.id), libelle: s.fr,
        sous: `${s.en}${s.quatre.some((x) => x.cible === "equipe") ? " · bonus d'équipe à 4 pièces" : ""}`,
    }));
}

function raffinementDefaut(idArme) {
    const a = referenceEnka().armes[idArme];
    return a && a.rarete < 5 && !(donneesBuffs().bp || []).includes(a.en) ? 5 : 1;
}

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
                    const info = referenceEnka().persos[o.id];
                    const compat = !membre?.arme || referenceEnka().armes[membre.arme.id]?.type === info?.arme;
                    membre = { id: o.id, c: membre?.c ?? 0, arme: compat ? membre?.arme || null : null, set: membre?.set || { m: "aucun", a: null, b: null } };
                }
                maj();
                dessiner();
            },
        });
        sp.definir(membre?.id);
        const entete = h("div", { classe: "sg-slot-ligne" }, h("div", { classe: "sg-slot-champ" }, sp.element));
        if (membre?.id) entete.append(listeNombres(0, 6, "C", membre.c ?? 0, (v) => { membre.c = v; maj(); }, "Constellation"));

        const contenu = [h("strong", { classe: "sg-slot-titre" }, `Coéquipier ${indice + 1}`), entete];
        if (membre?.id) {
            const sa = selecteur({
                placeholder: "Aucune arme",
                options: () => optionsArmes(membre),
                onChoix: (o) => {
                    membre.arme = o ? { id: o.id, r: membre.arme?.r || raffinementDefaut(o.id) } : null;
                    maj();
                    dessiner();
                },
            });
            sa.definir(membre.arme?.id);
            const ligneArme = h("div", { classe: "sg-slot-ligne" }, h("div", { classe: "sg-slot-champ" }, sa.element));
            if (membre.arme) ligneArme.append(listeNombres(1, 5, "R", membre.arme.r ?? 1, (v) => { membre.arme.r = v; maj(); }, "Raffinement"));
            contenu.push(ligneArme);

            const set = membre.set || (membre.set = { m: "aucun", a: null, b: null });
            const modes = [["aucun", "Aucun set"], ["4", "4 pièces"], ["22", "2 + 2"]];
            contenu.push(h("div", { classe: "sg-modes", role: "group", "aria-label": "Set d'artéfacts" }, modes.map(([m, texte]) =>
                h("button", { type: "button", classe: "sg-mode" + (set.m === m ? " actif" : ""), "aria-pressed": String(set.m === m), onclick: () => { set.m = m; maj(); dessiner(); } }, texte))));
            const choixSet = (cle) => {
                const s = selecteur({ placeholder: "Choisir un set", options: optionsSets, onChoix: (o) => { set[cle] = o ? o.id : null; maj(); } });
                s.definir(set[cle]);
                return s.element;
            };
            if (set.m === "4") contenu.push(choixSet("a"));
            if (set.m === "22") contenu.push(choixSet("a"), choixSet("b"));
        }
        remplacer(zone, h("div", { classe: "sg-slot-corps" }, contenu), noeudDetails);
    }
    dessiner();
    return zone;
}

// Renvoie { equipe: noeud, resultat: noeud } ; surSauvegarde(donnees) est appelé à chaque modification.
export function blocCombat(perso, donneesEnregistrees, surSauvegarde) {
    const donnees = { ...equipeVide(), ...(donneesEnregistrees || {}) };
    donnees.equipe = [0, 1, 2].map((i) => donnees.equipe?.[i] || null);
    donnees.etat = donnees.etat || {};
    const tableau = h("div", { classe: "sg-combat" });
    const cles = ["p", 0, 1, 2, "r"];
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
                h("summary", {}, c === "r" ? `Résonances (${actifs})` : `Détail des bonus (${actifs} actif${actifs > 1 ? "s" : ""} sur ${liste.length})`),
                liste.length
                    ? h("ul", { classe: "sg-effets-liste" }, liste.map((x) => {
                        const case_ = h("input", { type: "checkbox", disabled: x.statut === "hors" });
                        case_.checked = x.statut === "actif" || x.statut === "doublon";
                        case_.addEventListener("change", () => { donnees.etat[x.id] = case_.checked; surSauvegarde(donnees); afficher(); });
                        return h("li", { classe: `sg-effet ${x.statut}` },
                            h("label", {}, case_, h("span", { classe: "sg-effet-texte" }, h("strong", {}, x.origine), h("span", { classe: "doux" }, x.c || libelleStat(x.s))),
                                x.v !== undefined && x.v !== null ? h("span", { classe: "sg-effet-valeur" }, `${formaterEffet(x.s, Math.round(x.v * 10) / 10)} ${libelleStat(x.s)}`) : null),
                            x.statut === "doublon" || x.statut === "hors" ? h("span", { classe: "sg-effet-note petit" }, x.raison) : null);
                    }))
                    : h("p", { classe: "doux petit" }, "Aucun bonus."));
            noeud.open = ouvert;
        }
    }

    const surChange = () => {
        surSauvegarde(donnees);
        afficher();
    };
    const principal = h("div", { classe: "sg-slot sg-slot-principal" },
        h("strong", { classe: "sg-slot-titre" }, "Personnage principal"), h("span", { classe: "sg-slot-nom" }, perso.nom), details.p, details.r);
    const equipe = h("div", { classe: "sg-equipe" }, principal, [0, 1, 2].map((i) => emplacement(perso, donnees.equipe, i, surChange, details[i])));
    afficher();
    return { equipe, resultat: tableau };
}
