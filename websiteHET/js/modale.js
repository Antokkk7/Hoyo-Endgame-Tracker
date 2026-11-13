// Fenêtre superposée à la page (fermeture par Échap ou clic à l'extérieur), utilisée par le planificateur.
import { h } from "./dom.js";

export function ouvrirModale({ classe = "", etiquette, contenu, surFermeture = null }) {
    const fond = h("div", { classe: "fenetre-fond" });
    const fenetre = h("div", { classe: `fenetre ${classe}`.trim(), role: "dialog", "aria-modal": "true", "aria-label": etiquette }, [contenu].flat(Infinity));
    fond.append(fenetre);

    function fermer() {
        document.removeEventListener("keydown", surTouche);
        fond.remove();
        surFermeture?.();
    }

    function surTouche(e) {
        if (e.key === "Escape") fermer();
    }

    fond.addEventListener("mousedown", (e) => {
        if (e.target === fond) fermer();
    });
    document.addEventListener("keydown", surTouche);
    document.body.append(fond);
    return { fermer, element: fenetre };
}
