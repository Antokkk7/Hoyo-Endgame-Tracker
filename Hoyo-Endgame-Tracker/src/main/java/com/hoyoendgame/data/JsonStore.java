package com.hoyoendgame.data;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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

    static <T> List<T> readList(Path file, String key, Class<T> type) {
        List<T> out = new ArrayList<>();
        if (!Files.exists(file)) return out;
        try {
            JsonNode root = MAPPER.readTree(file.toFile());
            JsonNode arr = root == null ? null : (root.isArray() ? root : root.get(key));
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
