package com.hoyoendgame.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.text.Normalizer;

/** Genshin Impact elements (first 7) and Honkai: Star Rail elements (last 7). Serialized as the enum name. */
public enum Element {
    PYRO("Pyro", "#E2623F"),
    HYDRO("Hydro", "#3F86E8"),
    ANEMO("Anémo", "#35B08E"),
    ELECTRO("Électro", "#9660DC"),
    DENDRO("Dendro", "#72AA33"),
    CRYO("Cryo", "#68BEDB"),
    GEO("Géo", "#D19B2C"),

    // Honkai: Star Rail
    PHYSICAL("Physique", "#9AA3AD"),
    FIRE("Feu", "#F2644A"),
    ICE("Glace", "#57C7F0"),
    LIGHTNING("Foudre", "#B36BFF"),
    WIND("Vent", "#4FD6A6"),
    QUANTUM("Quantique", "#6F7CFF"),
    IMAGINARY("Imaginaire", "#F2D24B");

    /** The 7 Genshin Impact elements. */
    public static java.util.List<Element> genshin() {
        return java.util.List.of(PYRO, HYDRO, ANEMO, ELECTRO, DENDRO, CRYO, GEO);
    }

    /** The 7 Honkai: Star Rail elements. */
    public static java.util.List<Element> starRail() {
        return java.util.List.of(PHYSICAL, FIRE, ICE, LIGHTNING, WIND, QUANTUM, IMAGINARY);
    }

    private final String label;
    private final String color;

    Element(String label, String color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public String color() {
        return color;
    }

    @JsonValue
    public String json() {
        return name();
    }

    /** Tolerant parser: accepts "PYRO", "pyro", "Anémo", "anemo", "Électro"... */
    @JsonCreator
    public static Element parse(String s) {
        if (s == null) return null;
        String n = strip(s);
        for (Element e : values()) {
            if (e.name().equals(n) || strip(e.label).equals(n)) return e;
        }
        return null;
    }

    private static String strip(String s) {
        return Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "").toUpperCase();
    }

    @Override
    public String toString() {
        return label;
    }
}
