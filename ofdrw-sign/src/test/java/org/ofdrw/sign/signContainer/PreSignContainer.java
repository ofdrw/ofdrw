package org.ofdrw.sign.signContainer;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.ofdrw.core.signatures.SigType;
import org.ofdrw.gm.ses.v4.SESeal;
import org.ofdrw.sign.ExtendSignatureContainer;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;

public class PreSignContainer implements ExtendSignatureContainer {
    /**
     * 电子印章
     */
    private final SESeal seal;


    /**
     * PreSignContainer电子签章容器构造
     * @param seal 电子印章
     */
    public PreSignContainer(SESeal seal) {
        this.seal = seal;
    }

    @Override
    public MessageDigest getDigestFnc() {
        return new SM3.Digest();
    }

    @Override
    public ASN1ObjectIdentifier getSignAlgOID() {
        return GMObjectIdentifiers.sm2sign_with_sm3;
    }

    @Override
    public byte[] sign(InputStream inData, String propertyInfo) throws IOException, GeneralSecurityException {
        return IOUtils.toByteArray(inData);
    }

    @Override
    public byte[] getSeal() throws IOException {
        return seal.getEncoded("DER");
    }

    @Override
    public SigType getSignType() {
        return SigType.Seal;
    }
}
