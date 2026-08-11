package com.ranorat.JStereoImageCropper;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * ステレオスコピック用：画像の上部にグレーの余白エリアをドット単位で追加し、
 * その中に2色（黒丸×白丸）の補助点を描画するクラス
 */
public class StereoDotPainter {

    // 追加する上部グレー余白の高さ（ピクセル数）
    private static final int HEADER_HEIGHT = 20;

    /**
     * 画像の上部にグレーの余白を結合し、その中央に2色の補助点を描画した画像を返す
     */
    public static BufferedImage drawTargetDot(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        
        // 元の画像サイズより、縦幅を HEADER_HEIGHT 分だけ大きくした新規画像を作成
        BufferedImage dst = new BufferedImage(w, h + HEADER_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        
        // 描画品質の向上（ジャギー防止）
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 1. 新しく拡張した上部エリアをグレー（Color.LIGHT_GRAY）で塗りつぶす
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, w, HEADER_HEIGHT);
        
        // 2. 元の画像を、グレー余白の下（Y座標 = HEADER_HEIGHT）にそのまま描き写す
        g.drawImage(src, 0, 0 + HEADER_HEIGHT, null);
        
        // 3. グレー余白の中央（X座標は真ん中、Y座標は余白のジャスト中心）に補助点を描画
        int centerX = w / 2;
        int dotY = HEADER_HEIGHT / 2;
        
        // 外側の黒丸を描画（直径10ピクセル）
        int blackSize = 10;
        g.setColor(Color.BLACK);
        g.fillOval(centerX - (blackSize / 2), dotY - (blackSize / 2), blackSize, blackSize);
        
        // 内側の白丸を描画（直径4ピクセル）
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

        // 左右の幅を足し算して正確な横長画像を作る
        BufferedImage dst = new BufferedImage(wLeft + wRight, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dst.createGraphics();
        g.drawImage(leftImg, 0, 0, null);
        g.drawImage(rightImg, wLeft, 0, null);
        g.dispose();
        return dst;
    }
}
