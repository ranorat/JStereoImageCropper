package com.ranorat.JStereoImageCropper;

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
import java.util.Stack;
import java.net.URL;

public class MainStudioClass extends JFrame {

    private static final String VERSION = "1.0.0"; 
    private JLabel imageLabel, infoLabel; 
    private JScrollPane scrollPane; 
    private BufferedImage currentImage;
    private Stack<BufferedImage> undoBuffer = new Stack<>();
    private BufferedImage originalImage1, originalImage2;
    private JTextField topField, bottomField, leftField, rightField;
    
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

        JPanel configPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        configPanel.setBorder(new TitledBorder("切り取りピクセル数設定 (ドット単位)"));
        topField = new JTextField("10"); bottomField = new JTextField("10");
        leftField = new JTextField("15"); rightField = new JTextField("15");
        configPanel.add(createFieldPanel("上 (Top):", topField));
        configPanel.add(createFieldPanel("下 (Bottom):", bottomField));
        configPanel.add(createFieldPanel("左 (Left):", leftField));
        configPanel.add(createFieldPanel("右 (Right):", rightField));

        JPanel btnPanel = new JPanel(new GridLayout(2, 4, 8, 8));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JButton cropBtn = new JButton("4辺通常クロップ");
        JButton mirrorCropBtn = new JButton("上下左右対称クロップ"); 
        JButton swapBtn = new JButton("左右場所入れ替え");
        JButton saveBtn = new JButton("PNG画像を一括保存 (3枚)");
        JButton rotateLeftBtn = new JButton("同時90度左回転");
        JButton rotateRightBtn = new JButton("同時90度右回転");
        JButton zeroBtn = new JButton("入力をすべて0にする");
        
        JPanel utilityPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        JButton undoBtn = new JButton("戻る (Undo)");
        JButton resetBtn = new JButton("初期化 (Reset)");
        utilityPanel.add(undoBtn); utilityPanel.add(resetBtn);

        btnPanel.add(cropBtn); btnPanel.add(mirrorCropBtn); btnPanel.add(swapBtn); btnPanel.add(saveBtn);
        btnPanel.add(rotateLeftBtn); btnPanel.add(rotateRightBtn); btnPanel.add(zeroBtn); btnPanel.add(utilityPanel);

        JLabel versionLabel = new JLabel("JStereoImageCropper v" + VERSION + " | (c) 2026 ranorat", SwingConstants.RIGHT);
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 10));
        versionLabel.setForeground(Color.GRAY);
        versionLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 15));

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(configPanel, BorderLayout.NORTH);
        southPanel.add(btnPanel, BorderLayout.CENTER);
        southPanel.add(versionLabel, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);

        cropBtn.addActionListener(e -> executeCropOnly());
        mirrorCropBtn.addActionListener(e -> executeMirrorCrop());
        swapBtn.addActionListener(e -> executeSwapOnly());
        rotateLeftBtn.addActionListener(e -> executeRotate(false));  
        rotateRightBtn.addActionListener(e -> executeRotate(true));  
        saveBtn.addActionListener(e -> saveImagesAllAsPng());
        zeroBtn.addActionListener(e -> {
            topField.setText("0"); bottomField.setText("0"); leftField.setText("0"); rightField.setText("0");
        });
        undoBtn.addActionListener(e -> performUndo());
        resetBtn.addActionListener(e -> performReset());

        setMinimumSize(new Dimension(850, 520)); 
        setSize(1200, 850);
        setLocationRelativeTo(null);
    }

    private JPanel createFieldPanel(String labelText, JTextField textField) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.add(new JLabel(labelText), BorderLayout.WEST);
        p.add(textField, BorderLayout.CENTER);

        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (textField.getText().trim().isEmpty()) {
                    textField.setText("0");
                }
            }
        });
        return p;
    }

//******************************************************************************

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
                        // ★ ドロップ時はパソコンのファイルなので、File用のメソッドを呼ぶ (変更なし)
                        loadImagesFromFiles(files.get(0), files.get(1));
                        dtde.dropComplete(true);
                    } else { dtde.rejectDrop(); }
                } catch (Exception e) { e.printStackTrace(); dtde.rejectDrop(); }
            }
        });
    }

    public void loadImagesFromFiles(File f1, File f2) {
        try {
            if (!f1.exists() || !f2.exists()) {
                infoLabel.setText("初期画像ファイル (image1.png / image2.png) が見つかりません。画像をドロップしてください。");
                return;
            }
            BufferedImage raw1 = ImageIO.read(f1);
            BufferedImage raw2 = ImageIO.read(f2);
            if (raw1 == null || raw2 == null || raw1.getWidth() != raw2.getWidth() || raw1.getHeight() != raw2.getHeight()) {
                JOptionPane.showMessageDialog(this, "不適合な画像形式、または解像度不一致です。", "エラー", JOptionPane.ERROR_MESSAGE); return;
            }
            
            originalImage1 = new BufferedImage(raw1.getWidth(), raw1.getHeight(), BufferedImage.TYPE_INT_ARGB);
            originalImage1.createGraphics().drawImage(raw1, 0, 0, null);
            originalImage2 = new BufferedImage(raw2.getWidth(), raw2.getHeight(), BufferedImage.TYPE_INT_ARGB);
            originalImage2.createGraphics().drawImage(raw2, 0, 0, null);

            String name1 = f1.getName();
            String name2 = f2.getName();
            if (name1.contains(".")) name1 = name1.substring(0, name1.lastIndexOf("."));
            if (name2.contains(".")) name2 = name2.substring(0, name2.lastIndexOf("."));
            loadedBaseName = name1 + "_" + name2;

            undoBuffer.clear();
            currentImage = ImageProcessor.buildCombinedImage(originalImage1, originalImage2);
            imageLabel.setBackground(null);
            updateImageDisplay();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void executeCropOnly() {
        if (currentImage == null) return;
        int[] c = getCropValues(); if (c == null) return;
        if ((currentImage.getWidth()/2) - c[2] - c[3] <= 0 || currentImage.getHeight() - c[0] - c[1] <= 0) { showSizeError(); return; }
        undoBuffer.push(ImageProcessor.deepCopy(currentImage));
        currentImage = ImageProcessor.cropOnly(currentImage, c);
        updateImageDisplay();
    }

    private void executeMirrorCrop() {
        if (currentImage == null) return;
        int[] c = getCropValues(); if (c == null) return;
        if ((currentImage.getWidth()/2) - c[2] - c[3] <= 0 || currentImage.getHeight() - c[0] - c[1] <= 0) { showSizeError(); return; }
        undoBuffer.push(ImageProcessor.deepCopy(currentImage));
        currentImage = ImageProcessor.mirrorCrop(currentImage, c);
        updateImageDisplay();
    }

    private void executeSwapOnly() {
        if (currentImage == null) return;
        undoBuffer.push(ImageProcessor.deepCopy(currentImage));
        currentImage = ImageProcessor.swapOnly(currentImage);
        updateImageDisplay();
    }

    private void executeRotate(boolean isRight) {
        if (currentImage == null) return;
        undoBuffer.push(ImageProcessor.deepCopy(currentImage));
        currentImage = ImageProcessor.rotate(currentImage, isRight);
        updateImageDisplay();
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
        if (!undoBuffer.isEmpty()) { currentImage = undoBuffer.pop(); updateImageDisplay(); }
        else { JOptionPane.showMessageDialog(this, "これ以上戻れません。", "情報", JOptionPane.INFORMATION_MESSAGE); }
    }

    private void performReset() {
        if (originalImage1 == null || originalImage2 == null) return;
        if (JOptionPane.showConfirmDialog(this, "すべての履歴を破棄して初期状態に戻しますか？", "確認", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            undoBuffer.clear(); currentImage = ImageProcessor.buildCombinedImage(originalImage1, originalImage2); updateImageDisplay();
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
        infoLabel.setText(String.format("【現在の状態】 1枚あたり: %d×%d px | 結合: %d×%d px (履歴: %d件)", w, h, currentImage.getWidth(), h, undoBuffer.size()));
        imageLabel.revalidate(); imageLabel.repaint();
    }

    private int[] getCropValues() {
        try {
            int[] vals = { Integer.parseInt(topField.getText().trim()), Integer.parseInt(bottomField.getText().trim()), Integer.parseInt(leftField.getText().trim()), Integer.parseInt(rightField.getText().trim()) };
            if (vals[0] < 0 || vals[1] < 0 || vals[2] < 0 || vals[3] < 0) throw new NumberFormatException();
            return vals;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "0以上の整数を入力してください。", "入力エラー", JOptionPane.ERROR_MESSAGE); return null;
        }
    }

    private void showSizeError() {
        JOptionPane.showMessageDialog(this, "切り取りサイズが画像サイズを超えています。", "サイズエラー", JOptionPane.ERROR_MESSAGE);
    }



    //画像読み込みロジック (JAR内リソース用：起動時に連動) ★新しく追加
    // ==========================================
    public void loadImagesFromResources(String path1, String path2) {
        try {
            java.net.URL url1 = getClass().getResource(path1);
            java.net.URL url2 = getClass().getResource(path2);

            if (url1 == null || url2 == null) {
                infoLabel.setText("初期画像リソース (" + path1 + " / " + path2 + ") が見つかりません。画像をドロップしてください。");
                return;
            }

            // URLから直接画像を読み込む (JAR対応)
            java.awt.image.BufferedImage raw1 = javax.imageio.ImageIO.read(url1);
            java.awt.image.BufferedImage raw2 = javax.imageio.ImageIO.read(url2);

            if (raw1 == null || raw2 == null || raw1.getWidth() != raw2.getWidth() || raw1.getHeight() != raw2.getHeight()) {
                JOptionPane.showMessageDialog(this, "不適合な画像形式、または解像度不一致です。", "エラー", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // (以降のバッファ作成や画面更新、名前抽出のロジックはFile用とほぼ同じ)
            originalImage1 = new java.awt.image.BufferedImage(raw1.getWidth(), raw1.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            originalImage1.createGraphics().drawImage(raw1, 0, 0, null);
            originalImage2 = new java.awt.image.BufferedImage(raw2.getWidth(), raw2.getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
            originalImage2.createGraphics().drawImage(raw2, 0, 0, null);

            String name1 = path1.substring(path1.lastIndexOf("/") + 1);
            String name2 = path2.substring(path2.lastIndexOf("/") + 1);
            if (name1.contains(".")) name1 = name1.substring(0, name1.lastIndexOf("."));
            if (name2.contains(".")) name2 = name2.substring(0, name2.lastIndexOf("."));
            loadedBaseName = name1 + "_" + name2;

            undoBuffer.clear();
            currentImage = ImageProcessor.buildCombinedImage(originalImage1, originalImage2);
            imageLabel.setBackground(null);
            updateImageDisplay();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
        }
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainStudioClass studio = new MainStudioClass();
            studio.setVisible(true);

//            studio.loadImagesFromFiles(new File("image1.png"), new File("image2.png"));

            // ★ 起動時はJAR内部の「リソースパス」から読み込む
            studio.loadImagesFromResources("/resources/images/image1.png", "/resources/images/image2.png");

        });
    }
}
