package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One Spiral Abyss clear (genshinAbysses.json): stars on floors 9 to 12 and the two teams. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbyssClear {
    public String id = UUID.randomUUID().toString();
    public String patch = "";
    public String rotation = "Rotation 1";
    public int floor9;
    public int floor10;
    public int floor11;
    public int floor12;
    /** Team used for the first half of floors 11 and 12. */
    public List<TeamMember> team1 = new ArrayList<>();
    /** Team used for the second half of floors 11 and 12. */
    public List<TeamMember> team2 = new ArrayList<>();

    /** Total stars out of 36. */
    public int total() {
        return floor9 + floor10 + floor11 + floor12;
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (rotation == null) rotation = "";
        if (team1 == null) team1 = new ArrayList<>();
        if (team2 == null) team2 = new ArrayList<>();
    }
}
