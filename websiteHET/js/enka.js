// Récupération des personnages du compte via l'API d'Enka Network (relayée par /api/enka) et traduction de la
// réponse en un objet compact, mis en cache dans Supabase. Les noms et les statistiques sont en français.
import { ErreurApi } from "./api.js";

let reference = null;
let chargement = null;

export function chargerReferenceEnka() {
    if (!chargement) {
        chargement = fetch(new URL("../assets/donnees/enka_genshin.json", import.meta.url))
            .then((r) => (r.ok ? r.json() : null))
            .catch(() => null)
            .then((d) => {
                reference = d || { persos: {}, armes: {}, noms: {}, libelles: {}, ameliorations: {} };
                return reference;
            });
    }
    return chargement;
}

export function referenceEnka() {
    return reference || { persos: {}, armes: {}, noms: {}, libelles: {}, ameliorations: {} };
}

export function urlIconeEnka(nom) {
    return nom ? `https://enka.network/ui/${encodeURIComponent(nom)}.png` : null;
}

export async function recupererEnka(uid) {
    let reponse;
    try {
        reponse = await fetch(new URL(`../api/enka?uid=${encodeURIComponent(uid)}`, import.meta.url));
    } catch {
        throw new ErreurApi("reseau");
    }
    const corps = await reponse.json().catch(() => null);
    if (!reponse.ok) throw new ErreurApi(corps?.erreur || "enka_indisponible");
    return corps;
}

const PROP_VERS_AMELIORATION = {
    2: "FIGHT_PROP_HP", 3: "FIGHT_PROP_HP_PERCENT", 5: "FIGHT_PROP_ATTACK", 6: "FIGHT_PROP_ATTACK_PERCENT",
    8: "FIGHT_PROP_DEFENSE", 9: "FIGHT_PROP_DEFENSE_PERCENT", 20: "FIGHT_PROP_CRITICAL", 22: "FIGHT_PROP_CRITICAL_HURT",
    23: "FIGHT_PROP_CHARGE_EFFICIENCY", 28: "FIGHT_PROP_ELEMENT_MASTERY",
};
const POURCENTAGES = new Set([
    "FIGHT_PROP_HP_PERCENT", "FIGHT_PROP_ATTACK_PERCENT", "FIGHT_PROP_DEFENSE_PERCENT", "FIGHT_PROP_CRITICAL", "FIGHT_PROP_CRITICAL_HURT",
    "FIGHT_PROP_CHARGE_EFFICIENCY", "FIGHT_PROP_HEAL_ADD", "FIGHT_PROP_PHYSICAL_ADD_HURT", "FIGHT_PROP_FIRE_ADD_HURT", "FIGHT_PROP_ELEC_ADD_HURT",
    "FIGHT_PROP_WATER_ADD_HURT", "FIGHT_PROP_WIND_ADD_HURT", "FIGHT_PROP_ICE_ADD_HURT", "FIGHT_PROP_ROCK_ADD_HURT", "FIGHT_PROP_GRASS_ADD_HURT",
]);
const EMPLACEMENTS = { EQUIP_BRACER: [0, "Fleur"], EQUIP_NECKLACE: [1, "Plume"], EQUIP_SHOES: [2, "Sablier"], EQUIP_RING: [3, "Coupe"], EQUIP_DRESS: [4, "Couronne"] };
const BONUS_ELEMENT = { PYRO: "40", ELECTRO: "41", HYDRO: "42", DENDRO: "43", ANEMO: "44", GEO: "45", CRYO: "46" };
const LIBELLE_BONUS_ELEMENT = {
    PYRO: "FIGHT_PROP_FIRE_ADD_HURT", ELECTRO: "FIGHT_PROP_ELEC_ADD_HURT", HYDRO: "FIGHT_PROP_WATER_ADD_HURT",
    DENDRO: "FIGHT_PROP_GRASS_ADD_HURT", ANEMO: "FIGHT_PROP_WIND_ADD_HURT", GEO: "FIGHT_PROP_ROCK_ADD_HURT", CRYO: "FIGHT_PROP_ICE_ADD_HURT",
};

function arrondi(n, decimales = 1) {
    const f = 10 ** decimales;
    return Math.round(Number(n) * f) / f;
}

function libelle(prop) {
    const ref = referenceEnka();
    const base = ref.libelles[prop] || prop.replace("FIGHT_PROP_", "");
    return POURCENTAGES.has(prop) && /_PERCENT$/.test(prop) ? `${base} %` : base;
}

function valeurStat(s) {
    return Number(s?.statValue ?? s?.propValue ?? 0);
}

function normaliserArtefact(item) {
    const ref = referenceEnka();
    const flat = item.flat || {};
    const [rang, nomEmplacement] = EMPLACEMENTS[flat.equipType] || [9, "Artefact"];
    const decompte = {};
    for (const id of item.reliquary?.appendPropIdList || []) {
        const prop = PROP_VERS_AMELIORATION[ref.ameliorations[String(id)]];
        if (prop) decompte[prop] = (decompte[prop] || 0) + 1;
    }
    const sous = (flat.reliquarySubstats || []).map((s) => {
        const prop = s.appendPropId;
        return { prop, libelle: libelle(prop), valeur: arrondi(valeurStat(s)), pct: POURCENTAGES.has(prop), lancers: decompte[prop] || 1 };
    });
    const prop = flat.reliquaryMainstat?.mainPropId || "";
    const cv = sous.reduce((somme, s) => somme + (s.prop === "FIGHT_PROP_CRITICAL" ? 2 * s.valeur : s.prop === "FIGHT_PROP_CRITICAL_HURT" ? s.valeur : 0), 0);
    return {
        rang, emplacement: nomEmplacement, niveau: Math.max(0, (item.reliquary?.level || 1) - 1), rarete: flat.rankLevel || 5,
        icone: flat.icon || null, set: ref.noms[String(flat.setNameTextMapHash)] || "", setHash: String(flat.setNameTextMapHash || ""),
        principale: { prop, libelle: libelle(prop), valeur: arrondi(valeurStat(flat.reliquaryMainstat)), pct: POURCENTAGES.has(prop) },
        sous, cv: arrondi(cv),
    };
}

function normaliserArme(item) {
    const ref = referenceEnka();
    const flat = item.flat || {};
    const stats = flat.weaponStats || [];
    const sous = stats.find((s) => s.appendPropId !== "FIGHT_PROP_BASE_ATTACK");
    const affixes = Object.values(item.weapon?.affixMap || {});
    return {
        nom: ref.noms[String(flat.nameTextMapHash)] || ref.armes[String(item.itemId)]?.nom || "Arme",
        niveau: item.weapon?.level || 1, raffinement: (affixes.length ? Math.max(...affixes) : 0) + 1, rarete: flat.rankLevel || 3,
        icone: flat.icon || null, atq: arrondi(valeurStat(stats.find((s) => s.appendPropId === "FIGHT_PROP_BASE_ATTACK")), 0),
        sous: sous ? { libelle: libelle(sous.appendPropId), valeur: arrondi(valeurStat(sous)), pct: POURCENTAGES.has(sous.appendPropId) } : null,
    };
}

function normaliserPersonnage(a) {
    const ref = referenceEnka();
    const cle = ref.persos[`${a.avatarId}-${a.skillDepotId}`] ? `${a.avatarId}-${a.skillDepotId}` : String(a.avatarId);
    const info = ref.persos[cle] || { nom: `Personnage ${a.avatarId}`, en: "", element: null, rarete: 4, arme: null, talents: [], proud: {} };
    const fp = a.fightPropMap || {};
    const stats = [
        { libelle: ref.libelles.FIGHT_PROP_MAX_HP || "PV max", valeur: arrondi(fp["2000"], 0), pct: false },
        { libelle: ref.libelles.FIGHT_PROP_CUR_ATTACK || "ATQ", valeur: arrondi(fp["2001"], 0), pct: false },
        { libelle: ref.libelles.FIGHT_PROP_CUR_DEFENSE || "DÉF", valeur: arrondi(fp["2002"], 0), pct: false },
        { libelle: ref.libelles.FIGHT_PROP_ELEMENT_MASTERY || "Maîtrise élémentaire", valeur: arrondi(fp["28"], 0), pct: false },
        { libelle: ref.libelles.FIGHT_PROP_CRITICAL || "Taux CRIT", valeur: arrondi((fp["20"] || 0) * 100), pct: true, crit: true },
        { libelle: ref.libelles.FIGHT_PROP_CRITICAL_HURT || "DGT CRIT", valeur: arrondi((fp["22"] || 0) * 100), pct: true },
        { libelle: ref.libelles.FIGHT_PROP_CHARGE_EFFICIENCY || "Recharge d'énergie", valeur: arrondi((fp["23"] || 0) * 100), pct: true },
    ];
    const bonusElement = info.element ? fp[BONUS_ELEMENT[info.element]] : 0;
    if (info.element) {
        stats.push({ libelle: ref.libelles[LIBELLE_BONUS_ELEMENT[info.element]] || "Bonus de DGT", valeur: arrondi((bonusElement || 0) * 100), pct: true });
    }
    if (fp["26"]) stats.push({ libelle: ref.libelles.FIGHT_PROP_HEAL_ADD || "Bonus de soins", valeur: arrondi(fp["26"] * 100), pct: true });
    if (fp["30"]) stats.push({ libelle: ref.libelles.FIGHT_PROP_PHYSICAL_ADD_HURT || "Bonus de DGT physiques", valeur: arrondi(fp["30"] * 100), pct: true });

    const equipements = a.equipList || [];
    const artefacts = equipements.filter((e) => e.flat?.itemType === "ITEM_RELIQUARY").map(normaliserArtefact).sort((x, y) => x.rang - y.rang);
    const armeBrute = equipements.find((e) => e.flat?.itemType === "ITEM_WEAPON");
    const parSet = {};
    for (const art of artefacts) if (art.setHash) parSet[art.setHash] = { nom: art.set, pieces: (parSet[art.setHash]?.pieces || 0) + 1 };

    return {
        id: String(a.avatarId), nom: info.nom, en: info.en, element: info.element, rarete: info.rarete, typeArme: info.arme,
        niveau: Number(a.propMap?.["4001"]?.val ?? a.propMap?.["4001"]?.ival ?? 1), constellation: (a.talentIdList || []).length,
        talents: (info.talents || []).slice(0, 3).map((id) => {
            const bonus = Number(a.proudSkillExtraLevelMap?.[String(info.proud?.[String(id)])] || 0);
            return { niveau: Number(a.skillLevelMap?.[String(id)] || 1) + bonus, bonus };
        }),
        portrait: info.portrait || null, icone: info.icone || null,
        stats, arme: armeBrute ? normaliserArme(armeBrute) : null, artefacts,
        sets: Object.values(parSet).filter((s) => s.pieces >= 2).map((s) => ({ nom: s.nom, pieces: s.pieces >= 4 ? 4 : 2 })),
        cvTotal: arrondi(artefacts.reduce((somme, art) => somme + art.cv, 0)),
    };
}

// Réponse brute d'Enka -> objet enregistré dans Supabase. Lève enka_vitrine_vide si aucun personnage n'est exposé.
export function normaliserEnka(brut) {
    const joueur = brut?.playerInfo || {};
    const liste = brut?.avatarInfoList || [];
    if (!liste.length) throw new ErreurApi("enka_vitrine_vide");
    const ordre = (joueur.showAvatarInfoList || []).map((s) => String(s.avatarId));
    const personnages = liste.map(normaliserPersonnage);
    personnages.sort((x, y) => {
        const i = ordre.indexOf(x.id);
        const j = ordre.indexOf(y.id);
        return (i < 0 ? 99 : i) - (j < 0 ? 99 : j);
    });
    const ref = referenceEnka();
    const avatarId = String(joueur.profilePicture?.avatarId ?? joueur.profilePicture?.id ?? "");
    return {
        version: 1,
        joueur: {
            pseudo: joueur.nickname || "", niveau: joueur.level || 0, niveauMonde: joueur.worldLevel || 0, succes: joueur.finishAchievementNum || 0,
            abysse: joueur.towerFloorIndex ? `${joueur.towerFloorIndex}-${joueur.towerLevelIndex || 0}` : "", signature: joueur.signature || "",
            avatar: ref.persos[avatarId]?.en || personnages[0]?.en || "",
        },
        personnages,
    };
}
