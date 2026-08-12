package com.ranorat.app;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * ステレオスコピック画像の回転・クロップ・入れ替えなどの純粋な画像処理を担当するクラス
 */
public class ImageProcessor {

    // 4辺通常クロップ（左右それぞれ独立して上・下・左・右を削る）
    public static BufferedImage cropOnly(BufferedImage currentImage, int[] c) {
        int w = currentImage.getWidth() / 2;
        int h = currentImage.getHeight();

        BufferedImage left = currentImage.getSubimage(0, 0, w, h);
        BufferedImage right = currentImage.getSubimage(w, 0, w, h);

        BufferedImage cl = left.getSubimage(c[2], c[0], w - c[2] - c[3], h - c[0] - c[1]);
        BufferedImage cr = right.getSubimage(c[2], c[0], w - c[2] - c[3], h - c[0] - c[1]);
        return buildCombinedImage(cl, cr);
    }

    // 上下左右対称クロップ
    public static BufferedImage mirrorCrop(BufferedImage currentImage, int[] c) {
        int w = currentImage.getWidth() / 2;
        int h = currentImage.getHeight();
        int targetW = w - c[2] - c[3];
        int targetH = h - c[0] - c[1];

        BufferedImage left = currentImage.getSubimage(0, 0, w, h);
        BufferedImage right = currentImage.getSubimage(w, 0, w, h);

        // 左画像：上(外)=c[0]、下(内)をカット / 左(外)=c[2]、右(内)をカット
        BufferedImage cl = left.getSubimage(c[2], c[0], targetW, targetH);
        // 右画像：上(内)=c[1]、下(外)をカット / 左(内)=c[3]、右(外)をカット
        BufferedImage cr = right.getSubimage(c[3], c[1], targetW, targetH);
        
        return buildCombinedImage(cl, cr);
    }

    // 左右の場所入れ替え
    public static BufferedImage swapOnly(BufferedImage currentImage) {
        int halfWidth = currentImage.getWidth() / 2;
        int height = currentImage.getHeight();

        BufferedImage currentLeft = currentImage.getSubimage(0, 0, halfWidth, height);
        BufferedImage currentRight = currentImage.getSubimage(halfWidth, 0, halfWidth, height);

        return buildCombinedImage(currentRight, currentLeft);
    }

    // 左右同時回転
    public static BufferedImage rotate(BufferedImage currentImage, boolean isRight) {
        int halfWidth = currentImage.getWidth() / 2;
        int height = currentImage.getHeight();

        BufferedImage currentLeft = currentImage.getSubimage(0, 0, halfWidth, height);
        BufferedImage currentRight = currentImage.getSubimage(halfWidth, 0, halfWidth, height);

        BufferedImage rotatedLeft = rotateImage(currentLeft, isRight);
        BufferedImage rotatedRight = rotateImage(currentRight, isRight);

        return buildCombinedImage(rotatedLeft, rotatedRight);
    }

    // 画像を90度回転させる補助メソッド
    private static BufferedImage rotateImage(BufferedImage src, boolean isRight) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage dst = new BufferedImage(h, w, src.getType());
        Graphics2D g = dst.createGraphics();
        
        if (isRight) {
            g.translate(h, 0);
            g.rotate(Math.toRadians(90));
        } else {
            g.translate(0, w);
            g.rotate(Math.toRadians(-90));
        }
        
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return dst;
    }

    // 左右のパーツ画像を1つの横長画像に結合する
    public static BufferedImage buildCombinedImage(BufferedImage leftImg, BufferedImage rightImg) {
        int w = leftImg.getWidth(); 
        int h = leftImg.getHeight();
        BufferedImage combined = new BufferedImage(w * 2, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();
        g.drawImage(leftImg, 0, 0, null);
        g.drawImage(rightImg, w, 0, null);
        g.dispose();
        return combined;
    }

    // 履歴保存用のディープコピー作成
    public static BufferedImage deepCopy(BufferedImage img) {
        BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
        Graphics2D g = copy.createGraphics();
        g.drawImage(img, 0, 0, null); 
        g.dispose();
        return copy;
    }
}
