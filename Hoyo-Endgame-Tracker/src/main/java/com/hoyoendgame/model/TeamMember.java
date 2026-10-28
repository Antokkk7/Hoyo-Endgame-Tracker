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

    public static TeamMember from(GenshinCharacter c) {
        TeamMember m = new TeamMember();
        m.characterId = c.id;
        m.name = c.name;
        m.element = c.element;
        m.level = c.level;
        m.constellation = c.constellation;
        m.weapon = c.weapon;
        m.refinement = c.refinement;
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

    /** "C2 · R1" style summary. */
    public String meta() {
        return "C" + constellation + " · R" + refinement;
    }
}
