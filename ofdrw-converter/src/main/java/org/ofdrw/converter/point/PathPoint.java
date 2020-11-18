package org.ofdrw.converter.point;
/**
 * @author dltech21
 * @since 2020/8/11
 */
public class PathPoint {
    public String type;
    public float x1;
    public float y1;
    public float x2;
    public float y2;
    public float x3;
    public float y3;

    public float rx;
    public float ry;
    public float rotation;
    public float arc;
    public float sweep;
    public float x;
    public float y;

    public PathPoint(String type, float x1, float y1, float x2, float y2, float x3, float y3) {
        this.type = type;
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
    }

    public PathPoint(String type, float rx, float ry, float rotation, float arc, float sweep, float x, float y) {
        this.type = type;
        this.rx = rx;
        this.ry = ry;
        this.rotation = rotation;
        this.arc = arc;
        this.sweep = sweep;
        this.x = x;
        this.y = y;
    }

    public float getX1() {
        return x1;
    }

    public float getY1() {
        return y1;
    }

    public float getX2() {
        return x2;
    }

    public float getY2() {
        return y2;
    }

    public float getX3() {
        return x3;
    }

    public float getY3() {
        return y3;
    }


}
