// Données statiques générées depuis l'application de bureau (scripts/generer_donnees.py) :
// noms des armes et cônes, boss du Carnage chtonien par patch. Les icônes d'armes viennent d'Enka (js/icones.js).
import { h } from "./dom.js";
import { chargerIcones } from "./icones.js";
import { couleurDifficulte } from "./regles.js";

let armes = { genshin: [], starrail: [] };
let boss = {};
let chargement = null;

function charger(chemin, defaut) {
    return fetch(new URL(`../assets/donnees/${chemin}`, import.meta.url))
        .then((r) => (r.ok ? r.json() : defaut))
        .catch(() => defaut);
}

export function chargerDonnees() {
    if (!chargement) {
        chargement = Promise.all([charger("armes.json", armes), charger("boss.json", boss), chargerIcones()]).then(([a, b]) => {
            armes = { genshin: [], starrail: [], ...a };
            boss = b || {};
        });
    }
    return chargement;
}

export function armesPour(jeu) {
    return armes[jeu] || [];
}

export function bossDuPatch(patch) {
    return boss[String(patch || "").trim()] || { periode: "", boss: [] };
}

export function nomBoss(cle) {
    return String(cle || "").trim().replace(/([a-zà-ÿ])([A-Z])/g, "$1 $2");
}

export function urlBoss(cle) {
    return cle ? new URL(`../assets/img/genshinBoss/${encodeURIComponent(String(cle).replace(/\s+/g, ""))}.png`, import.meta.url).href : null;
}

export function urlDifficulte(n) {
    return new URL(`../assets/img/difficulte/diff${n}.png`, import.meta.url).href;
}

// Image de la difficulté du Carnage chtonien ; le chiffre coloré si l'image est introuvable.
export function imageDifficulte(n, classe = "") {
    return h("img", {
        classe, width: "40", height: "40", src: urlDifficulte(n), alt: `Difficulté ${n}`, title: `Difficulté ${n}`,
        onerror: (e) => e.target.replaceWith(h("span", { classe: "badge-difficulte", style: `--couleur:${couleurDifficulte(n)}`, title: `Difficulté ${n}` }, String(n))),
    });
}

const IMAGE_THEATRE = { EASY: 1, NORMAL: 2, HARD: 3, VISIONARY: 4, LUNAR: 5 };

// Image du mode du Théâtre de l'imaginarium, seulement quand tous les astres du mode sont obtenus (sinon null).
export function imageTheatre(t, bilan, classe = "") {
    const n = IMAGE_THEATRE[t.mode];
    if (!n || !bilan.max || bilan.total < bilan.max) return null;
    return h("img", {
        classe, width: "44", height: "44", alt: bilan.mode.nom, title: `${bilan.mode.nom} : tous les astres`,
        src: new URL(`../assets/img/difficulte/Theater_${n}.webp`, import.meta.url).href, onerror: (e) => e.target.remove(),
    });
}
