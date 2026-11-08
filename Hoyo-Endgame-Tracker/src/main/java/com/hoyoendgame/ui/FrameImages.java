package com.hoyoendgame.ui;

import com.hoyoendgame.data.StarRailStore;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Arbitration frame pictures from data/starrail/arbitrationCadre/. The file is found from keywords in its name:
 * bronze / argent|silver / dor|gold|or / platin|prism (platinum = prismatic frame).
 */
final class FrameImages {
    private static final Map<String, Image> CACHE = new HashMap<>();
    private static boolean warned;

    private FrameImages() {
    }

    /** @param frame "Cadre Bronze", "Cadre Argent", "Cadre Dorée" or "Cadre Prismatique" */
    static synchronized Image get(String frame) {
        if (frame == null || frame.isEmpty()) return null;
        if (CACHE.containsKey(frame)) return CACHE.get(frame);
        Image img = load(frame);
        if (img != null) CACHE.put(frame, img);
        else if (!warned && Files.isDirectory(StarRailStore.get().frameImagesDir())) {
            warned = true;
            System.err.println("[images] aucun cadre trouvé pour « " + frame + " » dans " + StarRailStore.get().frameImagesDir()
                    + " (le nom du fichier doit contenir bronze, argent/silver, dor/gold ou platin/prism)");
        }
        return img;
    }

    private static Image load(String frame) {
        Path dir = StarRailStore.get().frameImagesDir();
        if (!Files.isDirectory(dir)) return null;
        String[] words = switch (frame) {
            case "Cadre Bronze" -> new String[]{"bronze"};
            case "Cadre Argent" -> new String[]{"argent", "silver"};
            case "Cadre Dorée" -> new String[]{"dor", "gold"};
            default -> new String[]{"platin", "prism", "plat"};
        };
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                String file = p.getFileName().toString().toLowerCase(Locale.ROOT);
                int dot = file.lastIndexOf('.');
                if (dot <= 0 || !file.substring(dot + 1).matches("png|jpg|jpeg|gif|bmp")) continue;
                String key = ImageFolder.key(file.substring(0, dot));
                boolean ok = false;
                for (String w : words) if (key.contains(w)) ok = true;
                if (!ok && frame.equals("Cadre Dorée") && key.equals("or")) ok = true;
                if (ok) {
                    Image img = new Image(p.toUri().toString());
                    if (!img.isError() && img.getWidth() > 0) return img;
                }
            }
        } catch (IOException e) {
            System.err.println("[images] lecture impossible de " + dir + " : " + e.getMessage());
        }
        return null;
    }

    static ImageView view(String frame, double size) {
        Image img = get(frame);
        if (img == null) return null;
        ImageView iv = new ImageView(img);
        iv.setPreserveRatio(true);
        iv.setFitWidth(size);
        iv.setFitHeight(size);
        iv.setSmooth(true);
        return iv;
    }
}
