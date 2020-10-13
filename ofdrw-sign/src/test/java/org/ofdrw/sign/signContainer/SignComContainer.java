package org.ofdrw.sign.signContainer;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.sign.ExtendSignatureContainer;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

public class SignComContainer implements ExtendSignatureContainer {

    private byte[] xmlByte;
    private byte[] seal;
    private byte[] signedValue;

    public SignComContainer(byte[] xmlByte, byte[] seal, byte[] signedValue) {
        this.xmlByte = xmlByte;
        this.seal = seal;
        this.signedValue = signedValue;
    }

    @Override
    public MessageDigest getDigestFnc() {
        return null;
    }

    @Override
    public ASN1ObjectIdentifier getSignAlgOID() {
        return null;
    }

    @Override
    public byte[] sign(InputStream inData, String propertyInfo) throws IOException, GeneralSecurityException {
        return new byte[0];
    }

    @Override
    public byte[] getSeal() throws IOException {
        return seal;
    }

    public byte[] getXmlByte() {
        return xmlByte;
    }

    public byte[] getSignedValue() {
        return signedValue;
    }

    @Override
    public SigType getSignType() {
        return SigType.Seal;
    }
}
