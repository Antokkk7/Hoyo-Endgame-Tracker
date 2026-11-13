// Liste déroulante avec recherche, utilisée pour choisir un personnage ou une arme.
import { h, remplacer } from "./dom.js";

const MAX_AFFICHES = 80;

// options : tableau (ou fonction qui le renvoie) de { id, libelle, sous, visuel: () => noeud }.
export function selecteur({ placeholder, options, onChoix = () => {}, libre = false }) {
    let valeur = null;
    let ouvert = false;
    const bouton = h("button", { classe: "selecteur-bouton", type: "button", "aria-haspopup": "listbox", "aria-expanded": "false" });
    const recherche = h("input", { type: "search", placeholder: "Rechercher", autocomplete: "off", "aria-label": "Rechercher" });
    const liste = h("div", { classe: "selecteur-liste", role: "listbox" });
    const panneau = h("div", { classe: "selecteur-panneau cache" }, recherche, liste);
    const racine = h("div", { classe: "selecteur" }, bouton, panneau);

    const toutes = () => (typeof options === "function" ? options() : options);

    function dessinerBouton() {
        remplacer(bouton,
            valeur?.visuel ? valeur.visuel() : null,
            h("span", { classe: "selecteur-texte" + (valeur ? "" : " doux") }, valeur ? valeur.libelle : placeholder),
            h("span", { classe: "selecteur-fleche", "aria-hidden": "true" }, "▾"));
    }

    function choisir(option) {
        valeur = option && option.id !== "" ? option : null;
        dessinerBouton();
        fermer();
        onChoix(valeur);
    }

    function dessinerListe() {
        const saisie = recherche.value.trim().toLowerCase();
        const trouvees = toutes().filter((o) => !saisie || `${o.libelle} ${o.sous || ""}`.toLowerCase().includes(saisie));
        const lignes = trouvees.slice(0, MAX_AFFICHES).map((o) =>
            h("button", {
                classe: "selecteur-option" + (valeur && valeur.id === o.id ? " choisi" : ""), type: "button", role: "option",
                "aria-selected": String(Boolean(valeur && valeur.id === o.id)), onclick: () => choisir(o),
            }, o.visuel ? o.visuel() : null, h("span", { classe: "selecteur-texte" }, o.libelle, o.sous ? h("small", {}, o.sous) : null)));
        if (libre && saisie && !trouvees.some((o) => o.libelle.toLowerCase() === saisie)) {
            lignes.push(h("button", { classe: "selecteur-option", type: "button", onclick: () => choisir({ id: recherche.value.trim(), libelle: recherche.value.trim() }) },
                h("span", { classe: "selecteur-texte" }, `Utiliser « ${recherche.value.trim()} »`)));
        }
        if (trouvees.length > MAX_AFFICHES) lignes.push(h("p", { classe: "doux petit selecteur-note" }, `${trouvees.length - MAX_AFFICHES} autres résultats : affinez la recherche.`));
        if (!lignes.length) lignes.push(h("p", { classe: "doux petit selecteur-note" }, "Aucun résultat."));
        remplacer(liste, lignes);
    }

    function surClicExterieur(e) {
        if (!racine.contains(e.target)) fermer();
    }

    function surTouche(e) {
        if (e.key === "Escape") {
            fermer();
            bouton.focus();
        }
    }

    function ouvrir() {
        ouvert = true;
        panneau.classList.remove("cache");
        bouton.setAttribute("aria-expanded", "true");
        recherche.value = "";
        dessinerListe();
        document.addEventListener("mousedown", surClicExterieur);
        document.addEventListener("keydown", surTouche);
        recherche.focus();
    }

    function fermer() {
        if (!ouvert) return;
        ouvert = false;
        panneau.classList.add("cache");
        bouton.setAttribute("aria-expanded", "false");
        document.removeEventListener("mousedown", surClicExterieur);
        document.removeEventListener("keydown", surTouche);
    }

    bouton.addEventListener("click", () => (ouvert ? fermer() : ouvrir()));
    recherche.addEventListener("input", dessinerListe);
    dessinerBouton();

    return {
        element: racine,
        lire: () => valeur?.id ?? "",
        // Sélectionne par identifiant, sans déclencher onChoix. Un identifiant inconnu est conservé tel quel si libre.
        definir(id, forcer = false) {
            const cherche = String(id ?? "").toLowerCase();
            const trouve = cherche ? toutes().find((o) => o.id.toLowerCase() === cherche) : null;
            valeur = trouve || ((libre || forcer) && id ? { id: String(id), libelle: String(id) } : null);
            dessinerBouton();
        },
    };
}
