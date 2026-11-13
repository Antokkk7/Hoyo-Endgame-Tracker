// Création d'éléments sans innerHTML : tout texte passe par textContent (pas d'injection possible).
export function h(balise, attributs = {}, ...enfants) {
    const el = document.createElement(balise);
    for (const [cle, valeur] of Object.entries(attributs || {})) {
        if (valeur === null || valeur === undefined || valeur === false) continue;
        if (cle === "classe") el.className = valeur;
        else if (cle === "style") el.style.cssText = valeur;
        else if (cle.startsWith("on") && typeof valeur === "function") el.addEventListener(cle.slice(2), valeur);
        else if (valeur === true) el.setAttribute(cle, "");
        else el.setAttribute(cle, String(valeur));
    }
    ajouter(el, enfants);
    return el;
}

export function ajouter(parent, enfants) {
    for (const enfant of enfants.flat()) {
        if (enfant === null || enfant === undefined || enfant === false) continue;
        parent.append(enfant.nodeType ? enfant : document.createTextNode(String(enfant)));
    }
    return parent;
}

export function vider(el) {
    while (el.firstChild) el.removeChild(el.firstChild);
    return el;
}

export function remplacer(el, ...enfants) {
    vider(el);
    return ajouter(el, enfants);
}

const FORMAT = new Intl.NumberFormat("fr-FR");

export function nombre(n) {
    return FORMAT.format(n ?? 0);
}
