// Honkai: Star Rail : récupération des personnages d'un compte via l'API d'Enka Network (relayée par /api/enka?jeu=starrail)
// et traduction de la réponse en un objet compact, mis en cache dans Supabase. Les statistiques sont calculées ici d'après les
// tables de référence (assets/donnees/enka_starrail.json) : base du personnage, cône, reliques, bonus des sets et des traces.
import { ErreurApi } from "./api.js";

const VIDE = { persos: {}, base: {}, cones: {}, sets: {}, reliques: {}, principales: {}, secondaires: {}, arbre: {} };
let reference = null;
let chargement = null;

export function chargerReferenceStarRail() {
    if (!chargement) {
        chargement = fetch(new URL("../assets/donnees/enka_starrail.json", import.meta.url))
            .then((r) => (r.ok ? r.json() : null))
            .catch(() => null)
            .then((d) => {
                reference = d || VIDE;
                return reference;
            });
    }
    return chargement;
}

function ref() {
    return reference || VIDE;
}

export async function recupererStarRail(uid) {
    let reponse;
    try {
        reponse = await fetch(new URL(`../api/enka?jeu=starrail&uid=${encodeURIComponent(uid)}`, import.meta.url));
    } catch {
        throw new ErreurApi("reseau");
    }
    const corps = await reponse.json().catch(() => null);
    if (!reponse.ok) throw new ErreurApi(corps?.erreur || "enka_indisponible");
    return corps;
}

export const VOIES = {
    Warrior: "Destruction", Knight: "Préservation", Rogue: "Chasse", Mage: "Érudition", Shaman: "Harmonie",
    Warlock: "Nihilité", Priest: "Abondance", Memory: "Mémoire", Elation: "Allégresse",
};
export const ELEMENTS_HSR = { Fire: "FIRE", Ice: "ICE", Wind: "WIND", Thunder: "LIGHTNING", Lightning: "LIGHTNING", Physical: "PHYSICAL", Quantum: "QUANTUM", Imaginary: "IMAGINARY" };
const EMPLACEMENTS = { HEAD: [1, "Tête"], HAND: [2, "Mains"], BODY: [3, "Torse"], FOOT: [4, "Bottes"], NECK: [5, "Sphère"], OBJECT: [6, "Corde"] };
const TYPES_RELIQUE = { 1: "HEAD", 2: "HAND", 3: "BODY", 4: "FOOT", 5: "NECK", 6: "OBJECT" };

const LIBELLES = {
    HPDelta: "PV", AttackDelta: "ATQ", DefenceDelta: "DÉF", SpeedDelta: "VIT",
    HPAddedRatio: "PV %", AttackAddedRatio: "ATQ %", DefenceAddedRatio: "DÉF %", SpeedAddedRatio: "VIT %",
    CriticalChanceBase: "Taux CRIT", CriticalDamageBase: "DGT CRIT", StatusProbabilityBase: "Taux de touche des effets",
    StatusResistanceBase: "RÉS aux effets", BreakDamageAddedRatioBase: "Effet de rupture", HealRatioBase: "Bonus de soins",
    SPRatioBase: "Régén. d'énergie", PhysicalAddedRatio: "DGT Physique", FireAddedRatio: "DGT Feu", IceAddedRatio: "DGT Glace",
    ThunderAddedRatio: "DGT Foudre", WindAddedRatio: "DGT Vent", QuantumAddedRatio: "DGT Quantique", ImaginaryAddedRatio: "DGT Imaginaire",
};
const PLATS = new Set(["HPDelta", "AttackDelta", "DefenceDelta", "SpeedDelta"]);
const BONUS_ELEMENT = { Fire: "FireAddedRatio", Ice: "IceAddedRatio", Wind: "WindAddedRatio", Thunder: "ThunderAddedRatio", Lightning: "ThunderAddedRatio", Physical: "PhysicalAddedRatio", Quantum: "QuantumAddedRatio", Imaginary: "ImaginaryAddedRatio" };

function arrondi(n, d = 1) {
    const f = 10 ** d;
    return Math.round(Number(n) * f) / f;
}

function propriete(prop, valeur) {
    const pct = !PLATS.has(prop);
    return { prop, libelle: LIBELLES[prop] || prop, valeur: arrondi(pct ? valeur * 100 : valeur, pct ? 1 : 1), pct };
}

function normaliserRelique(r) {
    const tables = ref();
    const info = tables.reliques[String(r.tid)];
    const type = info?.[1] || TYPES_RELIQUE[r.type] || "HEAD";
    const [rang, emplacement] = EMPLACEMENTS[type] || [9, "Relique"];
    const niveau = Number(r.level || 0);
    const principale = tables.principales[String(info?.[4])]?.[String(r.mainAffixId)];
    const sous = (r.subAffixList || []).map((s) => {
        const def = tables.secondaires[String(info?.[5])]?.[String(s.affixId)];
        if (!def) return null;
        const lancers = Number(s.cnt || 1);
        return { ...propriete(def[0], def[1] * lancers + def[2] * Number(s.step || 0)), lancers };
    }).filter(Boolean);
    const cv = sous.reduce((somme, s) => somme + (s.prop === "CriticalChanceBase" ? 2 * s.valeur : s.prop === "CriticalDamageBase" ? s.valeur : 0), 0);
    const set = tables.sets[String(info?.[0])];
    return {
        rang, emplacement, niveau, rarete: info?.[2] || 5, icone: info?.[3] || null, setId: String(info?.[0] || ""), set: set?.nom || "", setEn: set?.en || "",
        principale: principale ? propriete(principale[0], principale[1] + principale[2] * niveau) : { prop: "", libelle: "-", valeur: 0, pct: false },
        sous, cv: arrondi(cv),
    };
}

function normaliserCone(e, voie) {
    if (!e) return null;
    const tables = ref();
    const info = tables.cones[String(e.tid)];
    return {
        id: String(e.tid), nom: info?.nom || `Cône ${e.tid}`, en: info?.en || "", niveau: Number(e.level || 1), rang: Number(e.rank || 1),
        rarete: info?.rarete || 3, voie: info?.voie || voie,
    };
}

function calculerStats(a, info, reliques) {
    const tables = ref();
    const promo = String(a.promotion ?? 0);
    const niveau = Number(a.level || 1);
    const b = tables.base[String(a.avatarId)]?.[promo] || [0, 0, 0, 0, 0, 0, 100, 0.05, 0.5];
    const cone = a.equipment ? tables.cones[String(a.equipment.tid)]?.base?.[String(a.equipment.promotion ?? 0)] : null;
    const nc = Number(a.equipment?.level || 1) - 1;
    const base = {
        hp: b[0] + b[1] * (niveau - 1) + (cone ? cone[0] + cone[1] * nc : 0),
        atk: b[2] + b[3] * (niveau - 1) + (cone ? cone[2] + cone[3] * nc : 0),
        def: b[4] + b[5] * (niveau - 1) + (cone ? cone[4] + cone[5] * nc : 0),
    };
    const somme = {};
    const ajouter = (prop, v) => {
        somme[prop] = (somme[prop] || 0) + Number(v);
    };
    for (const r of a.relicList || []) {
        const n = normaliserRelique(r);
        if (n.principale.prop) ajouter(n.principale.prop, n.principale.pct ? n.principale.valeur / 100 : n.principale.valeur);
        for (const s of n.sous) ajouter(s.prop, s.pct ? s.valeur / 100 : s.valeur);
    }
    const parSet = {};
    for (const r of reliques) if (r.setId) parSet[r.setId] = (parSet[r.setId] || 0) + 1;
    for (const [sid, nb] of Object.entries(parSet)) {
        for (const [seuil, props] of Object.entries(tables.sets[sid]?.bonus || {})) {
            if (nb >= Number(seuil)) for (const [p, v] of Object.entries(props)) ajouter(p, v);
        }
    }
    for (const t of a.skillTreeList || []) {
        const props = tables.arbre[String(t.pointId)]?.[String(t.level)];
        if (props) for (const [p, v] of Object.entries(props)) ajouter(p, v);
    }
    const s = (p) => somme[p] || 0;
    const hp = base.hp * (1 + s("HPAddedRatio")) + s("HPDelta");
    const atk = base.atk * (1 + s("AttackAddedRatio")) + s("AttackDelta");
    const def = base.def * (1 + s("DefenceAddedRatio")) + s("DefenceDelta");
    const vit = b[6] * (1 + s("SpeedAddedRatio")) + s("SpeedDelta");
    const stats = [
        { libelle: "PV", valeur: arrondi(hp, 0), pct: false },
        { libelle: "ATQ", valeur: arrondi(atk, 0), pct: false },
        { libelle: "DÉF", valeur: arrondi(def, 0), pct: false },
        { libelle: "VIT", valeur: arrondi(vit, 1), pct: false },
        { libelle: "Taux CRIT", valeur: arrondi((b[7] + s("CriticalChanceBase")) * 100), pct: true },
        { libelle: "DGT CRIT", valeur: arrondi((b[8] + s("CriticalDamageBase")) * 100), pct: true },
        { libelle: "Effet de rupture", valeur: arrondi(s("BreakDamageAddedRatioBase") * 100), pct: true },
        { libelle: "Régén. d'énergie", valeur: arrondi(100 + s("SPRatioBase") * 100), pct: true },
        { libelle: "Taux de touche des effets", valeur: arrondi(s("StatusProbabilityBase") * 100), pct: true },
        { libelle: "RÉS aux effets", valeur: arrondi(s("StatusResistanceBase") * 100), pct: true },
    ];
    const propElement = BONUS_ELEMENT[info.element];
    if (propElement) stats.push({ libelle: LIBELLES[propElement], valeur: arrondi(s(propElement) * 100), pct: true });
    if (s("HealRatioBase")) stats.push({ libelle: "Bonus de soins", valeur: arrondi(s("HealRatioBase") * 100), pct: true });
    return stats;
}

function normaliserPersonnage(a) {
    const tables = ref();
    const info = tables.persos[String(a.avatarId)] || { nom: `Personnage ${a.avatarId}`, en: "", element: "", voie: "", rarete: 4 };
    const reliques = (a.relicList || []).map(normaliserRelique).sort((x, y) => x.rang - y.rang);
    const parSet = {};
    for (const r of reliques) if (r.setId) parSet[r.setId] = { nom: r.set, en: r.setEn, pieces: (parSet[r.setId]?.pieces || 0) + 1 };
    return {
        id: String(a.avatarId), nom: info.nom, en: info.en, element: ELEMENTS_HSR[info.element] || String(info.element || "").toUpperCase(),
        voie: info.voie, rarete: info.rarete, niveau: Number(a.level || 1), rang: Number(a.rank || 0),
        cone: normaliserCone(a.equipment, info.voie), reliques,
        sets: Object.values(parSet).filter((s) => s.pieces >= 2).map((s) => ({ nom: s.nom, en: s.en, pieces: s.pieces >= 4 ? 4 : 2 })),
        stats: calculerStats(a, info, reliques),
        cvTotal: arrondi(reliques.reduce((somme, r) => somme + r.cv, 0)),
    };
}

// Réponse brute d'Enka -> objet enregistré dans Supabase. Lève enka_vitrine_vide_hsr si aucun personnage n'est exposé.
export function normaliserStarRail(brut) {
    const detail = brut?.detailInfo || {};
    const vus = new Set();
    const liste = [...(detail.avatarDetailList || []), ...(detail.assistAvatarList || []).filter((x) => (x.relicList || []).length || x.equipment)]
        .filter((a) => a && a.avatarId && !vus.has(a.avatarId) && vus.add(a.avatarId));
    if (!liste.length) throw new ErreurApi("enka_vitrine_vide_hsr");
    const personnages = liste.map(normaliserPersonnage);
    return {
        version: 1,
        joueur: {
            pseudo: detail.nickname || "", niveau: detail.level || 0, niveauMonde: detail.worldLevel || 0, signature: detail.signature || "",
            avatar: personnages[0]?.en || "",
        },
        personnages,
    };
}
