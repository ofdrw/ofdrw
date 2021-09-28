package font;


import org.apache.fontbox.ttf.TTFParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 权观宇
 * @since 2021-09-28 20:57:57
 */
public class Main {

    public static void main(String[] args) throws IOException {
        Path fontPath = Paths.get("ofdrw-converter/src/test/resources/font_10.ttf");
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        final TrueTypeFont trueTypeFont = new TrueTypeFont();
        trueTypeFont.parse(dataStream);
        System.out.println(trueTypeFont);

    }
}
