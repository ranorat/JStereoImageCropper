package com.ranorat.app;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 画像のUndo履歴と状態を管理するクラス
 */
public class HistoryManager {

    private final Deque<BufferedImage> undoStack = new ArrayDeque<>();
    private final Deque<BufferedImage> redoStack = new ArrayDeque<>(); // 追加
    private final int maxHistorySize;

    public HistoryManager() {
        this(20); // デフォルトで最大20件まで保持
    }

    public HistoryManager(int maxHistorySize) {
        this.maxHistorySize = maxHistorySize;
    }

    /**
     * 現在の画像状態を履歴スタックに保存します
     */
    public void push(BufferedImage image) {
        if (image == null) return;
        if (undoStack.size() >= maxHistorySize) {
            undoStack.removeLast(); // メモリ溢れ防止のため最古の履歴を破棄
        }
        undoStack.push(ImageProcessor.deepCopy(image));
        redoStack.clear(); // 新しい操作が行われたらRedoは無効化する
    }

    /**
     * 1つ前の状態を復元します
     */
    public BufferedImage undo(BufferedImage current) {
        if (canUndo()) {
            redoStack.push(ImageProcessor.deepCopy(current)); // 現在の状態をRedo用に保存
            return undoStack.pop();
        }
        return current;
    }

    public BufferedImage redo(BufferedImage current) {
        if (canRedo()) {
            undoStack.push(ImageProcessor.deepCopy(current)); // 現在の状態をUndo用に保存
            return redoStack.pop();
        }
        return current;
    }

    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    public void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public int size() {
        return undoStack.size();
    }
}
