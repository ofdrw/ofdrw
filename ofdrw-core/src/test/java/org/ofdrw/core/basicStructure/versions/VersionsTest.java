package org.ofdrw.core.basicStructure.versions;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import java.io.IOException;

public class VersionsTest {

    public static Versions versionsCase() {
        Versions versions = new Versions();
        Version version = new Version("1111", 1,
                new ST_Loc("./versions"));
        versions.addVersion(version)
                .addVersion(new Version("2222", 2,
                        new ST_Loc("./versions")));
        return versions;
    }

    @Test
    public void versionGen() throws IOException {
        TestTool.genXml("versions", document -> {
            Versions versions = versionsCase();
            document.add(versions);
        });

    }
}