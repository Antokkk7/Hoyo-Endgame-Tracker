// Résumé des derniers clears de chaque jeu, comme l'accueil de l'application de bureau.
import { h, nombre } from "./dom.js";
import { rpc } from "./api.js";
import { bilanAbysse, bilanApoc, bilanArbitre, bilanMoc, couleurDifficulte, scoreSallePf, secondesCarnage, bilanTheatre } from "./regles.js";
import { jeton } from "./session.js";

function ligne(nom, valeur, detail, { couleur = null, image = null } = {}) {
    return h("div", { classe: "synthese-ligne" + (couleur ? " mise-en-avant" : ""), style: couleur ? `--cadre:${couleur}` : null },
        image ? h("img", { classe: "synthese-cadre", src: new URL(`../assets/img/cadres/${image}`, import.meta.url).href, alt: "", onerror: (e) => e.target.remove() }) : null,
        h("div", { classe: "synthese-nom" }, nom, detail ? h("small", {}, detail) : null),
        h("div", { classe: "synthese-valeur" }, ...[].concat(valeur)));
}

function ligneVide(nom) {
    return h("div", { classe: "synthese-ligne" }, h("div", { classe: "synthese-nom" }, nom), h("div", { classe: "synthese-valeur doux petit" }, "Aucun clear"));
}

function carte(titre, lignes, pied, classe) {
    return h("article", { classe: `synthese-carte ${classe}` },
        h("h3", {}, titre), h("div", { classe: "synthese-lignes" }, lignes), h("p", { classe: "synthese-pied doux petit" }, pied));
}

function pied(equipes, personnages) {
    const e = equipes.length;
    return `${e} ${e > 1 ? "équipes" : "équipe"} · ${personnages} personnages`;
}

function carteGenshin(d, personnages) {
    const [a, t, c] = [d.abysses[0], d.theatre[0], d.carnage[0]];
    const lignes = [
        a ? (() => { const b = bilanAbysse(a); return ligne("Abysses spiralées", `${b.total} / ${b.max} ★`, `Patch ${a.patch} · Étage ${a.etage}`); })() : ligneVide("Abysses spiralées"),
        t ? (() => {
            const b = bilanTheatre(t);
            return ligne("Théâtre de l'imaginarium", `${b.total} / ${b.max} astres`, [b.mode.nom, t.arcanum ? `Arcanum ${t.arcanum}` : null, t.patch ? `Patch ${t.patch}` : null].filter(Boolean).join(" · "));
        })() : ligneVide("Théâtre de l'imaginarium"),
        c ? ligne("Carnage chtonien",
            [h("span", { classe: "badge-difficulte", style: `--couleur:${couleurDifficulte(c.difficulte)}` }, String(c.difficulte)), ` ${secondesCarnage(c)} s`],
            `Patch ${c.patch}`) : ligneVide("Carnage chtonien"),
    ];
    return carte("Genshin Impact", lignes, pied(d.equipes, personnages), "synthese-genshin");
}

function carteStarRail(d, personnages) {
    const [m, p, a, r] = [d.moc[0], d.pf[0], d.apoc[0], d.arbitre[0]];
    const lignes = [
        m ? (() => { const b = bilanMoc(m); return ligne("Salle oubliée", `${b.total} / ${b.max} ★${m.stellaire ? " + ★" : ""}`, `Patch ${m.patch}`); })() : ligneVide("Salle oubliée"),
        p ? ligne("Pure fiction", `${nombre(scoreSallePf(p, 3))} pts`, `Patch ${p.patch}`) : ligneVide("Pure fiction"),
        a ? (() => { const b = bilanApoc(a); return ligne("Fantômes de l'apocalypse", nombre(b.total), `Patch ${a.patch}`); })() : ligneVide("Fantômes de l'apocalypse"),
        r ? (() => {
            const b = bilanArbitre(r);
            return ligne("Arbitre de l'instable", `${b.total} / ${b.max} ★`, [`Patch ${r.patch}`, b.cadre?.nom].filter(Boolean).join(" · "),
                b.cadre ? { couleur: b.cadre.couleur, image: b.cadre.image } : {});
        })() : ligneVide("Arbitre de l'instable"),
    ];
    return carte("Honkai: Star Rail", lignes, pied(d.equipes, personnages), "synthese-starrail");
}

export async function blocSynthese() {
    const [g, s, cg, cs] = await Promise.all([
        rpc("endgames_genshin", { p_token: jeton() }),
        rpc("endgames_starrail", { p_token: jeton() }),
        rpc("catalogue", { p_jeu: "genshin" }),
        rpc("catalogue", { p_jeu: "starrail" }),
    ]);
    return h("section", { classe: "bloc-compte" }, h("h2", {}, "Derniers clears"),
        h("div", { classe: "synthese-grille" }, carteGenshin(g, cg.length), carteStarRail(s, cs.length)));
}
