package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One Pure Fiction clear (honkaiStarRailPf.json): the score of each of the 4 rooms, the stellar mode flag of
 * room 4 (3 teams, up to 120000 points instead of 80000) and the teams used in the last room.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PureFictionClear implements StarRailClear {
    public static final int ROOMS = 4;
    public static final int ROOM_MAX = 80000;
    public static final int ROOM_MAX_STELLAR = 120000;

    public String id = UUID.randomUUID().toString();
    public String patch = "";
    /** Score of rooms 1 to 4. */
    public List<Integer> scores = new ArrayList<>();
    public boolean stellar;
    /** Rooms 1 and 2 cleared with "auto clear" (full score). */
    public boolean auto1;
    public boolean auto2;
    public List<List<TeamMember>> teams = new ArrayList<>();

    @Override
    public String patch() {
        return patch;
    }

    public boolean autoOf(int room) {
        return room == 0 ? auto1 : room == 1 && auto2;
    }

    public int scoreOf(int room) {
        if (autoOf(room)) return ROOM_MAX;
        return room < scores.size() ? scores.get(room) : 0;
    }

    /** Maximum score of a room (room 4 in stellar mode: 120000). */
    public int maxOf(int room) {
        return room == ROOMS - 1 && stellar ? ROOM_MAX_STELLAR : ROOM_MAX;
    }

    @JsonIgnore
    public int totalScore() {
        return scoreOf(ROOMS - 1);
    }

    @JsonIgnore
    public int maxTotal() {
        return maxOf(ROOMS - 1);
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (scores == null) scores = new ArrayList<>();
        while (scores.size() < ROOMS) scores.add(0);
        for (int i = 0; i < ROOMS; i++) scores.set(i, Math.max(0, scores.get(i) == null ? 0 : scores.get(i)));
        if (teams == null) teams = new ArrayList<>();
    }
}
