package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One character slot inside a team, a snapshot of level / constellation / weapon. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TeamMember {
    /** Optional link to genshinCharacters.json. */
    public String characterId;
    @JsonAlias({"nom"})
    public String name = "";
    public Element element = Element.GEO;
    @JsonAlias({"niveau"})
    public int level = 90;
    @JsonAlias({"doublons"})
    public int constellation = 0;
    @JsonAlias({"arme"})
    public String weapon = "";
    @JsonAlias({"raffinement"})
    public int refinement = 1;

    /** New member with default parameters (level 90, C0, no weapon, R1) to be adjusted per situation. */
    public static TeamMember from(GenshinCharacter c) {
        TeamMember m = new TeamMember();
        m.characterId = c.id;
        m.name = c.name;
        m.element = c.element;
        return m;
    }

    public TeamMember copy() {
        TeamMember m = new TeamMember();
        m.characterId = characterId;
        m.name = name;
        m.element = element;
        m.level = level;
        m.constellation = constellation;
        m.weapon = weapon;
        m.refinement = refinement;
        return m;
    }

    /** "C2 · R1" style summary ("C2" alone when no weapon is set). */
    public String meta() {
        return weapon == null || weapon.isBlank() ? "C" + constellation : "C" + constellation + " · R" + refinement;
    }
}
