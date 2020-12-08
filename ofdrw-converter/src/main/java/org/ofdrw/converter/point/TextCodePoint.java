package org.ofdrw.converter.point;
/**
 * @author dltech21
 * @since 2020/8/11
 */
public class TextCodePoint {
    public double x;
    public double y;
    private String text;
    private String glyph;

    public TextCodePoint(double x, double y, String text) {
        this.x = x;
        this.y = y;
        this.text = text;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getGlyph() {
        return glyph;
    }

    public void setGlyph(String glyph) {
        this.glyph = glyph;
    }
}
