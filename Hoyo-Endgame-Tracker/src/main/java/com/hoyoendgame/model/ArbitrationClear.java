package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One Arbiter of the Unstable ("Arbitre de l'instable") clear (honkaiStarRailArbitration.json): cycles used against
 * each of the 3 Horsemen (stars come from the cycles, minus one if a character went down), then the cycles and the
 * stars of the King ("Échec et mat") and the team used against him.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArbitrationClear implements StarRailClear {
    public static final int HORSEMEN = 3;

    public String id = UUID.randomUUID().toString();
    public String patch = "";
    /** Cycles used against Horsemen 1 to 3. */
    public List<Integer> cycles = new ArrayList<>();
    /** A character went down during the fight of Horseman 1 to 3 (-1 star). */
    public List<Boolean> down = new ArrayList<>();
    /** Cycles used against the King. */
    public int kingCycles;
    /** A character went down during the King fight (-1 star). */
    public boolean kingDown;
    /** Team used against the King. */
    public List<TeamMember> team = new ArrayList<>();
    /** Team used against Horseman 1 to 3. */
    public List<List<TeamMember>> horsemenTeams = new ArrayList<>();

    @Override
    public String patch() {
        return patch;
    }

    /** 0 to 2 cycles = 3 stars, 3 to 4 = 2, 5 to 6 = 1, more = 0. */
    public static int starsForCycles(int cycles) {
        if (cycles <= 2) return 3;
        if (cycles <= 4) return 2;
        if (cycles <= 6) return 1;
        return 0;
    }

    public int cyclesOf(int horseman) {
        return horseman < cycles.size() ? cycles.get(horseman) : 0;
    }

    public boolean downOf(int horseman) {
        return horseman < down.size() && Boolean.TRUE.equals(down.get(horseman));
    }

    /** Stars of one Horseman: from the cycles, -1 when a character went down, never below 0. */
    public int starsOf(int horseman) {
        return Math.max(0, starsForCycles(cyclesOf(horseman)) - (downOf(horseman) ? 1 : 0));
    }

    /** Stars of the 3 Horsemen (max 9). */
    @JsonIgnore
    public int horsemenStars() {
        int s = 0;
        for (int i = 0; i < HORSEMEN; i++) s += starsOf(i);
        return s;
    }

    /** Stars of the King: from his cycles, -1 when a character went down, never below 0. */
    public int kingStars() {
        return Math.max(0, starsForCycles(kingCycles) - (kingDown ? 1 : 0));
    }

    /** All stars: Horsemen + King (max 12). */
    @JsonIgnore
    public int totalStars() {
        return horsemenStars() + kingStars();
    }

    /** Frame earned on the King: none, Bronze, Argent, Dorée, or Prismatique (3 stars in 0 cycles). */
    @JsonIgnore
    public String frame() {
        int ks = kingStars();
        if (ks <= 0) return "";
        if (ks == 1) return "Cadre Bronze";
        if (ks == 2) return "Cadre Argent";
        return kingCycles > 0 ? "Cadre Dorée" : "Cadre Prismatique";
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (cycles == null) cycles = new ArrayList<>();
        if (down == null) down = new ArrayList<>();
        while (cycles.size() < HORSEMEN) cycles.add(0);
        while (down.size() < HORSEMEN) down.add(false);
        for (int i = 0; i < HORSEMEN; i++) cycles.set(i, Math.max(0, cycles.get(i) == null ? 0 : cycles.get(i)));
        kingCycles = Math.max(0, kingCycles);
        if (team == null) team = new ArrayList<>();
        if (horsemenTeams == null) horsemenTeams = new ArrayList<>();
        while (horsemenTeams.size() < HORSEMEN) horsemenTeams.add(new ArrayList<>());
    }
}
