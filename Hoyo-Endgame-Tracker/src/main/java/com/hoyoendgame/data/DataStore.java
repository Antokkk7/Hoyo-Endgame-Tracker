package com.hoyoendgame.data;

import com.hoyoendgame.model.AbyssClear;
import com.hoyoendgame.model.CarnageClear;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.Team;
import com.hoyoendgame.model.TheatreClear;
import com.hoyoendgame.model.Weapon;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * In-memory model of everything the app tracks, backed by the JSON files of the data/ folder
 * (next to src/ when launched from the project root, or -Dhoyo.data.dir=... to override).
 * Every save*() call rewrites the matching file, so nothing is lost when the app closes.
 */
public final class DataStore {
    private static final DataStore INSTANCE = new DataStore();

    public static DataStore get() {
        return INSTANCE;
    }

    public static final String CHARACTERS_FILE = "genshinCharacters.json";
    public static final String TEAMS_FILE = "genshinEquipes.json";
    public static final String ABYSS_FILE = "genshinAbysses.json";
    public static final String THEATRE_FILE = "genshinTheatre.json";
    public static final String CARNAGE_FILE = "genshinCarnage.json";
    /** Weapon catalog, maintained by hand: the app only reads it. */
    public static final String WEAPONS_FILE = "genshinWeapons.json";

    private final Path dir = Paths.get(System.getProperty("hoyo.data.dir", "data/genshin")).toAbsolutePath();

    public final ObservableList<GenshinCharacter> characters = FXCollections.observableArrayList();
    public final ObservableList<Team> teams = FXCollections.observableArrayList();
    public final ObservableList<AbyssClear> abyss = FXCollections.observableArrayList();
    public final ObservableList<TheatreClear> theatre = FXCollections.observableArrayList();
    public final ObservableList<CarnageClear> carnage = FXCollections.observableArrayList();
    /** Read-only weapon catalog (rarity 5 first, as in the file). */
    public final ObservableList<Weapon> weapons = FXCollections.observableArrayList();

    /** Called when a file cannot be written (the UI shows an alert). */
    private Consumer<String> errorHandler = msg -> System.err.println(msg);

    private DataStore() {
    }

    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }

    /** Folder with the character pictures (personnagesIMG, next to the JSON files). */
    public Path imagesDir() {
        return dir.resolve("personnagesIMG");
    }

    public Path dataDir() {
        return dir;
    }

    /** Loads every file; missing files are created so the data/ folder is always complete. */
    public void load() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            errorHandler.accept("Impossible de créer le dossier " + dir + " : " + e.getMessage());
        }
        weapons.setAll(JsonStore.readWeapons(file(WEAPONS_FILE)));
        if (weapons.isEmpty()) {
            System.err.println("Aucune arme chargée : placez genshinWeapons.json dans " + dir);
        }
        characters.setAll(JsonStore.readList(file(CHARACTERS_FILE), "characters", GenshinCharacter.class));
        characters.forEach(GenshinCharacter::normalize);
        teams.setAll(JsonStore.readList(file(TEAMS_FILE), "teams", Team.class));
        teams.forEach(Team::normalize);
        abyss.setAll(JsonStore.readList(file(ABYSS_FILE), "clears", AbyssClear.class));
        abyss.forEach(AbyssClear::normalize);
        theatre.setAll(JsonStore.readList(file(THEATRE_FILE), "clears", TheatreClear.class));
        theatre.forEach(TheatreClear::normalize);
        sortTheatre();
        carnage.setAll(JsonStore.readList(file(CARNAGE_FILE), "clears", CarnageClear.class));
        carnage.forEach(CarnageClear::normalize);

        for (String f : List.of(CHARACTERS_FILE, TEAMS_FILE, ABYSS_FILE, THEATRE_FILE, CARNAGE_FILE)) {
            if (!Files.exists(file(f))) saveFile(f);
        }
    }

    public void saveAll() {
        saveCharacters();
        saveTeams();
        saveAbyss();
        saveTheatre();
        saveCarnage();
    }

    public void saveCharacters() {
        saveFile(CHARACTERS_FILE);
    }

    public void saveTeams() {
        saveFile(TEAMS_FILE);
    }

    public void saveAbyss() {
        saveFile(ABYSS_FILE);
    }

    public void saveTheatre() {
        saveFile(THEATRE_FILE);
    }

    public void saveCarnage() {
        saveFile(CARNAGE_FILE);
    }

    /** Most recent month first. */
    public void sortTheatre() {
        theatre.sort(Comparator.comparingInt((TheatreClear c) -> c.year * 100 + c.month).reversed());
    }

    private Path file(String name) {
        return dir.resolve(name);
    }

    private void saveFile(String name) {
        try {
            switch (name) {
                case CHARACTERS_FILE -> JsonStore.writeList(file(name), "characters", characters);
                case TEAMS_FILE -> JsonStore.writeList(file(name), "teams", teams);
                case ABYSS_FILE -> JsonStore.writeList(file(name), "clears", abyss);
                case THEATRE_FILE -> JsonStore.writeList(file(name), "clears", theatre);
                case CARNAGE_FILE -> JsonStore.writeList(file(name), "clears", carnage);
                default -> throw new IllegalArgumentException(name);
            }
        } catch (IOException e) {
            errorHandler.accept("Impossible d'enregistrer " + name + " : " + e.getMessage());
        }
    }
}
