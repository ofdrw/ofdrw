import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.GeneralConvertException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hello World!
 *
 * @author 权观宇
 * @since 2020-11-20 19:03:12
 */
public class HelloWorld {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorld.class);

    public static void main(String[] args) {

        FontLoader.getInstance()
            .addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
            .addAliasMapping(null, "KaiTi_GB2312", "楷体", "楷体")

            .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
            .addSimilarFontReplaceRegexMapping(null, ".*SimSun.*", null, "SimSun")
            .addSimilarFontReplaceRegexMapping(null, ".*Song.*", null, "宋体")
            .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun");

        FontLoader.setSimilarFontReplace(true);

        Path src = Paths.get("ofdrw-converter/src/test/resources/n.ofd");
        Path dst = Paths.get("target/n.pdf");
        try {
            ConvertHelper.toPdf(src, dst);
            logger.info("生成文档位置: " + dst.toAbsolutePath());
        } catch (GeneralConvertException e) {
            // GeneralConvertException 类型错误表明转换过程中发生异常
            e.printStackTrace();
        }
    }
}
