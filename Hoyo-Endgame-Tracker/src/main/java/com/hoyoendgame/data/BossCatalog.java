package com.hoyoendgame.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stygian Onslaught bosses of each patch, read from genshinBoss.json (read-only for the app):
 * {@code [ { "version": "7.1", "boss": ["Churin", "Automaton", "Pavlina"] }, ... ]}.
 */
public final class BossCatalog {
    private final Map<String, List<String>> byPatch = new LinkedHashMap<>();
    private final Map<String, String> periodByPatch = new LinkedHashMap<>();

    public static BossCatalog read(Path file) {
        BossCatalog catalog = new BossCatalog();
        if (!Files.isRegularFile(file)) {
            System.err.println("[boss] fichier introuvable : " + file);
            return catalog;
        }
        try {
            JsonNode root = new ObjectMapper().readTree(file.toFile());
            if (root != null && root.isObject()) {
                for (String key : new String[]{"patches", "bosses", "data"}) {
                    if (root.has(key)) {
                        root = root.get(key);
                        break;
                    }
                }
            }
            if (root != null && root.isArray()) {
                for (JsonNode n : root) {
                    String version = n.path("version").asText("").trim();
                    JsonNode bosses = n.path("boss");
                    if (!bosses.isArray()) bosses = n.path("bosses");
                    if (version.isEmpty() || !bosses.isArray()) continue;
                    List<String> names = new ArrayList<>();
                    for (JsonNode b : bosses) {
                        String s = b.asText("").trim();
                        if (!s.isEmpty()) names.add(s);
                    }
                    catalog.byPatch.put(version, names);
                    String start = n.path("debut").asText("").trim();
                    String end = n.path("fin").asText("").trim();
                    if (!start.isEmpty() && !end.isEmpty()) {
                        catalog.periodByPatch.put(version, dot(start) + " - " + dot(end));
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[boss] lecture impossible de " + file + " : " + e.getMessage());
        }
        return catalog;
    }

    /** Boss names of this patch (empty when the patch is not in the file). */
    public List<String> bosses(String patch) {
        if (patch == null) return List.of();
        return byPatch.getOrDefault(patch.trim(), List.of());
    }

    /** Period of the patch, e.g. "2026.09.30 - 2026.11.11" (empty when the file has no dates for it). */
    public String period(String patch) {
        if (patch == null) return "";
        return periodByPatch.getOrDefault(patch.trim(), "");
    }

    /** "2026-09-30" becomes "2026.09.30". */
    private static String dot(String date) {
        return date.replace('-', '.');
    }

    /** "LavaDrake" becomes "Lava Drake" (a space before each capital that follows a lower-case letter). */
    public static String pretty(String key) {
        if (key == null) return "";
        return key.trim().replaceAll("(?<=[a-zà-ÿ])(?=[A-Z])", " ");
    }
}
