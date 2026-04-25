package com.takenawa.mfpackager.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

public class MFPJsonManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, Integer> versionMap = Map.of("1.21.5", 55, "1.21.6", 63, "1.21.7", 64, "1.21.8", 64, "1.21.10", 69, "1.21.11", 75);

    public static boolean createMeta(String targetPath, String version) {
        try {
            Path finalTarget = Path.of(targetPath + "\\generated\\pack.mcmeta");

            if (targetPath.endsWith(File.separator)) {
                finalTarget = Path.of(targetPath + "generated\\pack.mcmeta");
            }

            String description = "MineFantasia resource pack.";
            int format = versionMap.get(version);

            JsonObject metaJson = new JsonObject();
            metaJson.addProperty("description", description);
            metaJson.addProperty("pack_format", format);

            JsonObject rootJson = new JsonObject();
            rootJson.add("pack", metaJson);

            try (Writer writer = new FileWriter(finalTarget.toFile())) {
                GSON.toJson(rootJson, writer);
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static boolean createModJson(String targetPath, String subIns) {
        try {
            Path target = Path.of(targetPath).resolve(subIns + ".json");
            int minOctave = MFPFileManager.getSubInsMinOctave(subIns);
            int maxOctave = MFPFileManager.getSubInsMaxOctave(subIns);

            JsonObject modJson = new JsonObject();
            modJson.addProperty("name", subIns);
            modJson.addProperty("minOctave", minOctave);
            modJson.addProperty("maxOctave", maxOctave);
            modJson.addProperty("fadeDuration", 1);

            try (Writer writer = new FileWriter(target.toFile())) {
                GSON.toJson(modJson, writer);
            }

        } catch (IOException e) {
            return false;
        }
        return true;
    }
}
