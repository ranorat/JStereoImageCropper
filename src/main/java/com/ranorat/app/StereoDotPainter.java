package com.ranorat.app;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * ステレオスコピック用：画像の上部にグレーの余白エリアをドット単位で追加し、
 * その中に2色（黒丸×白丸）の補助点を描画するクラス
 */
public class StereoDotPainter {

    private static final int HEADER_HEIGHT = 20;

    /**
     * 画像の上部にグレーの余白を結合し、その中央に2色の補助点を描画した画像を返す
     */
    public static BufferedImage drawTargetDot(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        
        BufferedImage dst = new BufferedImage(w, h + HEADER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, w, HEADER_HEIGHT);
        
        g.drawImage(src, 0, 0 + HEADER_HEIGHT, null);
        
        int centerX = w / 2;
        int dotY = HEADER_HEIGHT / 2;
        
        int blackSize = 10;
        g.setColor(Color.BLACK);
        g.fillOval(centerX - (blackSize / 2), dotY - (blackSize / 2), blackSize, blackSize);
        
        int whiteSize = 4;
        g.setColor(Color.WHITE);
        g.fillOval(centerX - (whiteSize / 2), dotY - (whiteSize / 2), whiteSize, whiteSize);
        
        g.dispose();
        return dst;
    }

    /**
     * 補助点エリア付きの左右画像を1つの横長画像に結合する
     */
    public static BufferedImage buildCombinedWithDots(BufferedImage leftImg, BufferedImage rightImg) {
        int wLeft = leftImg.getWidth();
        int wRight = rightImg.getWidth();
        int h = Math.max(leftImg.getHeight(), rightImg.getHeight());

        BufferedImage dst = new BufferedImage(wLeft + wRight, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(leftImg, 0, 0, null);
        g.drawImage(rightImg, wLeft, 0, null);
        g.dispose();
        return dst;
    }
}
