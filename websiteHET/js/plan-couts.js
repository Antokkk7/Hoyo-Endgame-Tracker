// Coûts de montée en niveau Genshin Impact : paliers d'ascension des personnages et niveaux de talents.
// Les EXP de niveau, l'ascension des armes et la résine ne sont pas comptés.

export const PALIERS_PERSONNAGE = [20, 40, 50, 60, 70, 80];
export const PALIERS_ARME_BASSE = [20, 40, 50, 60];

// Suite des niveaux atteignables : 1 à max, avec un état « ascendu » (✦) sur chaque palier.
export function sequenceNiveaux(max = 90, paliers = PALIERS_PERSONNAGE) {
    const suite = [];
    for (let n = 1; n <= max; n++) {
        suite.push({ niveau: n, asc: false });
        if (paliers.includes(n) && n < max) suite.push({ niveau: n, asc: true });
    }
    return suite;
}

export function libelleNiveau(n) {
    return `${n.niveau}${n.asc ? "✦" : ""}`;
}

export function rangAscension(niveau, asc, paliers = PALIERS_PERSONNAGE) {
    return paliers.filter((p) => p < niveau).length + (asc ? 1 : 0);
}

const VIDE = () => ({ mora: 0, enseignements: 0, conseils: 0, philosophies: 0, hebdo: 0, couronnes: 0, gemmes: 0, boss: 0, locales: 0, butin: 0 });

// Une ligne par palier d'ascension : mora, gemmes, matériaux de boss, spécialités locales, butin de monstres.
const ASCENSION = [
    { mora: 20000, gemmes: 1, boss: 0, locales: 3, butin: 3 },
    { mora: 40000, gemmes: 3, boss: 2, locales: 10, butin: 15 },
    { mora: 60000, gemmes: 6, boss: 4, locales: 20, butin: 12 },
    { mora: 80000, gemmes: 3, boss: 8, locales: 30, butin: 18 },
    { mora: 100000, gemmes: 6, boss: 12, locales: 45, butin: 12 },
    { mora: 120000, gemmes: 6, boss: 20, locales: 60, butin: 24 },
];

// Une ligne par niveau de talent atteint (2 à 10).
const TALENT = [
    { mora: 12500, enseignements: 3, butin: 6 },
    { mora: 17500, conseils: 2, butin: 3 },
    { mora: 25000, conseils: 4, butin: 4 },
    { mora: 30000, conseils: 6, butin: 6 },
    { mora: 37500, conseils: 9, butin: 9 },
    { mora: 120000, philosophies: 4, butin: 4, hebdo: 1 },
    { mora: 260000, philosophies: 6, butin: 6, hebdo: 1 },
    { mora: 450000, philosophies: 12, butin: 9, hebdo: 2 },
    { mora: 700000, philosophies: 16, butin: 12, hebdo: 2, couronnes: 1 },
];

function ajouter(total, ligne) {
    for (const [cle, valeur] of Object.entries(ligne)) total[cle] = (total[cle] || 0) + valeur;
}

export function coutPersonnage(d) {
    const total = VIDE();
    const de = rangAscension(d.niveauActuel ?? 1, d.ascActuelle);
    const vers = rangAscension(d.niveauDesire ?? 90, d.ascDesiree);
    for (let r = de; r < vers; r++) ajouter(total, ASCENSION[r]);
    for (const [actuel, desire] of d.talents || []) {
        for (let n = actuel + 1; n <= desire; n++) ajouter(total, TALENT[n - 2]);
    }
    return total;
}

export function coutObjectif(objectif) {
    return objectif.type === "personnage" ? coutPersonnage(objectif.donnees) : VIDE();
}

export function sommeCouts(liste) {
    const total = VIDE();
    for (const c of liste) ajouter(total, c);
    return total;
}

export function formaterMora(n) {
    if (n >= 1000000) return `~${(n / 1000000).toFixed(1).replace(".", ",")}M`;
    if (n >= 1000) return `~${Math.round(n / 1000)}K`;
    return n ? `~${n}` : "0";
}

// Pastilles de matériaux affichées sur une carte ou dans le résumé : [clé, libellé court, libellé complet, valeur].
export function pastillesCout(c) {
    const livres = c.enseignements + c.conseils + c.philosophies;
    return [
        ["livres", "Livres", `Livres de talents : ${c.enseignements} Enseignements, ${c.conseils} Conseils, ${c.philosophies} Philosophies`, livres],
        ["gemmes", "Gemmes", "Gemmes d'ascension", c.gemmes],
        ["boss", "Boss", "Matériaux de boss normal", c.boss],
        ["locales", "Locales", "Spécialités locales", c.locales],
        ["butin", "Butin", "Butin de monstres", c.butin],
        ["hebdo", "Hebdo", "Matériaux de boss hebdomadaire", c.hebdo],
        ["couronnes", "Couronnes", "Couronnes de sagesse", c.couronnes],
    ].filter((p) => p[3] > 0);
}
