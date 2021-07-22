package org.ofdrw.core.graph.pathObj;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Pos;

import java.util.LinkedList;
import java.util.List;

/**
 * 图形轮廓数据
 * <p>
 * 由一系列的紧缩的操作符和操作数构成
 * <p>
 * 9.1 表 35 36
 *
 * @author 权观宇
 * @since 2019-10-16 09:09:15
 */
public class AbbreviatedData extends OFDElement implements Cloneable {

    /**
     * 绘制数据队列
     */
    private LinkedList<OptVal> dataQueue;


    public AbbreviatedData(Element proxy) {
        super(proxy);
        dataQueue = parse(proxy.getText());
    }

    public AbbreviatedData() {
        super("AbbreviatedData");
        this.dataQueue = new LinkedList<>();
    }

    public AbbreviatedData(List<OptVal> list) {
        this();
        this.dataQueue = new LinkedList<>(list);
    }

    /**
     * 解析字符串构造数据队列
     * <p>
     * 数组中的两个元素间多个空格将会被理解成一个空格。
     * <p>
     * 无法解析转换的数字将会被当做0
     *
     * @param dataStr 紧缩字符串
     * @return 数据队列
     */
    public static LinkedList<OptVal> parse(String dataStr) {
        if (dataStr == null || dataStr.length() == 0) {
            return new LinkedList<>();
        }
        String[] arr = dataStr.split("\\s+");
        LinkedList<OptVal> res = new LinkedList<>();
        String opt = null;
        List<String> values = new LinkedList<>();
        for (String s : arr) {
            switch (s) {
                case "S":
                case "M":
                case "L":
                case "Q":
                case "B":
                case "A":
                case "C":
                    if (opt != null) {
                        res.add(new OptVal(opt, values));
                        values.clear();
                        opt = null;
                    }
                    opt = s;
                    break;
                default:
                    values.add(s);
            }
        }
        if (opt != null) {
            res.add(new OptVal(opt, values));
        }
        return res;
    }

    /**
     * 获取原始的操作符和操作数
     *
     * @return 操作符和操作数的结合
     */
    public LinkedList<OptVal> getRawOptVal() {
        return this.dataQueue;
    }

    /**
     * 刷新元素
     * <p>
     * 默认情况下，每次调用C都将会刷新元素内容
     *
     * @return this
     */
    public AbbreviatedData flush() {
        this.setText(this.toString());
        return this;
    }

    /**
     * 定义自绘制图形边线的起始点坐标 （x，y）
     *
     * @param x 目标点 x
     * @param y 目标点 y
     * @return this
     */
    public AbbreviatedData defineStart(double x, double y) {
//        dataQueue.add(new String[]{
//                "S", " ", STBase.fmt(x), " ", STBase.fmt(y)
//        });
        dataQueue.add(new OptVal("S", new double[]{x, y}));
        return this;
    }

    public AbbreviatedData s(ST_Pos start) {
        return defineStart(start.getX(), start.getY());
    }

    public AbbreviatedData S(double x, double y) {
        return defineStart(x, y);
    }

    /**
     * 当前点移动到制定点（x，y）
     *
     * @param x 目标点 x
     * @param y 目标点 y
     * @return this
     */
    public AbbreviatedData moveTo(double x, double y) {
//        dataQueue.add(new String[]{
//                "M", " ", STBase.fmt(x), " ", STBase.fmt(y)
//        });
        dataQueue.add(new OptVal("M", new double[]{x, y}));
        return this;
    }

    public AbbreviatedData M(double x, double y) {
        return moveTo(x, y);
    }

    public AbbreviatedData M(ST_Pos target) {
        return moveTo(target.getX(), target.getY());
    }

    /**
     * 从当前点连接一条指定点（x，y）的线段，并将当前点移动到制定点
     *
     * @param x 目标点 x
     * @param y 目标点 y
     * @return this
     */
    public AbbreviatedData lineTo(double x, double y) {
//        dataQueue.add(new String[]{
//                "L", " ", STBase.fmt(x), " ", STBase.fmt(y)
//        });
        dataQueue.add(new OptVal("L", new double[]{x, y}));
        return this;
    }

    public AbbreviatedData L(double x, double y) {
        return lineTo(x, y);
    }

    public AbbreviatedData L(ST_Pos p) {
        return lineTo(p.getX(), p.getY());
    }

    /**
     * 从当前点连接一条到点（x2，y2）的二次贝塞尔曲线，
     * 并将当前点移动到点（x2，y2），此贝塞尔曲线使用
     * 点（x1，y1）作为其控制点
     *
     * @param x1 控制点 x
     * @param y1 控制点 y
     * @param x2 目标点 x
     * @param y2 目标点 y
     * @return this
     */
    public AbbreviatedData quadraticBezier(double x1, double y1,
                                           double x2, double y2) {
//        dataQueue.add(new String[]{
//                "Q", " ", STBase.fmt(x1), " ", STBase.fmt(y1),
//                " ", STBase.fmt(x2), " ", STBase.fmt(y2)
//        });
        dataQueue.add(new OptVal("Q", new double[]{x1, y1, x2, y2}));
        return this;
    }

    public AbbreviatedData Q(double x1, double y1,
                             double x2, double y2) {
        return quadraticBezier(x1, y1, x2, y2);
    }

    public AbbreviatedData Q(ST_Pos control,
                             ST_Pos target) {
        return quadraticBezier(
                control.getX(), control.getY(),
                target.getX(), target.getY());
    }

    /**
     * 从当前点连接一条到点（x3，y3）的三次贝塞尔曲线，
     * 并将当前点移动到点（x3，y3），此贝塞尔曲线使用点
     * （x1，y1）和点（x2，y2）作为控制点
     *
     * @param x1 控制点 x1
     * @param y1 控制点 y1
     * @param x2 控制点 x2
     * @param y2 控制点 y2
     * @param x3 目标点 x3
     * @param y3 目标点 y3
     * @return this
     */
    public AbbreviatedData cubicBezier(double x1, double y1,
                                       double x2, double y2,
                                       double x3, double y3) {
//        dataQueue.add(new String[]{
//                "B", " ", STBase.fmt(x1), " ", STBase.fmt(y1),
//                " ", STBase.fmt(x2), " ", STBase.fmt(y2),
//                " ", STBase.fmt(x3), " ", STBase.fmt(y3)
//        });
        dataQueue.add(new OptVal("B", new double[]{x1, y1, x2, y2, x3, y3}));
        return this;
    }

    public AbbreviatedData B(double x1, double y1,
                             double x2, double y2,
                             double x3, double y3) {
        return cubicBezier(x1, y1,
                x2, y2,
                x3, y3);
    }

    public AbbreviatedData B(ST_Pos control1,
                             ST_Pos control2,
                             ST_Pos target) {
        return cubicBezier(
                control1.getX(), control1.getY(),
                control2.getX(), control2.getY(),
                target.getX(), target.getY());
    }


    /**
     * 从当前点连接到点（x，y）的圆弧，并将当前点移动到点（x，y）。
     * rx 表示椭圆的长轴长度，ry 表示椭圆的短轴长度。angle 表示
     * 椭圆在当前坐标系下旋转的角度，正值为顺时针，负值为逆时针，
     * large 为 1 时表示对应度数大于180°的弧，为 0 时表示对应
     * 度数小于 180°的弧。sweep 为 1 时表示由圆弧起始点到结束点
     * 是顺时针旋转，为 0 时表示由圆弧起始点到结束点是逆时针旋转。
     *
     * @param rx    椭圆长轴长度
     * @param ry    椭圆短轴长度
     * @param angle 旋转角度，正值顺时针，负值逆时针
     * @param large 1 时表示对应度数大于 180°的弧，0 时表示对应度数小于 180°的弧
     * @param sweep sweep 为 1 时表示由圆弧起始点到结束点是顺时针旋转，为 0 时表示由圆弧起始点到结束点是逆时针旋转。
     * @param x     目标点 x
     * @param y     目标点 y
     * @return this
     */
    public AbbreviatedData arc(double rx, double ry,
                               double angle, int large,
                               int sweep, double x, double y) {
        if (large != 0 && large != 1) {
            throw new NumberFormatException("large 只接受 0 或 1");
        }
        if (sweep != 0 && sweep != 1) {
            throw new NumberFormatException("sweep 只接受 0 或 1");
        }
//        dataQueue.add(new String[]{
//                "A", " ", STBase.fmt(rx), " ", STBase.fmt(ry),
//                " ", STBase.fmt(angle), " ", STBase.fmt(large),
//                " ", STBase.fmt(sweep), " ", STBase.fmt(x), " ", STBase.fmt(y)
//        });
        dataQueue.add(new OptVal("A", new double[]{
                rx, ry, angle, large,
                sweep, x, y
        }));
        return this;
    }

    public AbbreviatedData A(double rx, double ry,
                             double angle, int large,
                             int sweep, double x, double y) {
        return arc(rx, ry,
                angle, large,
                sweep, x, y);
    }

    public AbbreviatedData A(double rx, double ry,
                             double angle, int large,
                             int sweep, ST_Pos target) {
        return arc(rx, ry,
                angle, large,
                sweep, target.getX(), target.getY());
    }

    /**
     * SubPath 自动闭合，表示将当前点和 SubPath 的起始点用线段直连连接
     *
     * @return this
     */
    public AbbreviatedData close() {
//        dataQueue.add(new String[]{"C"});
        dataQueue.add(new OptVal("C"));
        return this;
    }

    public AbbreviatedData C() {
        return close();
    }

    /**
     * 撤销上一步操作
     *
     * @return this
     */
    public AbbreviatedData undo() {
        this.dataQueue.removeLast();
        return this;
    }

    /**
     * 序列化为操作序列
     *
     * @return 操作序列
     */
    @Override
    public String toString() {
        if (dataQueue.size() == 0) {
            throw new IllegalArgumentException("AbbreviatedData 不能为空");
        }
        StringBuilder dataBuilder = new StringBuilder();
        int cnt = 0;
        for (OptVal operatorItem : dataQueue) {
            cnt++;
            if (cnt != 1) {
                // 每个操作符之间使用空格间隔
                dataBuilder.append(" ");
            }
            dataBuilder.append(operatorItem.toString());
        }
        return dataBuilder.toString();
    }

    /**
     * 复制路径对象
     *
     * @return 复制之后的路径对象
     */
    @Override
    public AbbreviatedData clone() {
        AbbreviatedData clone = new AbbreviatedData();
        clone.dataQueue = new LinkedList<>();
        for (OptVal item : dataQueue) {
            clone.dataQueue.add(item.clone());
        }
        clone.flush();
        return clone;
    }
}
