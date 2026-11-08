package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One Memory of Chaos ("Salle oubliée") clear (honkaiStarRailMoc.json): stars and cycles used for each of the
 * 12 rooms, the stellar mode flag of room 12 (= the purple star) and the teams used in the last room.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MocClear implements StarRailClear {
    public static final int ROOMS = 12;

    public String id = UUID.randomUUID().toString();
    public String patch = "";
    /** Stars (0 to 3) of rooms 1 to 12. */
    public List<Integer> stars = new ArrayList<>();
    /** Cycles used in rooms 1 to 12. */
    public List<Integer> cycles = new ArrayList<>();
    /** Room 12 cleared in stellar mode (gives the purple star and a 3rd team). */
    public boolean stellar;
    /** Room 9 / room 10 cleared with "auto clear" (3 stars, no cycle). */
    public boolean auto9;
    public boolean auto10;
    public List<List<TeamMember>> teams = new ArrayList<>();

    @Override
    public String patch() {
        return patch;
    }

    /** Rooms 1 to 8 are not tracked (always 3 stars); rooms 9 and 10 can be auto-cleared. */
    public boolean autoOf(int room) {
        return room == 8 ? auto9 : room == 9 && auto10;
    }

    public int starsOf(int room) {
        return room < stars.size() ? stars.get(room) : 0;
    }

    public int cyclesOf(int room) {
        return room < cycles.size() ? cycles.get(room) : 0;
    }

    @JsonIgnore
    public int totalStars() {
        int s = 0;
        for (int i = 0; i < ROOMS; i++) s += starsOf(i);
        return s;
    }

    @JsonIgnore
    public int totalCycles() {
        int s = 0;
        for (int i = 0; i < ROOMS; i++) s += cyclesOf(i);
        return s;
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (stars == null) stars = new ArrayList<>();
        if (cycles == null) cycles = new ArrayList<>();
        while (stars.size() < ROOMS) stars.add(0);
        while (cycles.size() < ROOMS) cycles.add(0);
        for (int i = 0; i < ROOMS; i++) {
            stars.set(i, Math.max(0, Math.min(3, stars.get(i) == null ? 0 : stars.get(i))));
            cycles.set(i, Math.max(0, cycles.get(i) == null ? 0 : cycles.get(i)));
            if (i < 8 || autoOf(i)) {
                stars.set(i, 3);
                cycles.set(i, 0);
            }
        }
        if (teams == null) teams = new ArrayList<>();
    }
}
