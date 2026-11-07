package com.hoyoendgame.ui;

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
 * A folder of pictures looked up by name. The match ignores case, spaces, accents-less punctuation:
 * "Kamisato Ayaka" matches "kamisatoayaka.png" or "Kamisato_Ayaka.jpg".
 * Supported formats: png, jpg, jpeg, gif, bmp (JavaFX cannot read WebP).
 */
final class ImageFolder {
    private static final List<String> EXT = List.of("png", "jpg", "jpeg", "gif", "bmp");

    private final Path dir;
    private Map<String, Path> index;
    private final Map<String, Optional<Image>> cache = new HashMap<>();

    ImageFolder(Path dir) {
        this.dir = dir;
    }

    static String key(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private Map<String, Path> index() {
        if (index != null) return index;
        Map<String, Path> map = new HashMap<>();
        if (!Files.isDirectory(dir)) return map; // not cached: the folder may be created later
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                String file = p.getFileName().toString();
                int dot = file.lastIndexOf('.');
                if (dot <= 0) continue;
                String ext = file.substring(dot + 1).toLowerCase(Locale.ROOT);
                if (ext.equals("webp")) {
                    System.err.println("[images] WebP non supporté par JavaFX, convertissez en PNG : " + p);
                    continue;
                }
                if (EXT.contains(ext)) map.putIfAbsent(key(file.substring(0, dot)), p);
            }
        } catch (IOException e) {
            System.err.println("[images] lecture impossible de " + dir + " : " + e.getMessage());
        }
        index = map;
        return map;
    }

    /** The picture with this name, or null when there is none. */
    synchronized Image get(String name) {
        String k = key(name);
        if (k.isEmpty()) return null;
        Optional<Image> cached = cache.get(k);
        if (cached != null) return cached.orElse(null);
        Path p = index().get(k);
        if (p == null) return null; // not cached, so a file added later is found
        Image img = new Image(p.toUri().toString(), false);
        if (img.isError()) {
            System.err.println("[images] image illisible par JavaFX (un WebP renommé en .png ?) : " + p
                    + "\n         convertissez-la : python tools/convert_to_png.py <dossier>");
        }
        Optional<Image> res = img.isError() ? Optional.empty() : Optional.of(img);
        cache.put(k, res);
        return res.orElse(null);
    }
}
