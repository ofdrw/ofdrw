import org.junit.jupiter.api.Test;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.SVGMaker;
import org.ofdrw.reader.OFDReader;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class OFD2SVGTest {

    @Test
    public void test() throws IOException {

//        //为不规范的字体名创建映射
//        FontLoader.getInstance()
//                .addAliasMapping(null, "仿宋简体", null, "方正仿宋简体")
//                .addAliasMapping(null, "仿宋", null, "方正仿宋简体")
//                .addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
//                .addAliasMapping(null, "KaiTi_GB2312", "楷体", "楷体");

        FontLoader.getInstance()
            .addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
            .addAliasMapping(null, "KaiTi_GB2312", "楷体", "楷体")

            .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
            .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
            .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun")
            .addSimilarFontReplaceRegexMapping(null, ".*SimSun.*", null, "SimSun")
            .addSimilarFontReplaceRegexMapping(null, ".*Song.*", null, "宋体")
            .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun");

        FontLoader.setSimilarFontReplace(true);


        long start = System.currentTimeMillis();
//        toSVG("src/test/resources/999.ofd", "target/999.ofd");
//        toSVG("src/test/resources/zsbk.ofd", "target/zsbk.ofd");
//        toSVG("src/test/resources/ano.ofd", "target/ano.ofd");
//        toSVG("src/test/resources/文字横向-数科.ofd", "target/文字横向-数科.ofd");
//        toSVG("src/test/resources/z.ofd", "target/z.ofd");
//        toSVG("src/test/resources/发票示例.ofd", "target/发票示例.ofd");
        toSVG("src/test/resources/signout.ofd", "target/signout.ofd");
        toSVG("src/test/resources/n.ofd", "target/n.ofd");

//        toPng("src/test/resources/不规范资源路径.ofd", "target/不规范资源路径.ofd");
        System.out.printf(">> 总计花费: %dms\n", System.currentTimeMillis() - start);
    }

    private static void toSVG(String filename, String dirPath) throws IOException {
        Files.createDirectories(Paths.get(dirPath));
        Path src = Paths.get(filename);
        try(OFDReader reader = new OFDReader(src)){
            SVGMaker svgMaker = new SVGMaker(reader, 5d);
            svgMaker.config.setDrawBoundary(false);
            svgMaker.config.setClip(false);
            for (int i = 0; i < svgMaker.pageSize(); i++) {
                String svg = svgMaker.makePage(i);
                Path dist = Paths.get(dirPath, i + ".svg");
                Files.write(dist, svg.getBytes());
            }
        }

    }

}
