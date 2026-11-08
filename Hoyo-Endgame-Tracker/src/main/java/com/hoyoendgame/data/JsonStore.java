package com.hoyoendgame.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.hoyoendgame.model.Weapon;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads and writes the JSON files of the data/ folder.
 * Every file looks like {"version": 1, "<key>": [ ... ]}; a bare JSON array is accepted when reading.
 */
final class JsonStore {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private JsonStore() {
    }

    static <T> List<T> readList(Path file, String key, Class<T> type, String... alternativeKeys) {
        List<T> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            JsonNode arr = root == null ? null : (root.isArray() ? root : root.get(key));
            for (int i = 0; arr == null && root != null && i < alternativeKeys.length; i++) arr = root.get(alternativeKeys[i]);
            if (arr != null && arr.isArray()) {
                for (JsonNode n : arr) out.add(MAPPER.treeToValue(n, type));
            }
        } catch (IOException e) {
            // Corrupted file: keep a copy so nothing is silently lost, then start empty.
            System.err.println("Impossible de lire " + file + " : " + e.getMessage());
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // nothing more we can do
            }
            out.clear();
        }
        return out;
    }

    /**
     * Reads genshinWeapons.json: {"weapons": {"5_star": ["..."], "4_star": [...]}}.
     * A bare array of names, or an array under "weapons", is accepted too (rarity 0). Missing file = empty list.
     */
    static List<Weapon> readWeapons(Path file) {
        List<Weapon> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            JsonNode w = root == null ? null : (root.isArray() ? root : root.get("weapons"));
            if (w == null && root != null) w = root.get("cones"); // Honkai: Star Rail catalog
            if (w == null && root != null) w = root.get("cones_de_lumiere");
            if (w == null) return out;
            if (w.isObject()) {
                var fields = w.fields();
                while (fields.hasNext()) {
                    var e = fields.next();
                    int rarity = 0;
                    String digits = e.getKey().replaceAll("\\D", "");
                    if (!digits.isEmpty()) rarity = Integer.parseInt(digits);
                    for (JsonNode n : e.getValue()) out.add(new Weapon(n.asText(), rarity));
                }
            } else if (w.isArray()) {
                for (JsonNode n : w) {
                    String name = n.isObject() ? (n.has("name") ? n.get("name").asText() : n.path("nom").asText()) : n.asText();
                    int rarity = n.isObject() ? (n.has("rarity") ? n.get("rarity").asInt() : n.path("rarete").asInt(0)) : 0;
                    out.add(new Weapon(name, rarity));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Impossible de lire " + file + " : " + e.getMessage());
            out.clear();
        }
        return out;
    }

    /** Atomic write: the target file is never left half-written. */
    static void writeList(Path file, String key, List<?> items) throws IOException {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("version", 1);
        doc.put(key, new ArrayList<>(items));
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        MAPPER.writeValue(tmp.toFile(), doc);
        try {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
