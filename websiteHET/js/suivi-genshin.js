// Onglet « Suivi des personnages » de Genshin Impact : UID, puis les personnages de la vitrine du jeu (Enka Network).
// Les données sont lues dans le cache Supabase ; elles ne sont rechargées que lorsque l'UID est renvoyé.
import { h, nombre, remplacer } from "./dom.js";
import { rpc } from "./api.js";
import { chargerBuffs } from "./combat-genshin.js";
import { chargerReferenceEnka, normaliserEnka, recupererEnka, urlIconeEnka } from "./enka.js";
import { blocCombat } from "./equipe-combat.js";
import { element as infoElement } from "./elements.js";
import { portrait } from "./portraits.js";
import { jeton } from "./session.js";

const TYPES_ARME = { sword: "Épée à une main", claymore: "Épée à deux mains", polearm: "Arme d'hast", bow: "Arc", catalyst: "Catalyseur" };
const FORMAT_DATE = new Intl.DateTimeFormat("fr-FR", { dateStyle: "short", timeStyle: "short" });

function format(valeur, pct) {
    const texte = Number.isInteger(valeur) ? nombre(valeur) : new Intl.NumberFormat("fr-FR", { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(valeur);
    return pct ? `${texte} %` : texte;
}

// Note d'un artefact d'après la valeur critique de ses sous-statistiques (2 x taux CRIT + DGT CRIT).
export function noteArtefact(cv) {
    if (cv >= 40) return { texte: "SSS", classe: "sss" };
    if (cv >= 30) return { texte: "SS", classe: "ss" };
    if (cv >= 20) return { texte: "S+", classe: "splus" };
    if (cv >= 12) return { texte: "S", classe: "s" };
    if (cv >= 6) return { texte: "A", classe: "a" };
    return { texte: "B", classe: "b" };
}

function image(src, alt, classe, repli = null) {
    return h("img", { classe, src, alt, loading: "lazy", onerror: (e) => (repli ? e.target.replaceWith(repli()) : e.target.remove()) });
}

function carteArtefact(art) {
    const note = noteArtefact(art.cv);
    return h("article", { classe: "sg-artefact" },
        h("div", { classe: "sg-artefact-gauche" },
            art.icone ? image(urlIconeEnka(art.icone), art.emplacement, "sg-artefact-icone") : h("span", { classe: "sg-artefact-icone vide" }),
            h("span", { classe: "doux petit" }, `${art.emplacement} +${art.niveau}`),
            h("strong", { classe: "sg-principale" }, art.principale.libelle, h("span", {}, format(art.principale.valeur, art.principale.pct)))),
        // Trois colonnes alignées : nom de la substat, valeur, nombre d'améliorations (case vide s'il n'y en a pas).
        h("ul", { classe: "sg-sous" }, art.sous.map((s) =>
            h("li", {},
                h("span", { classe: "sg-sous-nom" }, s.libelle),
                h("span", { classe: "sg-sous-valeur" }, format(s.valeur, s.pct)),
                s.lancers > 1
                    ? h("span", { classe: `sg-lancers l${Math.min(3, s.lancers)}`, title: `${s.lancers - 1} amélioration${s.lancers > 2 ? "s" : ""}` }, String(s.lancers - 1))
                    : h("span", { classe: "sg-lancers vide", "aria-hidden": "true" })))),
        h("span", { classe: `sg-note ${note.classe}`, title: `Valeur critique : ${format(art.cv, false)}` }, note.texte));
}

function colonnePersonnage(p, contexte) {
    const el = infoElement(p.element);
    const splash = p.portrait ? urlIconeEnka(p.portrait) : null;
    const repli = () => h("div", { classe: "sg-illustration-repli" }, portrait("genshin", p.en || p.nom, { taille: 140, elementCle: p.element }));
    return h("div", { classe: "sg-col sg-perso", style: `--element:${el.couleur}` },
        h("div", { classe: "sg-carte-perso" },
            h("div", { classe: "sg-nom-ligne" }, h("h3", {}, p.nom), h("span", { classe: "sg-niveau" }, `Niv.${p.niveau}`)),
            h("p", { classe: "petit" }, `${el.nom || "-"} - ${TYPES_ARME[p.typeArme] || "-"}`),
            h("div", { classe: "sg-illustration" }, splash ? image(splash, p.nom, "sg-splash", repli) : repli()),
            h("p", { classe: "petit sg-constellations-titre" }, `Constellations : C${p.constellation}`),
            h("div", { classe: "sg-constellations" }, [1, 2, 3, 4, 5, 6].map((n) =>
                h("span", { classe: "sg-constellation" + (n <= p.constellation ? " active" : ""), "aria-label": `Constellation ${n}` }, String(n))))),
        h("div", { classe: "sg-talents" }, [["Attaque normale", 0], ["Compétence", 1], ["Déchaînement", 2]].map(([nom, i]) =>
            h("div", { classe: "sg-talent" + (p.talents[i]?.bonus ? " bonus" : "") }, h("strong", {}, String(p.talents[i]?.niveau ?? "-")), h("span", {}, nom)))),
        p.arme
            ? h("div", { classe: "sg-arme" },
                p.arme.icone ? image(urlIconeEnka(p.arme.icone), p.arme.nom, "sg-arme-icone") : h("span", { classe: "sg-arme-icone vide" }),
                h("div", {}, h("span", { classe: "sg-etiquette" }, "Arme"), h("strong", {}, p.arme.nom),
                    h("span", { classe: "doux petit" }, `Niv. ${p.arme.niveau} · Raffinement R${p.arme.raffinement}`),
                    h("span", { classe: "doux petit" }, [`ATQ de base ${nombre(p.arme.atq)}`, p.arme.sous ? `${p.arme.sous.libelle} ${format(p.arme.sous.valeur, p.arme.sous.pct)}` : null].filter(Boolean).join(" · "))))
            : h("div", { classe: "sg-arme" }, h("span", { classe: "doux" }, "Aucune arme équipée")),
        boutonRecos(p, contexte));
}

// Bouton vers les équipes recommandées, affiché seulement si le personnage est le principal d'une recommandation.
function boutonRecos(p, contexte) {
    const nom = String(p.en || p.nom || "");
    const n = contexte.recos.get(nom.toLowerCase());
    if (!n) return null;
    return h("button", {
        classe: "bouton bouton-principal sg-reco-bouton", type: "button",
        onclick: () => window.dispatchEvent(new CustomEvent("het:recos", { detail: { personnage: nom } })),
    }, `Équipes recommandées (${n})`);
}

function colonneStats(p, combat) {
    return h("div", { classe: "sg-col sg-stats" },
        h("section", { classe: "sg-bloc" }, h("h3", {}, p.brutes ? "Statistiques en combat" : "Statistiques"),
            p.brutes
                ? combat.resultat
                : [h("ul", { classe: "sg-liste-stats" }, p.stats.map((s) => h("li", {}, h("span", {}, s.libelle), h("strong", {}, format(s.valeur, s.pct))))), combat.resultat]));
}

function ligneEquipe(combat) {
    return h("section", { classe: "sg-bloc sg-sets sg-equipe-ligne" }, h("h3", {}, "Équipe"),
        h("p", { classe: "doux petit" }, "Choisissez les 3 coéquipiers : leurs bonus hors terrain, armes et sets d'artéfacts s'ajoutent aux statistiques. Le détail des bonus est déroulable sous chaque personnage."),
        combat.equipe);
}

function colonneArtefacts(p) {
    return h("div", { classe: "sg-col sg-artefacts" },
        p.artefacts.length ? p.artefacts.map(carteArtefact) : h("div", { classe: "etat-vide" }, "Aucun artefact équipé."));
}

function carteGlobale(cache, choisi, surChoix, contexte) {
    const { donnees } = cache;
    const p = donnees.personnages.find((x) => x.id === choisi) || donnees.personnages[0];
    const j = donnees.joueur;
    const combat = blocCombat(p, contexte.equipes[p.id], (d) => contexte.sauver(p.id, d));
    const details = [
        j.niveau ? `Rang d'aventure ${j.niveau}` : null, j.niveauMonde ? `NM ${j.niveauMonde}` : null, `UID ${cache.uid}`,
        `Dernière mise à jour : ${FORMAT_DATE.format(new Date(cache.maj_le))}`,
    ].filter(Boolean);
    return h("section", { classe: "suivi-global", "aria-label": "Personnages du compte" },
        h("header", { classe: "sg-entete" },
            portrait("genshin", j.avatar || p.en, { taille: 56, rond: true }),
            h("div", { classe: "sg-joueur" }, h("h2", {}, j.pseudo || "Voyageur"), h("p", { classe: "doux petit" }, details.join(" · "))),
            h("div", { classe: "sg-chips", role: "tablist", "aria-label": "Personnages" }, donnees.personnages.map((x) =>
                h("button", {
                    classe: "sg-chip", type: "button", role: "tab", "aria-selected": String(x.id === p.id), title: `${x.nom} · Niv.${x.niveau}`,
                    style: `--element:${infoElement(x.element).couleur}`, onclick: () => surChoix(x.id),
                }, portrait("genshin", x.en || x.nom, { taille: 46, rond: true, elementCle: x.element }))))),
        h("div", { classe: "sg-corps" }, colonnePersonnage(p, contexte), colonneStats(p, combat), colonneArtefacts(p)),
        ligneEquipe(combat));
}

export async function rendreSuiviGenshin(zone) {
    const [uidEnregistre, cacheInitial, equipesEnregistrees, indexRecos] = await Promise.all([
        rpc("uid_lire", { p_token: jeton(), p_jeu: "genshin" }),
        rpc("enka_lire", { p_token: jeton() }),
        rpc("suivi_equipe_lire", { p_token: jeton() }).catch(() => ({})),
        rpc("recos_personnages", { p_jeu: "genshin" }).catch(() => []),
        chargerReferenceEnka(),
        chargerBuffs(),
    ]);
    const equipes = equipesEnregistrees || {};
    const recos = new Map((indexRecos || []).map((r) => [String(r.personnage).toLowerCase(), r.categories]));
    const minuteries = {};
    const contexte = {
        equipes,
        recos,
        sauver(persoId, donnees) {
            equipes[persoId] = donnees;
            clearTimeout(minuteries[persoId]);
            minuteries[persoId] = setTimeout(() => {
                rpc("suivi_equipe_enregistrer", { p_token: jeton(), p_perso_id: String(persoId), p_donnees: donnees }).catch((err) => {
                    message.className = "message erreur";
                    message.textContent = err.message;
                });
            }, 500);
        },
    };
    let cache = cacheInitial && String(cacheInitial.uid) === String(uidEnregistre) ? cacheInitial : null;
    let choisi = null;

    const champ = h("input", { id: "champ-uid", type: "text", inputmode: "numeric", maxlength: "10", value: uidEnregistre ? String(uidEnregistre) : "", placeholder: "ex. 800123456" });
    const message = h("p", { classe: "message", role: "status" });
    const bouton = h("button", { classe: "bouton bouton-principal", type: "submit" }, "Enregistrer et charger");
    const zoneCarte = h("div", {});

    function dessiner() {
        if (!cache) {
            remplacer(zoneCarte, h("div", { classe: "etat-vide" },
                uidEnregistre || champ.value.trim() ? "Aucun personnage chargé. Cliquez sur « Enregistrer et charger »." : "Renseignez votre UID Genshin Impact pour afficher vos personnages."));
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
                    await rpc("uid_ecrire", { p_token: jeton(), p_jeu: "genshin", p_uid: "" });
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
                const donnees = normaliserEnka(await recupererEnka(uid));
                cache = await rpc("enka_enregistrer", { p_token: jeton(), p_uid: uid, p_donnees: donnees });
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
    h("div", { classe: "champ" }, h("label", { for: "champ-uid" }, "UID Genshin Impact"), champ), bouton);

    remplacer(zone, formulaire, message,
        h("p", { classe: "doux petit" }, "Les personnages viennent de la vitrine de votre profil en jeu (8 maximum), via Enka Network. Ils sont conservés ; ils ne sont mis à jour que lorsque vous rechargez l'UID."),
        zoneCarte);
    dessiner();
}
