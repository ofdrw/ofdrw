import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.GeneralConvertException;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Hello World!
 *
 * @author 权观宇
 * @since 2020-11-20 19:03:12
 */
public class HelloWorld {

    public static void main(String[] args) {
        Path src = Paths.get("ofdrw-converter/src/test/resources/helloworld.ofd");
        Path dst = Paths.get("target/helloworld.pdf");
        try {
            ConvertHelper.toPdf(src, dst);
            System.out.println("生成文档位置: " + dst.toAbsolutePath());
        } catch (GeneralConvertException e) {
            // GeneralConvertException 类型错误表明转换过程中发生异常
            e.printStackTrace();
        }
    }
}
