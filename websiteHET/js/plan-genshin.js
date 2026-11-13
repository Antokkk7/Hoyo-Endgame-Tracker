// Planificateur Genshin Impact : objectifs de montée en niveau (personnages et armes), sauvegardés dans Supabase.
import { h, remplacer, nombre } from "./dom.js";
import { rpc } from "./api.js";
import { element } from "./elements.js";
import { chargerReferenceEnka, referenceEnka, urlIconeEnka } from "./enka.js";
import { exigerConnexion, afficherErreur } from "./jeu-commun.js";
import { ouvrirModale } from "./modale.js";
import { sequenceNiveaux, libelleNiveau, PALIERS_ARME_BASSE, coutObjectif, sommeCouts, formaterMora, pastillesCout } from "./plan-couts.js";
import { estConnecte, jeton } from "./session.js";

const TYPES_ARME = { sword: "Épée", claymore: "Claymore", polearm: "Arme d'hast", bow: "Arc", catalyst: "Catalyseur" };
const ELEMENTS_ORDRE = ["PYRO", "HYDRO", "ANEMO", "ELECTRO", "DENDRO", "CRYO", "GEO"];
const COULEUR_RARETE = { 5: "#ffc83d", 4: "#c77dff", 3: "#4f9bff", 2: "#4fd6a6", 1: "#9aa3ad" };

function minuscules(t) {
    return String(t || "").toLowerCase().normalize("NFD").replace(/[̀-ͯ]/g, "");
}

function nomPerso(ref, en) {
    return Object.values(ref.persos).find((p) => p.en === en);
}

function infosObjectif(ref, o) {
    if (o.type === "personnage") {
        const p = nomPerso(ref, o.nom);
        return { affiche: p?.nom || o.nom, icone: p ? urlIconeEnka(p.icone) : null, couleur: p ? element(p.element).couleur : "#5f656e", rarete: p?.rarete || 4, element: p?.element };
    }
    const a = Object.values(ref.armes).find((x) => x.en === o.nom);
    const rarete = a?.rarete || o.donnees?.rarete || 4;
    return { affiche: a?.nom || o.nom, icone: a ? urlIconeEnka(a.icone) : null, couleur: COULEUR_RARETE[rarete] || "#9aa3ad", rarete, type: a?.type || o.donnees?.type };
}

function maxArme(rarete) {
    return rarete <= 2 ? 70 : 90;
}

function selectNiveaux(suite, valeur, nom) {
    const sel = h("select", { name: nom });
    suite.forEach((n, i) => sel.append(h("option", { value: String(i) }, libelleNiveau(n))));
    sel.value = String(Math.max(0, suite.findIndex((n) => n.niveau === valeur.niveau && n.asc === valeur.asc)));
    return sel;
}

function champ(libelle, ...contenu) {
    return h("label", { classe: "plan-champ" }, h("span", {}, libelle), ...contenu);
}

function selectEntiers(min, max, valeur) {
    const sel = h("select", {});
    for (let i = min; i <= max; i++) sel.append(h("option", { value: String(i) }, String(i)));
    sel.value = String(Math.min(max, Math.max(min, valeur)));
    return sel;
}

function boutonsDialogue(fermer, valider) {
    return h("div", { classe: "plan-boutons-dialogue" },
        h("button", { classe: "plan-rond plan-rond-non", type: "button", "aria-label": "Annuler", onclick: fermer }, "✕"),
        h("button", { classe: "plan-rond plan-rond-oui", type: "button", "aria-label": "Valider", onclick: valider }, "✓"));
}

function ligneDeuxColonnes(entete, gauche, droite) {
    return h("div", { classe: "plan-ligne-champs" }, h("strong", {}, entete), gauche, droite);
}

export async function rendrePlanGenshin(zone) {
    if (!estConnecte()) return exigerConnexion(zone, "Connectez-vous pour utiliser le planificateur.");
    const ref = await chargerReferenceEnka();
    let objectifs = (await rpc("plan_lire", { p_token: jeton() })) || [];

    const entete = h("div", { classe: "plan-entete" });
    const resume = h("div", { classe: "plan-resume" });
    const grille = h("div", { classe: "plan-grille" });
    const message = h("p", { classe: "message", role: "status" });

    async function appeler(nom, params) {
        try {
            objectifs = (await rpc(nom, { p_token: jeton(), ...params })) || [];
            message.textContent = "";
            dessiner();
            return true;
        } catch (err) {
            message.textContent = err?.message || "Une erreur est survenue.";
            return false;
        }
    }

    function dessiner() {
        const actifs = objectifs.filter((o) => o.actif);
        const total = sommeCouts(actifs.map(coutObjectif));
        remplacer(resume,
            h("span", { classe: "plan-resume-titre" }, `OBJECTIFS ACTIFS · ${actifs.length}`),
            h("span", { classe: "plan-resume-val", title: "Mora nécessaires (ascension et talents)" }, `${formaterMora(total.mora)} Mora`),
            pastillesCout(total).map(([cle, court, long, v]) => h("span", { classe: "plan-pastille", title: long }, `${court} ${nombre(v)}`)));
        if (!objectifs.length) {
            remplacer(grille, h("div", { classe: "etat-vide plan-vide" }, "Aucun objectif pour le moment. Ajoutez un personnage ou une arme."));
        } else {
            remplacer(grille, objectifs.map(carte));
        }
    }

    function carte(o) {
        const info = infosObjectif(ref, o);
        const d = o.donnees || {};
        const niveaux = `Niv. ${d.niveauActuel ?? 1}${d.ascActuelle ? "✦" : ""} → ${d.niveauDesire ?? 90}${d.ascDesiree ? "✦" : ""}`;
        const cout = coutObjectif(o);
        const corps = [];
        if (o.actif) {
            if (o.type === "personnage") {
                corps.push(h("div", { classe: "plan-ligne-info" }, `Constellation C${d.constellation ?? 0}`, " · ", `Talents ${(d.talents || []).map((t) => `${t[0]}→${t[1]}`).join(" / ")}`));
            } else {
                corps.push(h("div", { classe: "plan-ligne-info" }, `Raffinement R${d.raffinement ?? 1}`, d.evenement ? " · EXP ×1,5" : ""));
            }
            if (d.obtenu === false) corps.push(h("div", { classe: "plan-ligne-info doux" }, "Pas encore obtenu"));
            const pastilles = pastillesCout(cout);
            corps.push(h("div", { classe: "plan-mats" },
                cout.mora ? h("span", { classe: "plan-pastille plan-pastille-mora" }, `${formaterMora(cout.mora)} Mora`) : null,
                pastilles.map(([, court, long, v]) => h("span", { classe: "plan-pastille", title: long }, `${court} ${nombre(v)}`))));
            if (!pastilles.length && !cout.mora) corps.push(h("div", { classe: "plan-ligne-info doux" }, o.type === "arme" ? "Coût d'arme non calculé" : "Rien à farmer"));
        }
        return h("article", { classe: `plan-carte${o.actif ? "" : " inactive"}`, style: `--couleur:${info.couleur}` },
            h("div", { classe: "plan-carte-tete" },
                info.icone ? h("img", { src: info.icone, alt: "", loading: "lazy", classe: "plan-icone" }) : h("span", { classe: "plan-icone plan-icone-vide" }, info.affiche.slice(0, 2)),
                h("div", { classe: "plan-carte-titre" }, h("strong", {}, info.affiche), h("span", {}, niveaux)),
                h("div", { classe: "plan-carte-outils" },
                    h("button", { type: "button", classe: "plan-outil", title: "Modifier", "aria-label": `Modifier ${info.affiche}`, onclick: () => (o.type === "personnage" ? dialoguePerso(o.nom, o) : dialogueArme(o.nom, o)) }, "✎"),
                    h("button", { type: "button", classe: `plan-outil plan-outil-actif${o.actif ? " on" : ""}`, title: o.actif ? "Désactiver" : "Activer", "aria-pressed": o.actif ? "true" : "false", "aria-label": `Activer ou désactiver ${info.affiche}`, onclick: () => appeler("plan_basculer", { p_id: o.id, p_actif: !o.actif }) }, "⏻"))),
            corps.length ? h("div", { classe: "plan-carte-corps" }, corps) : null);
    }

    // Fenêtre d'ajout d'un personnage ou d'une arme
    function dialoguePerso(en, existant) {
        const p = nomPerso(ref, en);
        const d = existant?.donnees || {};
        const suite = sequenceNiveaux(90);
        const idx = (v) => suite[v];
        const actuel = selectNiveaux(suite, { niveau: d.niveauActuel ?? 1, asc: d.ascActuelle ?? false }, "actuel");
        const desire = selectNiveaux(suite, { niveau: d.niveauDesire ?? 90, asc: d.ascDesiree ?? false }, "desire");
        const consta = selectEntiers(0, 6, d.constellation ?? 0);
        const talents = [0, 1, 2].map((i) => [selectEntiers(1, 10, d.talents?.[i]?.[0] ?? 1), selectEntiers(1, 10, d.talents?.[i]?.[1] ?? 10)]);
        const obtenu = h("input", { type: "checkbox" });
        obtenu.checked = d.obtenu ?? true;
        const erreur = h("p", { classe: "message", role: "alert" });
        const modale = ouvrirModale({
            classe: "plan-dialogue", etiquette: `Objectif ${p?.nom || en}`,
            contenu: [
                h("h3", {}, p?.nom || en),
                ligneDeuxColonnes("Niv.", champ("Actuel", actuel), champ("Désiré", desire)),
                champ("Constellation", consta),
                ["Attaque normale", "Compétence élémentaire", "Déchaînement élémentaire"].map((nom, i) => ligneDeuxColonnes(nom, champ("Actuel", talents[i][0]), champ("Désiré", talents[i][1]))),
                h("label", { classe: "plan-case" }, obtenu, "Obtenu dans le jeu"),
                erreur,
                existant ? h("button", { classe: "bouton bouton-danger bouton-petit", type: "button", onclick: async () => { if (await appeler("plan_supprimer", { p_id: existant.id })) modale.fermer(); } }, "Supprimer l'objectif") : null,
                boutonsDialogue(() => modale.fermer(), async () => {
                    const a = idx(Number(actuel.value)), v = idx(Number(desire.value));
                    if (Number(desire.value) < Number(actuel.value)) { erreur.textContent = "Le niveau désiré doit être supérieur ou égal au niveau actuel."; return; }
                    const t = talents.map(([x, y]) => [Number(x.value), Number(y.value)]);
                    if (t.some(([x, y]) => y < x)) { erreur.textContent = "Un talent désiré ne peut pas être inférieur au talent actuel."; return; }
                    const donnees = { niveauActuel: a.niveau, ascActuelle: a.asc, niveauDesire: v.niveau, ascDesiree: v.asc, constellation: Number(consta.value), talents: t, obtenu: obtenu.checked };
                    if (await appeler("plan_enregistrer", { p_id: existant?.id ?? null, p_type: "personnage", p_nom: en, p_donnees: donnees })) modale.fermer();
                }),
            ],
        });
    }

    function dialogueArme(en, existant, perso = null) {
        const a = Object.values(ref.armes).find((x) => x.en === en);
        const d = existant?.donnees || {};
        const rarete = a?.rarete || d.rarete || perso?.rarete || 4;
        const max = maxArme(rarete);
        const suite = sequenceNiveaux(max, rarete <= 2 ? PALIERS_ARME_BASSE : undefined);
        const actuel = selectNiveaux(suite, { niveau: d.niveauActuel ?? 1, asc: d.ascActuelle ?? false }, "actuel");
        const desire = selectNiveaux(suite, { niveau: d.niveauDesire ?? max, asc: d.ascDesiree ?? false }, "desire");
        const raff = selectEntiers(1, 5, d.raffinement ?? 1);
        const evenement = h("input", { type: "checkbox" });
        evenement.checked = !!d.evenement;
        const obtenu = h("input", { type: "checkbox" });
        obtenu.checked = d.obtenu ?? true;
        const erreur = h("p", { classe: "message", role: "alert" });
        const modale = ouvrirModale({
            classe: "plan-dialogue", etiquette: `Objectif ${a?.nom || en}`,
            contenu: [
                h("h3", {}, a?.nom || en),
                ligneDeuxColonnes("Niv.", champ("Actuel", actuel), champ("Désiré", desire)),
                champ("Raffinement", raff),
                h("label", { classe: "plan-case" }, evenement, "Arme d'événement avec 1,5 bonus d'EXP."),
                h("label", { classe: "plan-case" }, obtenu, "Obtenu dans le jeu"),
                erreur,
                existant ? h("button", { classe: "bouton bouton-danger bouton-petit", type: "button", onclick: async () => { if (await appeler("plan_supprimer", { p_id: existant.id })) modale.fermer(); } }, "Supprimer l'objectif") : null,
                boutonsDialogue(() => modale.fermer(), async () => {
                    const x = suite[Number(actuel.value)], y = suite[Number(desire.value)];
                    if (Number(desire.value) < Number(actuel.value)) { erreur.textContent = "Le niveau désiré doit être supérieur ou égal au niveau actuel."; return; }
                    const donnees = { niveauActuel: x.niveau, ascActuelle: x.asc, niveauDesire: y.niveau, ascDesiree: y.asc, raffinement: Number(raff.value), evenement: evenement.checked, obtenu: obtenu.checked, rarete, type: a?.type || d.type || null, perso: !a };
                    if (await appeler("plan_enregistrer", { p_id: existant?.id ?? null, p_type: "arme", p_nom: en, p_donnees: donnees })) modale.fermer();
                }),
            ],
        });
    }

    function dialogueArmePerso() {
        const nom = h("input", { type: "text", maxlength: "60", placeholder: "Nom de l'arme" });
        const rar = selectEntiers(1, 5, 4);
        const type = h("select", {});
        Object.entries(TYPES_ARME).forEach(([k, v]) => type.append(h("option", { value: k }, v)));
        const erreur = h("p", { classe: "message", role: "alert" });
        const modale = ouvrirModale({
            classe: "plan-dialogue", etiquette: "Arme personnalisée",
            contenu: [h("h3", {}, "Arme personnalisée"), champ("Nom", nom), champ("Rareté", rar), champ("Type", type), erreur,
                boutonsDialogue(() => modale.fermer(), () => {
                    const n = nom.value.trim();
                    if (!n) { erreur.textContent = "Saisissez un nom."; return; }
                    if (objectifs.some((o) => o.type === "arme" && o.nom === n)) { erreur.textContent = "Cette arme est déjà dans le planificateur."; return; }
                    modale.fermer();
                    dialogueArme(n, null, { rarete: Number(rar.value) });
                })],
        });
    }

    function selecteurPerso() {
        const deja = new Set(objectifs.filter((o) => o.type === "personnage").map((o) => o.nom));
        const tous = Object.values(ref.persos).filter((p) => !deja.has(p.en)).sort((a, b) => b.rarete - a.rarete || a.nom.localeCompare(b.nom, "fr"));
        let filtre = "";
        let elem = "";
        const recherche = h("input", { type: "search", placeholder: "Rechercher un personnage", "aria-label": "Rechercher un personnage" });
        const liste = h("div", { classe: "plan-tuiles" });
        const filtres = h("div", { classe: "plan-filtres" });
        const modale = ouvrirModale({ classe: "plan-selecteur", etiquette: "Ajouter un personnage", contenu: [h("h3", {}, "Ajouter un personnage"), recherche, filtres, liste] });
        function maj() {
            remplacer(filtres, ELEMENTS_ORDRE.map((e) => h("button", { type: "button", classe: `plan-filtre${elem === e ? " on" : ""}`, style: `--couleur:${element(e).couleur}`, title: element(e).nom, onclick: () => { elem = elem === e ? "" : e; maj(); } }, element(e).nom)));
            const q = minuscules(filtre);
            const vus = tous.filter((p) => (!elem || p.element === elem) && (!q || minuscules(p.nom).includes(q) || minuscules(p.en).includes(q)));
            remplacer(liste, vus.length ? vus.map((p) => h("button", { type: "button", classe: "plan-tuile", style: `--couleur:${element(p.element).couleur};--rar:${COULEUR_RARETE[p.rarete]}`, onclick: () => { modale.fermer(); dialoguePerso(p.en, null); } },
                h("img", { src: urlIconeEnka(p.icone), alt: "", loading: "lazy" }), h("span", {}, p.nom))) : h("p", { classe: "doux" }, "Aucun personnage disponible."));
        }
        recherche.addEventListener("input", () => { filtre = recherche.value; maj(); });
        maj();
        recherche.focus();
    }

    function selecteurArme() {
        const deja = new Set(objectifs.filter((o) => o.type === "arme").map((o) => o.nom));
        const toutes = Object.values(ref.armes).filter((a) => a.rarete >= 3 && !deja.has(a.en));
        let filtre = "", type = "", tri = "etoiles";
        const recherche = h("input", { type: "search", placeholder: "Rechercher une arme", "aria-label": "Rechercher une arme" });
        const liste = h("div", { classe: "plan-tuiles" });
        const filtres = h("div", { classe: "plan-filtres" });
        const modale = ouvrirModale({ classe: "plan-selecteur", etiquette: "Ajouter une arme", contenu: [h("h3", {}, "Ajouter une arme"), recherche, filtres, liste] });
        function maj() {
            remplacer(filtres,
                h("button", { type: "button", classe: "plan-filtre on", title: "Changer le tri", onclick: () => { tri = tri === "etoiles" ? "alpha" : "etoiles"; maj(); } }, tri === "etoiles" ? "★ Rareté" : "A→Z Alphabétique"),
                Object.entries(TYPES_ARME).map(([k, v]) => h("button", { type: "button", classe: `plan-filtre${type === k ? " on" : ""}`, onclick: () => { type = type === k ? "" : k; maj(); } }, v)));
            const q = minuscules(filtre);
            const vues = toutes.filter((a) => (!type || a.type === type) && (!q || minuscules(a.nom).includes(q) || minuscules(a.en).includes(q)))
                .sort((a, b) => (tri === "etoiles" ? b.rarete - a.rarete : 0) || a.nom.localeCompare(b.nom, "fr"));
            remplacer(liste,
                h("button", { type: "button", classe: "plan-tuile plan-tuile-perso", onclick: () => { modale.fermer(); dialogueArmePerso(); } }, h("span", { classe: "plan-plus" }, "+"), h("span", {}, "Ajouter une Arme Personnalisée")),
                vues.map((a) => h("button", { type: "button", classe: "plan-tuile", style: `--couleur:${COULEUR_RARETE[a.rarete]};--rar:${COULEUR_RARETE[a.rarete]}`, onclick: () => { modale.fermer(); dialogueArme(a.en, null); } },
                    h("img", { src: urlIconeEnka(a.icone), alt: "", loading: "lazy" }), h("span", {}, a.nom))));
        }
        recherche.addEventListener("input", () => { filtre = recherche.value; maj(); });
        maj();
        recherche.focus();
    }

    // Gestion de la priorité : liste numérotée, ordre modifiable par glisser-déposer ou par les flèches du clavier
    function prioriteDialogue() {
        let ordre = objectifs.map((o) => o.id);
        const par = new Map(objectifs.map((o) => [o.id, o]));
        const liste = h("ol", { classe: "plan-priorite-liste" });
        let tire = null;
        function deplacer(id, vers) {
            const de = ordre.indexOf(id);
            if (vers < 0 || vers >= ordre.length || de === vers) return;
            ordre.splice(de, 1);
            ordre.splice(vers, 0, id);
            maj();
            liste.querySelector(`[data-id="${id}"]`)?.focus();
        }
        function maj() {
            remplacer(liste, ordre.map((id, i) => {
                const o = par.get(id);
                const info = infosObjectif(ref, o);
                const li = h("li", { classe: `plan-priorite-ligne${o.actif ? "" : " inactive"}`, style: `--couleur:${info.couleur}`, draggable: "true", tabindex: "0", "data-id": id, title: "Glissez pour changer l'ordre (ou Alt + flèches)" },
                    h("span", { classe: "plan-rang" }, String(i + 1)),
                    info.icone ? h("img", { src: info.icone, alt: "", loading: "lazy" }) : null,
                    h("span", { classe: "plan-priorite-nom" }, info.affiche),
                    h("span", { classe: "plan-priorite-type doux" }, o.type === "arme" ? "Arme" : "Personnage"));
                li.addEventListener("dragstart", (e) => { tire = id; e.dataTransfer.effectAllowed = "move"; e.dataTransfer.setData("text/plain", id); li.classList.add("tire"); });
                li.addEventListener("dragend", () => { tire = null; li.classList.remove("tire"); });
                li.addEventListener("dragover", (e) => { if (tire) { e.preventDefault(); e.dataTransfer.dropEffect = "move"; } });
                li.addEventListener("drop", (e) => { e.preventDefault(); if (tire && tire !== id) deplacer(tire, ordre.indexOf(id)); });
                li.addEventListener("keydown", (e) => {
                    if (!e.altKey) return;
                    if (e.key === "ArrowUp" || e.key === "ArrowLeft") { e.preventDefault(); deplacer(id, ordre.indexOf(id) - 1); }
                    if (e.key === "ArrowDown" || e.key === "ArrowRight") { e.preventDefault(); deplacer(id, ordre.indexOf(id) + 1); }
                });
                return li;
            }));
        }
        maj();
        const modale = ouvrirModale({
            classe: "plan-priorite", etiquette: "Gérer la priorité",
            contenu: [h("div", { classe: "plan-priorite-tete" }, h("h3", {}, "Gérer la priorité"), h("span", { classe: "doux" }, "Glissez pour changer l'ordre")),
                liste,
                boutonsDialogue(() => modale.fermer(), async () => { if (await appeler("plan_ordre", { p_ids: ordre })) modale.fermer(); })],
        });
    }

    remplacer(entete,
        h("div", { classe: "plan-titres" }, h("h2", {}, "Planification"), h("p", { classe: "doux" }, "Planifiez la montée en niveau de vos personnages et de vos armes.")),
        h("div", { classe: "plan-actions" },
            h("button", { classe: "bouton bouton-principal", type: "button", onclick: selecteurPerso }, "+ Ajouter un personnage"),
            h("button", { classe: "bouton bouton-principal", type: "button", onclick: selecteurArme }, "+ Ajouter une arme"),
            h("button", { classe: "bouton", type: "button", onclick: () => {} }, "Gérer l'inventaire"),
            h("button", { classe: "bouton", type: "button", onclick: () => { if (objectifs.length) prioriteDialogue(); else message.textContent = "Ajoutez d'abord des objectifs."; } }, "Gérer la priorité")));
    remplacer(zone, h("section", { classe: "plan" }, entete, resume, message, grille));
    dessiner();
}
