package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One Stygian Onslaught ("Carnage chtonien") clear (genshinCarnage.json). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CarnageClear {
    public String id = UUID.randomUUID().toString();
    public String patch = "";
    /** Free text, e.g. "2026.09.30 – 2026.11.03". */
    public String period = "";
    /** Difficulty from 1 to 6. */
    public int difficulty = 1;
    public List<BossRun> bosses = new ArrayList<>();

    /** Sum of the fight durations, in seconds. */
    public int totalSeconds() {
        int t = 0;
        for (BossRun b : bosses) t += b.seconds;
        return t;
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (period == null) period = "";
        difficulty = Math.max(1, Math.min(6, difficulty));
        if (bosses == null) bosses = new ArrayList<>();
        bosses.forEach(BossRun::normalize);
    }
}
