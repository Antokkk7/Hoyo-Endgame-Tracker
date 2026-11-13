// Logique partagée par les pages Genshin Impact et Honkai: Star Rail :
// onglets, suivi par UID, builds, recommandations d'équipes. Les endgames sont fournis par chaque page.
import { h, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { baseConfiguree } from "./config.js";
import { initConnexion, ouvrirConnexion } from "./connexion.js";
import { element } from "./elements.js";
import { chargerManifeste, portrait } from "./portraits.js";
import { estConnecte, jeton } from "./session.js";

export function exigerConnexion(zone, texte = "Connectez-vous pour accéder à vos données.") {
    remplacer(zone, h("div", { classe: "etat-vide" },
        h("p", {}, texte),
        h("p", {}, h("button", { classe: "bouton bouton-principal", type: "button", style: "margin-top:14px", onclick: () => ouvrirConnexion() }, "Connexion"))));
}

export function afficherErreur(zone, err) {
    remplacer(zone, h("div", { classe: "etat-vide" }, err?.message || "Une erreur est survenue."));
}

async function rendreSuivi(zone, cfg) {
    if (!estConnecte()) return exigerConnexion(zone);
    const [uid, suivis] = await Promise.all([
        rpc("uid_lire", { p_token: jeton(), p_jeu: cfg.jeu }),
        rpc("personnages_suivis", { p_token: jeton(), p_jeu: cfg.jeu }),
    ]);
    const champ = h("input", { id: "champ-uid", type: "text", inputmode: "numeric", maxlength: "12", value: uid || "", placeholder: "ex. 800123456" });
    const message = h("p", { classe: "message", role: "status" });
    const formulaire = h("form", {
        classe: "rangee-uid",
        novalidate: true,
        onsubmit: async (e) => {
            e.preventDefault();
            message.className = "message";
            try {
                await rpc("uid_ecrire", { p_token: jeton(), p_jeu: cfg.jeu, p_uid: champ.value.trim() });
                message.className = "message succes";
                message.textContent = champ.value.trim() ? "UID enregistré." : "UID supprimé.";
            } catch (err) {
                message.className = "message erreur";
                message.textContent = err.message;
            }
        },
    },
    h("div", { classe: "champ" }, h("label", { for: "champ-uid" }, `UID ${cfg.nomJeu}`), champ),
    h("button", { classe: "bouton bouton-principal", type: "submit" }, "Enregistrer"));

    const liste = suivis.length
        ? h("div", { classe: "suivi-grille" }, suivis.map((s) =>
            h("div", { classe: "suivi-carte" },
                portrait(cfg.jeu, s.nom, { taille: 48 }),
                h("div", {}, h("strong", {}, s.nom),
                    h("div", { classe: "doux petit" }, [s.niveau ? `Niv.${s.niveau}` : null, s.doublons !== null ? `${cfg.prefixes.doublons}${s.doublons}` : null].filter(Boolean).join(" · "))))))
        : h("div", { classe: "etat-vide" }, "Aucun personnage suivi pour l'instant. Le suivi automatique à partir de l'UID arrive prochainement.");
    remplacer(zone, formulaire, message, liste);
}

async function rendreBuilds(zone, cfg) {
    if (!estConnecte()) return exigerConnexion(zone);
    const builds = await rpc("builds", { p_token: jeton(), p_jeu: cfg.jeu });
    if (!builds.length) {
        remplacer(zone, h("div", { classe: "etat-vide" }, "Aucun build pour l'instant. Vous pourrez bientôt préciser le build de chaque personnage."));
        return;
    }
    remplacer(zone, h("div", { classe: "suivi-grille" }, builds.map((b) =>
        h("div", { classe: "suivi-carte" },
            portrait(cfg.jeu, b.personnage, { taille: 48 }),
            h("div", {}, h("strong", {}, b.personnage), h("div", { classe: "doux petit" }, [b.nom_build, b.arme].filter(Boolean).join(" · ")))))));
}

async function rendreRecos(zone, cfg) {
    const catalogue = await rpc("catalogue", { p_jeu: cfg.jeu });
    const resultats = h("div", {});
    const liste = h("datalist", { id: "liste-persos" }, catalogue.map((c) => h("option", { value: c.nom })));
    const champ = h("input", { id: "champ-perso", type: "text", list: "liste-persos", autocomplete: "off", placeholder: "Tapez le nom d'un personnage" });

    async function chercher() {
        const saisie = champ.value.trim().toLowerCase();
        const perso = catalogue.find((c) => c.nom.toLowerCase() === saisie);
        if (!perso) {
            remplacer(resultats);
            return;
        }
        try {
            const recos = await rpc("recos", { p_jeu: cfg.jeu, p_personnage: perso.nom });
            if (!recos.length) {
                remplacer(resultats, h("div", { classe: "etat-vide" }, `Aucune équipe recommandée pour ${perso.nom} pour l'instant.`));
                return;
            }
            remplacer(resultats, recos.map((r) =>
                h("article", { classe: "reco" },
                    h("div", { classe: "equipe" }, r.membres.map((nom) => {
                        const info = catalogue.find((c) => c.nom.toLowerCase() === nom.toLowerCase());
                        return h("div", { classe: "membre" },
                            portrait(cfg.jeu, nom, { taille: 60, elementCle: info?.element }),
                            h("span", { classe: "membre-meta" }, nom));
                    })),
                    h("div", { classe: "reco-noms" },
                        h("strong", {}, r.membres.join(", ")),
                        r.note ? h("span", { classe: "doux petit" }, r.note) : null))));
        } catch (err) {
            afficherErreur(resultats, err);
        }
    }
    champ.addEventListener("input", chercher);
    champ.addEventListener("change", chercher);
    remplacer(zone, h("div", { classe: "recherche-perso" },
        h("div", { classe: "champ" }, h("label", { for: "champ-perso" }, "Personnage"), champ, liste)), resultats);
    if (!catalogue.length) {
        resultats.append(h("div", { classe: "etat-vide" }, "Le catalogue de personnages est vide (voir sql/03_donnees_reference.sql)."));
    }
}

export function initJeu(cfg) {
    initConnexion();
    if (!baseConfiguree()) {
        document.getElementById("bandeau-base")?.classList.remove("cache");
    }
    const boutons = [...document.querySelectorAll(".onglets [data-onglet]")];
    const panneaux = [...document.querySelectorAll(".panneau[data-panneau]")];
    const rendeurs = {
        suivi: (zone) => rendreSuivi(zone, cfg),
        builds: (zone) => rendreBuilds(zone, cfg),
        recos: (zone) => rendreRecos(zone, cfg),
        endgames: async (zone) => {
            if (!estConnecte()) return exigerConnexion(zone, "Connectez-vous pour voir l'historique de vos endgames.");
            await cfg.endgames(zone);
        },
    };
    const faits = new Set();
    let courant = "suivi";

    async function rendre(id) {
        if (faits.has(id)) return;
        faits.add(id);
        const zone = document.querySelector(`.panneau[data-panneau="${id}"] [data-zone]`);
        try {
            await rendeurs[id](zone);
        } catch (err) {
            afficherErreur(zone, err);
        }
    }

    function afficher(id) {
        if (!rendeurs[id]) id = "suivi";
        courant = id;
        boutons.forEach((b) => b.setAttribute("aria-selected", String(b.dataset.onglet === id)));
        panneaux.forEach((p) => p.classList.toggle("cache", p.dataset.panneau !== id));
        history.replaceState(null, "", `#${id}`);
        rendre(id);
    }

    boutons.forEach((b) => b.addEventListener("click", () => afficher(b.dataset.onglet)));
    window.addEventListener("het:session", () => {
        faits.clear();
        rendre(courant);
    });
    chargerManifeste().then(() => afficher(location.hash.slice(1) || "suivi"));
}

// Barre de sous-onglets (par exemple un endgame par bouton) ; renvoie le conteneur à remplir.
export function sousOnglets(zone, entrees) {
    const barre = h("div", { classe: "sous-onglets", role: "tablist" });
    const contenu = h("div", {});
    function choisir(entree) {
        [...barre.children].forEach((b, i) => b.setAttribute("aria-selected", String(entrees[i] === entree)));
        remplacer(contenu, entree.rendu());
    }
    entrees.forEach((entree) => barre.append(h("button", { type: "button", role: "tab", onclick: () => choisir(entree) }, entree.titre)));
    remplacer(zone, barre, contenu);
    choisir(entrees[0]);
}

export function pastille(texte, couleur) {
    return h("span", { classe: "pastille", style: couleur ? `--couleur:${couleur}` : null }, texte);
}

export function pastilleElement(cle) {
    const e = element(cle);
    return pastille(e.nom, e.couleur);
}

export function etoiles(actives, total, classes = "") {
    const conteneur = h("span", { classe: "etoiles", "aria-label": `${actives} sur ${total}` });
    for (let i = 0; i < total; i++) {
        const classe = typeof classes === "function" ? classes(i) : classes;
        conteneur.append(h("span", { classe: `etoile ${classe} ${i < actives ? "on" : ""}`.replace(/\s+/g, " ").trim(), "aria-hidden": "true" }, "★"));
    }
    return conteneur;
}

export function vide(texte) {
    return h("div", { classe: "etat-vide" }, texte);
}
