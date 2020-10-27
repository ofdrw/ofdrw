package org.ofdrw.sign.stamppos;

/**
 * 两侧对开骑缝章时左右两边的比例
 *
 * @author DLTech21
 * @since 2020-10-26 11:38:50
 */
public class CuttingRatio {
    /**
     * 左的比例
     */
    private double left;
    /**
     * 右的比例
     */
    private double right;

    public double getLeft() {
        return left;
    }

    public void setLeft(double left) {
        this.left = left;
    }

    public double getRight() {
        return right;
    }

    public void setRight(double right) {
        this.right = right;
    }
}
