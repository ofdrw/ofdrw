import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.GeneralConvertException;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OFD2PDFTest {

    @Test
    public void convertPdf() {
        Path src = Paths.get("src/test/resources/发票示例.ofd");
        Path dst = Paths.get("target/发票示例.pdf");
        try {
            ConvertHelper.toPdf(src, dst);
        } catch (GeneralConvertException e) {
            e.printStackTrace();
        }
    }

}