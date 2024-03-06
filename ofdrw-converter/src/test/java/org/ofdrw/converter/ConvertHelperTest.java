package org.ofdrw.converter;

import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

class ConvertHelperTest {

	@Test
	void toItextPdf() throws Exception {
		ConvertHelper.useIText();
		Path ofdIn = Paths.get("src/test/resources/testImageNotFound.ofd");
		Path pdfOut = Paths.get("target/testImageNotFound.pdf");

		try {
			ConvertHelper.toPdf(ofdIn, pdfOut);
			assertTrue(true);
		} catch (Throwable e) {
			assertTrue(false, "转换失败");
		}
	}

}