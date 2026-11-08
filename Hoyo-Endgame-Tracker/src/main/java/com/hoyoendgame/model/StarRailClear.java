package com.hoyoendgame.model;

import java.util.List;

/** Common part of the Honkai: Star Rail endgame clears: an id and the patch ("4.7"), used for the history order. */
public interface StarRailClear {
    String patch();

    /** "4.10" gives 4010, so the most recent patch sorts first with a descending comparison. */
    static long versionKey(String patch) {
        if (patch == null) return -1;
        String[] p = patch.trim().split("\\.");
        try {
            long major = Long.parseLong(p[0].trim());
            long minor = p.length > 1 ? Long.parseLong(p[1].trim()) : 0;
            return major * 1000 + minor;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** Teams of a clear, each a list of up to 4 members. */
    static List<List<TeamMember>> copyTeams(List<List<TeamMember>> teams) {
        List<List<TeamMember>> out = new java.util.ArrayList<>();
        if (teams != null) {
            for (List<TeamMember> t : teams) {
                List<TeamMember> c = new java.util.ArrayList<>();
                if (t != null) for (TeamMember m : t) c.add(m.copy());
                out.add(c);
            }
        }
        return out;
    }
}
