package com.hoyoendgame.ui;

import com.hoyoendgame.data.DataStore;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Finds character pictures in {@code data/genshin/personnagesIMG}. A picture is matched by name, ignoring
 * case, accents-less punctuation and spaces: "Kamisato Ayaka" matches "kamisatoayaka.png" or "Kamisato_Ayaka.jpg".
 * Supported formats: png, jpg, jpeg, gif, bmp (JavaFX cannot read WebP).
 */
public final class CharacterImages {
    private static final List<String> EXT = List.of("png", "jpg", "jpeg", "gif", "bmp");
    private static final Map<String, Path> INDEX = new HashMap<>();
    private static final Map<String, Optional<Image>> CACHE = new HashMap<>();
    private static boolean indexed;

    private CharacterImages() {
    }

    static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static synchronized void index() {
        if (indexed) return;
        indexed = true;
        Path dir = DataStore.get().imagesDir();
        if (!Files.isDirectory(dir)) return;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                String file = p.getFileName().toString();
                int dot = file.lastIndexOf('.');
                if (dot <= 0) continue;
                String ext = file.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.equals("webp")) {
                    System.err.println("[images] WebP non supporté par JavaFX, convertissez en PNG : " + file);
                    continue;
                }
                if (EXT.contains(ext)) INDEX.putIfAbsent(key(file.substring(0, dot)), p);
            }
        } catch (IOException e) {
            System.err.println("[images] lecture impossible de " + dir + " : " + e.getMessage());
        }
    }

    /** The picture of this character, or null when there is none. */
    public static synchronized Image get(String name) {
        index();
        String k = key(name);
        if (k.isEmpty()) return null;
        return CACHE.computeIfAbsent(k, kk -> {
            Path p = INDEX.get(kk);
            if (p == null) return Optional.empty();
            Image img = new Image(p.toUri().toString(), false);
            return img.isError() ? Optional.empty() : Optional.of(img);
        }).orElse(null);
    }
}
