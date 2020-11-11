package org.ofdrw.reader.model;
/**
 * @author dltech21
 * @since 2020/8/11
 */
public class SealDataVo {
    private byte[] sealBytes;

    private String sealType;

    public byte[] getSealBytes() {
        return sealBytes;
    }

    public void setSealBytes(byte[] sealBytes) {
        this.sealBytes = sealBytes;
    }

    public String getSealType() {
        return sealType;
    }

    public void setSealType(String sealType) {
        this.sealType = sealType;
    }
}
