package com.ranorat.app;

import java.awt.image.BufferedImage;

/**
 * 画像処理・変換コマンドの抽象化インターフェース
 */
@FunctionalInterface
public interface ImageTransform {
    /**
     * 入力画像に処理を適用して新しい画像を返します
     * @param source 処理対象の画像
     * @return 処理適用後の画像
     */
    BufferedImage apply(BufferedImage source);
}
