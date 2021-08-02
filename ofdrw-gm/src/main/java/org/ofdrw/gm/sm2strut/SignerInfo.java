package org.ofdrw.gm.sm2strut;

import org.bouncycastle.asn1.ASN1Object;
import org.bouncycastle.asn1.ASN1Primitive;

/**
 * GMT 35275-2017 signerInfo 类型
 *
 * @author 权观宇
 * @since 2021-08-02 20:10:24
 */
public class SignerInfo extends ASN1Object {

    /*
        SignerInfo ::= SEQUENCE {
            version Version,
            issuerAngSerialNumber IssuerAndSerialNumber,
            digestAlgorithm DigestAlgorithmIdentifier,
            authenticatedAttributes[0] IMPLICIT Attributes OPTIONAL,
            digestEncryptionAlgorithm DigestEncryptionAlgorithmIdentifier,
            encryptedDigest EncryptedDigest,
            unauthenticatedAttributes[1] IMPLICIT Attributes OPTIONAL
        }

        EncryptedDigest ::= OCTET STRING
     */


    @Override
    public ASN1Primitive toASN1Primitive() {
        return null;
    }
}
