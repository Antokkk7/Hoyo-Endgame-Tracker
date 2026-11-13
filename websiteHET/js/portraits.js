// Portraits de personnages : image du dossier assets/personnages si elle existe, sinon initiales colorées.
import { h } from "./dom.js";
import { element } from "./elements.js";

const VIDE = { genshin: {}, starrail: {} };
let manifeste = null;
let chargement = null;

export function chargerManifeste() {
    if (manifeste) return Promise.resolve(manifeste);
    if (!chargement) {
        chargement = fetch(new URL("../assets/personnages/manifeste.json", import.meta.url))
            .then((r) => (r.ok ? r.json() : VIDE))
            .catch(() => VIDE)
            .then((m) => {
                manifeste = { ...VIDE, ...m };
                return manifeste;
            });
    }
    return chargement;
}

export function cle(nom) {
    return String(nom || "").toLowerCase().replace(/[^a-z0-9]/g, "");
}

export function urlPortrait(jeu, nom) {
    const fichier = manifeste?.[jeu]?.[cle(nom)];
    if (!fichier) return null;
    return new URL(`../assets/personnages/${jeu}/${encodeURIComponent(fichier)}`, import.meta.url).href;
}

export function listePortraits(jeu) {
    return Object.entries(manifeste?.[jeu] || {}).map(([k, fichier]) => ({
        cle: k,
        fichier,
        nom: fichier.replace(/\.[^.]+$/, "").replace(/_/g, " "),
    }));
}

function initiales(nom) {
    const n = String(nom || "?").trim();
    return n.slice(0, 2).toUpperCase() || "?";
}

export function portrait(jeu, nom, { taille = 56, elementCle = null, rond = false } = {}) {
    const url = urlPortrait(jeu, nom);
    const couleur = elementCle ? element(elementCle).couleur : null;
    const el = h("span", {
        classe: "portrait" + (rond ? " rond" : ""),
        style: `--taille:${taille}px;` + (couleur ? `--couleur:${couleur};` : ""),
        title: nom || "",
    });
    if (url) {
        el.append(h("img", { src: url, alt: nom || "", loading: "lazy" }));
    } else {
        el.append(h("span", { classe: "portrait-initiales" }, initiales(nom)));
    }
    return el;
}
