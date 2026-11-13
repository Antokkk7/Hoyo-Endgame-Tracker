// Modèle de configuration. Copiez ce fichier en js/config.js (ignoré par git) ou laissez Vercel le générer
// depuis les variables d'environnement SUPABASE_URL et SUPABASE_ANON_KEY (scripts/generer-config.mjs).
// Ne mettez JAMAIS la clé "service_role" ici.
export const CONFIG = {
    SUPABASE_URL: "https://VOTRE-PROJET.supabase.co",
    SUPABASE_ANON_KEY: "VOTRE_CLE_ANON",
};

export function baseConfiguree() {
    return !CONFIG.SUPABASE_URL.includes("VOTRE-PROJET") && !CONFIG.SUPABASE_ANON_KEY.includes("VOTRE_CLE");
}
