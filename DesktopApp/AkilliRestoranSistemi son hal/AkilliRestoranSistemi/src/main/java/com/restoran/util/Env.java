package com.restoran.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Ortam değişkenlerini ve isteğe bağlı .env dosyasını okur.
 * Öncelik: sistem ortamı &gt; .env &gt; varsayılan değer.
 */
public final class Env {

    private static final Map<String, String> FILE_VARS = loadDotEnv();

    private Env() { }

    public static String get(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        value = FILE_VARS.get(key);
        if (value != null && !value.isBlank()) {
            return value;
        }
        return defaultValue;
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> vars = new HashMap<>();
        Path cwd = Path.of(System.getProperty("user.dir", "."));
        Path[] candidates = {
            cwd.resolve(".env"),
            cwd.resolve("..").resolve(".env").normalize(),
            cwd.resolve("../..").resolve(".env").normalize()
        };
        for (Path path : candidates) {
            if (Files.isRegularFile(path)) {
                readFile(path, vars);
                break;
            }
        }
        return vars;
    }

    private static void readFile(Path path, Map<String, String> vars) {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int sep = line.indexOf('=');
                String key = line.substring(0, sep).trim();
                String value = line.substring(sep + 1).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                vars.putIfAbsent(key, value);
            }
        } catch (IOException ignored) {
        }
    }
}
