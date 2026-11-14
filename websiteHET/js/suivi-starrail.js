// Onglet « Suivi des personnages » de Honkai: Star Rail : UID, puis les personnages du profil en jeu avec leur cône et leurs reliques (Enka Network).
// Les données sont lues dans le cache Supabase ; elles ne sont rechargées que lorsque l'UID est renvoyé.
import { h, nombre, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { chargerReferenceStarRail, normaliserStarRail, recupererStarRail, VOIES } from "./enka-starrail.js";
import { element as infoElement } from "./elements.js";
import { imageEnka, urlsArme, urlsSplash } from "./icones.js";
import { portrait } from "./portraits.js";
import { jeton } from "./session.js";

const BASE_ICONES = "https://enka.network/ui/";
const FORMAT_DATE = new Intl.DateTimeFormat("fr-FR", { dateStyle: "short", timeStyle: "short" });

function format(valeur, pct) {
    const texte = Number.isInteger(valeur) ? nombre(valeur) : new Intl.NumberFormat("fr-FR", { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valeur);
    return pct ? `${texte} %` : texte;
}

// Note d'une relique d'après la valeur critique de ses sous-statistiques (2 x taux CRIT + DGT CRIT), comme pour les artefacts de Genshin.
function noteRelique(cv) {
    if (cv >= 40) return { texte: "SSS", classe: "sss" };
    if (cv >= 30) return { texte: "SS", classe: "ss" };
    if (cv >= 20) return { texte: "S+", classe: "splus" };
    if (cv >= 12) return { texte: "S", classe: "s" };
    if (cv >= 6) return { texte: "A", classe: "a" };
    return { texte: "B", classe: "b" };
}

function carteRelique(r) {
    const note = noteRelique(r.cv);
    return h("article", { classe: "sg-artefact" },
        h("div", { classe: "sg-artefact-gauche" },
            r.icone ? imageEnka([BASE_ICONES + r.icone], { classe: "sg-artefact-icone", alt: r.emplacement, repli: () => h("span", { classe: "sg-artefact-icone vide" }) }) : h("span", { classe: "sg-artefact-icone vide" }),
            h("span", { classe: "doux petit" }, `${r.emplacement} +${r.niveau}`),
            h("strong", { classe: "sg-principale" }, r.principale.libelle, h("span", {}, format(r.principale.valeur, r.principale.pct)))),
        h("ul", { classe: "sg-sous" }, r.sous.map((s) =>
            h("li", {},
                h("span", { classe: "sg-sous-nom" }, s.libelle),
                h("span", { classe: "sg-sous-valeur" }, format(s.valeur, s.pct)),
                s.lancers > 1
                    ? h("span", { classe: `sg-lancers l${Math.min(3, s.lancers)}`, title: `${s.lancers - 1} amélioration${s.lancers > 2 ? "s" : ""}` }, String(s.lancers - 1))
                    : h("span", { classe: "sg-lancers vide", "aria-hidden": "true" })))),
        h("span", { classe: `sg-note ${note.classe}`, title: `Valeur critique : ${format(r.cv, false)}` }, note.texte));
}

function colonnePersonnage(p, contexte) {
    const el = infoElement(p.element);
    const cone = p.cone;
    return h("div", { classe: "sg-col sg-perso", style: `--element:${el.couleur}` },
        h("div", { classe: "sg-carte-perso" },
            h("div", { classe: "sg-nom-ligne" }, h("h3", {}, p.nom), h("span", { classe: "sg-niveau" }, `Niv.${p.niveau}`)),
            h("p", { classe: "petit" }, `${el.nom || "-"} - ${VOIES[p.voie] || "-"}`),
            h("div", { classe: "sg-illustration" }, imageEnka(urlsSplash("starrail", p.id), { classe: "sg-splash", alt: p.nom, repli: () => h("div", { classe: "sg-illustration-repli" }, portrait("starrail", p.en || p.nom, { taille: 140, elementCle: p.element })) })),
            h("p", { classe: "petit sg-constellations-titre" }, `Éidolons : E${p.rang}`),
            h("div", { classe: "sg-constellations" }, [1, 2, 3, 4, 5, 6].map((n) =>
                h("span", { classe: "sg-constellation" + (n <= p.rang ? " active" : ""), "aria-label": `Éidolon ${n}` }, String(n))))),
        cone
            ? h("div", { classe: "sg-arme" },
                imageEnka(urlsArme("starrail", cone.en || cone.nom), { classe: "sg-arme-icone", alt: cone.nom, repli: () => h("span", { classe: "sg-arme-icone vide" }) }),
                h("div", {}, h("span", { classe: "sg-etiquette" }, "Cône de lumière"), h("strong", {}, cone.nom),
                    h("span", { classe: "doux petit" }, `Niv. ${cone.niveau} · Superposition S${cone.rang}`)))
            : h("div", { classe: "sg-arme" }, h("span", { classe: "doux" }, "Aucun cône équipé")),
        p.sets.length
            ? h("div", { classe: "sg-bloc sg-sets" }, h("h3", {}, "Sets de reliques"),
                p.sets.map((s) => h("div", { classe: "sg-set" }, h("span", {}, s.nom), h("span", { classe: "sg-pieces" }, `${s.pieces}P`))))
            : null,
        boutonRecos(p, contexte));
}

// Bouton vers les équipes recommandées, affiché seulement si le personnage est le principal d'une recommandation.
function boutonRecos(p, contexte) {
    const nom = String(p.en || p.nom || "");
    const n = contexte.recos.get(nom.toLowerCase()) || contexte.recos.get(String(p.nom || "").toLowerCase());
    if (!n) return null;
    return h("button", {
        classe: "bouton bouton-principal sg-reco-bouton", type: "button",
        onclick: () => window.dispatchEvent(new CustomEvent("het:recos", { detail: { personnage: contexte.recos.has(nom.toLowerCase()) ? nom : p.nom } })),
    }, `Équipes recommandées (${n})`);
}

function colonneStats(p) {
    return h("div", { classe: "sg-col sg-stats" },
        h("section", { classe: "sg-bloc" }, h("h3", {}, "Statistiques"),
            h("ul", { classe: "sg-liste-stats" }, p.stats.map((s) => h("li", {}, h("span", {}, s.libelle), h("strong", {}, format(s.valeur, s.pct))))),
            h("p", { classe: "doux petit" }, "Base, cône, reliques, bonus de sets et traces. Les effets conditionnels (passifs du cône, talents, éidolons) ne sont pas inclus.")));
}

function colonneReliques(p) {
    return h("div", { classe: "sg-col sg-artefacts" },
        p.reliques.length ? p.reliques.map(carteRelique) : h("div", { classe: "etat-vide" }, "Aucune relique équipée."));
}

function carteGlobale(cache, choisi, surChoix, contexte) {
    const { donnees } = cache;
    const p = donnees.personnages.find((x) => x.id === choisi) || donnees.personnages[0];
    const j = donnees.joueur;
    const details = [
        j.niveauMonde ? `Niveau d'équilibre ${j.niveauMonde}` : null, j.niveau ? `Rang d'aventure ${j.niveau}` : null, `UID ${cache.uid}`,
        `Dernière mise à jour : ${FORMAT_DATE.format(new Date(cache.maj_le))}`,
    ].filter(Boolean);
    return h("section", { classe: "suivi-global", "aria-label": "Personnages du compte" },
        h("header", { classe: "sg-entete" },
            portrait("starrail", j.avatar || p.en, { taille: 56, rond: true }),
            h("div", { classe: "sg-joueur" }, h("h2", {}, j.pseudo || "Pionnier"), h("p", { classe: "doux petit" }, details.join(" · "))),
            h("div", { classe: "sg-chips", role: "tablist", "aria-label": "Personnages" }, donnees.personnages.map((x) =>
                h("button", {
                    classe: "sg-chip", type: "button", role: "tab", "aria-selected": String(x.id === p.id), title: `${x.nom} · Niv.${x.niveau}`,
                    style: `--element:${infoElement(x.element).couleur}`, onclick: () => surChoix(x.id),
                }, portrait("starrail", x.en || x.nom, { taille: 46, rond: true, elementCle: x.element }))))),
        h("div", { classe: "sg-corps" }, colonnePersonnage(p, contexte), colonneStats(p), colonneReliques(p)));
}

export async function rendreSuiviStarRail(zone) {
    const [uidEnregistre, cacheInitial, indexRecos] = await Promise.all([
        rpc("uid_lire", { p_token: jeton(), p_jeu: "starrail" }),
        rpc("enka_jeu_lire", { p_token: jeton(), p_jeu: "starrail" }),
        rpc("recos_personnages", { p_jeu: "starrail" }).catch(() => []),
        chargerReferenceStarRail(),
    ]);
    const contexte = { recos: new Map((indexRecos || []).map((r) => [String(r.personnage).toLowerCase(), r.categories])) };
    let cache = cacheInitial && String(cacheInitial.uid) === String(uidEnregistre) ? cacheInitial : null;
    let choisi = null;

    const champ = h("input", { id: "champ-uid", type: "text", inputmode: "numeric", maxlength: "10", value: uidEnregistre ? String(uidEnregistre) : "", placeholder: "ex. 800123456" });
    const message = h("p", { classe: "message", role: "status" });
    const bouton = h("button", { classe: "bouton bouton-principal", type: "submit" }, "Enregistrer et charger");
    const zoneCarte = h("div", {});

    function dessiner() {
        if (!cache) {
            remplacer(zoneCarte, h("div", { classe: "etat-vide" },
                uidEnregistre || champ.value.trim() ? "Aucun personnage chargé. Cliquez sur « Enregistrer et charger »." : "Renseignez votre UID Honkai: Star Rail pour afficher vos personnages."));
            return;
        }
        remplacer(zoneCarte, carteGlobale(cache, choisi, (id) => {
            choisi = id;
            dessiner();
        }, contexte));
    }

    const formulaire = h("form", {
        classe: "rangee-uid", novalidate: true,
        onsubmit: async (e) => {
            e.preventDefault();
            const uid = champ.value.trim();
            message.className = "message";
            if (!uid) {
                try {
                    await rpc("uid_ecrire", { p_token: jeton(), p_jeu: "starrail", p_uid: "" });
                    cache = null;
                    message.className = "message succes";
                    message.textContent = "UID supprimé.";
                    dessiner();
                } catch (err) {
                    message.className = "message erreur";
                    message.textContent = err.message;
                }
                return;
            }
            if (!/^[0-9]{9,10}$/.test(uid)) {
                message.className = "message erreur";
                message.textContent = "UID invalide : 9 ou 10 chiffres.";
                return;
            }
            bouton.disabled = true;
            message.textContent = "Chargement depuis Enka Network…";
            try {
                const donnees = normaliserStarRail(await recupererStarRail(uid));
                cache = await rpc("enka_jeu_enregistrer", { p_token: jeton(), p_jeu: "starrail", p_uid: uid, p_donnees: donnees });
                choisi = null;
                message.className = "message succes";
                message.textContent = `${donnees.personnages.length} personnage${donnees.personnages.length > 1 ? "s" : ""} chargé${donnees.personnages.length > 1 ? "s" : ""}.`;
                dessiner();
            } catch (err) {
                message.className = "message erreur";
                message.textContent = err.message;
            }
            bouton.disabled = false;
        },
    },
    h("div", { classe: "champ" }, h("label", { for: "champ-uid" }, "UID Honkai: Star Rail"), champ), bouton);

    remplacer(zone, formulaire, message,
        h("p", { classe: "doux petit" }, "Les personnages viennent de la vitrine de votre profil en jeu, via Enka Network. Ils sont conservés ; ils ne sont mis à jour que lorsque vous rechargez l'UID."),
        zoneCarte);
    dessiner();
}
