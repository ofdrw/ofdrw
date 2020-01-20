package org.ofdrw.pkg.dir;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ResDirTest {

    @Test
    void collect() throws IOException {
        Path path = Paths.get("src/test/resources", "Font4.ttf");
        ResDir res = new ResDir();
        res.add(path);
        TT.dumpToTmpReview(res);
    }
}