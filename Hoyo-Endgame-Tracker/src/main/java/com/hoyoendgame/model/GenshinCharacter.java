package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

/**
 * A character of the user's roster (genshinCharacters.json).
 * Field aliases make the loader tolerant to French key names.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenshinCharacter {
    public String id = UUID.randomUUID().toString();
    @JsonAlias({"nom"})
    public String name = "";
    @JsonAlias({"élément", "Element"})
    public Element element = Element.GEO;
    @JsonAlias({"niveau", "lvl"})
    public int level = 90;
    @JsonAlias({"doublons", "constellations"})
    public int constellation = 0;
    @JsonAlias({"arme"})
    public String weapon = "";
    @JsonAlias({"raffinement", "weaponRefinement"})
    public int refinement = 1;

    /** Fills missing values after loading a hand-written file. */
    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (name == null) name = "";
        if (element == null) element = Element.GEO;
        if (weapon == null) weapon = "";
        level = Math.max(1, Math.min(90, level));
        constellation = Math.max(0, Math.min(6, constellation));
        refinement = Math.max(1, Math.min(5, refinement));
    }

    public GenshinCharacter copy() {
        GenshinCharacter c = new GenshinCharacter();
        c.id = id;
        c.name = name;
        c.element = element;
        c.level = level;
        c.constellation = constellation;
        c.weapon = weapon;
        c.refinement = refinement;
        return c;
    }

    @Override
    public String toString() {
        return name;
    }
}
