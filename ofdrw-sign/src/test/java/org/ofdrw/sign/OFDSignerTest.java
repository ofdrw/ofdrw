package org.ofdrw.sign;

import org.apache.commons.io.FilenameUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.signContainer.GMDigestSignatureContainer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OFD签名引擎测试
 *
 * @author 权观宇
 * @since 2020-04-18 11:10:43
 */
class OFDSignerTest {



}