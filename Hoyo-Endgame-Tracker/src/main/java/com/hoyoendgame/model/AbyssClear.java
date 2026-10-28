package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One Spiral Abyss clear (genshinAbysses.json): the floor the player cleared (9 to 12), the stars
 * earned before that floor (adjustable, full value by default) and the stars of its 3 rooms,
 * plus the team used for the highest cleared floor.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AbyssClear {
    public String id = UUID.randomUUID().toString();
    public String patch = "";
    public String rotation = "Rotation 1";
    /** Floor of the clear: 9, 10, 11 or 12. */
    public int floor = 12;
    /** Stars earned on the floors below the cleared floor (0 to 9 * (floor - 9)). */
    public int baseStars;
    /** Stars of room 1 of the cleared floor (0 to 3). */
    public int room1;
    /** Stars of room 2 of the cleared floor (0 to 3). */
    public int room2;
    /** Stars of room 3 of the cleared floor (0 to 3). */
    public int room3;
    /** Team used for the highest cleared floor. */
    public List<TeamMember> team = new ArrayList<>();

    /** Maximum stars before the cleared floor (full run below it). */
    public int maxBase() {
        return 9 * (floor - 9);
    }

    /** Maximum total stars for this clear (floor 9 = 9, 10 = 18, 11 = 27, 12 = 36). */
    public int maxTotal() {
        return 9 * (floor - 8);
    }

    /** Total stars earned. */
    public int total() {
        return baseStars + room1 + room2 + room3;
    }

    /** Stars of room i (0-based: 0, 1 or 2). */
    public int roomStars(int i) {
        return switch (i) {
            case 0 -> room1;
            case 1 -> room2;
            default -> room3;
        };
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (rotation == null) rotation = "";
        if (floor < 9) floor = 9;
        if (floor > 12) floor = 12;
        if (baseStars < 0) baseStars = 0;
        if (baseStars > maxBase()) baseStars = maxBase();
        room1 = clampRoom(room1);
        room2 = clampRoom(room2);
        room3 = clampRoom(room3);
        if (team == null) team = new ArrayList<>();
    }

    private static int clampRoom(int v) {
        return Math.max(0, Math.min(3, v));
    }
}