// Couleurs et libellés des éléments, identiques à l'application de bureau.
export const ELEMENTS = {
    PYRO: { nom: "Pyro", couleur: "#E2623F" },
    HYDRO: { nom: "Hydro", couleur: "#3F86E8" },
    ANEMO: { nom: "Anémo", couleur: "#35B08E" },
    ELECTRO: { nom: "Électro", couleur: "#9660DC" },
    DENDRO: { nom: "Dendro", couleur: "#72AA33" },
    CRYO: { nom: "Cryo", couleur: "#68BEDB" },
    GEO: { nom: "Géo", couleur: "#D19B2C" },
    PHYSICAL: { nom: "Physique", couleur: "#9AA3AD" },
    FIRE: { nom: "Feu", couleur: "#F2644A" },
    ICE: { nom: "Glace", couleur: "#57C7F0" },
    LIGHTNING: { nom: "Foudre", couleur: "#B36BFF" },
    WIND: { nom: "Vent", couleur: "#4FD6A6" },
    QUANTUM: { nom: "Quantique", couleur: "#6F7CFF" },
    IMAGINARY: { nom: "Imaginaire", couleur: "#F2D24B" },
};

export function element(cle) {
    return ELEMENTS[String(cle || "").toUpperCase()] || { nom: cle || "", couleur: "#5F656E" };
}
