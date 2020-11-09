import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.jbig2.util.log.Logger;
import org.apache.pdfbox.jbig2.util.log.LoggerFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.CommonUtil;
import org.ofdrw.converter.PdfboxMaker;
import org.ofdrw.reader.DLOFDReader;
import org.ofdrw.reader.model.OfdPageVo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class OFD2PDFTest {
    protected static final String basePath = Objects.requireNonNull(OFD2PDFTest.class.getClassLoader().getResource("")).getPath();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    public void convertPdfbox() {
        String ofdFilePath = basePath + "999.ofd";
        String pdfOutPath = basePath + "999-from-ofd.pdf";

        try {
            byte[] ofdBytes = FileUtils.readFileToByteArray(new File(ofdFilePath));
            toPdfbox(ofdBytes, pdfOutPath);



        } catch (Exception e) {
            logger.error("test convert failed", e);
        }
    }

    private  void toPdfbox(byte[] ofdBytes, String desDir) {
        String tempFilePath = CommonUtil.generateTempFilePath();
        DLOFDReader reader = null;

        try {
            FileUtils.writeByteArrayToFile(new File(tempFilePath), ofdBytes);
            Path src = Paths.get(tempFilePath);

            reader = new DLOFDReader(src);

            List<OfdPageVo> ofdPageVoList = reader.getOFDDocumentVo().getOfdPageVoList();


            PDDocument pdfDocument = new PDDocument();

            long start;
            long end;
            int pageNum = 1;

            PdfboxMaker pdfMaker = new PdfboxMaker(reader, pdfDocument);
            for (OfdPageVo ofdPageVo : ofdPageVoList) {
                start = System.currentTimeMillis();
                PDPage pdPage = pdfMaker.makePage(ofdPageVo);
                end = System.currentTimeMillis();
                logger.info(String.format("page %d speed time %d", pageNum++, end - start));
            }
            pdfDocument.save(desDir);
            pdfDocument.close();
        } catch (Exception e) {
            logger.error("convert to pdf failed", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                logger.error("close OFDReader failed", e);
            }
        }
    }

}