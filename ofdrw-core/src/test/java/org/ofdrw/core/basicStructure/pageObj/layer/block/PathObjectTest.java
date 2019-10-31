package org.ofdrw.core.basicStructure.pageObj.layer.block;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.axialShd.CT_AxialShdTest;

/**
 * 路径对象测试用例
 */
public class PathObjectTest {

    private static AbbreviatedData DATA = new AbbreviatedData()
            .M(0, 0)
            .L(140, 0)
            .L(140, 40)
            .L(0, 40)
            .C();

    /**
     * 参考自 8.3.4.2 轴向渐变
     * <p>
     * 轴向渐变的 MapType 示例
     *
     * @return 用例
     */
    public static PathObject pathObjectCase() {


        return (PathObject) new PathObject(ST_ID.getInstance("10005"))
                .setBoundary(new ST_Box(10, 10, 140, 40))
                .setFill(true)
                .setFillColor(new CT_Color(CT_AxialShdTest.axialShdCase()))
                .setAbbreviatedData(DATA);
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("PathObject", pathObjectCase());
    }


    @Test
    public void genReflect() throws Exception {
        PathObject pathObjectReflect = (PathObject) pathObjectCase()
                .setID(ST_ID.getInstance("10007"))
                .setBoundary(new ST_Box(10, 110, 140, 40))
                .setFillColor(new CT_Color(CT_AxialShdTest.axialShdReflectCase()));
        TestTool.genXml("PathObjectReflect", pathObjectReflect);
    }

    @Test
    public void genRepeat() throws Exception {
        PathObject pathObjectRepeat = (PathObject) pathObjectCase()
                .setID(ST_ID.getInstance("10006"))
                .setBoundary(new ST_Box(10, 60, 140, 40))
                .setFillColor(new CT_Color(CT_AxialShdTest.axialShdRepeatCase()));
        TestTool.genXml("PathObjectRepeat", pathObjectRepeat);
    }
}