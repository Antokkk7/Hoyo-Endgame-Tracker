// Calcul des statistiques en combat : statistiques d'Enka + bonus des 3 coéquipiers (hors terrain), des armes, des sets
// d'artéfacts et des résonances. Les données viennent de assets/donnees/genshin_*.json (générées par scripts/generer_buffs.py).
import { referenceEnka } from "./enka.js";

let buffs = null;
let chargement = null;

function lire(nom) {
    return fetch(new URL(`../assets/donnees/${nom}`, import.meta.url)).then((r) => (r.ok ? r.json() : null)).catch(() => null);
}

export function chargerBuffs() {
    if (!chargement) {
        chargement = Promise.all([lire("genshin_buffs_persos.json"), lire("genshin_sets.json"), lire("genshin_buffs_armes.json")]).then(([p, s, a]) => {
            const sets = s?.sets || [];
            buffs = {
                persos: p?.persos || {}, resonances: p?.resonances || {}, armes: a?.armes || {}, bp: a?.bp || [], sets,
                setsParId: Object.fromEntries(sets.map((x) => [String(x.id), x])),
                setsParHash: Object.fromEntries(sets.map((x) => [String(x.hash), x])),
            };
            return buffs;
        });
    }
    return chargement;
}

export function donneesBuffs() {
    return buffs || { persos: {}, resonances: {}, armes: {}, bp: [], sets: [], setsParId: {}, setsParHash: {} };
}

export const NOMS_STATS = {
    hp_pct: "PV", atk_pct: "ATQ", def_pct: "DÉF", hp: "PV (fixe)", atk: "ATQ (fixe)", def: "DÉF (fixe)", em: "Maîtrise élémentaire", cr: "Taux CRIT", cd: "DGT CRIT", er: "Recharge d'énergie",
    dmg: "Bonus de DGT (tous types)", dmg_na: "DGT attaque normale", dmg_ca: "DGT attaque chargée", dmg_pa: "DGT attaque plongeante", dmg_skill: "DGT compétence", dmg_burst: "DGT déchaînement",
    heal: "Bonus de soins", rx_lunar: "DGT des réactions lunaires", rx_stellar: "DGT des réactions stellaires", base_stellar: "DGT de base stellaires",
    stellar_direct: "DGT stellaires directs", rx_superconduct: "DGT Supraconduction", rx_overloaded: "DGT Surcharge", rx_burning: "DGT Brûlure", rx_vaporize: "DGT Vaporisation",
    rx_melt: "DGT Fonte", rx_swirl: "DGT Tourbillon", rx_stellar_swirl: "DGT Tourbillon stellaire", rx_electrocharged: "DGT Électrocution", rx_hyperbloom: "DGT Hyperfloraison",
    rx_bloom: "DGT Floraison", rx_stellar_conduct: "DGT Conduction stellaire", rx_lunar_crystallize: "DGT Lunaire-Cristallisation", rx_lunar_bloom: "DGT Lunaire-Floraison", rx_lunar_charged: "DGT Lunaire-Foudroyage", rx_burgeon: "DGT Brasier", rx_shatter: "DGT Éclatement", rx_crystallize: "DGT Cristallisation",
    shield: "Puissance de bouclier", def_red: "Réduction de DÉF ennemie", def_ign: "DÉF ignorée", autre: "Effet spécial",
};
const ELEMENTS_FR = { PYRO: "Pyro", HYDRO: "Hydro", ELECTRO: "Électro", CRYO: "Cryo", ANEMO: "Anémo", GEO: "Géo", DENDRO: "Dendro", PHYS: "Physique" };

export function libelleStat(s) {
    if (NOMS_STATS[s]) return NOMS_STATS[s];
    const m = /^(dmg|res)_(\w+)$/.exec(s);
    if (m) return m[1] === "dmg" ? `Bonus de DGT ${ELEMENTS_FR[m[2]] || "élémentaire"}` : `RÉS ${ELEMENTS_FR[m[2]] || "élémentaire"} de l'ennemi réduite`;
    return s;
}

const POURCENT = new Set(["base_stellar", "stellar_direct", "hp_pct", "atk_pct", "def_pct", "cr", "cd", "er", "dmg", "heal", "shield", "def_red", "def_ign"]);

export function formaterEffet(s, v) {
    if (v === undefined || v === null) return "";
    const pct = POURCENT.has(s) || /^(dmg|res|rx)_/.test(s);
    const nombre = Number.isInteger(v) ? String(v) : String(Math.round(v * 10) / 10).replace(".", ",");
    return `${v >= 0 ? "+" : ""}${nombre}${pct ? " %" : ""}`;
}

function elementPerso(id) {
    return referenceEnka().persos[id]?.element || null;
}

function nomAnglaisArme(id) {
    return referenceEnka().armes[String(id)]?.en || null;
}

// Un effet "brut" des données devient un ou plusieurs effets prêts à évaluer, avec une identité stable.
function preparer(liste, origine, idBase, contexte, qui) {
    const sortie = [];
    (liste || []).forEach((eff, i) => {
        if (eff.cm && contexte.constellation < eff.cm) return;
        const valeur = Array.isArray(eff.v) ? eff.v[Math.min(4, Math.max(0, (contexte.raffinement || 1) - 1))] : eff.v;
        const stats = Array.isArray(eff.s) ? eff.s : [eff.s];
        const indice = Math.min(4, Math.max(0, (contexte.raffinement || 1) - 1));
        const f = eff.f && Array.isArray(eff.f.k) ? { ...eff.f, k: eff.f.k[indice] } : eff.f;
        stats.forEach((s, j) => sortie.push({ ...eff, f, s, v: valeur, origine, qui, id: `${idBase}:${i}${stats.length > 1 ? `.${j}` : ""}`, groupe: eff.g || null }));
    });
    return sortie;
}

function conditionEquipe(eff, elements) {
    if (!eff.eq) return true;
    if (eff.eq === "pyroelectro") return elements.every((e) => e === "PYRO" || e === "ELECTRO") && elements.includes("PYRO") && elements.includes("ELECTRO");
    const m = /^([A-Z]+)>=(\d)$/.exec(eff.eq);
    if (m) return elements.filter((e) => e === m[1]).length >= Number(m[2]);
    return true;
}

// perso : personnage d'Enka ; equipe : [{ id, c, arme: {id, r} | null, set: {m: "aucun"|"4"|"22", a, b}}] ; etat : { idEffet: booléen }
export function calculerCombat(perso, equipe, etat = {}) {
    const b = donneesBuffs();
    const ref = referenceEnka();
    const S = perso.brutes;
    if (!S) return null;
    const elementPrincipal = perso.element;
    const membres = (equipe || []).filter((m) => m && m.id && ref.persos[m.id]);
    const elements = [elementPrincipal, ...membres.map((m) => elementPerso(m.id))].filter(Boolean);
    const effets = [];

    // Personnage lui-même (sur le terrain)
    effets.push(...preparer(b.persos[perso.en]?.on, `${perso.nom} (talents)`, `p:${perso.en}:on`, { constellation: perso.constellation }, "p"));

    // Coéquipiers hors terrain : talents, arme, set
    membres.forEach((m, k) => {
        const info = ref.persos[m.id];
        const origine = info.nom;
        effets.push(...preparer(b.persos[info.en]?.off, `${origine} (talents)`, `p:${info.en}:off`, { constellation: m.c || 0 }, k));
        if (m.arme?.id) {
            const en = nomAnglaisArme(m.arme.id);
            const liste = (b.armes[en] || []).filter((x) => x.cible === "equipe");
            effets.push(...preparer(liste, `${origine} : ${ref.armes[m.arme.id].nom}`, `a:${en}:${k}`, { constellation: 0, raffinement: m.arme.r || 1 }, k));
        }
        const set = m.set || {};
        if (set.m === "4" && set.a && b.setsParId[set.a]) {
            const s = b.setsParId[set.a];
            effets.push(...preparer(s.quatre.filter((x) => x.cible === "equipe"), `${origine} : ${s.fr} (4 pièces)`, `s:${s.id}:4:${k}`, { constellation: 0 }, k));
        }
    });

    // Arme et artéfacts du personnage
    if (perso.arme) {
        const en = nomAnglaisArme(perso.arme.id);
        effets.push(...preparer(b.armes[en] || [], `${perso.nom} : ${perso.arme.nom}`, `a:${en}:soi`, { constellation: 0, raffinement: perso.arme.raffinement || 1 }, "p"));
    }
    const decompte = {};
    for (const art of perso.artefacts || []) if (art.setHash) decompte[art.setHash] = (decompte[art.setHash] || 0) + 1;
    for (const [hash, n] of Object.entries(decompte)) {
        const s = b.setsParHash[hash];
        if (!s) continue;
        if (n >= 2) effets.push(...preparer(s.deux.filter((x) => !x.perm), `${s.fr} (2 pièces)`, `s:${s.id}:2:soi`, { constellation: 0 }, "p"));
        if (n >= 4) effets.push(...preparer(s.quatre, `${s.fr} (4 pièces)`, `s:${s.id}:4:soi`, { constellation: 0 }, "p"));
    }

    // Résonances élémentaires
    for (const el of new Set(elements)) {
        if (elements.filter((x) => x === el).length >= 2) effets.push(...preparer(b.resonances[el], `Résonance ${ELEMENTS_FR[el] || el}`, `r:${el}`, { constellation: 0 }, "r"));
    }

    // Filtres de condition, état (activé ou non) et substitution de l'élément du bénéficiaire
    const tousElementsMembres = membres.map((m) => elementPerso(m.id));
    const evalues = effets.map((eff) => {
        const copie = { ...eff };
        if (/_ELEM$/.test(copie.s)) copie.s = copie.s.replace("ELEM", elementPrincipal || "PHYS");
        let raison = null;
        if (copie.pour && !copie.pour.includes(elementPrincipal)) raison = "Bénéficiaire d'un autre élément";
        else if (!conditionEquipe(copie, elements)) raison = "Composition d'équipe non remplie";
        else if (copie.tw && !copie.tw.includes(perso.typeArme)) raison = "Type d'arme non concerné";
        if (copie.par) {
            const n = tousElementsMembres.filter((e) => (copie.par === "meme" ? e === elementPrincipal : e && e !== elementPrincipal)).length;
            const nombre = Math.min(3, n);
            if (!nombre) raison = raison || "Aucun coéquipier concerné";
            else copie.v = copie.v * nombre;
        }
        const actifVoulu = etat[copie.id] !== undefined ? Boolean(etat[copie.id]) : !copie.d;
        copie.statut = raison ? "hors" : actifVoulu ? "actif" : "inactif";
        copie.raison = raison;
        return copie;
    });

    // Non-cumul (sets d'artéfacts uniquement ; talents, armes et constellations se cumulent toujours) : à valeur égale d'un même groupe et d'une même statistique, seul le meilleur bonus compte
    const meilleurs = {};
    for (const eff of evalues) {
        if (eff.statut !== "actif" || !eff.groupe || !eff.id.startsWith("s:") || eff.v === undefined || eff.v === null) continue;
        const cle = `${eff.groupe}|${eff.s}`;
        if (!meilleurs[cle] || eff.v > meilleurs[cle].v) meilleurs[cle] = eff;
    }
    for (const eff of evalues) {
        if (eff.statut === "actif" && eff.groupe && eff.id.startsWith("s:") && meilleurs[`${eff.groupe}|${eff.s}`] !== eff && eff.v !== undefined && eff.v !== null) {
            eff.statut = "doublon";
            eff.raison = "Non cumulable avec le même bonus d'un autre personnage";
        }
    }

    // Cumul des statistiques, en deux passes (les effets proportionnels à une statistique lisent la première)
    const somme = {};
    const ajouter = (eff, v) => {
        somme[eff.s] = (somme[eff.s] || 0) + v;
    };
    const retenus = evalues.filter((eff) => eff.statut === "actif");
    for (const eff of retenus) if (!eff.f && eff.v !== undefined && eff.v !== null) ajouter(eff, eff.v);

    const finaux = (s) => {
        const sm = (k) => somme[k] || 0;
        return {
            hp: S.hp + S.bhp * sm("hp_pct") / 100 + sm("hp"),
            atk: S.atk + S.batk * sm("atk_pct") / 100 + sm("atk"),
            def: S.def + S.bdef * sm("def_pct") / 100 + sm("def"),
            em: S.em + sm("em"), cr: S.cr + sm("cr"), cd: S.cd + sm("cd"), er: S.er + sm("er"),
        };
    };
    const passe1 = finaux();
    for (const eff of retenus) {
        if (!eff.f) continue;
        let v = eff.f.k * (passe1[eff.f.b] ?? 0);
        if (eff.f.mx !== undefined) v = Math.min(v, eff.f.mx);
        eff.v = Math.round(v * 10) / 10;
        ajouter(eff, eff.v);
    }
    const f = finaux();

    const cleElement = elementPrincipal || "PHYS";
    const dgtElement = (S.dgt?.[cleElement] || 0) + (somme[`dmg_${cleElement}`] || 0);
    const lignes = [
        { cle: "hp", libelle: "PV max", base: S.hp, final: f.hp, pct: false },
        { cle: "atk", libelle: "ATQ", base: S.atk, final: f.atk, pct: false },
        { cle: "def", libelle: "DÉF", base: S.def, final: f.def, pct: false },
        { cle: "em", libelle: "Maîtrise élémentaire", base: S.em, final: f.em, pct: false },
        { cle: "cr", libelle: "Taux CRIT", base: S.cr, final: f.cr, pct: true },
        { cle: "cd", libelle: "DGT CRIT", base: S.cd, final: f.cd, pct: true },
        { cle: "er", libelle: "Recharge d'énergie", base: S.er, final: f.er, pct: true },
        { cle: "dgt", libelle: `Bonus de DGT ${ELEMENTS_FR[cleElement] || ""}`.trim(), base: S.dgt?.[cleElement] || 0, final: dgtElement, pct: true },
    ];
    if ((S.dgt?.PHYS || 0) > 0 && cleElement !== "PHYS") lignes.push({ cle: "phys", libelle: "Bonus de DGT Physique", base: S.dgt.PHYS, final: S.dgt.PHYS + (somme.dmg_PHYS || 0), pct: true });
    if (S.heal > 0 || somme.heal) lignes.push({ cle: "heal", libelle: "Bonus de soins", base: S.heal, final: S.heal + (somme.heal || 0), pct: true });

    const autres = [];
    for (const [s, v] of Object.entries(somme)) {
        if (/^(hp_pct|atk_pct|def_pct|hp|atk|def|em|cr|cd|er|heal|autre)$/.test(s)) continue;
        if (s === `dmg_${cleElement}` || s === "dmg_PHYS" && lignes.some((l) => l.cle === "phys")) continue;
        if (/^dmg_[A-Z]+$/.test(s)) continue;
        autres.push({ cle: s, libelle: libelleStat(s), valeur: v });
    }
    return { lignes, autres, effets: evalues, elements };
}

export function equipeVide() {
    return { equipe: [null, null, null], etat: {} };
}
