// Portraits de personnages : icône d'Enka Network (js/icones.js), sauf pour les images locales de
// assets/personnages (les Traveler de Genshin, voir manifeste.json) qui restent prioritaires ; sinon initiales colorées.
import { h } from "./dom.js";
import { element } from "./elements.js";
import { chargerIcones, cle, imageEnka, listePersonnages, urlsPerso } from "./icones.js";

const VIDE = { genshin: {}, starrail: {} };
let manifeste = null;
let chargement = null;

// Charge le manifeste des images locales et la table des icônes Enka.
export function chargerManifeste() {
    if (manifeste) return Promise.resolve(manifeste);
    if (!chargement) {
        chargement = Promise.all([
            fetch(new URL("../assets/personnages/manifeste.json", import.meta.url)).then((r) => (r.ok ? r.json() : VIDE)).catch(() => VIDE),
            chargerIcones(),
        ]).then(([m]) => {
            manifeste = { ...VIDE, ...m };
            return manifeste;
        });
    }
    return chargement;
}

export { cle };

function urlLocale(jeu, nom, elementCle) {
    let k = cle(nom);
    // Le Traveler d'Enka n'a pas d'élément dans son nom : on le déduit de l'élément du personnage.
    if (jeu === "genshin" && /^(traveler|voyageur|voyageuse|aether|lumine)$/.test(k) && elementCle) k = `traveler${cle(elementCle)}`;
    const fichier = manifeste?.[jeu]?.[k];
    return fichier ? new URL(`../assets/personnages/${jeu}/${encodeURIComponent(fichier)}`, import.meta.url).href : null;
}

export function urlsPortrait(jeu, nom, elementCle = null) {
    const locale = urlLocale(jeu, nom, elementCle);
    return [locale, ...urlsPerso(jeu, nom)].filter(Boolean);
}

// Personnages proposés comme image de profil : catalogue du site (icônes Enka) et images locales (Traveler).
export function listePortraits(jeu) {
    const liste = listePersonnages(jeu);
    const connus = new Set(liste.map((p) => cle(p.nom)));
    for (const [k, fichier] of Object.entries(manifeste?.[jeu] || {})) {
        if (!connus.has(k)) liste.push({ nom: fichier.replace(/\.[^.]+$/, "") });
    }
    return liste.sort((a, b) => a.nom.localeCompare(b.nom, "fr", { sensitivity: "base" }));
}

function initiales(nom) {
    const n = String(nom || "?").trim();
    return n.slice(0, 2).toUpperCase() || "?";
}

export function portrait(jeu, nom, { taille = 56, elementCle = null, rond = false } = {}) {
    const couleur = elementCle ? element(elementCle).couleur : null;
    const el = h("span", {
        classe: "portrait" + (rond ? " rond" : ""),
        style: `--taille:${taille}px;` + (couleur ? `--couleur:${couleur};` : ""),
        title: nom || "",
    });
    const repli = () => h("span", { classe: "portrait-initiales" }, initiales(nom));
    el.append(imageEnka(urlsPortrait(jeu, nom, elementCle), { alt: nom || "", repli }) || repli());
    return el;
}
