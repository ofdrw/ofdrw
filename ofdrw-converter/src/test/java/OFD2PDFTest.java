import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;

import java.io.FileInputStream;
import java.util.Objects;

public class OFD2PDFTest {
    protected static final String basePath = Objects.requireNonNull(OFD2PDFTest.class.getClassLoader().getResource("")).getPath();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void convertPdf() {
        String ofdFilePath = basePath + "intro-数科.ofd";
        String pdfOutPath = basePath + "intro-from-ofd.pdf";

        try {
            ConvertHelper.toPdf(new FileInputStream(ofdFilePath), pdfOutPath);
        } catch (Exception e) {
            logger.error("test convert failed", e);
        }
    }

}