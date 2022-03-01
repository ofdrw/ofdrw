import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OFD转换HTML
 *
 * @author yuanfang
 * @since 2021-6-7 9:45
 */
public class OFD2HTMLTest {

    @Test
    public void convertHtml() {
        try {
            // 1. 提供文档
//            Path ofdIn = Paths.get("src/test/resources/n.ofd");
//            Path htmlOut = Paths.get("target/n.html");
            //发票示例.ofd
            Path ofdIn = Paths.get("src/test/resources/发票示例.ofd");
            Path htmlOut = Paths.get("target/发票示例.ofd/发票示例.html");
            // 2. [可选]配置字体，别名，扫描目录等
            // FontLoader.getInstance().addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
            // FontLoader.getInstance().scanFontDir(new File("src/test/resources/fonts"));
            // 3. 配置参数（HTML页面宽度(px)），转换并存储HTML到文件。
            ConvertHelper.toHtml(ofdIn, htmlOut, 1000);
        } catch (GeneralConvertException | IOException e) {
            e.printStackTrace();
        }
    }
}
