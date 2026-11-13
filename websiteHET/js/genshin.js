// Page Genshin Impact : historique des Abysses spiralées, du Théâtre de l'imaginarium et du Carnage chtonien.
import { h, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { urlDifficulte } from "./donnees.js";
import { formulaireAbysse, formulaireCarnage, formulaireTheatre } from "./edition-genshin.js";
import { rangeeEquipe, normaliserMembre } from "./equipes.js";
import { initJeu, pastille, pastilleElement, etoiles, vide } from "./jeu-commun.js";
import { portrait } from "./portraits.js";
import { bilanAbysse, bilanTheatre, couleurDifficulte, MOIS, secondesCarnage } from "./regles.js";
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

const TYPES = {
    abysse: {
        cle: "abysses", titre: "Abysses spiralées", carte: carteAbysse, formulaire: formulaireAbysse, enregistrer: "abysse_enregistrer",
        vide: "Aucun clear d'Abysse enregistré.",
    },
    theatre: {
        cle: "theatre", titre: "Théâtre de l'imaginarium", carte: carteTheatre, formulaire: formulaireTheatre, enregistrer: "theatre_enregistrer",
        vide: "Aucun clear de Théâtre enregistré.",
    },
    carnage: {
        cle: "carnage", titre: "Carnage chtonien", carte: carteCarnage, formulaire: formulaireCarnage, enregistrer: "carnage_enregistrer",
        vide: "Aucun clear de Carnage enregistré.",
    },
};

function nombreClears(n) {
    return `${n} ${n > 1 ? "clears enregistrés" : "clear enregistré"}`;
}

function carteAccueil(type, donnees, ouvrir) {
    const liste = donnees[TYPES[type].cle];
    const dernier = liste[0];
    let droite;
    if (type === "abysse") {
        const b = dernier ? bilanAbysse(dernier) : null;
        droite = [h("span", { classe: "eg-grand vert" }, b ? `${b.total}/${b.max}` : "-"), h("span", { classe: "eg-legende" }, b ? `étoiles · patch ${dernier.patch}` : "aucun clear")];
    } else if (type === "theatre") {
        const b = dernier ? bilanTheatre(dernier) : null;
        const arcanum = dernier && dernier.mode === "LUNAR" ? ` · Arcanum ${dernier.arcanum}/2` : "";
        droite = [h("span", { classe: "eg-grand magenta" }, b ? `${b.total}/${b.max}` : "-"), h("span", { classe: "eg-legende" }, b ? `astres${arcanum}` : "aucun clear")];
    } else {
        droite = dernier
            ? [h("img", { classe: "eg-difficulte", src: urlDifficulte(dernier.difficulte), alt: `Difficulté ${dernier.difficulte}`, onerror: (e) => e.target.replaceWith(h("span", { classe: "badge-difficulte", style: `--couleur:${couleurDifficulte(dernier.difficulte)}` }, String(dernier.difficulte))) }),
                h("span", { classe: "eg-legende" }, `${secondesCarnage(dernier)} s`)]
            : [h("span", { classe: "eg-grand cyan" }, "-"), h("span", { classe: "eg-legende" }, "aucun clear")];
    }
    return h("article", { classe: `eg-accueil eg-${type}` },
        h("div", { classe: "eg-gauche" },
            h("h3", {}, TYPES[type].titre),
            h("p", { classe: "doux petit" }, nombreClears(liste.length)),
            h("button", { classe: "bouton bouton-principal", type: "button", onclick: ouvrir }, "Ouvrir")),
        h("div", { classe: "eg-droite" }, droite));
}

async function endgames(zone) {
    let donnees = await rpc("endgames_genshin", { p_token: jeton() });
    const catalogue = await rpc("catalogue", { p_jeu: JEU });
    let aSupprimer = null;

    function accueil() {
        remplacer(zone, h("div", { classe: "eg-liste" }, ["abysse", "theatre", "carnage"].map((t) => carteAccueil(t, donnees, () => historique(t)))));
    }

    function historique(type, erreur = null) {
        const def = TYPES[type];
        const clears = donnees[def.cle];
        remplacer(zone,
            h("div", { classe: "barre-actions" },
                h("button", { classe: "bouton", type: "button", onclick: () => { aSupprimer = null; accueil(); } }, "\u2190 Retour"),
                h("h2", { classe: "titre-historique" }, def.titre),
                h("button", { classe: "bouton bouton-principal", type: "button", onclick: () => formulaire(type, null) }, "Ajouter un clear")),
            erreur ? h("p", { classe: "message erreur", role: "alert" }, erreur) : null,
            clears.length
                ? h("div", { classe: "historique" }, clears.map((c) => {
                    const carte = def.carte(c);
                    carte.append(h("div", { classe: "carte-actions" },
                        h("button", { classe: "bouton bouton-petit", type: "button", onclick: () => formulaire(type, c) }, "Modifier"),
                        aSupprimer === c.id
                            ? [h("button", { classe: "bouton bouton-petit bouton-danger", type: "button", onclick: () => supprimer(type, c) }, "Confirmer la suppression"),
                                h("button", { classe: "bouton bouton-petit", type: "button", onclick: () => { aSupprimer = null; historique(type); } }, "Garder")]
                            : h("button", { classe: "bouton bouton-petit bouton-danger", type: "button", onclick: () => { aSupprimer = c.id; historique(type); } }, "Supprimer")));
                    return carte;
                }))
                : vide(def.vide));
    }

    async function supprimer(type, clear) {
        aSupprimer = null;
        try {
            donnees = await rpc("clear_genshin_supprimer", { p_token: jeton(), p_type: type, p_id: clear.id });
            historique(type);
        } catch (err) {
            historique(type, err.message);
        }
    }

    function formulaire(type, existant) {
        const def = TYPES[type];
        remplacer(zone, def.formulaire({
            catalogue, equipes: donnees.equipes, existant,
            annuler: () => historique(type),
            surFin: async (payload) => {
                donnees = await rpc(def.enregistrer, { p_token: jeton(), p_id: existant?.id ?? null, p_data: payload });
                historique(type);
            },
        }));
    }

    accueil();
}

initJeu({ jeu: JEU, nomJeu: "Genshin Impact", prefixes: PREFIXES, endgames });
