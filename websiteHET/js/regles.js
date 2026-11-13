// Règles de calcul des endgames, partagées par les pages de jeu et par le résumé de la page Compte.

export const MODES_THEATRE = {
    EASY: { nom: "Facile", actes: 3 },
    NORMAL: { nom: "Normal", actes: 6 },
    HARD: { nom: "Difficile", actes: 8 },
    VISIONARY: { nom: "Défi Visionnaire", actes: 10 },
    LUNAR: { nom: "Défi Lunaire", actes: 12 },
};

export const MOIS = ["janvier", "février", "mars", "avril", "mai", "juin", "juillet", "août", "septembre", "octobre", "novembre", "décembre"];
export const COULEURS_DIFFICULTE = ["#7BD88F", "#B6DC6B", "#E6D45F", "#F2AE55", "#EE7F5E", "#E4577A"];

export function couleurDifficulte(difficulte) {
    return COULEURS_DIFFICULTE[Math.min(6, Math.max(1, difficulte)) - 1];
}

export function bilanAbysse(a) {
    return { total: a.etoiles_base + a.salle1 + a.salle2 + a.salle3, max: 9 * (a.etage - 8) };
}

export function bilanTheatre(t) {
    const mode = MODES_THEATRE[t.mode] || { nom: t.mode, actes: t.etoiles_flags.length };
    const flags = t.etoiles_flags.length ? t.etoiles_flags : Array.from({ length: mode.actes }, (_, i) => i < t.etoiles);
    return { mode, flags, total: flags.filter(Boolean).length, max: flags.length };
}

export function secondesCarnage(c) {
    return (c.boss || []).reduce((somme, b) => somme + (Number(b.seconds) || 0), 0);
}

export const SCORE_SALLE = 80000;
export const SCORE_SALLE_STELLAIRE = 120000;
export const SCORE_BOSS = 4000;
export const MODES_APOC = {
    D3: { nom: "Difficulté 3", boss: 2 },
    D4: { nom: "Difficulté 4", boss: 2 },
    D4_STELLAR: { nom: "Difficulté 4 · stellaire", boss: 3 },
};

const CADRES = [
    null,
    { nom: "Cadre Bronze", couleur: "#CD7F32", image: "bronze.png" },
    { nom: "Cadre Argent", couleur: "#C8D0DC", image: "argent.png" },
    { nom: "Cadre Dorée", couleur: "#FFC83D", image: "dore.png" },
];
const CADRE_PRISMATIQUE = { nom: "Cadre Prismatique", couleur: "#C77DFF", image: "prismatique.png" };

export function cycles(n) {
    return `${n} ${n > 1 ? "cycles" : "cycle"}`;
}

export function etoilesPourCycles(n) {
    if (n <= 2) return 3;
    if (n <= 4) return 2;
    if (n <= 6) return 1;
    return 0;
}

export function etoilesCombat(n, ko) {
    return Math.max(0, etoilesPourCycles(n) - (ko ? 1 : 0));
}

export function bilanMoc(c) {
    return { total: c.etoiles.reduce((a, b) => a + b, 0), cycles: c.cycles.reduce((a, b) => a + b, 0), max: 36 };
}

export function maxSallePf(c, i) {
    return i === 3 && c.stellaire ? SCORE_SALLE_STELLAIRE : SCORE_SALLE;
}

export function scoreSallePf(c, i) {
    return (i === 0 && c.auto1) || (i === 1 && c.auto2) ? SCORE_SALLE : c.scores[i] ?? 0;
}

export function bilanApoc(c) {
    const mode = MODES_APOC[c.mode] || MODES_APOC.D4;
    return { mode, total: c.scores.slice(0, mode.boss).reduce((a, b) => a + b, 0), max: mode.boss * SCORE_BOSS };
}

export function bilanArbitre(c) {
    const etoilesRoi = etoilesCombat(c.cycles_roi, c.roi_ko);
    const etoilesCavaliers = [0, 1, 2].map((i) => etoilesCombat(c.cycles[i] ?? 0, c.ko[i]));
    let cadre;
    if (etoilesRoi === 3) cadre = c.cycles_roi > 0 ? CADRES[3] : CADRE_PRISMATIQUE;
    else cadre = CADRES[etoilesRoi];
    return { etoilesRoi, etoilesCavaliers, total: etoilesCavaliers.reduce((a, b) => a + b, 0) + etoilesRoi, max: 12, cadre };
}
