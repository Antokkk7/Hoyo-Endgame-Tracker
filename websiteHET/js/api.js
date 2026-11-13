// Appels aux fonctions SQL de Supabase (API REST /rest/v1/rpc), sans dépendance externe.
import { CONFIG, baseConfiguree } from "./config.js";
import { effacerSession } from "./session.js";

const MESSAGES = {
    base_non_configuree: "La base de données n'est pas encore configurée (voir js/config.js).",
    reseau: "Impossible de joindre le serveur. Vérifiez votre connexion.",
    session_invalide: "Votre session a expiré, reconnectez-vous.",
    identifiants_invalides: "Pseudo ou mot de passe incorrect.",
    trop_de_tentatives: "Trop de tentatives. Réessayez dans quelques minutes.",
    pseudo_invalide: "Pseudo invalide : 3 à 24 caractères (lettres, chiffres, _ . -).",
    pseudo_deja_pris: "Ce pseudo est déjà utilisé.",
    mot_de_passe_invalide: "Mot de passe invalide : 8 à 72 caractères.",
    ancien_mot_de_passe_incorrect: "L'ancien mot de passe est incorrect.",
    avatar_invalide: "Image de profil invalide.",
    uid_invalide: "UID invalide : 6 à 12 chiffres.",
    acces_refuse: "Accès refusé.",
    patch_invalide: "Patch invalide (exemple : 7.1).",
    clear_invalide: "Les valeurs de ce clear ne sont pas valides.",
    theatre_elements: "Sélectionnez exactement 3 éléments différents.",
    membres_invalides: "Équipe invalide : 1 à 4 personnages, chacun avec un nom.",
    equipe_nom_invalide: "Donnez un nom à l'équipe (40 caractères maximum).",
    limite_atteinte: "Limite atteinte.",
    introuvable: "Élément introuvable (supprimé depuis un autre appareil ?).",
    jeu_invalide: "Jeu invalide.",
    uid_inconnu: "Saisissez d'abord votre UID.",
    donnees_invalides: "Les données reçues sont invalides.",
    plan_invalide: "Objectif invalide.",
    plan_deja_present: "Cet élément est déjà dans votre planificateur.",
    enka_400: "UID invalide pour Enka Network.",
    enka_404: "Cet UID n'existe pas (ou le joueur est introuvable).",
    enka_424: "Le jeu est en maintenance : Enka Network est temporairement indisponible.",
    enka_429: "Trop de requêtes vers Enka Network. Réessayez dans une minute.",
    enka_500: "Enka Network rencontre un problème. Réessayez plus tard.",
    enka_503: "Enka Network est indisponible. Réessayez plus tard.",
    enka_indisponible: "Impossible de joindre Enka Network. Réessayez plus tard.",
    enka_vitrine_vide: "Aucun personnage n'est affiché dans la vitrine de votre profil. Dans le jeu : Profil > Vitrine des personnages, ajoutez-en et autorisez l'affichage des détails.",
};

export class ErreurApi extends Error {
    constructor(code) {
        super(MESSAGES[code] || "Une erreur est survenue.");
        this.code = code;
    }
}

export async function rpc(nom, parametres = {}) {
    if (!baseConfiguree()) throw new ErreurApi("base_non_configuree");
    let reponse;
    try {
        reponse = await fetch(`${CONFIG.SUPABASE_URL}/rest/v1/rpc/${nom}`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                apikey: CONFIG.SUPABASE_ANON_KEY,
                Authorization: `Bearer ${CONFIG.SUPABASE_ANON_KEY}`,
            },
            body: JSON.stringify(parametres),
        });
    } catch {
        throw new ErreurApi("reseau");
    }
    if (reponse.status === 204) return null;
    const corps = await reponse.json().catch(() => null);
    if (!reponse.ok) {
        const code = corps && typeof corps.message === "string" ? corps.message : "inconnue";
        if (code === "session_invalide") effacerSession();
        throw new ErreurApi(code);
    }
    return corps;
}
