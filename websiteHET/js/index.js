// Page d'accueil : sélection du jeu, puis section de connexion en dessous tant que personne n'est connecté.
import { h, remplacer } from "./dom.js";
import { formulaireAuth, initConnexion } from "./connexion.js";
import { estConnecte } from "./session.js";

const corps = document.body;
const bouton = document.getElementById("defilement");
const section = document.getElementById("connexion");
const haut = document.getElementById("haut");

const auth = formulaireAuth({ idPrefixe: "i" });
remplacer(document.getElementById("zone-connexion"), auth.element);

function enBas() {
    return window.scrollY > window.innerHeight / 2;
}

function majBouton() {
    const bas = enBas();
    bouton.querySelector(".defilement-texte").textContent = bas ? "Choisir un jeu" : "Se connecter";
    bouton.querySelector(".defilement-fleche").textContent = bas ? "↑" : "↓";
    bouton.setAttribute("aria-label", bas ? "Revenir au choix du jeu" : "Aller à la connexion");
}

function majEtat() {
    const connecte = estConnecte();
    corps.classList.toggle("connecte", connecte);
    section.hidden = connecte;
    bouton.hidden = connecte;
    if (connecte) {
        window.scrollTo(0, 0);
        if (location.hash === "#connexion") history.replaceState(null, "", location.pathname);
    } else {
        majBouton();
    }
}

bouton.addEventListener("click", () => (enBas() ? haut : section).scrollIntoView({ behavior: "smooth" }));
window.addEventListener("scroll", majBouton, { passive: true });
window.addEventListener("het:session", majEtat);

initConnexion({ masquerDeconnecte: true });
majEtat();
if (!estConnecte() && location.hash === "#connexion") section.scrollIntoView();
