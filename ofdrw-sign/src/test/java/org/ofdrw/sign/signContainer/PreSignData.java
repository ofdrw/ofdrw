package org.ofdrw.sign.signContainer;

public class PreSignData {
    private byte[] xmlBytes;
    private byte[] dataHash;

    public PreSignData(byte[] xmlBytes, byte[] dataHash) {
        this.xmlBytes = xmlBytes;
        this.dataHash = dataHash;
    }

    public byte[] getXmlBytes() {
        return xmlBytes;
    }

    public void setXmlBytes(byte[] xmlBytes) {
        this.xmlBytes = xmlBytes;
    }

    public byte[] getDataHash() {
        return dataHash;
    }

    public void setDataHash(byte[] dataHash) {
        this.dataHash = dataHash;
    }
}
