package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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

    /** Fills missing values after loading a hand-written file. */
    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (name == null) name = "";
        if (element == null) element = Element.GEO;
    }

    public GenshinCharacter copy() {
        GenshinCharacter c = new GenshinCharacter();
        c.id = id;
        c.name = name;
        c.element = element;
        return c;
    }

    @Override
    public String toString() {
        return name;
    }
}
