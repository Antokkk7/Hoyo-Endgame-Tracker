// Données statiques générées depuis l'application de bureau (scripts/generer_donnees.py) :
// armes et cônes, boss du Carnage chtonien par patch.
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
        chargement = Promise.all([charger("armes.json", armes), charger("boss.json", boss)]).then(([a, b]) => {
            armes = { genshin: [], starrail: [], ...a };
            boss = b || {};
        });
    }
    return chargement;
}

export function armesPour(jeu) {
    return armes[jeu] || [];
}

export function urlArme(jeu, nom) {
    const arme = armesPour(jeu).find((a) => a.nom.toLowerCase() === String(nom || "").toLowerCase());
    return arme?.image ? new URL(`../assets/armes/${jeu}/${encodeURIComponent(arme.image)}`, import.meta.url).href : null;
}

export function bossDuPatch(patch) {
    return boss[String(patch || "").trim()] || { periode: "", boss: [] };
}

export function nomBoss(cle) {
    return String(cle || "").trim().replace(/([a-zà-ÿ])([A-Z])/g, "$1 $2");
}

export function urlBoss(cle) {
    return cle ? new URL(`../assets/img/boss/${encodeURIComponent(String(cle).replace(/\s+/g, ""))}.png`, import.meta.url).href : null;
}

export function urlDifficulte(n) {
    return new URL(`../assets/img/difficulte/diff${n}.png`, import.meta.url).href;
}
