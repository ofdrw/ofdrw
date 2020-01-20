package org.ofdrw.pkg.dir;

import org.ofdrw.core.signatures.sig.Signature;
import org.ofdrw.pkg.tool.DocObjDump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 签名资源容器
 *
 * @author 权观宇
 * @since 2020-01-18 03:43:52
 */
public class SignDir implements DirCollect {

    /**
     * 表示第几个签名
     */
    private Integer index = 0;

    /**
     * 签名/签章 描述文件
     */
    private Signature signature;

    /**
     * 电子印章文件
     * <p>
     * Seal.esl
     */
    private Path seal;

    /**
     * 签名值文件
     */
    private Path signedValue;

    public SignDir() {

    }

    /**
     * @return 第几个签名，从1开始
     */
    public Integer getIndex() {
        return index;
    }


    /**
     * 设置页码
     *
     * @param index 第几个签名
     */
    public SignDir setIndex(Integer index) {
        this.index = index;
        return this;
    }

    /**
     * @return 签名/签章 描述文件
     */
    public Signature getSignature() {
        return signature;
    }

    /**
     * 设置 签名/签章 描述文件
     *
     * @param signature 签名/签章 描述文件
     * @return this
     */
    public SignDir setSignature(Signature signature) {
        this.signature = signature;
        return this;
    }

    /**
     * @return 电子印章文件
     */
    public Path getSeal() {
        return seal;
    }

    /**
     * 设置电子印章文件
     *
     * @param seal 电子印章文件
     * @return this
     */
    public SignDir setSeal(Path seal) {
        this.seal = seal;
        return this;
    }

    /**
     * @return 签名值文件
     */
    public Path getSignedValue() {
        return signedValue;
    }

    /**
     * 设置签名值文件
     *
     * @param signedValue 签名值文件
     * @return this
     */
    public SignDir setSignedValue(Path signedValue) {
        this.signedValue = signedValue;
        return this;
    }

    /**
     * 创建目录并复制文件
     *
     * @param base 基础路径
     * @return 创建的目录路径
     * @throws IOException IO异常
     */
    @Override
    public Path collect(String base) throws IOException {
        Path path = Paths.get(base, "Sign_" + index);
        path = Files.createDirectories(path);
        String dir = path.toAbsolutePath().toString();
        if (signature == null) {
            throw new IllegalArgumentException("缺少签名/签章描述文件（signature）");
        }
        DocObjDump.dump(signature, Paths.get(dir, "Signature.xml"));
        if (seal != null) {
            Files.copy(seal, Paths.get(dir, "Seal.esl"));
        }
        if (signedValue != null) {
            Files.copy(signedValue, Paths.get(dir, "SignedValue.dat"));
        }
        return path;
    }
}
