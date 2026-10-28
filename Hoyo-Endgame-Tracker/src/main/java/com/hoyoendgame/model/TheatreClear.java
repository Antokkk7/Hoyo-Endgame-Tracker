package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** One Imaginarium Theater clear (genshinTheatre.json). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TheatreClear {
    private static final String[] MONTHS = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet",
            "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

    public String id = UUID.randomUUID().toString();
    public int year = 2026;
    /** 1 to 12. */
    public int month = 1;
    public String patch = "";
    public TheatreMode mode = TheatreMode.LUNAR;
    /** The three elements of the month. */
    public List<Element> elements = new ArrayList<>();
    /** Stars ("astres") earned, out of mode.acts. */
    public int stars;
    /** One boolean per star ("astre"): true = obtained (violet), false = missed (grey). */
    public List<Boolean> starFlags = new ArrayList<>();
    /** Arcanum challenges completed (Défi Lunaire only). */
    public int arcanum;
    /** Best act reached. */
    public int bestAct;
    public TeamMember mvp;
    /** True when the MVP was an intro / trial (borrowed) character. */
    public boolean mvpTrial;

    public static String[] months() {
        return MONTHS.clone();
    }

    public String monthLabel() {
        int m = Math.max(1, Math.min(12, month));
        return MONTHS[m - 1] + " " + year;
    }

    /** Star flags for display: falls back to "first `stars` stars obtained" when starFlags is missing. */
    public List<Boolean> starFlagsOrFallback() {
        if (starFlags != null && starFlags.size() == mode.acts) return starFlags;
        List<Boolean> flags = new ArrayList<>();
        for (int i = 0; i < mode.acts; i++) flags.add(i < stars);
        return flags;
    }

    public void normalize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (patch == null) patch = "";
        if (mode == null) mode = TheatreMode.LUNAR;
        if (elements == null) elements = new ArrayList<>();
        elements.removeIf(e -> e == null);
        if (starFlags == null) starFlags = new ArrayList<>();
        if (starFlags.size() > mode.acts) starFlags = new ArrayList<>(starFlags.subList(0, mode.acts));
        if (starFlags.size() < mode.acts) {
            List<Boolean> base = starFlags;
            starFlags = new ArrayList<>(base);
            for (int i = base.size(); i < mode.acts; i++) starFlags.add(i < stars);
        }
        stars = 0;
        for (Boolean b : starFlags) if (b != null && b) stars++;
    }
}