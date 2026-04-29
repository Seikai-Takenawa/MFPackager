package com.takenawa.mfpackager.utils;

import com.takenawa.mfpackager.MFPackager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
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

    public static boolean isTargetPathAssetSubPath(String assetPath, String targetPath) {
        Path parentPath = Path.of(assetPath);
        Path childPath = Path.of(targetPath);
        try {
            Path parent = parentPath.toRealPath();
            Path child = childPath.toRealPath();
            return child.startsWith(parent);
        } catch (IOException e) {
            Path parent = parentPath.normalize();
            Path child = childPath.normalize();
            return child.startsWith(parent);
        }
    }

    public static boolean copySoundsToTarget(String sourcePath, boolean onlyTop) {
        Path sourceDir = Path.of(sourcePath);

        boolean hasSubDirectory;
        try (Stream<Path> list = Files.list(sourceDir)) {
            hasSubDirectory = list.anyMatch(Files::isDirectory);
        } catch (IOException e) {
            return false;
        }
        if (!onlyTop && !hasSubDirectory) {
            JOptionPane.showMessageDialog(MFPackager.getInstance(), "源目录中没有子目录!", "自定义乐器打包错误", JOptionPane.ERROR_MESSAGE);
            return false;
        } else if (onlyTop && hasSubDirectory) {
            JOptionPane.showMessageDialog(MFPackager.getInstance(), "源目录中有目录! \n请直接定位到存放ogg文件的目录下", "模组乐器打包错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }


        try (Stream<Path> stream = Files.walk(sourceDir)) {
            stream.forEach(soundsSource -> {
                Path targetDir = soundSourceTargetPath.resolve(sourceDir.relativize(soundsSource));

                if (onlyTop) {
                    if (!Files.isDirectory(soundsSource)) {
                        try {
                            Path parent = targetDir.getParent();
                            if (parent != null && !Files.exists(parent)) {
                                Files.createDirectories(parent);
                            }
                            Files.copy(soundsSource, targetDir, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else {
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
                            Path parent = targetDir.getParent();
                            if (parent != null && !Files.exists(parent)) {
                                Files.createDirectories(parent);
                            }
                            Files.copy(soundsSource, targetDir, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean cleanGeneratedDirectory(String targetPath) {
        Component component = MFPackager.getInstance();
        if (targetPath == null || targetPath.trim().isEmpty()) {
            System.err.println("目标路径为空，无法清空临时文件夹");
            return false;
        }

        String generatedPath = targetPath + (targetPath.endsWith(File.separator) ? "generated" : "\\generated");
        File generatedDir = new File(generatedPath);

        if (!generatedDir.exists()) {
            return true;
        }

        if (!generatedDir.isDirectory()) {
            System.err.println("路径不是目录: " + generatedPath);
            return false;
        }

        try (Stream<Path> stream = Files.walk(generatedDir.toPath())) {
            stream.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(file -> {
                if (!file.delete()) {
                    JOptionPane.showMessageDialog(component, "无法删除: " + file.getAbsolutePath());
                }
            });

            if (generatedDir.exists()) {
                JOptionPane.showMessageDialog(component, "清空后文件夹仍存在，可能部分文件未删除!");
                return false;
            }

            return true;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(component, "清空临时文件夹失败: " + e.getMessage());
            return false;
        }
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
                JOptionPane.showMessageDialog(MFPackager.getInstance(), "源目录中没有子目录!", "自定义乐器打包错误", JOptionPane.ERROR_MESSAGE);
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
