package org.ofdrw.sign.signContainer;

public class PreSignData {
    private byte[] xmlBytes;
    private byte[] dataHash;
    private String propertyInfo;
    private byte[] seal;

    public PreSignData(byte[] xmlBytes, byte[] dataHash, String propertyInfo, byte[] seal) {
        this.xmlBytes = xmlBytes;
        this.dataHash = dataHash;
        this.propertyInfo = propertyInfo;
        this.seal = seal;
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

    public String getPropertyInfo() {
        return propertyInfo;
    }

    public void setPropertyInfo(String propertyInfo) {
        this.propertyInfo = propertyInfo;
    }

    public byte[] getSeal() {
        return seal;
    }

    public void setSeal(byte[] seal) {
        this.seal = seal;
    }
}
