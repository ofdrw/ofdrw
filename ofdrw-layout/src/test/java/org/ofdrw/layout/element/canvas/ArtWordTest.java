package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.OFDDoc;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.Position;
import org.ofdrw.layout.element.TextAlign;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 韩兴元
 * @date 2024/7/24
 */
public class ArtWordTest {
    @Test
    public void testArtWord() throws IOException {
        Path outPath = Paths.get("target/testArtWord.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(outPath)) {
            VirtualPage virtualPage = new VirtualPage(ofdDoc.getPageLayout());

            ArtWord artWord = new ArtWord(200D, 50D);
            artWord.setPosition(Position.Absolute);
            artWord.setXY(0D, 0D);
            artWord.setHorizontalInclination(0.5);
            artWord.setText("这是一个测试抬头");
            artWord.setFontSize(4.9378);
            artWord.setBorder(1D);
            virtualPage.add(artWord);


            ArtWord artWord2 = new ArtWord(200D, 50D);
            artWord2.setPosition(Position.Absolute);
            artWord2.setXY(0D, 51D);
            artWord2.setText("这是一个测试抬头");
            artWord2.setFontSize(4.9378);
            artWord2.setBorder(1D);
            artWord2.setColor("#FF0000");
            artWord2.setTextLayoutAlign(TextAlign.center);
            artWord2.setUnderline(true, 5D, 1D);
            artWord2.setLetterSpacing(4.9378);
            virtualPage.add(artWord2);
            ofdDoc.addVPage(virtualPage);
        }
    }
}
