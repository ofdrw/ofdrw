import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.GeneralConvertException;

import java.nio.file.Paths;

/**
 * @author yuanfang
 * @since 2021-6-7 9:45
 */
public class OFD2HTMLTest {


    @Test
    public void convertHtml() {

        FontLoader.getInstance()
            .addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
            .addAliasMapping(null, "KaiTi_GB2312", "楷体", "楷体")

            .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
            .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
            .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun")
            .addSimilarFontReplaceRegexMapping(null, ".*SimSun.*", null, "SimSun")
            .addSimilarFontReplaceRegexMapping(null, ".*Song.*", null, "宋体")
            .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun");

        FontLoader.enableSimilarFontReplace(true);

        try {
//            ConvertHelper.toPdf(src, dst);
//                        ConvertHelper.toHtml(Paths.get("src/test/resources/1.ofd"), Paths.get("target/signout.pdf"));
            ConvertHelper.toHtml(Paths.get("src/test/resources/signout.ofd"), Paths.get("target/signout.pdf"));
//            ConvertHelper.toHtml(Paths.get("src/test/resources/n.ofd"), Paths.get("target/n.pdf"));

        } catch (GeneralConvertException e) {
            e.printStackTrace();
        }
    }
}
