// Fonction Vercel : relaie la requête vers l'API publique d'Enka Network (https://enka.network/api/uid/UID).
// Elle existe pour envoyer le User-Agent exigé par Enka (impossible depuis un navigateur) et pour limiter
// la requête à ce seul service. Elle ne lit ni n'écrit rien : la mise en cache se fait dans Supabase.
const USER_AGENT = "HoyoEndgameTracker/1.0 (+https://github.com/Antokkk7/Hoyo-Endgame-Tracker)";

module.exports = async function handler(req, res) {
    const uid = String((req.query && req.query.uid) || "");
    if (!/^[0-9]{9,10}$/.test(uid)) {
        res.status(400).json({ erreur: "uid_invalide" });
        return;
    }
    try {
        const reponse = await fetch(`https://enka.network/api/uid/${uid}`, {
            headers: { "User-Agent": USER_AGENT, Accept: "application/json" },
            signal: AbortSignal.timeout(9000),
        });
        if (!reponse.ok) {
            // 400 format d'UID, 404 joueur introuvable, 424 maintenance du jeu, 429 trop de requêtes, 5xx indisponible
            res.status(reponse.status).json({ erreur: `enka_${reponse.status}` });
            return;
        }
        const donnees = await reponse.json();
        res.setHeader("Cache-Control", "public, s-maxage=60, stale-while-revalidate=60");
        res.status(200).json(donnees);
    } catch {
        res.status(502).json({ erreur: "enka_indisponible" });
    }
};
