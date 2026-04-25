package com.takenawa.mfpackager.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MFPZipper {
    public MFPZipper() {}

    public enum PackStatus {
        SUCCESS,
        SOURCE_NOT_FOUND,
        SOURCE_NOT_DIRECTORY,
        TARGET_DIR_CREATE_FAILED,
        IO_ERROR,
        CANCELLED
    }

    public record PackResult(PackStatus status, String message, String zipFilePath) {
        public boolean isSuccess() {
            return status == PackStatus.SUCCESS;
        }
    }

    /**
     * Pack the source directory into a ZIP file (with progress callback)
     * @param sourcePath Source directory path
     * @param targetPath Target ZIP file path (full path, including filename)
     * @param progressCallback Progress callback for updating the UI (parameter: currently processed file path)
     * @param cancelChecker Cancellation checker: returns true if the packaging should be canceled
     * @return packing result
     */
    public static PackResult packToZip(String sourcePath, String targetPath,
                                       Consumer<String> progressCallback,
                                       CancelChecker cancelChecker) {
        // Verify source directory
        File sourceDir = new File(sourcePath);
        if (!sourceDir.exists()) {
            return new PackResult(PackStatus.SOURCE_NOT_FOUND,
                    "源路径不存在: " + sourcePath, null);
        }
        if (!sourceDir.isDirectory()) {
            return new PackResult(PackStatus.SOURCE_NOT_DIRECTORY,
                    "源路径不是目录: " + sourcePath, null);
        }

        // Ensure the target directory exists
        File targetFile = new File(targetPath);
        File parentDir = targetFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            if (!parentDir.mkdirs()) {
                return new PackResult(PackStatus.TARGET_DIR_CREATE_FAILED,
                        "无法创建目标目录: " + parentDir.getAbsolutePath(), null);
            }
        }

        // Start packing
        try (FileOutputStream fos = new FileOutputStream(targetFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            Path sourcePathObj = sourceDir.toPath();
            long[] packedCount = {0};

            try (Stream<Path> walk = Files.walk(sourcePathObj)) {
                for (Path path : (Iterable<Path>) walk::iterator) {
                    // if canceled
                    if (cancelChecker != null && cancelChecker.shouldCancel()) {
                        return new PackResult(PackStatus.CANCELLED,
                                "打包已取消", null);
                    }

                    if (!Files.isDirectory(path)) {
                        String relativePath = sourcePathObj.relativize(path).toString();
                        relativePath = relativePath.replace(File.separatorChar, '/');

                        ZipEntry zipEntry = new ZipEntry(relativePath);
                        zos.putNextEntry(zipEntry);
                        Files.copy(path, zos);
                        zos.closeEntry();

                        packedCount[0]++;

                        if (progressCallback != null) {
                            progressCallback.accept(relativePath);
                        }
                    }
                }
            }

            return new PackResult(PackStatus.SUCCESS,
                    String.format("成功打包 %d 个文件", packedCount[0]),
                    targetFile.getAbsolutePath());

        } catch (IOException e) {
            return new PackResult(PackStatus.IO_ERROR,
                    "IO错误: " + e.getMessage(), null);
        }
    }

    /**
     * Cancellation checker interface
     */
    @FunctionalInterface
    public interface CancelChecker {
        boolean shouldCancel();
    }

    /**
     * Generate a ZIP filename with a timestamp
     * @param baseName Base name
     * @return fileName with a timestamp
     */
    public static String generateZipFileName(String baseName) {
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss")
                .format(new java.util.Date());
        return String.format("%s_%s.zip", baseName, timestamp);
    }

    /**
     * Construct the full ZIP file path
     * @param targetDir Target directory
     * @param fileName File name
     * @return Full path
     */
    public static String buildZipFilePath(String targetDir, String fileName) {
        if (targetDir.endsWith(File.separator)) {
            return targetDir + fileName;
        } else {
            return targetDir + File.separator + fileName;
        }
    }

    /**
     * Get ZIP file size (formatted)
     * @param filePath ZIP file path
     * @return Formatted size string
     */
    public static String getFormattedFileSize(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return "0 B";
        }
        long size = file.length();
        String[] units = {"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double displaySize = size;
        while (displaySize >= 1024 && unitIndex < units.length - 1) {
            displaySize /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", displaySize, units[unitIndex]);
    }
}
