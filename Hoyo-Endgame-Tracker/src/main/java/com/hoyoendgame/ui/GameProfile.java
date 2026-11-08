package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import com.hoyoendgame.data.StarRailStore;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.Team;
import com.hoyoendgame.model.Weapon;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * What differs between the two games for the team editor: where the characters / weapons come from,
 * the maximum level, the words used (arme or cône, raffinement or superposition) and the picture folders.
 */
public final class GameProfile {
    public final String name;
    public final ObservableList<GenshinCharacter> characters;
    public final ObservableList<Weapon> weapons;
    public final ObservableList<Team> teams;
    public final Runnable saveTeams;
    public final ImageFolder characterImages;
    public final ImageFolder weaponImages;
    public final int maxLevel;
    /** "C" for Genshin constellations, "E" for Honkai: Star Rail eidolons. */
    public final String consPrefix;
    /** "R" for refinements, "S" for superpositions. */
    public final String refinePrefix;
    /** "Arme" / "Cône". */
    public final String weaponTitle;
    /** "arme" / "cône" (lower case, in sentences). */
    public final String weaponWord;
    /** "Raffinement" / "Superposition". */
    public final String refineTitle;
    public final List<String> usages;
    private final String cssFile;

    private GameProfile(String name, ObservableList<GenshinCharacter> characters, ObservableList<Weapon> weapons,
                        ObservableList<Team> teams, Runnable saveTeams, ImageFolder characterImages, ImageFolder weaponImages,
                        int maxLevel, String consPrefix, String refinePrefix, String weaponTitle, String weaponWord,
                        String refineTitle, List<String> usages, String cssFile) {
        this.name = name;
        this.characters = characters;
        this.weapons = weapons;
        this.teams = teams;
        this.saveTeams = saveTeams;
        this.characterImages = characterImages;
        this.weaponImages = weaponImages;
        this.maxLevel = maxLevel;
        this.consPrefix = consPrefix;
        this.refinePrefix = refinePrefix;
        this.weaponTitle = weaponTitle;
        this.weaponWord = weaponWord;
        this.refineTitle = refineTitle;
        this.usages = usages;
        this.cssFile = cssFile;
    }

    /** Stylesheet of the game (genshin.css / starrail.css), also needed by the dialogs. */
    public String css() {
        return cssFile;
    }

    private static GameProfile genshin;
    private static GameProfile starRail;

    public static synchronized GameProfile genshin() {
        if (genshin == null) {
            DataStore s = DataStore.get();
            genshin = new GameProfile("Genshin Impact", s.characters, s.weapons, s.teams, s::saveTeams,
                    CharacterImages.folder(), new ImageFolder(s.weaponImagesDir()), 90, "C", "R",
                    "Arme", "arme", "Raffinement",
                    List.of("Libre", "Abysses · première moitié", "Abysses · seconde moitié",
                            "Théâtre de l'imaginarium", "Carnage chtonien"),
                    "genshin.css");
        }
        return genshin;
    }

    public static synchronized GameProfile starRail() {
        if (starRail == null) {
            StarRailStore s = StarRailStore.get();
            starRail = new GameProfile("Honkai: Star Rail", s.characters, s.cones, s.teams, s::saveTeams,
                    new ImageFolder(s.imagesDir()), new ImageFolder(s.coneImagesDir()), 80, "E", "S",
                    "Cône", "cône", "Superposition",
                    List.of("Libre", "Salle oubliée", "Pure fiction", "Fantômes de l'apocalypse", "Arbitre de l'instable"),
                    "starrail.css");
        }
        return starRail;
    }
}
