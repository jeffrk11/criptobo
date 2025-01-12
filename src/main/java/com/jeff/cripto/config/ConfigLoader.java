package com.jeff.cripto.config;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

public class ConfigLoader {
    private static Map<String, Object> config;

    static {
        try (InputStream inputStream = ConfigLoader.class.getClassLoader().getResourceAsStream("config.yaml")) {
            if (inputStream == null) {
                throw new RuntimeException("Config file 'config.yaml' not found");
            }
            Yaml yaml = new Yaml();
            config = yaml.load(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load configuration", e);
        }
    }

    public static String get(String key) {
        String[] keys = key.split("\\.");
        Object value = config;
        for (String k : keys) {
            value = ((Map<String, Object>) value).get(k);
        }
        return value.toString();
    }
}
