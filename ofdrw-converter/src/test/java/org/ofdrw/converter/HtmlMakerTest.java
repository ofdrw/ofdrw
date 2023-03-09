package org.ofdrw.converter;

import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class HtmlMakerTest {

    @Test
    void makePage() throws Exception{
        Path ofdIn = Paths.get("src/test/resources/999.ofd");
        Path htmlOut = Paths.get("target/999.html");

        try (OFDReader reader = new OFDReader(ofdIn)) {
            HtmlMaker htmlMaker = new HtmlMaker(reader,1000);
            SVGMaker svgMaker = new SVGMaker(reader, 0);
            String div = htmlMaker.makePageDiv(svgMaker, 0);
            Files.write(htmlOut, div.getBytes());
        }

    }

    @Test
    void parse() throws Exception{
        Path ofdIn = Paths.get("src/test/resources/999.ofd");
        Path htmlOut = Paths.get("target/999.html");

        try (OFDReader reader = new OFDReader(ofdIn)) {
            HtmlMaker htmlMaker = new HtmlMaker(reader,htmlOut,1000);
            htmlMaker.parse();
        }

    }
}