// Données statiques générées depuis l'application de bureau (scripts/generer_donnees.py) :
// noms des armes et cônes, boss du Carnage chtonien par patch. Les icônes d'armes viennent d'Enka (js/icones.js).
import { chargerIcones } from "./icones.js";

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
