package com.hoyoendgame.model;

/** Imaginarium Theater difficulty modes and their constraints. */
public enum TheatreMode {
    EASY("Facile", 8, 10, 60, 3, 0),
    NORMAL("Normal", 12, 14, 60, 6, 0),
    HARD("Difficile", 16, 20, 70, 8, 0),
    VISIONARY("Défi Visionnaire", 22, 26, 70, 10, 0),
    LUNAR("Défi Lunaire", 28, 32, 70, 12, 2);

    public final String label;
    public final int minCharacters;
    public final int maxCharacters;
    public final int minLevel;
    /** Number of acts, also the maximum number of stars ("astres"). */
    public final int acts;
    /** Number of Arcanum challenges (only Défi Lunaire). */
    public final int arcanum;

    TheatreMode(String label, int minCharacters, int maxCharacters, int minLevel, int acts, int arcanum) {
        this.label = label;
        this.minCharacters = minCharacters;
        this.maxCharacters = maxCharacters;
        this.minLevel = minLevel;
        this.acts = acts;
        this.arcanum = arcanum;
    }

    public boolean hasArcanum() {
        return arcanum > 0;
    }

    public String requirements() {
        return minCharacters + " à " + maxCharacters + " personnages\nNiveau " + minLevel + " minimum\n"
                + acts + " actes" + (hasArcanum() ? " + Arcanum" : "");
    }

    @Override
    public String toString() {
        return label;
    }
}
