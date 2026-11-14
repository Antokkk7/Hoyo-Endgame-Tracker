// Icônes d'Enka Network (https://enka.network/ui/) pour les armes, cônes, sets d'artéfacts et personnages
// de Genshin Impact et Honkai: Star Rail. Aucune de ces images n'est stockée dans le site : la table
// assets/donnees/enka_icones.json (scripts/generer_icones.py) relie un nom à un nom de fichier Enka.
import { h } from "./dom.js";

const VIDE_JEU = { persos: {}, armes: {}, sets: {}, liste: [] };
const VIDE = { base: "https://enka.network/ui/", genshin: VIDE_JEU, starrail: VIDE_JEU };
let table = null;
let chargement = null;

export function chargerIcones() {
    if (!chargement) {
        chargement = fetch(new URL("../assets/donnees/enka_icones.json", import.meta.url))
            .then((r) => (r.ok ? r.json() : VIDE))
            .catch(() => VIDE)
            .then((t) => {
                table = { ...VIDE, ...t };
                return table;
            });
    }
    return chargement;
}

export function cle(nom) {
    return String(nom || "").toLowerCase().replace(/[^a-z0-9]/g, "");
}

function jeuTable(jeu) {
    return (table || VIDE)[jeu] || VIDE_JEU;
}

function url(chemin) {
    return (table || VIDE).base + chemin;
}

// Adresses candidates, de la plus probable à la moins probable : l'image bascule sur la suivante si une adresse échoue.
export function urlsArme(jeu, nom) {
    const ref = jeuTable(jeu).armes[cle(nom)];
    if (!ref) return [];
    if (jeu === "starrail") {
        return [`hsr/SpriteOutput/LightConeMediumIcon/${ref}.png`, `hsr/SpriteOutput/LightConeFigures/${ref}.png`, `hsr/SpriteOutput/LightConeMaxFigures/${ref}.png`].map(url);
    }
    return [url(`${ref}.png`)];
}

export function urlsSet(jeu, nom) {
    const ref = jeuTable(jeu).sets[cle(nom)];
    return ref ? [url(jeu === "starrail" ? ref : `${ref}.png`)] : [];
}

export function urlsPerso(jeu, nom) {
    const ref = jeuTable(jeu).persos[cle(nom)];
    if (!ref) return [];
    if (jeu === "starrail") return [url(`hsr/SpriteOutput/AvatarRoundIcon/Avatar/${ref}.png`)];
    return [url(`UI_AvatarIcon_${ref}.png`), url(`UI_AvatarIcon_${ref}_Circle.png`), url(`UI_AvatarIcon_Side_${ref}.png`)];
}

// Personnages du catalogue du site avec leur icône : [{ nom }]
export function listePersonnages(jeu) {
    return jeuTable(jeu).liste.map(([nom]) => ({ nom }));
}

// Un nom de set peut en désigner deux (« Set A + Set B » pour un 2P+2P) : une entrée par set.
export function nomsDeSets(nom) {
    return String(nom || "").split(/\s+\+\s+/).map((n) => n.trim()).filter(Boolean);
}

// Image avec repli : essaie chaque adresse, puis affiche repli() (ou rien) si aucune ne répond.
export function imageEnka(urls, { classe = "", alt = "", repli = null } = {}) {
    const liste = (urls || []).filter(Boolean);
    if (!liste.length) return repli ? repli() : null;
    let i = 0;
    const img = h("img", { classe, src: liste[0], alt, loading: "lazy", referrerpolicy: "no-referrer" });
    img.addEventListener("error", () => {
        i += 1;
        if (i < liste.length) img.src = liste[i];
        else if (repli) img.replaceWith(repli());
        else img.remove();
    });
    return img;
}
