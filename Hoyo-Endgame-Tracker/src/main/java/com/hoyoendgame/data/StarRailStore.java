package com.hoyoendgame.data;

import com.hoyoendgame.model.ApocClear;
import com.hoyoendgame.model.ArbitrationClear;
import com.hoyoendgame.model.GenshinCharacter;
import com.hoyoendgame.model.MocClear;
import com.hoyoendgame.model.PureFictionClear;
import com.hoyoendgame.model.StarRailClear;
import com.hoyoendgame.model.Team;
import com.hoyoendgame.model.TeamMember;
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
 * Everything the app tracks for Honkai: Star Rail, backed by the JSON files of data/starrail/
 * (or -Dhoyo.starrail.dir=...). Same principle as {@link DataStore}: every save*() rewrites the matching file.
 * Vocabulary: a "weapon" is a Cône, the "constellation" of a team member is the Éidolon, the "refinement" is the Superposition.
 */
public final class StarRailStore {
    private static final StarRailStore INSTANCE = new StarRailStore();

    public static StarRailStore get() {
        return INSTANCE;
    }

    public static final String CHARACTERS_FILE = "honkaiStarRailCharacters.json";
    public static final String CONES_FILE = "honkaiStarRailCones.json";
    public static final String TEAMS_FILE = "honkaiStarRailEquipes.json";
    public static final String MOC_FILE = "honkaiStarRailMoc.json";
    public static final String PF_FILE = "honkaiStarRailPf.json";
    public static final String APOC_FILE = "honkaiStarRailApoc.json";
    public static final String ARBITRATION_FILE = "honkaiStarRailArbitration.json";

    private final Path dir = Paths.get(System.getProperty("hoyo.starrail.dir", "data/starrail")).toAbsolutePath();

    public final ObservableList<GenshinCharacter> characters = FXCollections.observableArrayList();
    public final ObservableList<Team> teams = FXCollections.observableArrayList();
    /** Read-only cone catalog (the app never rewrites honkaiStarRailCones.json). */
    public final ObservableList<Weapon> cones = FXCollections.observableArrayList();
    public final ObservableList<MocClear> moc = FXCollections.observableArrayList();
    public final ObservableList<PureFictionClear> pf = FXCollections.observableArrayList();
    public final ObservableList<ApocClear> apoc = FXCollections.observableArrayList();
    public final ObservableList<ArbitrationClear> arbitration = FXCollections.observableArrayList();

    private Consumer<String> errorHandler = msg -> System.err.println(msg);

    private StarRailStore() {
    }

    public void setErrorHandler(Consumer<String> handler) {
        this.errorHandler = handler;
    }

    public Path dataDir() {
        return dir;
    }

    /** Character pictures: data/starrail/personnagesIMG/&lt;name&gt;.png. */
    public Path imagesDir() {
        return dir.resolve("personnagesIMG");
    }

    /** Cone pictures: data/starrail/conesIMG/&lt;name&gt;.png. */
    /** Pictures of the Arbitration frames (arbitrationCadre/). */
    public Path frameImagesDir() {
        return dir.resolve("arbitrationCadre");
    }

    public Path coneImagesDir() {
        return dir.resolve("conesIMG");
    }

    public void load() {
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            errorHandler.accept("Impossible de créer le dossier " + dir + " : " + e.getMessage());
        }
        cones.setAll(JsonStore.readWeapons(file(CONES_FILE)));
        characters.setAll(JsonStore.readList(file(CHARACTERS_FILE), "characters", GenshinCharacter.class, "personnages"));
        characters.forEach(GenshinCharacter::normalize);
        teams.setAll(JsonStore.readList(file(TEAMS_FILE), "teams", Team.class));
        teams.forEach(Team::normalize);
        moc.setAll(JsonStore.readList(file(MOC_FILE), "clears", MocClear.class));
        moc.forEach(MocClear::normalize);
        pf.setAll(JsonStore.readList(file(PF_FILE), "clears", PureFictionClear.class));
        pf.forEach(PureFictionClear::normalize);
        apoc.setAll(JsonStore.readList(file(APOC_FILE), "clears", ApocClear.class));
        apoc.forEach(ApocClear::normalize);
        arbitration.setAll(JsonStore.readList(file(ARBITRATION_FILE), "clears", ArbitrationClear.class));
        arbitration.forEach(ArbitrationClear::normalize);
        sortAll();
        for (String f : List.of(TEAMS_FILE, MOC_FILE, PF_FILE, APOC_FILE, ARBITRATION_FILE)) {
            if (!Files.exists(file(f))) saveFile(f);
        }
    }

    public void saveAll() {
        saveTeams();
        saveMoc();
        savePf();
        saveApoc();
        saveArbitration();
    }

    public void saveTeams() {
        saveFile(TEAMS_FILE);
    }

    public void saveMoc() {
        saveFile(MOC_FILE);
    }

    public void savePf() {
        saveFile(PF_FILE);
    }

    public void saveApoc() {
        saveFile(APOC_FILE);
    }

    public void saveArbitration() {
        saveFile(ARBITRATION_FILE);
    }

    /** Most recent patch first (not by date added). */
    public void sortAll() {
        sort(moc);
        sort(pf);
        sort(apoc);
        sort(arbitration);
    }

    public static <T extends StarRailClear> void sort(ObservableList<T> list) {
        list.sort(Comparator.comparingLong((T c) -> StarRailClear.versionKey(c.patch())).reversed());
    }

    /** Members of every team of a clear are plain snapshots; this just counts non-empty teams. */
    public static int countMembers(List<List<TeamMember>> teams) {
        int n = 0;
        for (List<TeamMember> t : teams) n += t.size();
        return n;
    }

    private Path file(String name) {
        return dir.resolve(name);
    }

    private void saveFile(String name) {
        try {
            switch (name) {
                case TEAMS_FILE -> JsonStore.writeList(file(name), "teams", teams);
                case MOC_FILE -> JsonStore.writeList(file(name), "clears", moc);
                case PF_FILE -> JsonStore.writeList(file(name), "clears", pf);
                case APOC_FILE -> JsonStore.writeList(file(name), "clears", apoc);
                case ARBITRATION_FILE -> JsonStore.writeList(file(name), "clears", arbitration);
                default -> throw new IllegalArgumentException(name);
            }
        } catch (IOException e) {
            errorHandler.accept("Impossible d'enregistrer " + name + " : " + e.getMessage());
        }
    }
}
