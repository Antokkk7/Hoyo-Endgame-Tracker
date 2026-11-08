package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One Apocalyptic Shadow ("Fantômes de l'apocalypse") clear (honkaiStarRailApoc.json): the difficulty chosen
 * (3 = 2 bosses, 4 = 2 bosses, 4 stellar = 3 bosses), the score of each boss and the team of each boss.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApocClear implements StarRailClear {
    /** Score cap of one boss. */
    public static final int BOSS_MAX = 4000;

    /** The three ways to clear: difficulty 3, difficulty 4, difficulty 4 in stellar mode. */
    public enum Mode {
        D3("Difficulté 3", 2),
        D4("Difficulté 4", 2),
        D4_STELLAR("Difficulté 4 · mode stellaire", 3);

        public final String label;
        public final int bosses;

        Mode(String label, int bosses) {
            this.label = label;
            this.bosses = bosses;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public String id = UUID.randomUUID().toString();
    public String patch = "";
    public Mode mode = Mode.D4;
    /** Score of each boss (2 or 3 values). */
    public List<Integer> scores = new ArrayList<>();
    /** Team of each boss. */
    public List<List<TeamMember>> teams = new ArrayList<>();

    @Override
    public String patch() {
        return patch;
    }

    public int scoreOf(int boss) {
        return boss < scores.size() ? scores.get(boss) : 0;
    }

    @JsonIgnore
    public int totalScore() {
        int s = 0;
        for (int i = 0; i < mode.bosses; i++) s += scoreOf(i);
        return s;
    }

    /** 8000 for 2 bosses, 12000 for 3 (stellar). */
    @JsonIgnore
    public int maxTotal() {
        return BOSS_MAX * mode.bosses;
    }

    @JsonIgnore
    public boolean stellar() {
        return mode == Mode.D4_STELLAR;
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (mode == null) mode = Mode.D4;
        if (scores == null) scores = new ArrayList<>();
        while (scores.size() < mode.bosses) scores.add(0);
        for (int i = 0; i < scores.size(); i++) scores.set(i, Math.max(0, scores.get(i) == null ? 0 : scores.get(i)));
        if (teams == null) teams = new ArrayList<>();
    }
}
