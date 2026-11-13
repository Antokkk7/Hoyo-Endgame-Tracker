// Génère js/config.js à partir des variables d'environnement (Vercel : Settings > Environment Variables).
// Exécuté par la commande de build de vercel.json. En local, copiez plutôt js/config.exemple.js en js/config.js.
import { writeFileSync } from "node:fs";

const url = process.env.SUPABASE_URL;
const cle = process.env.SUPABASE_ANON_KEY;

if (!url || !cle) {
    console.error("Variables manquantes : définissez SUPABASE_URL et SUPABASE_ANON_KEY dans Vercel (Settings > Environment Variables).");
    process.exit(1);
}
if (!/^https:\/\/[a-z0-9-]+\.supabase\.co\/?$/i.test(url)) {
    console.error("SUPABASE_URL doit ressembler à https://xxxx.supabase.co");
    process.exit(1);
}

const contenu = `// Fichier généré au build, ne pas modifier ni versionner.
export const CONFIG = {
    SUPABASE_URL: ${JSON.stringify(url.replace(/\/$/, ""))},
    SUPABASE_ANON_KEY: ${JSON.stringify(cle)},
};

export function baseConfiguree() {
    return Boolean(CONFIG.SUPABASE_URL && CONFIG.SUPABASE_ANON_KEY);
}
`;
writeFileSync(new URL("../js/config.js", import.meta.url), contenu);
console.log("js/config.js généré.");
