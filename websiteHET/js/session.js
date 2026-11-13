// Jeton de session et compte courant, conservés dans le navigateur (localStorage).
const CLE_JETON = "het_jeton";
const CLE_COMPTE = "het_compte";

function lire(cle) {
    try {
        return localStorage.getItem(cle);
    } catch {
        return null;
    }
}

function ecrire(cle, valeur) {
    try {
        if (valeur === null) localStorage.removeItem(cle);
        else localStorage.setItem(cle, valeur);
    } catch {
        // stockage indisponible : la session ne survivra pas au rechargement
    }
}

let jetonMemoire = null;
let compteMemoire = null;

export function jeton() {
    return jetonMemoire ?? lire(CLE_JETON);
}

export function compte() {
    if (compteMemoire) return compteMemoire;
    try {
        return JSON.parse(lire(CLE_COMPTE) || "null");
    } catch {
        return null;
    }
}

export function estConnecte() {
    return Boolean(jeton());
}

export function estAdmin() {
    return compte()?.role === "administrateur";
}

export function enregistrerSession(nouveauJeton, nouveauCompte) {
    jetonMemoire = nouveauJeton;
    compteMemoire = nouveauCompte;
    ecrire(CLE_JETON, nouveauJeton);
    ecrire(CLE_COMPTE, JSON.stringify(nouveauCompte));
    diffuser();
}

export function majCompte(nouveauCompte) {
    compteMemoire = nouveauCompte;
    ecrire(CLE_COMPTE, JSON.stringify(nouveauCompte));
    diffuser();
}

export function effacerSession() {
    jetonMemoire = null;
    compteMemoire = null;
    ecrire(CLE_JETON, null);
    ecrire(CLE_COMPTE, null);
    diffuser();
}

function diffuser() {
    window.dispatchEvent(new CustomEvent("het:session"));
}
