package org.ofdrw.gm.ses.parse;

/**
 * 电子签章版本号
 *
 * @author 权观宇
 * @since 2020-04-20 09:20:34
 */
public enum SESVersion {
    v1(1),
    v4(4);

    /**
     * 版本号
     */
    private int version;

    SESVersion(int version) {
        this.version = version;
    }

    public int getVersion() {
        return version;
    }
}
