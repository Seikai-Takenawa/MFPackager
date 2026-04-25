package com.takenawa.mfpackager.utils;

import com.takenawa.mfpackager.MFPackager;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class MFPFileManager {
    private static Path soundSourceTargetPath;
    private static final Map<String, HashSet<Integer>> subInstrumentInfo = new HashMap<>();

    public static String soundsJsonTargetPath;

    public static boolean createWorkingDirectory(String targetPath, String mainIns) {
        String workingDirectory = targetPath + "\\generated\\assets\\minefantasia\\sounds\\" + mainIns;
        String jsonDirectory = targetPath + "\\generated\\assets\\minefantasia";
        if (targetPath.endsWith(File.separator)) {
            workingDirectory = targetPath + "generated\\assets\\minefantasia\\sounds\\" + mainIns;
            jsonDirectory = targetPath + "genderated\\assets\\minefantasia";
        }
        try {
            Path workingDir = Path.of(workingDirectory);
            if (!Files.exists(workingDir)) {
                Files.createDirectories(workingDir);
            }
            soundSourceTargetPath = workingDir;
            soundsJsonTargetPath = jsonDirectory;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean copySoundsToTarget(String sourcePath) {
        Path sourceDir = Path.of(sourcePath);
        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.forEach(soundsSource -> {
                Path targetDir = soundSourceTargetPath.resolve(sourceDir.relativize(soundsSource));
                if (Files.isDirectory(soundsSource)) {
                    if (!Files.exists(targetDir)) {
                        try {
                            Files.createDirectories(targetDir);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
                    try {
                        Files.copy(soundsSource, targetDir, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean calculateSubInsInformation(String sourcePath) {
        int minOctave = 9;
        int maxOctave = 1;
        boolean mainFolderNull = true;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(sourcePath), Files::isDirectory)) {
            for (Path path : stream) {
                mainFolderNull = false;
                String subInstrument = path.getFileName().toString();
                try (DirectoryStream<Path> subStream = Files.newDirectoryStream(path, Files::isRegularFile)) {
                    for (Path subPath : subStream) {
                        int octave = Character.getNumericValue(subPath.getFileName().toString().charAt(0));
                        if (octave < minOctave) {
                            minOctave = octave;
                        }
                        if (octave > maxOctave) {
                            maxOctave = octave;
                        }
                    }
                }
                HashSet<Integer> octaveSet = new HashSet<>();
                octaveSet.add(minOctave);
                octaveSet.add(maxOctave);
                subInstrumentInfo.put(subInstrument, octaveSet);
                minOctave = 9;
                maxOctave = 1;
            }
            if (mainFolderNull) {
                JOptionPane.showMessageDialog(MFPackager.getInstance(), "源目录中没有子目录", "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;
    }

    public static Set<String> getAllSubInstrument() {
        return subInstrumentInfo.keySet();
    }

    public static int getSubInsMinOctave(String subIns) {
        if (subInstrumentInfo.containsKey(subIns)) {
            return subInstrumentInfo.get(subIns).stream().toList().getFirst();
        }
        return 2;
    }

    public static int getSubInsMaxOctave(String subIns) {
        if (subInstrumentInfo.containsKey(subIns)) {
            return subInstrumentInfo.get(subIns).stream().toList().getLast();
        }
        return 8;
    }

    public static String validateSourcePath(String sourcePath) {
        if (sourcePath == null || sourcePath.trim().isEmpty()) {
            return "源路径不能为空";
        }
        File sourceDir = new File(sourcePath);
        if (!sourceDir.exists()) {
            return "源路径不存在: " + sourcePath;
        }
        if (!sourceDir.isDirectory()) {
            return "源路径不是目录: " + sourcePath;
        }
        return null;
    }
}
