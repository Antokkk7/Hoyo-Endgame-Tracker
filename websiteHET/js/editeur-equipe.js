// Éditeur d'équipe : 4 emplacements (personnage, niveau, doublons, arme ou cône, raffinement), comme dans l'application.
import { h, remplacer } from "./dom.js";
import { armesPour, urlArme } from "./donnees.js";
import { element as infoElement } from "./elements.js";
import { normaliserMembre } from "./equipes.js";
import { portrait } from "./portraits.js";
import { selecteur } from "./selecteur.js";

export const TEXTES = {
    genshin: { arme: "Arme", raffinement: "Raffinement", niveauMax: 90, doublons: "C", raff: "R", choisirArme: "Choisir une arme" },
    starrail: { arme: "Cône", raffinement: "Superposition", niveauMax: 80, doublons: "E", raff: "S", choisirArme: "Choisir un cône" },
};

function pilules(valeurs, prefixe, surChangement) {
    let courant = valeurs[0];
    const conteneur = h("div", { classe: "pilules" });
    function dessiner() {
        remplacer(conteneur, valeurs.map((v) =>
            h("button", {
                classe: "pilule", type: "button", "aria-pressed": String(v === courant),
                onclick: () => {
                    courant = v;
                    dessiner();
                    surChangement?.(v);
                },
            }, `${prefixe}${v}`)));
    }
    dessiner();
    return { element: conteneur, lire: () => courant, definir: (v) => { courant = valeurs.includes(Number(v)) ? Number(v) : valeurs[0]; dessiner(); } };
}

// modeles : équipes enregistrées proposées pour pré-remplir les emplacements (facultatif).
export function editeurEquipe({ jeu, catalogue, colonnes = 2, modeles = null }) {
    const t = TEXTES[jeu];
    const slots = [];
    const grille = h("div", { classe: "editeur-equipe", style: `--colonnes:${colonnes}` });

    function optionsPersonnages() {
        return [{ id: "", libelle: "Aucun personnage" }, ...catalogue.map((c) => ({
            id: c.nom, libelle: c.nom, sous: infoElement(c.element).nom || c.voie || "",
            visuel: () => portrait(jeu, c.nom, { taille: 30, elementCle: c.element }),
        }))];
    }

    function optionsArmes() {
        return [{ id: "", libelle: "Aucune" }, ...armesPour(jeu).map((a) => ({
            id: a.nom, libelle: a.nom, sous: a.rarete ? `${a.rarete}★` : "",
            visuel: () => visuelArme(a.nom),
        }))];
    }

    function visuelArme(nom) {
        const url = urlArme(jeu, nom);
        return h("span", { classe: "tuile-arme" }, url ? h("img", { src: url, alt: "", loading: "lazy" }) : h("span", { classe: "doux" }, "?"));
    }

    function creerSlot(numero) {
        const infos = { perso: "" };
        const boite = h("div", { classe: "slot" });
        const niveau = h("input", { type: "number", min: "1", max: String(t.niveauMax), value: String(t.niveauMax), "aria-label": `Niveau du personnage ${numero}` });
        const doublons = pilules([0, 1, 2, 3, 4, 5, 6], t.doublons);
        const raffinement = pilules([1, 2, 3, 4, 5], t.raff);
        const persoSel = selecteur({ placeholder: `Personnage ${numero}`, options: optionsPersonnages, onChoix: (o) => majPerso(o?.id || "") });
        const armeSel = selecteur({ placeholder: t.choisirArme, options: optionsArmes, libre: true, onChoix: majArme });

        function majPerso(nom) {
            infos.perso = nom;
            const c = catalogue.find((x) => x.nom === nom);
            const couleur = c ? infoElement(c.element).couleur : null;
            boite.style.cssText = couleur ? `--couleur:${couleur}` : "";
            boite.classList.toggle("rempli", Boolean(nom));
        }

        function majArme() {}

        boite.append(
            h("div", { classe: "slot-haut" },
                h("div", { classe: "champ slot-perso" }, h("label", {}, `Personnage ${numero}`), persoSel.element),
                h("div", { classe: "champ slot-niveau" }, h("label", {}, "Niveau"), niveau)),
            h("div", { classe: "champ" }, h("label", {}, "Doublons du personnage"), doublons.element),
            h("div", { classe: "slot-arme" },
                h("div", { classe: "champ slot-arme-choix" }, h("label", {}, t.arme), armeSel.element),
                h("div", { classe: "champ" }, h("label", {}, t.raffinement), raffinement.element)));
        majPerso("");
        majArme(null);

        return {
            element: boite,
            lire() {
                if (!infos.perso) return null;
                const c = catalogue.find((x) => x.nom === infos.perso);
                return {
                    characterId: null, name: infos.perso, element: c?.element || null,
                    level: Math.max(1, Math.min(t.niveauMax, Number(niveau.value) || t.niveauMax)),
                    constellation: doublons.lire(), weapon: armeSel.lire(), refinement: raffinement.lire(),
                };
            },
            definir(brut) {
                if (!brut) {
                    persoSel.definir("");
                    majPerso("");
                    niveau.value = String(t.niveauMax);
                    doublons.definir(0);
                    armeSel.definir("");
                    majArme(null);
                    raffinement.definir(1);
                    return;
                }
                const m = normaliserMembre(brut);
                const connu = catalogue.find((x) => x.nom.toLowerCase() === m.nom.toLowerCase());
                const nom = connu ? connu.nom : m.nom;
                persoSel.definir(nom, true);
                infos.perso = nom;
                majPerso(nom);
                niveau.value = String(Math.max(1, Math.min(t.niveauMax, m.niveau || t.niveauMax)));
                doublons.definir(m.doublons || 0);
                armeSel.definir(m.arme || "");
                majArme(m.arme ? { id: m.arme } : null);
                raffinement.definir(m.raffinement || 1);
            },
        };
    }

    for (let i = 1; i <= 4; i++) {
        const s = creerSlot(i);
        slots.push(s);
        grille.append(s.element);
    }

    function definir(membres) {
        slots.forEach((s, i) => s.definir(membres?.[i] || null));
    }

    const enfants = [];
    if (modeles?.length) {
        const choix = h("select", { "aria-label": "Équipe enregistrée" },
            h("option", { value: "" }, "Équipe enregistrée…"),
            modeles.map((e, i) => h("option", { value: String(i) }, e.nom)));
        choix.addEventListener("change", () => {
            if (choix.value !== "") definir(modeles[Number(choix.value)].membres);
        });
        enfants.push(h("div", { classe: "champ modele-equipe" }, h("label", {}, "Équipe pré-enregistrée"), choix));
    }
    enfants.push(grille);

    return { element: h("div", {}, enfants), lire: () => slots.map((s) => s.lire()).filter(Boolean), definir };
}
