package com.ranorat.app;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.net.URL;

public class MainStudioClass extends JFrame {

    private static final String VERSION = "1.0.2"; 
    private JLabel imageLabel, infoLabel; 
    private JScrollPane scrollPane; 
    private BufferedImage currentImage;
    
    private final HistoryManager historyManager = new HistoryManager(30);
    private BufferedImage originalImage1, originalImage2;
    
    // クロップ用フィールド
    private JTextField cropAmountField;
    private JRadioButton rbOuter, rbInner, rbLeftLeft, rbRightRight, rbTLBR, rbBLTR, rbTopTop, rbBotBot;
    
    private File lastSavedDirectory = null; 
    private String loadedBaseName = "processed_image";

    public MainStudioClass() {
        setTitle("ステレオ画像クロッパー(微調整ソフト)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        infoLabel = new JLabel("画像を読み込んでください (D&Dまたは自動読み込み)", SwingConstants.CENTER);
        infoLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        infoLabel.setOpaque(true);
        infoLabel.setBackground(new Color(240, 240, 240));
        add(infoLabel, BorderLayout.NORTH);

        imageLabel = new JLabel("ここに2枚の画像を同時にドラッグ＆ドロップ", SwingConstants.CENTER);
        imageLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(Color.LIGHT_GRAY);
        
        scrollPane = new JScrollPane(imageLabel);
        add(scrollPane, BorderLayout.CENTER);
        setupDropTarget();

        scrollPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) { updateImageDisplay(); }
        });

        // --- クロップ設定パネル (真ん中寄せレイアウト) ---
        JPanel configPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5)); // 水平間隔を広めにとる
        configPanel.setBorder(new TitledBorder("クロップ設定"));


        // 1. ラジオボタン群を右寄せで配置するコンテナ
        JPanel radioGrid = new JPanel(new GridLayout(4, 2, 5, 2)); // 2列の距離を5に設定

        // 1. 左側: ラジオボタン (2列x4行)
        rbOuter = new JRadioButton("端同士", true);
        rbInner = new JRadioButton("内側同士");
        rbLeftLeft = new JRadioButton("左辺同士");
        rbRightRight = new JRadioButton("右辺同士");
        rbTLBR = new JRadioButton("左上/右下");
        rbBLTR = new JRadioButton("左下/右上");
        rbTopTop = new JRadioButton("上辺同士");
        rbBotBot = new JRadioButton("下辺同士");

        ButtonGroup group = new ButtonGroup();
        for (JRadioButton rb : new JRadioButton[]{rbOuter, rbInner, rbLeftLeft, rbRightRight, rbTLBR, rbBLTR, rbTopTop, rbBotBot}) {
            group.add(rb);
            radioGrid.add(rb);
        }

        // 2. 右側のコントロールパネル (入力フィールドとボタン)
        JPanel controlPanel = new JPanel(new GridLayout(2, 1, 5, 5));


        // 入力フィールド部
        cropAmountField = new JTextField("10", 6);
        // 空欄時0補完処理
        cropAmountField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (cropAmountField.getText().trim().isEmpty()) cropAmountField.setText("0");
            }
        });

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        inputPanel.add(new JLabel("ピクセル数:"));
        inputPanel.add(cropAmountField);

        // 実行ボタン
        JButton cropExecuteBtn = new JButton("クロップ実行");
        cropExecuteBtn.setPreferredSize(new Dimension(100, 30));

        // ▼ 以下のリスナーを追加して executeSelectiveCrop() を呼び出すようにします
        cropExecuteBtn.addActionListener(e -> executeSelectiveCrop());

        controlPanel.add(inputPanel);
        controlPanel.add(cropExecuteBtn);

        // 3. すべてを FlowLayout(CENTER) のパネルに格納して配置
        configPanel.add(radioGrid);
        configPanel.add(controlPanel);

        // --- SOUTH パネルの全体構成 ---
        JPanel southPanel = new JPanel(new BorderLayout(5, 5));

        // 1. クロップ設定パネル (North)
        southPanel.add(configPanel, BorderLayout.NORTH);

        // 2. ボタン操作パネル群 (Center)
        JPanel operationsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        // グループA: 変形・操作
        JPanel transformPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        transformPanel.setBorder(new TitledBorder("画像変形・操作"));
        JButton rotateLeftBtn = new JButton("左90°回転");
        JButton rotateRightBtn = new JButton("右90°回転");
        JButton swapBtn = new JButton("左右入れ替え");
        JButton zeroBtn = new JButton("数値を0に");
        transformPanel.add(rotateLeftBtn);
        transformPanel.add(rotateRightBtn);
        transformPanel.add(swapBtn);
        transformPanel.add(zeroBtn);

        rotateLeftBtn.addActionListener(e -> applyTransform(img -> ImageProcessor.rotate(img, false)));  
        rotateRightBtn.addActionListener(e -> applyTransform(img -> ImageProcessor.rotate(img, true)));  
        swapBtn.addActionListener(e -> applyTransform(ImageProcessor::swapOnly));
        zeroBtn.addActionListener(e -> cropAmountField.setText("0"));

        // グループB: 履歴・保存
        JPanel historyPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        historyPanel.setBorder(new TitledBorder("履歴・保存"));
        JButton undoBtn = new JButton("Undo");
        JButton redoBtn = new JButton("Redo");
        JButton resetBtn = new JButton("初期化");
        JButton saveBtn = new JButton("PNG一括保存");
        historyPanel.add(undoBtn); historyPanel.add(redoBtn);
        historyPanel.add(resetBtn); historyPanel.add(saveBtn);

        undoBtn.addActionListener(e -> performUndo());
        redoBtn.addActionListener(e -> performRedo()); // 追加
        resetBtn.addActionListener(e -> performReset());
        saveBtn.addActionListener(e -> saveImagesAllAsPng());

        operationsPanel.add(transformPanel);
        operationsPanel.add(historyPanel);

        southPanel.add(operationsPanel, BorderLayout.CENTER);

        // バージョン情報 (South)
        JLabel versionLabel = new JLabel("JStereoImageCropper v" + VERSION + " | (c) 2026 ranorat", SwingConstants.RIGHT);
        southPanel.add(versionLabel, BorderLayout.SOUTH);
        
        add(southPanel, BorderLayout.SOUTH);


        setMinimumSize(new Dimension(850, 600)); 
        setSize(1200, 900);
        setLocationRelativeTo(null);
    }

    private void executeSelectiveCrop() {
        try {
            int val = Integer.parseInt(cropAmountField.getText().trim());
            String mode = rbOuter.isSelected() ? "outer" : rbInner.isSelected() ? "inner" :
                          rbLeftLeft.isSelected() ? "leftleft" : rbRightRight.isSelected() ? "rightright" :
                          rbTLBR.isSelected() ? "tlbr" : rbBLTR.isSelected() ? "bltr" :
                          rbTopTop.isSelected() ? "toptop" : "botbot";
            
            applyTransform(img -> ImageProcessor.executeSelectiveCrop(img, mode, val));
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "0以上の整数を入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyTransform(ImageTransform transform) {
        if (currentImage == null) return;
        historyManager.push(currentImage);
        currentImage = transform.apply(currentImage);
        updateImageDisplay();
    }

    // --- 以下、既存のヘルパーメソッド ---
    private void setupDropTarget() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @Override
            public void drop(DropTargetDropEvent dtde) {
                try {
                    Transferable tr = dtde.getTransferable();
                    if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        dtde.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                        if (files.size() != 2) {
                            JOptionPane.showMessageDialog(MainStudioClass.this, "必ず2枚同時にドロップしてください。", "エラー", JOptionPane.ERROR_MESSAGE);
                            dtde.dropComplete(false); return;
                        }
                        loadImagesFromFiles(files.get(0), files.get(1));
                        dtde.dropComplete(true);
                    } else { dtde.rejectDrop(); }
                } catch (Exception e) { e.printStackTrace(); dtde.rejectDrop(); }
            }
        });
    }

    private void initStereoImages(BufferedImage raw1, BufferedImage raw2, String baseName) {
        if (raw1 == null || raw2 == null || raw1.getWidth() != raw2.getWidth() || raw1.getHeight() != raw2.getHeight()) {
            JOptionPane.showMessageDialog(this, "不適合な画像形式、または解像度不一致です。", "エラー", JOptionPane.ERROR_MESSAGE);
            return;
        }

        originalImage1 = new BufferedImage(raw1.getWidth(), raw1.getHeight(), BufferedImage.TYPE_INT_ARGB);
        originalImage1.createGraphics().drawImage(raw1, 0, 0, null);
        originalImage2 = new BufferedImage(raw2.getWidth(), raw2.getHeight(), BufferedImage.TYPE_INT_ARGB);
        originalImage2.createGraphics().drawImage(raw2, 0, 0, null);

        this.loadedBaseName = baseName;
        historyManager.clear();
        currentImage = ImageProcessor.buildCombinedImage(originalImage1, originalImage2);
        imageLabel.setBackground(null);
        updateImageDisplay();
    }

    public void loadImagesFromFiles(File f1, File f2) {
        try {
            if (!f1.exists() || !f2.exists()) {
                infoLabel.setText("初期画像ファイル (image1.png / image2.png) が見つかりません。画像をドロップしてください。");
                return;
            }
            BufferedImage raw1 = ImageIO.read(f1);
            BufferedImage raw2 = ImageIO.read(f2);

            String name1 = f1.getName().replaceAll("\\.[^.]+$", "");
            String name2 = f2.getName().replaceAll("\\.[^.]+$", "");
            initStereoImages(raw1, raw2, name1 + "_" + name2);

        } catch (IOException ex) { ex.printStackTrace(); }
    }

    public void loadImagesFromResources(String path1, String path2) {
        try {
            URL url1 = getClass().getResource(path1);
            URL url2 = getClass().getResource(path2);

            if (url1 == null || url2 == null) {
                infoLabel.setText("初期画像リソース (" + path1 + " / " + path2 + ") が見つかりません。画像をドロップしてください。");
                return;
            }

            BufferedImage raw1 = ImageIO.read(url1);
            BufferedImage raw2 = ImageIO.read(url2);

            String name1 = path1.substring(path1.lastIndexOf("/") + 1).replaceAll("\\.[^.]+$", "");
            String name2 = path2.substring(path2.lastIndexOf("/") + 1).replaceAll("\\.[^.]+$", "");
            initStereoImages(raw1, raw2, name1 + "_" + name2);

        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void saveImagesAllAsPng() {
        if (currentImage == null) return;
        int dotChoice = JOptionPane.showConfirmDialog(this, "保存する【結合画像】に「補助点」を書き込みますか？\n（※左右単体画像には補助点は書き込まれません）", "選択", JOptionPane.YES_NO_CANCEL_OPTION);
        if (dotChoice == JOptionPane.CANCEL_OPTION || dotChoice == JOptionPane.CLOSED_OPTION) return;

        JFileChooser chooser = new JFileChooser();
        if (lastSavedDirectory != null && lastSavedDirectory.exists()) {
            chooser.setCurrentDirectory(lastSavedDirectory);
        }
        
        String timeStamp = new SimpleDateFormat("_yyyyMMdd_HHmmss").format(new Date());
        chooser.setSelectedFile(new File(loadedBaseName + timeStamp));
        
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File baseFile = chooser.getSelectedFile();
            String parentDir = baseFile.getParent();
            lastSavedDirectory = chooser.getCurrentDirectory();

            String baseName = baseFile.getName().toLowerCase().endsWith(".png") ? baseFile.getName().substring(0, baseFile.getName().length() - 4) : baseFile.getName();

            int w = currentImage.getWidth() / 2;
            BufferedImage leftImg = currentImage.getSubimage(0, 0, w, currentImage.getHeight());
            BufferedImage rightImg = currentImage.getSubimage(w, 0, w, currentImage.getHeight());
            BufferedImage combinedImg = currentImage;

            if (dotChoice == JOptionPane.YES_OPTION) {
                BufferedImage leftWithDot = StereoDotPainter.drawTargetDot(leftImg);
                BufferedImage rightWithDot = StereoDotPainter.drawTargetDot(rightImg);
                combinedImg = StereoDotPainter.buildCombinedWithDots(leftWithDot, rightWithDot);
                baseName += "_with_dots";
            }

            try {
                ImageIO.write(leftImg, "png", new File(parentDir, baseName + "_left.png"));
                ImageIO.write(rightImg, "png", new File(parentDir, baseName + "_right.png"));
                ImageIO.write(combinedImg, "png", new File(parentDir, baseName + "_combined.png"));
                JOptionPane.showMessageDialog(this, "3枚のPNG画像を保存しました。", "保存完了", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) { ex.printStackTrace(); }
        }
    }

    private void performUndo() {
        if (historyManager.canUndo()) { 
            currentImage = historyManager.undo(currentImage); 
            updateImageDisplay(); 
        } else { 
            JOptionPane.showMessageDialog(this, "これ以上戻れません。", "情報", JOptionPane.INFORMATION_MESSAGE); 
        }
    }

    // --- メソッドの追加 ---
    private void performRedo() {
        if (historyManager.canRedo()) {
            currentImage = historyManager.redo(currentImage);
            updateImageDisplay();
        } else {
            JOptionPane.showMessageDialog(this, "これ以上進めません。", "情報", JOptionPane.INFORMATION_MESSAGE);
        }
    }


    private void performReset() {
        if (originalImage1 == null || originalImage2 == null) return;
        if (JOptionPane.showConfirmDialog(this, "すべての履歴を破棄して初期状態に戻しますか？", "確認", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            historyManager.clear(); 
            currentImage = ImageProcessor.buildCombinedImage(originalImage1, originalImage2); 
            cropAmountField.setText("10");
            updateImageDisplay();
        }
    }
    private ImageIcon getScaledIcon(BufferedImage src) {
        int imgW = src.getWidth(), imgH = src.getHeight();
        int panelW = scrollPane.getViewport().getWidth() - 10, panelH = scrollPane.getViewport().getHeight() - 10;
        if (panelW <= 0 || panelH <= 0 || (imgW <= panelW && imgH <= panelH)) return new ImageIcon(src);

        double scale = Math.min((double) panelW / imgW, (double) panelH / imgH);
        return new ImageIcon(src.getScaledInstance((int) (imgW * scale), (int) (imgH * scale), Image.SCALE_SMOOTH));
    }

    private void updateImageDisplay() {
        if (currentImage == null) return;
        imageLabel.setText(""); 
        int w = currentImage.getWidth() / 2, h = currentImage.getHeight();
        BufferedImage leftWithDot = StereoDotPainter.drawTargetDot(currentImage.getSubimage(0, 0, w, h));
        BufferedImage rightWithDot = StereoDotPainter.drawTargetDot(currentImage.getSubimage(w, 0, w, h));
        imageLabel.setIcon(getScaledIcon(StereoDotPainter.buildCombinedWithDots(leftWithDot, rightWithDot)));
        infoLabel.setText(String.format("【現在の状態】 1枚あたり: %d×%d px | 結合: %d×%d px (履歴: %d件)", w, h, currentImage.getWidth(), h, historyManager.size()));
        imageLabel.revalidate(); imageLabel.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainStudioClass studio = new MainStudioClass();
            studio.setVisible(true);
            studio.loadImagesFromResources("/resources/images/image1.png", "/resources/images/image2.png");
        });
    }
}
