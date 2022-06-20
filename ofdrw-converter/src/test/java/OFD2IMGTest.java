import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.ImageMaker;
import org.ofdrw.reader.DLOFDReader;
import org.ofdrw.reader.OFDReader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class OFD2IMGTest {

    @Test
    public void test() throws IOException {

        //为不规范的字体名创建映射
        FontLoader.getInstance()
                .addAliasMapping("小标宋体", "方正小标宋简体")
                .addAliasMapping("KaiTi_GB2312", "楷体")
                .addAliasMapping("楷体", "KaiTi")

                .addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体")
                .addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体")
                .addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun")
                .addSimilarFontReplaceRegexMapping(".*SimSun.*", "SimSun")
                .addSimilarFontReplaceRegexMapping(".*Song.*", "宋体")
                .addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun");

        FontLoader.getInstance().scanFontDir(new File("src/main/resources/fonts"));
        FontLoader.setSimilarFontReplace(true);
        long start = System.currentTimeMillis();

        toPng("src/test/resources/helloworld.ofd", "target/helloworld.ofd");
//        toPng("src/test/resources/999.ofd", "target/999.ofd");
//        toPng("src/test/resources/zsbk.ofd", "target/zsbk.ofd");
//        toPng("src/test/resources/ano.ofd", "target/ano.ofd");
//        toPng("src/test/resources/文字横向-数科.ofd", "target/文字横向-数科.ofd");
//        toPng("src/test/resources/z.ofd", "target/z.ofd");
//        toPng("src/test/resources/不规范资源路径.ofd", "target/不规范资源路径.ofd");
//        toPng("src/test/resources/发票示例.ofd", "target/发票示例.ofd");
        System.out.printf(">> 总计花费: %dms\n", System.currentTimeMillis() - start);
    }

    private static void toPng(String filename, String dirPath) throws IOException {
        Files.createDirectories(Paths.get(dirPath));
        Path src = Paths.get(filename);

        try (OFDReader reader = new OFDReader(src);) {
            ImageMaker imageMaker = new ImageMaker(reader, 15);
            imageMaker.config.setDrawBoundary(false);
            for (int i = 0; i < imageMaker.pageSize(); i++) {
                BufferedImage image = imageMaker.makePage(i);
                Path dist = Paths.get(dirPath, i + ".png");
                ImageIO.write(image, "PNG", dist.toFile());
            }
        }

    }

}
