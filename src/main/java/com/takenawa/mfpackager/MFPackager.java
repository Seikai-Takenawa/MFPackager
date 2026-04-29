package com.takenawa.mfpackager;

import com.takenawa.mfpackager.utils.MFPFileManager;
import com.takenawa.mfpackager.utils.MFPJsonManager;
import com.takenawa.mfpackager.utils.MFPZipper;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.List;

public class MFPackager extends JFrame {
    private static MFPackager instance;
    private static final String version = " - v1.0.2";
    private static final String[] VERSIONS = {"1.21.5", "1.21.6", "1.21.7", "1.21.8", "1.21.10", "1.21.11"};

    private JTextField assetsField, targetField;
    private JComboBox<String> mainInsComboBox;
    private JComboBox<String> versionComboBox;
    private JTextArea statusArea;
    private JButton startButton, cancelButton;

    private volatile boolean cancelRequested = false;

    public static MFPackager getInstance() {
        if (instance == null) {
            instance = new MFPackager();
        }
        return instance;
    }

    private MFPackager() {
        setTitle("Mine Fantasia Packager" + version);
        setSize(600, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setIconImage(new ImageIcon(Objects.requireNonNull(getClass().getResource("/icon/icon.png"))).getImage());

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setBorder(BorderFactory.createEmptyBorder(60, 80, 60, 80));
        JPanel inputPanel = createInputPanel();
        JScrollPane inputScrollPane = new JScrollPane(inputPanel);
        inputScrollPane.setBorder(null);
        inputScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        JPanel buttonPanel = createButtonPanel();
        mainContent.add(inputScrollPane, BorderLayout.CENTER);
        mainContent.add(buttonPanel, BorderLayout.SOUTH);
        JPanel statusPanel = createStatusPanel();

        add(mainContent, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        int row = 0;

        // line 1: asset path
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Assets 路径:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        assetsField = new JTextField(20);
        assetsField.setToolTipText("输入存放所有子乐器音源文件夹的地址");
        panel.add(assetsField, gbc);
        row++;

        // line 2: target path
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Target 路径:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        targetField = new JTextField(20);
        targetField.setToolTipText("输入存放生成的资源包的地址");
        panel.add(targetField, gbc);
        row++;

        // line 3: minecraft version
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Minecraft 版本:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        versionComboBox = new JComboBox<>(VERSIONS);
        panel.add(versionComboBox, gbc);
        row++;

        // line 4: mainInstrument
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("主乐器:"), gbc);

        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        mainInsComboBox = new JComboBox<>(new String[]{"custom_synth", "piano", "violin", "harp", "flute", "kalimba", "middle_age_synth", "happy_day_synth", "flute_synth"});
        panel.add(mainInsComboBox, gbc);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("状态信息"));

        statusArea = new JTextArea(1, 40);
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Monospaced", Font.PLAIN, 8));

        JScrollPane scrollPane = new JScrollPane(statusArea);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));

        startButton = new JButton("开始打包");
        cancelButton = new JButton("取消");

        Dimension buttonSize = new Dimension(140, 35);
        startButton.setPreferredSize(buttonSize);
        cancelButton.setPreferredSize(buttonSize);

        Font buttonFont = new Font("微软雅黑", Font.BOLD, 14);
        startButton.setFont(buttonFont);
        cancelButton.setFont(buttonFont);

        cancelButton.setEnabled(false);

        startButton.addActionListener(e -> startPacking());
        cancelButton.addActionListener(e -> {
            cancelRequested = true;
            appendStatus("正在取消打包操作...");
            cancelButton.setEnabled(false);
        });

        panel.add(startButton);
        panel.add(cancelButton);

        return panel;
    }

    private void startPacking() {
        String assetsPath = assetsField.getText().trim();
        String targetPath = targetField.getText().trim();
        String version = Objects.requireNonNull(versionComboBox.getSelectedItem()).toString();
        String instrument = Objects.requireNonNull(mainInsComboBox.getSelectedItem()).toString();
        String finalZipFileName = "minefantasia-" + instrument + "-" + "-" + version;


        String validationError = MFPFileManager.validateSourcePath(assetsPath);
        if (validationError != null) {
            appendStatus("错误: " + validationError);
            JOptionPane.showMessageDialog(this, validationError, "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (targetPath.isEmpty()) {
            appendStatus("错误: 请填写Target路径");
            JOptionPane.showMessageDialog(this, "请填写Target路径", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (MFPFileManager.isTargetPathAssetSubPath(assetsPath, targetPath)) {
            appendStatus("错误: 目标路径需与源路径不同且不位于源路径的子目录下!");
            JOptionPane.showMessageDialog(this, "目标路径需与源路径不同且不位于源路径的子目录下!", "错误", JOptionPane.ERROR_MESSAGE);
            return;
        }

        cancelRequested = false;
        startButton.setEnabled(false);
        cancelButton.setEnabled(true);

        PackingWorker worker = new PackingWorker(assetsPath, targetPath, version, instrument, finalZipFileName);
        worker.execute();
    }

    private class PackingWorker extends SwingWorker<Void, String> {
        private final String assetsPath;
        private final String targetPath;
        private final String version;
        private final String instrument;
        private final String finalZipFileName;
        private final boolean isCustom;
        private String actualZipFilePath = null;

        private boolean jsonCreated = true;

        public PackingWorker(String assetsPath, String targetPath, String version,
                             String instrument, String finalZipFileName) {
            this.assetsPath = assetsPath;
            this.targetPath = targetPath;
            this.version = version;
            this.instrument = instrument;
            this.finalZipFileName = finalZipFileName;
            this.isCustom = instrument.contains("custom");
        }

        @Override
        protected Void doInBackground() {
            publish("========== 正在创建工作目录 ==========");
            boolean dirCreated = MFPFileManager.createWorkingDirectory(targetPath, instrument);
            if (!dirCreated) {
                publish("意外中止");
                return null;
            }
            publish("完成!");

            if (cancelRequested) {
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }

            publish("========== 正在收集所需信息 ==========");
            boolean infoCollected = true;
            if (isCustom) {
                infoCollected = MFPFileManager.calculateSubInsInformation(assetsPath);
            }
            if (!infoCollected) {
                publish("意外中止");
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }
            publish("完成!");

            if (cancelRequested) {
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }

            publish("========== 正在复制目标声音文件到工作目录 ==========");
            boolean soundsCopied;
            if (!isCustom) {
                soundsCopied = MFPFileManager.copySoundsToTarget(assetsPath, true);
            } else {
                soundsCopied = MFPFileManager.copySoundsToTarget(assetsPath, false);
            }
            if (!soundsCopied) {
                publish("意外中止");
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }
            publish("完成!");

            if (cancelRequested) {
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }

            publish("========== 正在处理META信息 ==========");
            boolean metaCreated = MFPJsonManager.createMeta(targetPath, version);
            if (!metaCreated) {
                publish("意外中止");
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }
            publish("完成!");

            if (cancelRequested) {
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }

            publish("========== 正在生成模组JSON文件 ==========");
            if (isCustom) {
                for (String subIns : MFPFileManager.getAllSubInstrument()) {
                    jsonCreated = MFPJsonManager.createModJson(targetPath, subIns);
                    if (!jsonCreated) break;
                }
            }
            if (!jsonCreated) {
                publish("意外中止");
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }
            publish("完成!");

            if (cancelRequested) {
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }

            StringBuilder instruments = new StringBuilder();
            for (int i = 0; i < MFPFileManager.getAllSubInstrument().size(); i++) {
                if (i > 0) instruments.append(", ");
                instruments.append(MFPFileManager.getAllSubInstrument().stream().toList().get(i));
            }
            publish("========== 开始打包任务 ==========");
            publish("Assets路径: " + assetsPath);
            publish("Target路径: " + targetPath);
            publish("输出文件: " + MFPZipper.generateZipFileName(finalZipFileName));
            publish("Minecraft版本: " + version);
            publish("主乐器: " + instrument);
            if (isCustom) {
                publish("子乐器: " + instruments);
            }
            publish("=================================");

            String zipFileName = MFPZipper.generateZipFileName(finalZipFileName);
            actualZipFilePath = MFPZipper.buildZipFilePath(targetPath, zipFileName);
            String packSourcePath = targetPath + (targetPath.endsWith(File.separator) ? "generated" : "\\generated");

            MFPZipper.PackResult result = MFPZipper.packToZip(
                    packSourcePath, actualZipFilePath,
                    (filePath) -> publish("打包中: " + filePath),
                    () -> cancelRequested
            );

            if (cancelRequested) {
                MFPFileManager.cleanGeneratedDirectory(targetPath);
                return null;
            }

            if (result.isSuccess()) {
                publish("已完成: " + result.message());
                publish("文件大小: " + MFPZipper.getFormattedFileSize(result.zipFilePath()));
                publish("文件位置: " + result.zipFilePath());
                publish("=================================");
            } else {
                publish("打包失败: " + result.message());
                publish("=================================");
            }

            return null;
        }

        @Override
        protected void process(List<String> chunks) {
            for (String message : chunks) {
                appendStatus(message);
            }
        }

        @Override
        protected void done() {
            startButton.setEnabled(true);
            cancelButton.setEnabled(false);

            try {
                get();
            } catch (Exception e) {
                appendStatus("发生错误: " + e.getCause().getMessage());
                JOptionPane.showMessageDialog(MFPackager.this,
                        "发生错误: " + e.getCause().getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (cancelRequested) {
                appendStatus("打包操作已取消");
                cancelRequested = false;
                JOptionPane.showMessageDialog(MFPackager.this,
                        "打包操作已取消",
                        "信息", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (actualZipFilePath != null) {
                File zipFile = new File(actualZipFilePath);
                if (zipFile.exists() && !cancelRequested) {
                    boolean deletedWorkingDir = MFPFileManager.cleanGeneratedDirectory(targetPath);

                    int option = JOptionPane.showConfirmDialog(MFPackager.this,
                            String.format("打包完成！\n文件大小: %s\n\n是否打开目标文件夹？",
                                    MFPZipper.getFormattedFileSize(actualZipFilePath)),
                            "打包完成", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        try {
                            Desktop.getDesktop().open(new File(targetField.getText().trim()));
                        } catch (IOException ex) {
                            appendStatus("无法打开文件夹: " + ex.getMessage());
                        }
                    }
                    if (!deletedWorkingDir) {
                        JOptionPane.showMessageDialog(MFPackager.this, "工作文件未能成功删除，请手动删除！", "警告", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(MFPackager.this,
                            "打包失败，请检查上方状态信息",
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
            cancelRequested = false;
        }
    }

    private void appendStatus(String message) {
        statusArea.append(message + "\n");
        statusArea.setCaretPosition(statusArea.getDocument().getLength());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> getInstance().setVisible(true));
    }
}
