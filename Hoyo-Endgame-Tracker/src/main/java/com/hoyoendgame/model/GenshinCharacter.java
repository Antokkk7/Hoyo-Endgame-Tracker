package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * A character of the game (genshinCharacters.json): only its name and element.
 * Level, constellation ("doublons"), weapon and refinement are chosen per team, see {@link TeamMember}.
 * Old files that still contain those fields are read fine: unknown properties are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenshinCharacter {
    public String id = UUID.randomUUID().toString();
    @JsonAlias({"nom"})
    public String name = "";
    @JsonAlias({"élément", "Element"})
    public Element element = Element.GEO;
    /** Honkai: Star Rail only: the character path ("voie"), e.g. "Nihilité". Empty for Genshin. */
    @JsonAlias({"voie"})
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public String path = "";
    /** Honkai: Star Rail only: 4 or 5 stars. 0 = unknown / not used (Genshin). */
    @JsonAlias({"rarete", "rareté"})
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public int rarity;

    /** Fills missing values after loading a hand-written file. */
    public void normalize() {
        if (name == null) name = "";
        if (id == null || id.isBlank()) id = "c-" + name.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (id.equals("c-")) id = UUID.randomUUID().toString();
        if (path == null) path = "";
        if (element == null) element = Element.GEO;
    }

    public GenshinCharacter copy() {
        GenshinCharacter c = new GenshinCharacter();
        c.id = id;
        c.name = name;
        c.element = element;
        c.path = path;
        c.rarity = rarity;
        return c;
    }

    @Override
    public String toString() {
        return name;
    }
}
