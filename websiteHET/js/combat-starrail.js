// Calcul des statistiques en combat (Star Rail) : statistiques du personnage + bonus de ses 3 coéquipiers (talents, cône,
// sets de reliques et ornement planaire), de son propre cône et de ses sets. Les bonus viennent de assets/donnees/starrail_buffs.json
// (format décrit dans le champ « aide » de ce fichier).
import { brutesDepuisStats, referenceStarRail } from "./enka-starrail.js";

let buffs = null;
let chargement = null;

export function chargerBuffsStarRail() {
    if (!chargement) {
        chargement = fetch(new URL("../assets/donnees/starrail_buffs.json", import.meta.url))
            .then((r) => (r.ok ? r.json() : null))
            .catch(() => null)
            .then((d) => {
                buffs = { persos: d?.persos || {}, cones: d?.cones || {}, sets: d?.sets || {} };
                return buffs;
            });
    }
    return chargement;
}

export function donneesBuffsStarRail() {
    return buffs || { persos: {}, cones: {}, sets: {} };
}

const ELEMENTS = { PHYSICAL: "Physique", FIRE: "Feu", ICE: "Glace", LIGHTNING: "Foudre", WIND: "Vent", QUANTUM: "Quantique", IMAGINARY: "Imaginaire" };

export const NOMS_STATS = {
    hp_pct: "PV", atk_pct: "ATQ", def_pct: "DÉF", spd_pct: "VIT", hp: "PV (fixe)", atk: "ATQ (fixe)", def: "DÉF (fixe)", spd: "VIT (fixe)",
    cr: "Taux CRIT", cd: "DGT CRIT", brk: "Effet de rupture", err: "Régén. d'énergie", ehr: "Taux de touche des effets", eres: "RÉS aux effets",
    dmg: "Bonus de DGT (tous types)", heal: "Bonus de soins", vuln: "Vulnérabilité de l'ennemi", res_pen: "Pénétration de RÉS", def_red: "Réduction de DÉF ennemie",
    shield: "Puissance de bouclier", autre: "Effet spécial",
};
const POURCENT = new Set(["hp_pct", "atk_pct", "def_pct", "spd_pct", "cr", "cd", "brk", "err", "ehr", "eres", "dmg", "heal", "vuln", "res_pen", "def_red", "shield"]);

export function libelleStat(s) {
    if (NOMS_STATS[s]) return NOMS_STATS[s];
    const m = /^dmg_([A-Z]+)$/.exec(s);
    return m ? `Bonus de DGT ${ELEMENTS[m[1]] || "élémentaire"}` : s;
}

export function formaterEffet(s, v) {
    if (v === undefined || v === null) return "";
    const pct = POURCENT.has(s) || /^dmg_/.test(s);
    const nombre = Number.isInteger(v) ? String(v) : String(Math.round(v * 10) / 10).replace(".", ",");
    return `${v >= 0 ? "+" : ""}${nombre}${pct ? " %" : ""}`;
}

// Sets d'ornement planaire : ceux dont une pièce est une sphère ou une corde.
export function idsOrnements() {
    const ids = new Set();
    for (const r of Object.values(referenceStarRail().reliques)) if (r[1] === "NECK" || r[1] === "OBJECT") ids.add(String(r[0]));
    return ids;
}

function preparer(liste, origine, idBase, contexte, qui, avecOrigine = false) {
    const indice = Math.min(4, Math.max(0, (contexte.superposition || 1) - 1));
    const sortie = [];
    (liste || []).forEach((eff, i) => {
        if (eff.cm && (contexte.eidolon || 0) < eff.cm) return;
        const v = Array.isArray(eff.v) ? eff.v[indice] : eff.v;
        sortie.push({ ...eff, v, origine: avecOrigine && eff.o ? `${origine} · ${eff.o}` : origine, qui, id: `${idBase}:${i}` });
    });
    return sortie;
}

// perso : personnage enregistré (avec brutes) ; equipe : [{ id, e, cone: {id, r} | null, sets: { m: "aucun"|"4"|"22", a, b, o }}] ; etat : { idEffet: booléen }
export function calculerCombat(perso, equipe, etat = {}) {
    const b = donneesBuffsStarRail();
    const ref = referenceStarRail();
    const S = perso.brutes || brutesDepuisStats(perso);
    if (!S) return null;
    const membres = (equipe || []).filter((m) => m && m.id && ref.persos[m.id]);
    const effets = [];

    effets.push(...preparer(b.persos[perso.id]?.on, perso.nom, `p:${perso.en}:on`, { eidolon: perso.rang }, "p", true));
    if (perso.cone) effets.push(...preparer(b.cones[perso.cone.en], `${perso.nom} : ${perso.cone.nom}`, `c:${perso.cone.en}:soi`, { superposition: perso.cone.rang }, "p"));
    for (const s of perso.sets || []) {
        const d = b.sets[s.en];
        if (!d) continue;
        effets.push(...preparer(d.deux, `${s.nom} (2 pièces)`, `s:${s.en}:2:soi`, {}, "p"));
        if (s.pieces >= 4) effets.push(...preparer(d.quatre, `${s.nom} (4 pièces)`, `s:${s.en}:4:soi`, {}, "p"));
    }

    membres.forEach((m, k) => {
        const info = ref.persos[m.id];
        effets.push(...preparer(b.persos[m.id]?.off, info.nom, `p:${info.en}:off`, { eidolon: m.e || 0 }, k, true));
        if (m.cone?.id && ref.cones[m.cone.id]) {
            const c = ref.cones[m.cone.id];
            effets.push(...preparer((b.cones[c.en] || []).filter((x) => x.cible === "equipe"), `${info.nom} : ${c.nom}`, `c:${c.en}:${k}`, { superposition: m.cone.r || 1 }, k));
        }
        const sets = m.sets || {};
        const parNom = (id) => ref.sets[id];
        const ajouterSet = (id, pieces) => {
            const s = parNom(id);
            const d = s && b.sets[s.en];
            if (!d) return;
            const liste = (pieces === 4 ? d.quatre : d.deux).filter((x) => x.cible === "equipe");
            effets.push(...preparer(liste, `${info.nom} : ${s.nom} (${pieces} pièces)`, `s:${s.en}:${pieces}:${k}`, {}, k));
        };
        if (sets.m === "4" && sets.a) ajouterSet(sets.a, 4);
        if (sets.m === "22") {
            if (sets.a) ajouterSet(sets.a, 2);
            if (sets.b && sets.b !== sets.a) ajouterSet(sets.b, 2);
        }
        if (sets.o) ajouterSet(sets.o, 2);
    });

    const evalues = effets.map((eff) => {
        const copie = { ...eff };
        let raison = null;
        if (copie.pour && !copie.pour.includes(perso.element) && !copie.pour.includes(perso.voie)) raison = "Bénéficiaire non concerné";
        const actifVoulu = etat[copie.id] !== undefined ? Boolean(etat[copie.id]) : !copie.d;
        copie.statut = raison ? "hors" : actifVoulu ? "actif" : "inactif";
        copie.raison = raison;
        return copie;
    });

    const somme = {};
    for (const eff of evalues) if (eff.statut === "actif" && eff.v !== undefined && eff.v !== null) somme[eff.s] = (somme[eff.s] || 0) + eff.v;
    const sm = (k) => somme[k] || 0;
    const f = {
        hp: S.hp + S.bhp * sm("hp_pct") / 100 + sm("hp"),
        atk: S.atk + S.batk * sm("atk_pct") / 100 + sm("atk"),
        def: S.def + S.bdef * sm("def_pct") / 100 + sm("def"),
        spd: S.spd + S.bspd * sm("spd_pct") / 100 + sm("spd"),
    };
    const cleElement = `dmg_${perso.element}`;
    const lignes = [
        { cle: "hp", libelle: "PV", base: S.hp, final: f.hp, pct: false },
        { cle: "atk", libelle: "ATQ", base: S.atk, final: f.atk, pct: false },
        { cle: "def", libelle: "DÉF", base: S.def, final: f.def, pct: false },
        { cle: "spd", libelle: "VIT", base: S.spd, final: f.spd, pct: false },
        { cle: "cr", libelle: "Taux CRIT", base: S.cr, final: S.cr + sm("cr"), pct: true },
        { cle: "cd", libelle: "DGT CRIT", base: S.cd, final: S.cd + sm("cd"), pct: true },
        { cle: "brk", libelle: "Effet de rupture", base: S.brk, final: S.brk + sm("brk"), pct: true },
        { cle: "err", libelle: "Régén. d'énergie", base: S.err, final: S.err + sm("err"), pct: true },
        { cle: "ehr", libelle: "Taux de touche des effets", base: S.ehr, final: S.ehr + sm("ehr"), pct: true },
        { cle: "eres", libelle: "RÉS aux effets", base: S.eres, final: S.eres + sm("eres"), pct: true },
        { cle: "dgt", libelle: `Bonus de DGT ${ELEMENTS[perso.element] || ""}`.trim(), base: S.dgt, final: S.dgt + sm(cleElement) + sm("dmg"), pct: true },
    ];
    if (S.heal > 0 || sm("heal")) lignes.push({ cle: "heal", libelle: "Bonus de soins", base: S.heal, final: S.heal + sm("heal"), pct: true });

    const autres = [];
    for (const [s, v] of Object.entries(somme)) {
        if (/^(hp_pct|atk_pct|def_pct|spd_pct|hp|atk|def|spd|cr|cd|brk|err|ehr|eres|heal|dmg|autre)$/.test(s) || /^dmg_[A-Z]+$/.test(s)) continue;
        autres.push({ cle: s, libelle: libelleStat(s), valeur: v });
    }
    return { lignes, autres, effets: evalues };
}

export function equipeVide() {
    return { equipe: [null, null, null], etat: {} };
}
