package org.ofdrw.sign;

import org.apache.commons.io.FilenameUtils;
import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-18 11:10:43
 */
class OFDSignerTest {


    @Test
    void df(){
        System.out.println(OFDSigner.DF.format(LocalDateTime.now()));
    }

    @Test
    void fileWalk() throws IOException {
        Path path = Paths.get("src/test/resources", "helloworld.ofd");
        try (OFDReader reader = new OFDReader(path)) {
            OFDDir ofdDir = reader.getOFDDir();
            Path containerPath = ofdDir.getContainerPath();
            String cAbsP = FilenameUtils.separatorsToUnix(containerPath.toAbsolutePath().toString());
            List<ToDigestFileInfo> res = new LinkedList<>();
            Files.walkFileTree(containerPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    String p = FilenameUtils.separatorsToUnix(file.toAbsolutePath().toString());
                    p = p.replace(cAbsP, "");
                    res.add(new ToDigestFileInfo(p, file));
                    System.out.println(">> " + p);
                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}